package com.example.myapplication.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.databinding.FragmentShopBinding;
import com.example.myapplication.ui.adapter.ShopItemAdapter;
import com.example.myapplication.util.AnimUtils;
import com.example.myapplication.util.RewardCalculator;

import java.text.NumberFormat;
import java.util.Locale;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private ShopViewModel viewModel;
    private ShopItemAdapter adapter;
    private int selectedTab = 0; // 0=avatar, 1=theme, 2=prop

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ShopViewModel.class);

        adapter = new ShopItemAdapter(new ShopItemAdapter.OnShopActionListener() {
            @Override
            public void onBuyClick(ShopItemEntity item) {
                if (RewardCalculator.TYPE_PROP.equals(item.getType())) {
                    viewModel.purchaseProp(item.getId(), item.getPrice());
                } else {
                    viewModel.purchaseItem(item.getId(), item.getPrice());
                }
            }

            @Override
            public void onEquipClick(ShopItemEntity item) {
                viewModel.equipItem(item.getId(), item.getType());
            }
        });

        // 2-column grid with 12dp spacing
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        binding.rvShopItems.setLayoutManager(gridLayoutManager);
        binding.rvShopItems.setAdapter(adapter);
        binding.rvShopItems.addItemDecoration(new GridSpacingItemDecoration(2, 12, false));

        // Button animations
        AnimUtils.scaleOnClick(binding.btnTabAvatar);
        AnimUtils.scaleOnClick(binding.btnTabTheme);
        AnimUtils.scaleOnClick(binding.btnTabProp);
        AnimUtils.scaleOnClick(binding.btnRecharge);

        // Tab switching
        updateTabStyle(0);
        binding.btnTabAvatar.setOnClickListener(v -> {
            if (selectedTab != 0) {
                selectedTab = 0;
                viewModel.setSelectedType(RewardCalculator.TYPE_AVATAR);
                binding.tvSectionLabel.setText("像素头像");
                updateTabStyle(0);
            }
        });
        binding.btnTabTheme.setOnClickListener(v -> {
            if (selectedTab != 1) {
                selectedTab = 1;
                viewModel.setSelectedType(RewardCalculator.TYPE_THEME);
                binding.tvSectionLabel.setText("主题样式");
                updateTabStyle(1);
            }
        });
        binding.btnTabProp.setOnClickListener(v -> {
            if (selectedTab != 2) {
                selectedTab = 2;
                viewModel.setSelectedType(RewardCalculator.TYPE_PROP);
                binding.tvSectionLabel.setText("道具");
                updateTabStyle(2);
            }
        });

        // Earn button
        binding.btnRecharge.setOnClickListener(v ->
                Toast.makeText(requireContext(), "完成任务赚取更多金币！", Toast.LENGTH_SHORT).show());

        // Observe data
        viewModel.getShopItems().observe(getViewLifecycleOwner(), items -> adapter.submitList(items));
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String formatted = NumberFormat.getNumberInstance(Locale.CHINA).format(user.getCurrentCoins());
                binding.tvCoinBalance.setText(formatted);
                updateTabStyle(selectedTab);
            }
        });
        viewModel.getCurrentOwnedIds().observe(getViewLifecycleOwner(), ids -> adapter.setOwnedIds(ids));
        viewModel.getCurrentEquippedId().observe(getViewLifecycleOwner(), id -> adapter.setEquippedId(id));
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // Entrance animations
        AnimUtils.slideUpFadeIn(binding.tvCoinBalance, 0L);
        AnimUtils.slideUpFadeIn(binding.rvShopItems, 120L);
    }

    /**
     * Update tab button styles:
     * Active tab: dark background, white text, no border
     * Inactive tab: transparent background, muted text, border
     */
    private void updateTabStyle(int activeTab) {
        if (getContext() == null) return;
        int white = requireContext().getColor(R.color.white);
        int onSurface = requireContext().getColor(R.color.md_on_surface);
        int onSurfaceVariant = requireContext().getColor(R.color.md_on_surface_variant);
        int outlineVariant = requireContext().getColor(R.color.md_outline_variant);

        com.google.android.material.button.MaterialButton[] tabs = {
                binding.btnTabAvatar, binding.btnTabTheme, binding.btnTabProp
        };

        for (int i = 0; i < tabs.length; i++) {
            boolean isActive = (i == activeTab);
            if (isActive) {
                tabs[i].setBackgroundColor(onSurface);
                tabs[i].setTextColor(white);
                tabs[i].setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
                tabs[i].setStrokeWidth(0);
                tabs[i].setIconTint(ColorStateList.valueOf(white));
            } else {
                tabs[i].setBackgroundColor(0);
                tabs[i].setTextColor(onSurfaceVariant);
                tabs[i].setStrokeColor(ColorStateList.valueOf(outlineVariant));
                tabs[i].setStrokeWidth(2);
                tabs[i].setIconTint(ColorStateList.valueOf(onSurfaceVariant));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Grid spacing item decoration for equal spacing between grid items.
     */
    public static class GridSpacingItemDecoration extends androidx.recyclerview.widget.RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacingPx, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacingPx;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                  @NonNull View view,
                                  @NonNull androidx.recyclerview.widget.RecyclerView parent,
                                  @NonNull androidx.recyclerview.widget.RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
            }
            if (position >= spanCount) {
                outRect.top = spacing;
            }
        }
    }
}
