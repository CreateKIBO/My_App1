package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.example.myapplication.util.DateUtils;

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
                Bundle args = new Bundle();
                args.putLong("taskId", task.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_calendarFragment_to_addEditTaskFragment, args);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
