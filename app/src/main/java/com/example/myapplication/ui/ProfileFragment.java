package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.databinding.FragmentProfileBinding;
import com.example.myapplication.ui.adapter.RewardAdapter;
import com.example.myapplication.util.RewardCalculator;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private RewardAdapter rewardAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        rewardAdapter = new RewardAdapter();
        binding.rvRewards.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRewards.setAdapter(rewardAdapter);

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;

            binding.tvDisplayName.setText(user.getDisplayName());
            int level = RewardCalculator.calculateLevel(user.getTotalXp());
            binding.tvLevel.setText(getString(R.string.profile_level, level));

            int xpProgress = viewModel.getXpProgressInLevel(user.getTotalXp());
            int xpNeeded = viewModel.getXpForNextLevel(level);
            binding.progressXp.setMax(xpNeeded);
            binding.progressXp.setProgress(xpProgress);
            binding.tvXpProgress.setText(getString(R.string.profile_xp_progress, xpProgress, xpNeeded));

            binding.tvCoins.setText(String.valueOf(user.getCurrentCoins()));
            binding.tvTotalXp.setText(String.valueOf(user.getTotalXp()));
            binding.tvStreak.setText(String.valueOf(user.getCurrentStreak()));

            // Update avatar based on avatarId
            updateAvatar(user.getAvatarId());
        });

        viewModel.getRecentRewards().observe(getViewLifecycleOwner(), rewards -> {
            rewardAdapter.submitList(rewards);
        });

        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_settingsFragment));
    }

    private void updateAvatar(int avatarId) {
        ImageView ivAvatar = binding.ivAvatar;
        if (avatarId <= 0) {
            ivAvatar.setImageResource(R.drawable.ic_profile);
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ShopItemEntity item = viewModel.getAvatarItem(avatarId);
            if (item != null && item.getIconResName() != null && isAdded()) {
                int resId = getResources().getIdentifier(item.getIconResName(), "drawable", requireContext().getPackageName());
                requireActivity().runOnUiThread(() -> {
                    if (resId != 0) {
                        ivAvatar.setImageResource(resId);
                        ivAvatar.setImageTintList(null);
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_profile);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}