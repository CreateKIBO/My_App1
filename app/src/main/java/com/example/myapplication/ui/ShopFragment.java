package com.example.myapplication.ui;

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
import com.example.myapplication.data.local.UserEntity;
import com.example.myapplication.databinding.FragmentShopBinding;
import com.example.myapplication.ui.adapter.ShopItemAdapter;
import com.example.myapplication.util.RewardCalculator;

public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private ShopViewModel viewModel;
    private ShopItemAdapter adapter;

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

        // Grid layout: 2 columns
        adapter = new ShopItemAdapter(new ShopItemAdapter.OnShopActionListener() {
            @Override
            public void onBuyClick(ShopItemEntity item) {
                viewModel.purchaseItem(item.getId(), item.getPrice());
            }

            @Override
            public void onEquipClick(ShopItemEntity item) {
                viewModel.equipItem(item.getId(), item.getType());
            }
        });

        binding.rvShopItems.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvShopItems.setAdapter(adapter);

        // Type tabs
        binding.chipAvatar.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) viewModel.setSelectedType(RewardCalculator.TYPE_AVATAR);
        });
        binding.chipTheme.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) viewModel.setSelectedType(RewardCalculator.TYPE_THEME);
        });

        // Observe data
        viewModel.getShopItems().observe(getViewLifecycleOwner(), items -> adapter.submitList(items));
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) binding.tvCoins.setText(String.valueOf(user.getCurrentCoins()));
        });
        viewModel.getCurrentOwnedIds().observe(getViewLifecycleOwner(), ids -> adapter.setOwnedIds(ids));
        viewModel.getCurrentEquippedId().observe(getViewLifecycleOwner(), id -> adapter.setEquippedId(id));
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
