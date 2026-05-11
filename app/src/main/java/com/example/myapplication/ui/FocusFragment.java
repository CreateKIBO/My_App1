package com.example.myapplication.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ForgettingCurveItemEntity;
import com.example.myapplication.data.local.ReviewTaskEntity;
import com.example.myapplication.databinding.FragmentFocusBinding;
import com.example.myapplication.ui.adapter.CurveItemAdapter;
import com.example.myapplication.ui.adapter.ReviewTaskAdapter;
import com.example.myapplication.util.AnimUtils;
import com.example.myapplication.util.ThemeManager;

import java.util.Locale;

public class FocusFragment extends Fragment {

    private FragmentFocusBinding binding;
    private FocusViewModel viewModel;

    private CurveItemAdapter curveItemAdapter;
    private ReviewTaskAdapter reviewTaskAdapter;

    private boolean isPomodoroTab = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFocusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(FocusViewModel.class);

        setupTabSwitch();
        setupPomodoro();
        setupForgettingCurve();
        setupObservers();
        applyThemeColors();
    }

    private void setupTabSwitch() {
        AnimUtils.scaleOnClick(binding.btnTabPomodoro);
        AnimUtils.scaleOnClick(binding.btnTabCurve);

        binding.btnTabPomodoro.setOnClickListener(v -> {
            if (isPomodoroTab) return;
            isPomodoroTab = true;
            updateTabUI();
        });

        binding.btnTabCurve.setOnClickListener(v -> {
            if (!isPomodoroTab) return;
            isPomodoroTab = false;
            updateTabUI();
        });
    }

    private void updateTabUI() {
        if (isPomodoroTab) {
            binding.btnTabPomodoro.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnTabPomodoro.setBackgroundResource(R.drawable.bg_tab_selected);
            binding.btnTabCurve.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant));
            binding.btnTabCurve.setBackgroundResource(R.drawable.bg_tab_unselected);
            binding.layoutPomodoro.setVisibility(View.VISIBLE);
            binding.layoutCurve.setVisibility(View.GONE);
        } else {
            binding.btnTabCurve.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.btnTabCurve.setBackgroundResource(R.drawable.bg_tab_selected);
            binding.btnTabPomodoro.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant));
            binding.btnTabPomodoro.setBackgroundResource(R.drawable.bg_tab_unselected);
            binding.layoutPomodoro.setVisibility(View.GONE);
            binding.layoutCurve.setVisibility(View.VISIBLE);
        }
    }

    private void setupPomodoro() {
        // Duration selector
        TextView[] durationBtns = {
                binding.btnDuration15, binding.btnDuration25,
                binding.btnDuration45, binding.btnDuration60
        };
        int[] durations = {15, 25, 45, 60};

        for (int i = 0; i < durationBtns.length; i++) {
            AnimUtils.scaleOnClick(durationBtns[i]);
            final int idx = i;
            durationBtns[i].setOnClickListener(v -> {
                viewModel.setWorkDurationMinutes(durations[idx]);
                updateDurationSelection(idx);
                if (viewModel.getTimerState().getValue() == FocusViewModel.TimerState.IDLE) {
                    updateTimerDisplay(durations[idx] * 60L);
                }
            });
        }

        // Start/Pause button
        AnimUtils.scaleOnClick(binding.btnStartPause);
        binding.btnStartPause.setOnClickListener(v -> {
            FocusViewModel.TimerState state = viewModel.getTimerState().getValue();
            if (state == null) state = FocusViewModel.TimerState.IDLE;

            if (state == FocusViewModel.TimerState.RUNNING) {
                viewModel.pauseTimer();
            } else {
                EditText etTask = binding.etTaskName;
                viewModel.setCurrentTaskTitle(etTask.getText().toString().trim());
                viewModel.startTimer();
            }
        });

        // Reset button
        AnimUtils.scaleOnClick(binding.btnReset);
        binding.btnReset.setOnClickListener(v -> viewModel.resetTimer());

        // Skip button
        AnimUtils.scaleOnClick(binding.btnSkip);
        binding.btnSkip.setOnClickListener(v -> viewModel.skipToBreak());

        // Initial display
        updateTimerDisplay(viewModel.getWorkDurationMinutes() * 60L);
        updateDurationSelection(1); // default 25min
    }

    private void updateDurationSelection(int selectedIdx) {
        TextView[] durationBtns = {
                binding.btnDuration15, binding.btnDuration25,
                binding.btnDuration45, binding.btnDuration60
        };
        for (int i = 0; i < durationBtns.length; i++) {
            if (i == selectedIdx) {
                durationBtns[i].setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                durationBtns[i].setBackgroundResource(R.drawable.bg_pill_selected);
            } else {
                durationBtns[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface_variant));
                durationBtns[i].setBackgroundResource(R.drawable.bg_pill_unselected);
            }
        }
    }

    private void updateTimerDisplay(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        binding.tvTimerDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
    }

    private void setupForgettingCurve() {
        reviewTaskAdapter = new ReviewTaskAdapter(item -> viewModel.completeReview(item.getCurveItemId()));
        binding.rvTodayReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTodayReviews.setAdapter(reviewTaskAdapter);

        curveItemAdapter = new CurveItemAdapter();
        binding.rvCurveItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCurveItems.setAdapter(curveItemAdapter);
    }

    private void setupObservers() {
        viewModel.getTimerState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            switch (state) {
                case IDLE:
                    binding.btnStartPause.setText("开始专注");
                    binding.btnReset.setVisibility(View.GONE);
                    binding.btnSkip.setVisibility(View.GONE);
                    binding.tvTimerLabel.setText("专注时间");
                    binding.etTaskName.setEnabled(true);
                    updateTimerDisplay(viewModel.getWorkDurationMinutes() * 60L);
                    binding.timerRing.setProgress(0f);
                    break;
                case RUNNING:
                    binding.btnStartPause.setText("暂停");
                    binding.btnReset.setVisibility(View.VISIBLE);
                    binding.btnSkip.setVisibility(View.VISIBLE);
                    binding.tvTimerLabel.setText("专注中...");
                    binding.etTaskName.setEnabled(false);
                    break;
                case PAUSED:
                    binding.btnStartPause.setText("继续");
                    binding.tvTimerLabel.setText("已暂停");
                    break;
                case BREAK:
                    binding.btnStartPause.setText("休息中");
                    binding.btnStartPause.setEnabled(false);
                    binding.btnReset.setVisibility(View.VISIBLE);
                    binding.btnSkip.setVisibility(View.GONE);
                    binding.tvTimerLabel.setText("休息一下 ☕");
                    break;
            }
        });

        viewModel.getRemainingSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds == null) return;
            updateTimerDisplay(seconds);

            Integer total = viewModel.getTotalSeconds().getValue();
            if (total != null && total > 0) {
                float progress = 1f - (seconds / (float) total);
                binding.timerRing.setProgress(progress);
            }
        });

        viewModel.getTotalSeconds().observe(getViewLifecycleOwner(), total -> {
            if (total != null && total > 0) {
                Long remaining = viewModel.getRemainingSeconds().getValue();
                if (remaining != null) {
                    float progress = 1f - (remaining / (float) total);
                    binding.timerRing.setProgress(progress);
                }
            }
        });

        viewModel.getCompletedTodayCount().observe(getViewLifecycleOwner(), count -> {
            binding.tvTodayCount.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getTodayFocusMinutes().observe(getViewLifecycleOwner(), minutes -> {
            binding.tvTodayMinutes.setText(String.valueOf(minutes != null ? minutes : 0));
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Forgetting curve observers
        viewModel.getTodayReviews().observe(getViewLifecycleOwner(), reviews -> {
            boolean empty = reviews == null || reviews.isEmpty();
            binding.rvTodayReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.tvNoReviews.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) reviewTaskAdapter.submitList(reviews);
        });

        viewModel.getActiveItems().observe(getViewLifecycleOwner(), items -> {
            boolean empty = items == null || items.isEmpty();
            binding.rvCurveItems.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.tvNoItems.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) curveItemAdapter.submitList(items);
        });

        viewModel.getActiveCount().observe(getViewLifecycleOwner(), count ->
                binding.tvActiveCount.setText(String.valueOf(count != null ? count : 0)));

        viewModel.getDueTodayCount().observe(getViewLifecycleOwner(), count ->
                binding.tvDueTodayCount.setText(String.valueOf(count != null ? count : 0)));

        viewModel.getMasteredCount().observe(getViewLifecycleOwner(), count ->
                binding.tvMasteredCount.setText(String.valueOf(count != null ? count : 0)));
    }

    private void applyThemeColors() {
        if (getContext() == null) return;
        int primary = ThemeManager.getThemePrimaryInt(requireContext());

        // Tab selected background
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setShape(GradientDrawable.RECTANGLE);
        selectedBg.setCornerRadius(dpToPx(20));
        selectedBg.setColor(primary);
        binding.btnTabPomodoro.setBackground(selectedBg);

        // Timer ring
        binding.timerRing.setProgress(binding.timerRing.getProgress());

        // Start button
        binding.btnStartPause.setBackgroundColor(primary);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
