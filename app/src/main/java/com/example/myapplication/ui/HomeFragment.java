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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.ui.adapter.TaskAdapter;
import com.example.myapplication.util.DateUtils;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TaskAdapter adapter;

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

        if (getArguments() != null) {
            String selectedDate = getArguments().getString("selectedDate");
            if (selectedDate != null) {
                viewModel.setSelectedDate(selectedDate);
            }
        }
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
            adapter.submitList(tasks);
            boolean empty = tasks == null || tasks.isEmpty();
            binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getCompletedCount().observe(getViewLifecycleOwner(), c -> updateProgress());
        viewModel.getTotalCount().observe(getViewLifecycleOwner(), t -> updateProgress());
    }

    private void updateProgress() {
        Integer completed = viewModel.getCompletedCount().getValue();
        Integer total = viewModel.getTotalCount().getValue();
        if (completed == null) completed = 0;
        if (total == null) total = 0;

        String dateStr = viewModel.getSelectedDate();
        if (dateStr != null) {
            binding.toolbar.setTitle(DateUtils.getDisplayDate(dateStr));
        }

        binding.tvProgress.setText(getString(R.string.home_progress, completed, total));
    }

    private void setupClickListeners() {
        binding.fabAdd.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("selectedDate", viewModel.getSelectedDate());
            Navigation.findNavController(v)
                    .navigate(R.id.action_homeFragment_to_addEditTaskFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}