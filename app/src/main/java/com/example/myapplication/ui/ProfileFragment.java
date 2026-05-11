package com.example.myapplication.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.example.myapplication.util.AvatarHelper;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.AnimUtils;
import com.example.myapplication.util.RewardCalculator;
import com.example.myapplication.util.ThemeManager;

import java.text.NumberFormat;
import java.util.Locale;

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

        applyThemeColors();
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;

            // Display name
            binding.tvDisplayName.setText(user.getDisplayName());

            // XP progress
            int level = RewardCalculator.calculateLevel(user.getTotalXp());
            String levelTitle = RewardCalculator.getLevelTitle(level);
            binding.tvLevel.setText(getString(R.string.profile_level_format, level, levelTitle));

            // XP progress
            int xpProgress = RewardCalculator.getXpProgressInLevel(user.getTotalXp());
            int xpNeeded = RewardCalculator.getXpForNextLevel(level);
            binding.tvXpProgress.setText(
                    getString(R.string.profile_xp_progress_format, xpProgress, xpNeeded));

            // Next level label
            String nextTitle = RewardCalculator.getLevelTitle(level + 1);
            binding.tvNextLevel.setText(getString(R.string.profile_next_level, nextTitle));

            // XP bar fill
            FrameLayout xpBar = binding.viewXpFill.getParent() instanceof FrameLayout
                    ? (FrameLayout) binding.viewXpFill.getParent() : null;
            if (xpBar != null) {
                int barWidth = xpBar.getWidth();
                if (barWidth > 0 && xpNeeded > 0) {
                    int fillWidth = (int) ((xpProgress / (float) xpNeeded) * barWidth);
                    binding.viewXpFill.getLayoutParams().width = fillWidth;
                    binding.viewXpFill.requestLayout();
                }
            }

            // Stats
            NumberFormat fmt = NumberFormat.getInstance(Locale.CHINA);
            binding.tvCoins.setText(fmt.format(user.getCurrentCoins()));
            binding.tvTotalXp.setText(fmt.format(user.getTotalXp()));
            binding.tvStreak.setText(String.valueOf(user.getCurrentStreak()));

            // Avatar
            updateAvatar(user);

            // Equipped section
            updateEquippedSection(user.getAvatarId(), user.getThemeId());

            // Re-apply theme colors with user's theme
            applyThemeColors();
        });

        viewModel.getCompletedCount().observe(getViewLifecycleOwner(), count -> {
            binding.tvCompleted.setText(String.valueOf(count != null ? count : 0));
        });

        viewModel.getRecentRewards().observe(getViewLifecycleOwner(), rewards -> {
            boolean empty = rewards == null || rewards.isEmpty();
            binding.rvRewards.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.tvNoRewards.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) {
                rewardAdapter.submitList(rewards);
            }
        });
    }

    private void setupClickListeners() {
        // Button animations
        AnimUtils.scaleOnClick(binding.btnChangeEquipped);
        AnimUtils.scaleOnClick(binding.itemSettingsNotifications);
        AnimUtils.scaleOnClick(binding.itemSettingsExport);
        AnimUtils.scaleOnClick(binding.itemSettingsAbout);
        AnimUtils.scaleOnClick(binding.itemStreakEntry);

        // Streak entry -> navigate to streak page
        binding.itemStreakEntry.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_streakFragment);
            } catch (Exception ignored) {}
        });

        // Equipped change button -> navigate to shop
        binding.btnChangeEquipped.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.action_profileFragment_to_avatarEditFragment);
            } catch (Exception ignored) {}
        });

        // Settings items
        binding.itemSettingsNotifications.setOnClickListener(v ->
                NotificationHelper.showNotificationDialog(requireContext()));

        binding.itemSettingsExport.setOnClickListener(v ->
                Toast.makeText(requireContext(), "敬请期待", Toast.LENGTH_SHORT).show());

        binding.itemSettingsAbout.setOnClickListener(v ->
                Toast.makeText(requireContext(), "日迹 v1.0 — 你的每日任务伙伴", Toast.LENGTH_LONG).show());
    }

    private void applyThemeColors() {
        if (getContext() == null) return;

        // Hero card gradient: dark theme-aware gradient
        int primary = ThemeManager.getThemePrimaryInt(requireContext());
        int primaryDark = ThemeManager.getThemePrimaryDarkInt(requireContext());
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{primaryDark, primaryDark}
        );
        gradient.setCornerRadius(dpToPx(20));
        binding.profileHeader.setBackground(gradient);

        // Level dot accent color
        View dot = binding.getRoot().findViewById(R.id.view_level_dot);
        if (dot != null) {
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(primary);
            dot.setBackground(dotDrawable);
        }

        // XP fill gradient (green -> lighter green)
        GradientDrawable xpFill = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{
                        ContextCompat.getColor(requireContext(), R.color.xp_green),
                        ContextCompat.getColor(requireContext(), R.color.xp_green_light)
                }
        );
        xpFill.setCornerRadius(dpToPx(9999));
        binding.viewXpFill.setBackground(xpFill);

        // Update XP bar width after layout
        binding.profileHeader.post(this::updateUserXpBar);
    }

    private void updateUserXpBar() {
        UserEntity user = viewModel.getUser().getValue();
        if (user == null) return;

        int level = RewardCalculator.calculateLevel(user.getTotalXp());
        int xpProgress = RewardCalculator.getXpProgressInLevel(user.getTotalXp());
        int xpNeeded = RewardCalculator.getXpForNextLevel(level);

        FrameLayout xpBar = (FrameLayout) binding.viewXpFill.getParent();
        if (xpBar == null) return;

        int barWidth = xpBar.getWidth();
        if (barWidth > 0 && xpNeeded > 0) {
            int fillWidth = (int) ((xpProgress / (float) xpNeeded) * barWidth);
            binding.viewXpFill.getLayoutParams().width = Math.max(fillWidth, 0);
            binding.viewXpFill.requestLayout();
        }
    }

    private void updateAvatar(UserEntity user) {
        AvatarHelper.applyCircleClip(binding.ivAvatar);
        AvatarHelper.loadAvatar(requireContext(), user, binding.ivAvatar);
    }

    private void updateEquippedSection(int avatarId, int themeId) {
        // Update equipped avatar
        ImageView ivEquipped = binding.ivEquippedAvatar;
        if (avatarId > 0) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ShopItemEntity avatarItem = viewModel.getEquippedAvatarItem(avatarId);
                if (avatarItem != null && avatarItem.getIconResName() != null && isAdded()) {
                    int resId = getResources().getIdentifier(
                            avatarItem.getIconResName(), "drawable", requireContext().getPackageName());
                    requireActivity().runOnUiThread(() -> {
                        if (resId != 0) {
                            ivEquipped.setImageResource(resId);
                            ivEquipped.setImageTintList(null);
                        } else {
                            ivEquipped.setImageResource(R.drawable.ic_profile);
                        }
                        binding.tvEquippedName.setText(avatarItem.getName());
                    });
                }
            });
        } else {
            ivEquipped.setImageResource(R.drawable.ic_profile);
            binding.tvEquippedName.setText("默认头像");
        }

        // Update equipped theme description
        if (themeId > 0) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ShopItemEntity themeItem = viewModel.getEquippedThemeItem(themeId);
                if (themeItem != null && isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            binding.tvEquippedDesc.setText(themeItem.getName()));
                }
            });
        } else {
            binding.tvEquippedDesc.setText("默认蓝主题");
        }
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
