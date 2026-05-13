package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.databinding.FragmentCalendarBinding;
import com.example.myapplication.ui.adapter.CalendarAdapter;
import com.example.myapplication.ui.adapter.TaskAdapter;
import com.example.myapplication.util.AnimUtils;
import com.example.myapplication.util.DateUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarAdapter calendarAdapter;
    private TaskAdapter taskAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);

        setupCalendar();
        setupDetailTaskList();
        setupObservers();
        setupClickListeners();
        setupStreakBanner();

        // Entrance animations
        AnimUtils.slideUpFadeIn(binding.tvMonthYear, 0L);
        AnimUtils.slideUpFadeIn(binding.rvCalendar, 120L);
    }

    private void setupStreakBanner() {
        // Load streak data from ViewModel
        viewModel.getStreakCount().observe(getViewLifecycleOwner(), streak -> {
            if (streak == null || streak == 0) {
                binding.streakBanner.setVisibility(View.GONE);
            } else {
                binding.streakBanner.setVisibility(View.VISIBLE);
                binding.tvStreakNum.setText(streak + " 天连续");
                binding.tvStreakSub.setText(getStreakMessage(streak));
            }
        });
    }

    private String getStreakMessage(int streak) {
        if (streak == 0) return "开始你的连续打卡吧！";
        int nextMilestone;
        if (streak < 3) nextMilestone = 3;
        else if (streak < 7) nextMilestone = 7;
        else if (streak < 14) nextMilestone = 14;
        else if (streak < 30) nextMilestone = 30;
        else nextMilestone = 60;

        int remaining = nextMilestone - streak;
        if (remaining <= 0) {
            return "太厉害了！继续保持！";
        }
        return "保持下去，" + nextMilestone + "天里程碑即将达成！";
    }

    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter(date -> {
            viewModel.setSelectedDate(date);
            calendarAdapter.setSelectedDate(date);
            updateDetailDateLabel(date);
        }, requireContext());

        binding.rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvCalendar.setAdapter(calendarAdapter);

        // Initialize with current month
        int[] month = viewModel.getCurrentMonth().getValue();
        if (month != null) {
            updateCalendarGrid(month[0], month[1]);
        }

        // Initialize selected date
        String selectedDate = viewModel.getSelectedDate();
        if (selectedDate != null) {
            calendarAdapter.setSelectedDate(selectedDate);
            updateDetailDateLabel(selectedDate);
        }
    }

    private void setupDetailTaskList() {
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskCompleteListener() {
            @Override
            public void onTaskComplete(TaskEntity task) {
                viewModel.setSelectedDate(viewModel.getSelectedDate());
            }

            @Override
            public void onTaskClicked(TaskEntity task) {
                showTaskDetailDialog(task);
            }
        });

        binding.rvDetailTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDetailTasks.setAdapter(taskAdapter);
    }

    private void updateCalendarGrid(int year, int month) {
        binding.tvMonthYear.setText(viewModel.getMonthYearLabel(year, month));
        calendarAdapter.setDays(CalendarAdapter.generateDaysForMonth(year, month));
    }

    private void updateDetailDateLabel(String dateStr) {
        binding.tvDetailDate.setText(viewModel.getSelectedDateDisplayLabel(dateStr));
    }

    private void setupObservers() {
        // Observe month changes
        viewModel.getCurrentMonth().observe(getViewLifecycleOwner(), month -> {
            if (month != null) updateCalendarGrid(month[0], month[1]);
        });

        // Observe dates with tasks (for dot indicators)
        viewModel.getDatesWithTasksLiveData().observe(getViewLifecycleOwner(), dates -> {
            viewModel.updateTaskDates(dates);
        });

        viewModel.getTaskDatesSet().observe(getViewLifecycleOwner(), datesSet -> {
            calendarAdapter.setDatesWithTasks(datesSet);
        });

        // Observe selected date changes
        viewModel.getSelectedDateLiveData().observe(getViewLifecycleOwner(), dateStr -> {
            if (dateStr != null) {
                calendarAdapter.setSelectedDate(dateStr);
                updateDetailDateLabel(dateStr);
            }
        });

        // Observe tasks for the selected date
        viewModel.getTasksForDate().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                taskAdapter.submitList(tasks);
                binding.rvDetailTasks.setVisibility(View.VISIBLE);
                binding.layoutDetailEmpty.setVisibility(View.GONE);
                binding.cardDetail.setVisibility(View.VISIBLE);
            } else {
                taskAdapter.submitList(tasks);
                binding.rvDetailTasks.setVisibility(View.GONE);
                binding.layoutDetailEmpty.setVisibility(View.VISIBLE);
                binding.cardDetail.setVisibility(View.VISIBLE);
            }
            updateDetailBadge();
        });

        // Observe completion counts for badge
        viewModel.getCompletedCount().observe(getViewLifecycleOwner(), count -> updateDetailBadge());
        viewModel.getTotalCount().observe(getViewLifecycleOwner(), count -> updateDetailBadge());
    }

    private void updateDetailBadge() {
        Integer completed = viewModel.getCompletedCount().getValue();
        Integer total = viewModel.getTotalCount().getValue();
        if (completed == null || total == null || total == 0) {
            binding.tvDetailBadge.setVisibility(View.GONE);
            return;
        }

        binding.tvDetailBadge.setVisibility(View.VISIBLE);
        if (completed.equals(total)) {
            // All done
            binding.tvDetailBadge.setText("✓ 全部完成");
            binding.tvDetailBadge.setBackground(
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_done));
            binding.tvDetailBadge.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.xp_green));
        } else {
            // Partial
            binding.tvDetailBadge.setText(completed + "/" + total + " 已完成");
            binding.tvDetailBadge.setBackground(
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_partial));
            binding.tvDetailBadge.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.md_primary));
        }
    }

    private void setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPrevMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
        binding.btnToday.setOnClickListener(v -> viewModel.goToToday());
    }

    private void showTaskDetailDialog(TaskEntity task) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task_detail, null);
        dialog.setContentView(view);

        // Title
        TextView tvTitle = view.findViewById(R.id.tv_title);
        tvTitle.setText(task.getTitle());

        // Status
        ImageView ivStatus = view.findViewById(R.id.iv_status_icon);
        TextView tvStatus = view.findViewById(R.id.tv_status);
        boolean completed = task.isCompleted();
        ivStatus.setImageResource(completed
                ? R.drawable.ic_check_circle_outline
                : R.drawable.ic_radio_button_unchecked);
        tvStatus.setText(completed ? "已完成" : "进行中");
        int statusColor = completed ? 0xFF10B981 : 0xFFF59E0B;
        ivStatus.setColorFilter(statusColor);
        tvStatus.setTextColor(statusColor);

        // Category badge
        TextView tvCategory = view.findViewById(R.id.tv_category_badge);
        String category = task.getCategory();
        tvCategory.setText(getCategoryLabel(category));
        tvCategory.setTextColor(getCategoryTextColor(category));
        tvCategory.setBackgroundColor(getCategoryBgColor(category));

        // Date
        TextView tvDate = view.findViewById(R.id.tv_date);
        tvDate.setText(task.getDate());

        // Time
        TextView tvTime = view.findViewById(R.id.tv_time);
        int start = task.getStartTime();
        int end = task.getEndTime();
        if (start > 0 || end > 0) {
            tvTime.setText(DateUtils.minutesToTime(start) + " - " + DateUtils.minutesToTime(end));
        } else {
            tvTime.setText("未设置");
        }

        // Priority
        TextView tvPriority = view.findViewById(R.id.tv_priority);
        tvPriority.setText(getPriorityLabel(category));
        tvPriority.setTextColor(getPriorityColor(category));

        // Reward
        TextView tvCoins = view.findViewById(R.id.tv_coins);
        TextView tvXp = view.findViewById(R.id.tv_xp);
        tvCoins.setText("+" + task.getCoinsEarned() + " 金币");
        tvXp.setText("+" + task.getXpEarned() + " XP");

        // Description
        String desc = task.getDescription();
        TextView tvDescLabel = view.findViewById(R.id.tv_description_label);
        MaterialCardView cardDesc = view.findViewById(R.id.card_description);
        TextView tvDesc = view.findViewById(R.id.tv_description);
        if (desc != null && !desc.isEmpty()) {
            tvDescLabel.setVisibility(View.VISIBLE);
            cardDesc.setVisibility(View.VISIBLE);
            tvDesc.setText(desc);
        }

        // Edit button
        MaterialButton btnEdit = view.findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle args = new Bundle();
            args.putLong("taskId", task.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_calendarFragment_to_addEditTaskFragment, args);
        });

        // Close button
        MaterialButton btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Staggered entrance animation
        View infoCard = view.findViewById(R.id.card_info);
        View rewardCard = view.findViewById(R.id.card_reward);
        AnimUtils.slideUpFadeIn(tvTitle, 0L);
        AnimUtils.slideUpFadeIn(view.findViewById(R.id.layout_status), 60L);
        AnimUtils.slideUpFadeIn(infoCard, 120L);
        AnimUtils.slideUpFadeIn(rewardCard, 180L);
        AnimUtils.slideUpFadeIn(btnEdit, 240L);
    }

    private String getCategoryLabel(String category) {
        if (category == null) return "其他";
        switch (category) {
            case "Work": return "工作";
            case "Study": return "学习";
            case "Exercise": return "运动";
            case "Personal": return "个人";
            default: return "其他";
        }
    }

    private String getPriorityLabel(String category) {
        if (category == null) return "普通";
        switch (category) {
            case "Work": return "高";
            case "Study": return "中";
            case "Exercise": return "中";
            case "Personal": return "低";
            default: return "普通";
        }
    }

    private int getPriorityColor(String category) {
        if (category == null) return 0xFF6B7280;
        switch (category) {
            case "Work": return 0xFFEF4444;
            case "Study": return 0xFFF59E0B;
            case "Exercise": return 0xFFF59E0B;
            case "Personal": return 0xFF10B981;
            default: return 0xFF6B7280;
        }
    }

    private int getCategoryTextColor(String category) {
        if (category == null) return 0xFF6B7280;
        switch (category) {
            case "Work": return 0xFF3B82F6;
            case "Study": return 0xFF8B5CF6;
            case "Exercise": return 0xFF10B981;
            case "Personal": return 0xFFF59E0B;
            default: return 0xFF6B7280;
        }
    }

    private int getCategoryBgColor(String category) {
        if (category == null) return 0x1A6B7280;
        switch (category) {
            case "Work": return 0x1A3B82F6;
            case "Study": return 0x1A8B5CF6;
            case "Exercise": return 0x1A10B981;
            case "Personal": return 0x1AF59E0B;
            default: return 0x1A6B7280;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
