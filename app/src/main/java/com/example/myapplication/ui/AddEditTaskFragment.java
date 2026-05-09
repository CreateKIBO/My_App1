package com.example.myapplication.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        // Category chips
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_work)) selectedCategory = RewardCalculator.CAT_WORK;
            else if (checkedIds.contains(R.id.chip_study)) selectedCategory = RewardCalculator.CAT_STUDY;
            else if (checkedIds.contains(R.id.chip_exercise)) selectedCategory = RewardCalculator.CAT_EXERCISE;
            else if (checkedIds.contains(R.id.chip_personal)) selectedCategory = RewardCalculator.CAT_PERSONAL;
            updateRewardPreview();
        });
        binding.chipWork.setChecked(true);

        // Time pickers
        binding.etStartTime.setOnClickListener(v -> showTimePicker(0));
        binding.etEndTime.setOnClickListener(v -> showTimePicker(1));

        // Date
        binding.etDate.setText(DateUtils.getTodayString());
        binding.etDate.setOnClickListener(v -> {
            // Simple date display, uses today by default
        });

        // Edit mode
        long taskId = getArguments() != null ? getArguments().getLong("taskId", -1) : -1;
        if (taskId > 0) {
            viewModel.loadTask(taskId);
            viewModel.getTask().observe(getViewLifecycleOwner(), task -> {
                if (task == null) return;
                binding.etTaskTitle.setText(task.getTitle());
                binding.etDate.setText(task.getDate());
                if (task.getStartTime() != null) binding.etStartTime.setText(DateUtils.minutesToTime(task.getStartTime()));
                if (task.getEndTime() != null) binding.etEndTime.setText(DateUtils.minutesToTime(task.getEndTime()));
                selectedCategory = task.getCategory();
                selectCategoryChip();
                updateRewardPreview();
            });
        }

        // Save
        binding.btnSave.setOnClickListener(v -> saveTask(taskId));

        updateRewardPreview();
    }

    private void selectCategoryChip() {
        switch (selectedCategory) {
            case "WORK": binding.chipWork.setChecked(true); break;
            case "STUDY": binding.chipStudy.setChecked(true); break;
            case "EXERCISE": binding.chipExercise.setChecked(true); break;
            case "PERSONAL": binding.chipPersonal.setChecked(true); break;
        }
    }

    private void updateRewardPreview() {
        RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(selectedCategory);
        binding.tvRewardPreview.setText("+" + reward.coins + " 金币");
        binding.tvXpPreview.setText("+" + reward.xp + " XP");
    }

    private void showTimePicker(int type) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            String time = String.format("%02d:%02d", hour, minute);
            if (type == 0) binding.etStartTime.setText(time);
            else binding.etEndTime.setText(time);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void saveTask(long taskId) {
        String title = binding.etTaskTitle.getText() != null ? binding.etTaskTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            binding.etTaskTitle.setError("请输入标题");
            return;
        }

        String date = binding.etDate.getText() != null ? binding.etDate.getText().toString().trim() : DateUtils.getTodayString();
        String startStr = binding.etStartTime.getText() != null ? binding.etStartTime.getText().toString().trim() : null;
        String endStr = binding.etEndTime.getText() != null ? binding.etEndTime.getText().toString().trim() : null;

        Integer startMinutes = startStr != null && !startStr.isEmpty() ? DateUtils.timeToMinutes(startStr) : null;
        Integer endMinutes = endStr != null && !endStr.isEmpty() ? DateUtils.timeToMinutes(endStr) : null;

        if (taskId > 0) {
            viewModel.updateTask(taskId, title, date, startMinutes, endMinutes, selectedCategory);
        } else {
            viewModel.createTask(title, date, startMinutes, endMinutes, selectedCategory);
        }

        Navigation.findNavController(requireView()).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
