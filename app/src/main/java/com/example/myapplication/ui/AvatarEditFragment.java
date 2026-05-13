package com.example.myapplication.ui;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.databinding.FragmentAvatarEditBinding;
import com.example.myapplication.ui.adapter.AvatarGridAdapter;
import com.example.myapplication.util.AvatarHelper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AvatarEditFragment extends Fragment {

    private FragmentAvatarEditBinding binding;
    private AvatarEditViewModel viewModel;
    private AvatarGridAdapter ownedAdapter;
    private AvatarGridAdapter lockedAdapter;

    private String customAvatarPath = null;

    // Image picker launcher
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                handleSelectedImage(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAvatarEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AvatarEditViewModel.class);

        // Setup adapters
        ownedAdapter = new AvatarGridAdapter(new AvatarGridAdapter.OnAvatarActionListener() {
            @Override
            public void onEquipClick(ShopItemEntity item) {
                viewModel.equipAvatar(item.getId());
            }

            @Override
            public void onBuyClick(ShopItemEntity item) {
                // Owned items should not have buy click, but handle gracefully
                showBuyDialog(item);
            }
        });

        lockedAdapter = new AvatarGridAdapter(new AvatarGridAdapter.OnAvatarActionListener() {
            @Override
            public void onEquipClick(ShopItemEntity item) {
                // Locked items cannot be equipped directly
            }

            @Override
            public void onBuyClick(ShopItemEntity item) {
                showBuyDialog(item);
            }
        });

        // 4-column grids with 10dp spacing
        GridLayoutManager ownedLayoutManager = new GridLayoutManager(requireContext(), 4);
        binding.rvOwnedAvatars.setLayoutManager(ownedLayoutManager);
        binding.rvOwnedAvatars.setAdapter(ownedAdapter);
        binding.rvOwnedAvatars.addItemDecoration(
                new ShopFragment.GridSpacingItemDecoration(4, 10, false));

        GridLayoutManager lockedLayoutManager = new GridLayoutManager(requireContext(), 4);
        binding.rvLockedAvatars.setLayoutManager(lockedLayoutManager);
        binding.rvLockedAvatars.setAdapter(lockedAdapter);
        binding.rvLockedAvatars.addItemDecoration(
                new ShopFragment.GridSpacingItemDecoration(4, 10, false));

        setupClickListeners();
        setupObservers();
    }

    private void setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Select image button
        binding.btnSelectImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Upload area click also triggers image picker
        binding.layoutUploadArea.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Equip custom avatar
        binding.btnEquipCustom.setOnClickListener(v -> {
            viewModel.equipCustomAvatar();
        });

        // Delete custom avatar
        binding.btnDeleteCustom.setOnClickListener(v -> {
            showDeleteDialog();
        });

        // Save name
        binding.btnSaveName.setOnClickListener(v -> {
            String name = binding.etDisplayName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "昵称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.updateDisplayName(name);
        });
    }

    private void setupObservers() {
        // Observe current user
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null || !isAdded()) return;
            updateCurrentAvatarDisplay(user);
            updateCoinBalance(user);
            updateNameEdit(user);
            updateCustomAvatarSection(user);
            // Refresh grid ownership when user data changes (e.g. after purchase)
            java.util.List<ShopItemEntity> items = viewModel.getAvatarItems().getValue();
            if (items != null) {
                splitAndUpdateGrids(items, user);
            }
        });

        // Observe avatar items and split into owned/locked
        viewModel.getAvatarItems().observe(getViewLifecycleOwner(), items -> {
            if (items == null || !isAdded()) return;
            UserEntity user = viewModel.getCurrentUser().getValue();
            splitAndUpdateGrids(items, user);
        });

        // Observe messages
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && isAdded()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCurrentAvatarDisplay(UserEntity user) {
        AvatarHelper.applyCircleClip(binding.ivCurrentAvatar);
        AvatarHelper.loadAvatar(requireContext(), user, binding.ivCurrentAvatar);

        // Update avatar name
        String customPath = user.getCustomAvatarPath();
        if (customPath != null && !customPath.isEmpty() && new File(customPath).exists()
                && user.getAvatarId() == -1) {
            binding.tvCurrentAvatarName.setText("自定义头像");
        } else if (user.getAvatarId() > 0) {
            // Load avatar name from database
            AppDatabase.databaseWriteExecutor.execute(() -> {
                ShopItemEntity item = viewModel.getShopItemById(user.getAvatarId());
                if (item != null && isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            binding.tvCurrentAvatarName.setText(item.getName()));
                }
            });
        } else {
            binding.tvCurrentAvatarName.setText("默认头像");
        }
    }

    private void updateCoinBalance(UserEntity user) {
        String formatted = NumberFormat.getNumberInstance(Locale.CHINA).format(user.getCurrentCoins());
        binding.tvCoinBalance.setText(formatted);
    }

    private void updateNameEdit(UserEntity user) {
        binding.etDisplayName.setText(user.getDisplayName());
        // Limit name length
        binding.etDisplayName.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(12)
        });
    }

    private void updateCustomAvatarSection(UserEntity user) {
        String customPath = user.getCustomAvatarPath();
        boolean hasCustom = customPath != null && !customPath.isEmpty()
                && new File(customPath).exists();

        customAvatarPath = hasCustom ? customPath : null;

        if (hasCustom) {
            binding.layoutUploadArea.setVisibility(View.GONE);
            binding.layoutUploadedPreview.setVisibility(View.VISIBLE);

            AvatarHelper.applyCircleClip(binding.ivCustomPreview);
            AvatarHelper.loadCustomAvatar(customPath, binding.ivCustomPreview);
        } else {
            // Show upload area, hide preview
            binding.layoutUploadArea.setVisibility(View.VISIBLE);
            binding.layoutUploadedPreview.setVisibility(View.GONE);
        }
    }

    private void splitAndUpdateGrids(java.util.List<ShopItemEntity> items, UserEntity user) {
        java.util.List<AvatarGridAdapter.AvatarGridItem> ownedList = new ArrayList<>();
        java.util.List<AvatarGridAdapter.AvatarGridItem> lockedList = new ArrayList<>();

        boolean isCustomEquipped = user != null && user.getAvatarId() == -1
                && user.getCustomAvatarPath() != null
                && !user.getCustomAvatarPath().isEmpty();

        for (ShopItemEntity item : items) {
            boolean isOwned = user != null && viewModel.isAvatarOwned(item.getId(), user);
            boolean isEquipped = !isCustomEquipped && user != null
                    && user.getAvatarId() == item.getId();

            AvatarGridAdapter.AvatarGridItem gridItem =
                    new AvatarGridAdapter.AvatarGridItem(item, isOwned, isEquipped);

            if (isOwned) {
                ownedList.add(gridItem);
            } else {
                lockedList.add(gridItem);
            }
        }

        ownedAdapter.submitList(ownedList);
        lockedAdapter.submitList(lockedList);

        // Show/hide locked section
        boolean hasLocked = !lockedList.isEmpty();
        binding.tvLockedLabel.setVisibility(hasLocked ? View.VISIBLE : View.GONE);
        binding.rvLockedAvatars.setVisibility(hasLocked ? View.VISIBLE : View.GONE);
    }

    /**
     * Handle the selected image URI from the image picker.
     */
    private void handleSelectedImage(Uri uri) {
        if (!isAdded()) return;

        ContentResolver resolver = requireContext().getContentResolver();

        // Validate type
        String mimeType = resolver.getType(uri);
        if (mimeType == null ||
                (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png")
                        && !mimeType.startsWith("image/"))) {
            Toast.makeText(requireContext(), "请选择 JPG 或 PNG 图片", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read stream once and cache to ByteArrayOutputStream
        byte[] imageData;
        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            imageData = baos.toByteArray();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate size (5MB max)
        if (imageData.length > 5 * 1024 * 1024) {
            Toast.makeText(requireContext(), "图片大小不能超过 5MB", Toast.LENGTH_SHORT).show();
            return;
        }

        // Copy to internal storage from cached bytes
        try (InputStream savedStream = new ByteArrayInputStream(imageData)) {
            String savedPath = viewModel.saveCustomAvatarFromStream(savedStream);
            if (savedPath != null) {
                customAvatarPath = savedPath;
                // Cleanup old avatars
                viewModel.cleanupOldAvatars(savedPath);
                Toast.makeText(requireContext(), "头像已上传", Toast.LENGTH_SHORT).show();
                // Refresh custom avatar section
                UserEntity user = viewModel.getCurrentUser().getValue();
                if (user != null) {
                    updateCustomAvatarSection(user);
                }
            } else {
                Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show buy confirmation dialog for a locked avatar.
     */
    private void showBuyDialog(ShopItemEntity item) {
        if (!isAdded()) return;

        UserEntity user = viewModel.getCurrentUser().getValue();
        if (user == null) return;

        // Get emoji for avatar
        String emoji = getEmojiForAvatar(item.getIconResName());

        // Check if user has enough coins
        int deficit = item.getPrice() - user.getCurrentCoins();
        String priceInfo;
        if (deficit > 0) {
            priceInfo = "金币不足！还需 " + deficit + " 金币";
        } else {
            priceInfo = "花费 " + item.getPrice() + " 金币购买并装备此头像";
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(emoji + "  购买 " + item.getName() + "？")
                .setMessage(priceInfo)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认", (dialog, which) -> {
                    if (deficit > 0) {
                        Toast.makeText(requireContext(), "金币不足！",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        viewModel.purchaseAvatar(item.getId(), item.getPrice());
                    }
                })
                .show();
    }

    /**
     * Show delete confirmation dialog for custom avatar.
     */
    private void showDeleteDialog() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除自定义头像？")
                .setMessage("删除后无法恢复，将自动切换为勇士头像")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deleteCustomAvatar();
                })
                .show();
    }

    /**
     * Map avatar icon resource names to emoji characters.
     */
    private String getEmojiForAvatar(String iconResName) {
        if (iconResName == null) return "👤";
        switch (iconResName) {
            case "ic_avatar_warrior": return "⚔️";
            case "ic_avatar_mage":    return "🧙";
            case "ic_avatar_ninja":   return "🥷";
            case "ic_avatar_knight":  return "🛡️";
            case "ic_avatar_dragon":  return "🐉";
            case "ic_avatar_robot":   return "🤖";
            case "ic_avatar_cat":     return "🐱";
            case "ic_avatar_ghost":   return "👻";
            default:                  return "👤";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}