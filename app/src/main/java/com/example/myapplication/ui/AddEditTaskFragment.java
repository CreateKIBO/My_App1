package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentAddEditTaskBinding;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.RewardCalculator;

import java.util.Calendar;

public class AddEditTaskFragment extends Fragment {

    private FragmentAddEditTaskBinding binding;
    private AddEditTaskViewModel viewModel;
    private String selectedCategory = RewardCalculator.CAT_WORK;
    private long taskId = -1;
    private String selectedDate;
    private int startHour = 9, startMinute = 0;
    private int endHour = 10, endMinute = 0;

    private LinearLayout[] pillViews;
    private final String[] categories = {
            RewardCalculator.CAT_WORK, RewardCalculator.CAT_STUDY,
            RewardCalculator.CAT_EXERCISE, RewardCalculator.CAT_PERSONAL
    };
    private final String[] categoryLabels = {"工作", "学习", "运动", "个人"};
    private final String[] categoryEmojis = {"💼", "📚", "🏃", "🏠"};
    private final int[] categoryColors = {0xFF3B82F6, 0xFF7C3AED, 0xFF16A34A, 0xFFD97706};
    private final int[] categoryLightBgs = {0xFFEFF6FF, 0xFFF5F3FF, 0xFFF0FDF4, 0xFFFFFBEB};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddEditTaskViewModel.class);

        if (getArguments() != null) {
            taskId = getArguments().getLong("taskId", -1);
            String dateArg = getArguments().getString("selectedDate");
            if (dateArg != null) selectedDate = dateArg;
        }

        pillViews = new LinearLayout[]{binding.pillWork, binding.pillStudy, binding.pillExercise, binding.pillPersonal};

        // Back button
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Done button
        binding.btnDone.setOnClickListener(v -> saveTask());

        // Category pills
        for (int i = 0; i < pillViews.length; i++) {
            final int index = i;
            pillViews[i].setOnClickListener(v -> {
                selectedCategory = categories[index];
                updateCategoryPills();
                updateRewardPreview();
            });
        }

        // Title text watcher for submit state
        binding.etTaskTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateSubmitState();
            }
        });

        // Time pickers
        binding.etStartTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
                startHour = hour;
                startMinute = minute;
                binding.etStartTime.setText(DateUtils.minutesToTime(hour * 60 + minute));
            }, startHour, startMinute, true).show();
        });

        binding.etEndTime.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
                endHour = hour;
                endMinute = minute;
                binding.etEndTime.setText(DateUtils.minutesToTime(hour * 60 + minute));
            }, endHour, endMinute, true).show();
        });

        // Date
        if (selectedDate == null) selectedDate = DateUtils.getTodayString();
        binding.etDate.setText(DateUtils.getDisplayDate(selectedDate));
        binding.etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DateUtils.DATE_FORMAT_STR, java.util.Locale.getDefault());
                cal.setTime(sdf.parse(selectedDate));
            } catch (Exception ignored) {}
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                selectedDate = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                binding.etDate.setText(DateUtils.getDisplayDate(selectedDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Edit mode
        if (taskId != -1) {
            viewModel.loadTask(taskId);
            viewModel.getTaskLiveData().observe(getViewLifecycleOwner(), task -> {
                if (task == null) return;
                binding.etTaskTitle.setText(task.getTitle());
                selectedDate = task.getDate();
                binding.etDate.setText(DateUtils.getDisplayDate(selectedDate));
                startHour = task.getStartTime() / 60;
                startMinute = task.getStartTime() % 60;
                endHour = task.getEndTime() / 60;
                endMinute = task.getEndTime() % 60;
                binding.etStartTime.setText(DateUtils.minutesToTime(task.getStartTime()));
                binding.etEndTime.setText(DateUtils.minutesToTime(task.getEndTime()));
                selectedCategory = task.getCategory();
                updateCategoryPills();
                updateRewardPreview();
            });
        } else {
            binding.etStartTime.setText(DateUtils.minutesToTime(startHour * 60 + startMinute));
            binding.etEndTime.setText(DateUtils.minutesToTime(endHour * 60 + endMinute));
        }

        // Submit button
        binding.btnSubmit.setOnClickListener(v -> saveTask());

        // Continue adding button
        binding.btnContinueAdd.setOnClickListener(v -> {
            binding.layoutSuccessOverlay.setVisibility(View.GONE);
            resetForm();
        });

        // Done adding button — navigate back to home
        binding.btnDoneAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        updateCategoryPills();
        updateRewardPreview();
        updateSubmitState();
    }

    private void updateCategoryPills() {
        for (int i = 0; i < pillViews.length; i++) {
            boolean isSelected = categories[i].equals(selectedCategory);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(12f);
            if (isSelected) {
                bg.setColor(categoryLightBgs[i]);
                bg.setStroke(2, categoryColors[i]);
            } else {
                bg.setColor(0xFFFFFFFF);
                bg.setStroke(2, 0xFFE5E7EB);
            }
            pillViews[i].setBackground(bg);

            // Update text color
            int childCount = pillViews[i].getChildCount();
            for (int c = 0; c < childCount; c++) {
                View child = pillViews[i].getChildAt(c);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(isSelected ? categoryColors[i] : 0xFF111827);
                }
            }
        }
    }

    private void updateRewardPreview() {
        int catIndex = 0;
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(selectedCategory)) { catIndex = i; break; }
        }

        RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(selectedCategory);

        // Update icon background
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(categoryLightBgs[catIndex]);
        binding.vRewardBg.setBackground(iconBg);
        binding.tvRewardEmoji.setText(categoryEmojis[catIndex]);
        binding.tvRewardName.setText(categoryLabels[catIndex] + "任务奖励");
        binding.tvRewardXp.setText("⚡ +" + reward.xp + " XP");
        binding.tvRewardCoins.setText("¢ +" + reward.coins);
    }

    private void updateSubmitState() {
        String title = binding.etTaskTitle.getText() != null ? binding.etTaskTitle.getText().toString().trim() : "";
        boolean canSubmit = !title.isEmpty();
        binding.btnSubmit.setEnabled(canSubmit);
        binding.btnDone.setAlpha(canSubmit ? 1.0f : 0.3f);
        binding.btnDone.setClickable(canSubmit);
    }

    private void saveTask() {
        String title = binding.etTaskTitle.getText() != null ? binding.etTaskTitle.getText().toString().trim() : "";
        if (title.isEmpty()) return;

        int startTime = DateUtils.timeToMinutes(startHour, startMinute);
        int endTime = DateUtils.timeToMinutes(endHour, endMinute);

        viewModel.saveTask(title, "", selectedDate, startTime, endTime, selectedCategory, taskId);

        if (taskId != -1) {
            Navigation.findNavController(requireView()).navigateUp();
        } else {
            // New task added — if it's for today, notify HomeViewModel to check streak
            if (DateUtils.getTodayString().equals(selectedDate)) {
                HomeViewModel homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
                homeViewModel.onTaskAddedToday();
            }
            showSuccessOverlay(title);
        }
    }

    private void showSuccessOverlay(String taskName) {
        RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(selectedCategory);
        binding.tvSuccessDesc.setText("「" + taskName + "」已添加到 " + DateUtils.getDisplayDate(selectedDate) + " 的日程中");
        binding.tvSuccessXp.setText("⚡ +" + reward.xp + " XP");
        binding.tvSuccessCoins.setText("¢ +" + reward.coins);
        binding.layoutSuccessOverlay.setVisibility(View.VISIBLE);
    }

    private void resetForm() {
        taskId = -1;
        selectedCategory = RewardCalculator.CAT_WORK;
        startHour = 9; startMinute = 0;
        endHour = 10; endMinute = 0;
        selectedDate = DateUtils.getTodayString();
        binding.etTaskTitle.setText("");
        binding.etDate.setText(DateUtils.getDisplayDate(selectedDate));
        binding.etStartTime.setText(DateUtils.minutesToTime(startHour * 60 + startMinute));
        binding.etEndTime.setText(DateUtils.minutesToTime(endHour * 60 + endMinute));
        updateCategoryPills();
        updateRewardPreview();
        updateSubmitState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
