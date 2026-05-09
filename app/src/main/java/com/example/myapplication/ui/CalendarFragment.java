package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentCalendarBinding;
import com.example.myapplication.ui.adapter.CalendarAdapter;
import com.example.myapplication.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarAdapter adapter;

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
        setupObservers();
        setupClickListeners();
    }

    private void setupCalendar() {
        adapter = new CalendarAdapter(date -> {
            Bundle args = new Bundle();
            args.putString("selectedDate", date);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_calendarFragment_to_homeFragment, args);
        }, requireContext());

        binding.rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvCalendar.setAdapter(adapter);

        int[] month = viewModel.getCurrentMonth().getValue();
        if (month != null) {
            updateCalendar(month[0], month[1]);
        }
    }

    private void updateCalendar(int year, int month) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月", Locale.CHINESE);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month, 1);
        binding.tvMonthYear.setText(sdf.format(cal.getTime()));

        adapter.setDays(CalendarAdapter.generateDaysForMonth(year, month));
        adapter.setSelectedDate(DateUtils.getTodayString());
    }

    private void setupObservers() {
        viewModel.getCurrentMonth().observe(getViewLifecycleOwner(), month -> {
            if (month != null) updateCalendar(month[0], month[1]);
        });

        viewModel.getDatesWithTasksLiveData().observe(getViewLifecycleOwner(), dates -> {
            viewModel.updateTaskDates(dates);
        });

        viewModel.getTaskDatesSet().observe(getViewLifecycleOwner(), datesSet -> {
            adapter.setDatesWithTasks(datesSet);
        });
    }

    private void setupClickListeners() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPrevMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
        binding.btnToday.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("selectedDate", DateUtils.getTodayString());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_calendarFragment_to_homeFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}