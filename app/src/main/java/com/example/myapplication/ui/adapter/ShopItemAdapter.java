package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ShopItemEntity;

import java.util.Set;

public class ShopItemAdapter extends ListAdapter<ShopItemEntity, ShopItemAdapter.ShopViewHolder> {

    private final OnShopActionListener listener;
    private Set<Long> ownedIds;
    private long equippedId = -1;

    public interface OnShopActionListener {
        void onBuyClick(ShopItemEntity item);
        void onEquipClick(ShopItemEntity item);
    }

    public ShopItemAdapter(OnShopActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ShopItemEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ShopItemEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ShopItemEntity oldItem, @NonNull ShopItemEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull ShopItemEntity oldItem, @NonNull ShopItemEntity newItem) {
            return oldItem.getName().equals(newItem.getName()) && oldItem.getPrice() == newItem.getPrice();
        }
    };

    public void setOwnedIds(Set<Long> ids) {
        this.ownedIds = ids;
        notifyDataSetChanged();
    }

    public void setEquippedId(long id) {
        this.equippedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ShopViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivItemIcon;
        private final TextView tvItemName;
        private final TextView tvItemPrice;
        private final com.google.android.material.button.MaterialButton btnAction;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemIcon = itemView.findViewById(R.id.iv_item_icon);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvItemPrice = itemView.findViewById(R.id.tv_item_price);
            btnAction = itemView.findViewById(R.id.btn_action);
        }

        public void bind(ShopItemEntity item) {
            tvItemName.setText(item.getName());

            // Set icon
            int resId = itemView.getContext().getResources().getIdentifier(
                    item.getIconResName(), "drawable", itemView.getContext().getPackageName());
            if (resId != 0) {
                ivItemIcon.setImageResource(resId);
            } else {
                ivItemIcon.setImageResource(R.drawable.ic_profile);
            }

            boolean isOwned = ownedIds != null && ownedIds.contains(item.getId());
            boolean isEquipped = equippedId == item.getId();

            if (isEquipped) {
                tvItemPrice.setText("已装备");
                tvItemPrice.setTextColor(itemView.getContext().getColor(R.color.md_primary));
                btnAction.setText("使用中");
                btnAction.setIcon(null);
                btnAction.setEnabled(false);
            } else if (isOwned) {
                tvItemPrice.setText("已拥有");
                tvItemPrice.setTextColor(itemView.getContext().getColor(R.color.xp_green));
                btnAction.setText("装备");
                btnAction.setIconResource(R.drawable.ic_check);
                btnAction.setEnabled(true);
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onEquipClick(item);
                });
            } else {
                tvItemPrice.setText(item.getPrice() == 0 ? "免费" : String.valueOf(item.getPrice()));
                tvItemPrice.setTextColor(itemView.getContext().getColor(R.color.coin_gold));
                btnAction.setText(item.getPrice() == 0 ? "领取" : "购买");
                btnAction.setIconResource(R.drawable.ic_coin);
                btnAction.setEnabled(true);
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onBuyClick(item);
                });
            }
        }
    }
}
