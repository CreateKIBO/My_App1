package com.example.myapplication.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.databinding.ItemShopBinding;
import com.example.myapplication.util.RewardCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.ViewHolder> {

    public interface OnShopActionListener {
        void onBuyClick(ShopItemEntity item);
        void onEquipClick(ShopItemEntity item);
    }

    private List<ShopItemEntity> items = new ArrayList<>();
    private Set<Long> ownedIds = new java.util.HashSet<>();
    private long equippedId = -1;
    private final OnShopActionListener listener;

    public ShopItemAdapter(OnShopActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ShopItemEntity> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOwnedIds(Set<Long> ids) {
        ownedIds = ids != null ? ids : new java.util.HashSet<>();
        notifyDataSetChanged();
    }

    public void setEquippedId(long id) {
        equippedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShopBinding binding = ItemShopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemShopBinding binding;

        ViewHolder(ItemShopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ShopItemEntity item) {
            String type = item.getType();
            boolean isAvatar = RewardCalculator.TYPE_AVATAR.equals(type);
            boolean isTheme = RewardCalculator.TYPE_THEME.equals(type);
            boolean isProp = RewardCalculator.TYPE_PROP.equals(type);
            boolean isOwned = ownedIds.contains(item.getId());
            boolean isEquipped = equippedId == item.getId();

            int bgColor = parseColor(item.getColorHex(), 0xFF3B82F6);
            int bgColorDark = parseColor(item.getColorHexDark(), darken(bgColor));

            // Icon area
            if (isAvatar || isProp) {
                binding.layoutAvatarIcon.setVisibility(View.VISIBLE);
                binding.layoutThemePreview.setVisibility(View.GONE);

                binding.tvEmoji.setText(item.getEmoji() != null ? item.getEmoji() : "");

                GradientDrawable avatarBg = new GradientDrawable();
                avatarBg.setShape(GradientDrawable.RECTANGLE);
                avatarBg.setCornerRadius(16);
                avatarBg.setColor(bgColor);
                binding.vAvatarBg.setBackground(avatarBg);
            } else if (isTheme) {
                binding.layoutAvatarIcon.setVisibility(View.GONE);
                binding.layoutThemePreview.setVisibility(View.VISIBLE);

                GradientDrawable themeBg = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{bgColorDark, bgColor}
                );
                themeBg.setCornerRadius(12);
                binding.vThemeBg.setBackground(themeBg);

                GradientDrawable c1 = new GradientDrawable();
                c1.setShape(GradientDrawable.OVAL);
                c1.setColor(bgColor);
                c1.setAlpha(80);
                binding.vThemeCircle1.setBackground(c1);

                GradientDrawable c2 = new GradientDrawable();
                c2.setShape(GradientDrawable.OVAL);
                c2.setColor(bgColor);
                c2.setAlpha(80);
                binding.vThemeCircle2.setBackground(c2);
            }

            // Name & description
            binding.tvItemName.setText(item.getName());
            binding.tvItemDesc.setText(item.getDescription());

            // Price / status badge area — always visible to keep card height consistent
            if (isEquipped) {
                // Equipped: show "装备中" badge in price area
                binding.layoutPrice.setVisibility(View.GONE);
                binding.tvFreeTag.setVisibility(View.VISIBLE);
                binding.tvFreeTag.setText("装备中");
                binding.tvFreeTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.xp_green));
                GradientDrawable ownedBg = new GradientDrawable();
                ownedBg.setShape(GradientDrawable.RECTANGLE);
                ownedBg.setCornerRadius(9999);
                ownedBg.setColor(0xFFDCFCE7);
                binding.tvFreeTag.setBackground(ownedBg);
            } else if (isOwned && !isProp) {
                // Owned: show "已拥有" badge in price area
                binding.layoutPrice.setVisibility(View.GONE);
                binding.tvFreeTag.setVisibility(View.VISIBLE);
                binding.tvFreeTag.setText("已拥有");
                binding.tvFreeTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_primary));
                GradientDrawable ownedBg = new GradientDrawable();
                ownedBg.setShape(GradientDrawable.RECTANGLE);
                ownedBg.setCornerRadius(9999);
                ownedBg.setColor(0xFFE8EAF6);
                binding.tvFreeTag.setBackground(ownedBg);
            } else if (isProp || !isOwned) {
                // Not owned or prop: show price
                if (item.getPrice() > 0) {
                    binding.layoutPrice.setVisibility(View.VISIBLE);
                    binding.tvFreeTag.setVisibility(View.GONE);
                    binding.tvItemPrice.setText(String.valueOf(item.getPrice()));
                } else {
                    binding.layoutPrice.setVisibility(View.GONE);
                    binding.tvFreeTag.setVisibility(View.VISIBLE);
                    binding.tvFreeTag.setText("免费");
                    binding.tvFreeTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.coin_gold_dark));
                    binding.tvFreeTag.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_price_badge));
                }
            }

            // Corner status tag — only show for locked items
            binding.tvStatusTag.setVisibility(View.GONE);
            if (!isOwned && !isProp) {
                binding.tvStatusTag.setVisibility(View.VISIBLE);
                binding.tvStatusTag.setText("未解锁");
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_on_surface_variant));
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setShape(GradientDrawable.RECTANGLE);
                tagBg.setCornerRadius(6);
                tagBg.setColor(0xFFF1F1F1);
                binding.tvStatusTag.setBackground(tagBg);
            }

            // Click handling
            itemView.setOnClickListener(v -> {
                if (isProp) {
                    listener.onBuyClick(item);
                } else if (isOwned && !isEquipped) {
                    listener.onEquipClick(item);
                } else if (!isOwned) {
                    listener.onBuyClick(item);
                }
            });
        }

        private int parseColor(String hex, int fallback) {
            try {
                return Color.parseColor(hex);
            } catch (Exception e) {
                return fallback;
            }
        }

        private int darken(int color) {
            float factor = 0.7f;
            int r = (int) (((color >> 16) & 0xFF) * factor);
            int g = (int) (((color >> 8) & 0xFF) * factor);
            int b = (int) ((color & 0xFF) * factor);
            return (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
