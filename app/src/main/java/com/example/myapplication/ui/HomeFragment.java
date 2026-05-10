package com.example.myapplication.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.ui.adapter.TaskAdapter;
import com.example.myapplication.util.AvatarHelper;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.ThemeManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TaskAdapter adapter;

    private String currentFilter = "all";
    private List<TaskEntity> allTasks = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        setupCategoryFilter();
        applyThemeColors();
        viewModel.checkAndUpdateStreak();

        if (getArguments() != null) {
            String selectedDate = getArguments().getString("selectedDate");
            if (selectedDate != null && !selectedDate.isEmpty()) {
                viewModel.setSelectedDate(selectedDate);
            }
        }
    }

    private void applyThemeColors() {
        if (getContext() == null) return;
        int primary = ThemeManager.getThemePrimaryInt(requireContext());
        int primaryDark = ThemeManager.getThemePrimaryDarkInt(requireContext());

        // Apply theme to level badge
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(primary);
        badgeBg.setSize(44, 44);
        binding.tvLevelBadge.setBackground(badgeBg);

        // Apply theme to level card gradient
        GradientDrawable cardGradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF2D2D42, 0xFF1E1E36}
        );
        cardGradient.setCornerRadius(20);
        binding.layoutLevelCard.setBackground(cardGradient);

        // Apply theme to XP fill
        GradientDrawable xpFill = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{primary, 0xFF22C55E}
        );
        xpFill.setCornerRadius(9999);
        binding.viewXpFill.setBackground(xpFill);
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(new TaskAdapter.OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(TaskEntity task) {
                viewModel.completeTask(task);
            }

            @Override
            public void onTaskClicked(TaskEntity task) {
                Bundle args = new Bundle();
                args.putLong("taskId", task.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_homeFragment_to_addEditTaskFragment, args);
            }
        });

        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            applyFilter();
        });

        viewModel.getCompletedCount().observe(getViewLifecycleOwner(), c -> updateHeaderStats());
        viewModel.getTotalCount().observe(getViewLifecycleOwner(), t -> updateHeaderStats());
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.tvHeaderCoins.setText(String.valueOf(user.getCurrentCoins()));
                binding.tvStreakPreview.setText(String.valueOf(user.getCurrentStreak()));
                binding.tvUserName.setText(user.getDisplayName());
                binding.tvGreeting.setText(getGreeting());
                applyThemeColors();
                updateXpBar(user);
                AvatarHelper.applyCircleClip(binding.ivAvatar);
                AvatarHelper.loadAvatar(requireContext(), user, binding.ivAvatar);
            }
        });
    }

    private void updateHeaderStats() {
        Integer completed = viewModel.getCompletedCount().getValue();
        Integer total = viewModel.getTotalCount().getValue();
        if (completed == null) completed = 0;
        if (total == null) total = 0;

        // Ring count
        binding.tvRingCount.setText(completed + "/" + total);

        // Task count label
        binding.tvTaskCount.setText(completed + "/" + total + " 已完成");

        // Progress description
        int remaining = total - completed;
        if (remaining > 0) {
            binding.tvProgressDesc.setText("还剩 " + remaining + " 个任务，全部完成可获得额外奖励！");
        } else if (total > 0) {
            binding.tvProgressDesc.setText("今日任务全部完成！太棒了！");
        } else {
            binding.tvProgressDesc.setText("还剩 0 个任务，全部完成可获得额外奖励！");
        }

        // XP and coin previews
        int remainingXp = 0;
        int remainingCoins = 0;
        if (viewModel.getTasks().getValue() != null) {
            for (TaskEntity task : viewModel.getTasks().getValue()) {
                if (!task.isCompleted()) {
                    RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(task.getCategory());
                    remainingXp += reward.xp;
                    remainingCoins += reward.coins;
                }
            }
        }
        binding.tvXpPreview.setText("+" + remainingXp + " XP");
        binding.tvCoinPreview.setText("+" + remainingCoins + " ¢");

        // Update progress ring
        updateProgressRing(completed, total);
    }

    private void updateProgressRing(int completed, int total) {
        float pct = total > 0 ? (float) completed / total : 0f;
        binding.ivProgressRing.setImageAlpha(Math.round(pct * 255));
    }

    private void updateXpBar(UserEntity user) {
        if (user == null) return;
        int level = RewardCalculator.calculateLevel(user.getTotalXp());
        int xpProgress = RewardCalculator.getXpProgressInLevel(user.getTotalXp());
        int xpNeeded = RewardCalculator.getXpForNextLevel(level);

        // Update XP label and percent
        binding.tvXpLabel.setText(xpProgress + " / " + xpNeeded + " XP");
        int pct = xpNeeded > 0 ? (xpProgress * 100 / xpNeeded) : 0;
        binding.tvXpPercent.setText(pct + "%");

        // Update level badge and title
        binding.tvLevelBadge.setText(String.valueOf(level));
        binding.tvLevelTitle.setText(RewardCalculator.getLevelTitle(level));
        binding.tvLevelSub.setText("距离下一等级还需 " + (xpNeeded - xpProgress) + " XP");

        // Update XP fill bar width after layout is ready
        binding.viewXpFill.post(() -> {
            if (binding == null) return;
            FrameLayout xpBar = binding.viewXpFill.getParent() instanceof FrameLayout
                    ? (FrameLayout) binding.viewXpFill.getParent() : null;
            if (xpBar != null) {
                int barWidth = xpBar.getWidth();
                if (barWidth > 0 && xpNeeded > 0) {
                    int fillWidth = (int) ((xpProgress / (float) xpNeeded) * barWidth);
                    binding.viewXpFill.getLayoutParams().width = Math.max(fillWidth, 0);
                    binding.viewXpFill.requestLayout();
                }
            }
        });
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "早上好 ☀️";
        if (hour >= 12 && hour < 18) return "下午好 ☀️";
        return "晚上好 🌙";
    }

    private void setupCategoryFilter() {
        View.OnClickListener filterListener = v -> {
            int id = v.getId();
            if (id == R.id.btn_cat_all) {
                currentFilter = "all";
            } else if (id == R.id.btn_cat_work) {
                currentFilter = RewardCalculator.CAT_WORK;
            } else if (id == R.id.btn_cat_study) {
                currentFilter = RewardCalculator.CAT_STUDY;
            } else if (id == R.id.btn_cat_exercise) {
                currentFilter = RewardCalculator.CAT_EXERCISE;
            } else if (id == R.id.btn_cat_personal) {
                currentFilter = RewardCalculator.CAT_PERSONAL;
            } else {
                currentFilter = "all";
            }
            updateFilterAppearance();
            applyFilter();
        };

        binding.btnCatAll.setOnClickListener(filterListener);
        binding.btnCatWork.setOnClickListener(filterListener);
        binding.btnCatStudy.setOnClickListener(filterListener);
        binding.btnCatExercise.setOnClickListener(filterListener);
        binding.btnCatPersonal.setOnClickListener(filterListener);
    }

    private void updateFilterAppearance() {
        // Reset all to default
        binding.btnCatAll.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_default));
        binding.btnCatWork.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_default));
        binding.btnCatStudy.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_default));
        binding.btnCatExercise.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_default));
        binding.btnCatPersonal.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_default));

        // Set active one
        if ("all".equals(currentFilter)) {
            binding.btnCatAll.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_selected));
        } else if (RewardCalculator.CAT_WORK.equals(currentFilter)) {
            binding.btnCatWork.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_selected));
        } else if (RewardCalculator.CAT_STUDY.equals(currentFilter)) {
            binding.btnCatStudy.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_selected));
        } else if (RewardCalculator.CAT_EXERCISE.equals(currentFilter)) {
            binding.btnCatExercise.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_selected));
        } else if (RewardCalculator.CAT_PERSONAL.equals(currentFilter)) {
            binding.btnCatPersonal.setBackground(getResources().getDrawable(R.drawable.bg_cat_filter_selected));
        }
    }

    private void applyFilter() {
        List<TaskEntity> filtered;
        if ("all".equals(currentFilter)) {
            filtered = allTasks;
        } else {
            filtered = new ArrayList<>();
            for (TaskEntity task : allTasks) {
                if (task.getCategory().equals(currentFilter)) {
                    filtered.add(task);
                }
            }
        }
        adapter.submitList(filtered);

        boolean empty = allTasks.isEmpty();
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void setupClickListeners() {
        binding.fabAdd.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("selectedDate", viewModel.getSelectedDate());
            Navigation.findNavController(v)
                    .navigate(R.id.action_homeFragment_to_addEditTaskFragment, args);
        });

        binding.btnNotification.setOnClickListener(v ->
                NotificationHelper.showNotificationDialog(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
