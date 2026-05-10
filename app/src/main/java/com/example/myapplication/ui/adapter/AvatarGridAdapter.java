package com.example.myapplication.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ShopItemEntity;

/**
 * ListAdapter for avatar grid items in 4-column grid.
 * Data class: AvatarGridItem wraps ShopItemEntity with owned/equipped state.
 * ViewHolder shows avatar icon, name, status tag, optional price badge, optional equipped top bar.
 */
public class AvatarGridAdapter extends ListAdapter<AvatarGridAdapter.AvatarGridItem, AvatarGridAdapter.AvatarViewHolder> {

    private final OnAvatarActionListener listener;

    public interface OnAvatarActionListener {
        void onEquipClick(ShopItemEntity item);
        void onBuyClick(ShopItemEntity item);
    }

    /**
     * Data class combining a ShopItemEntity with its owned/equipped state.
     */
    public static class AvatarGridItem {
        private final ShopItemEntity item;
        private final boolean isOwned;
        private final boolean isEquipped;

        public AvatarGridItem(ShopItemEntity item, boolean isOwned, boolean isEquipped) {
            this.item = item;
            this.isOwned = isOwned;
            this.isEquipped = isEquipped;
        }

        public ShopItemEntity getItem() { return item; }
        public boolean isOwned() { return isOwned; }
        public boolean isEquipped() { return isEquipped; }
    }

    public AvatarGridAdapter(OnAvatarActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<AvatarGridItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AvatarGridItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull AvatarGridItem oldItem, @NonNull AvatarGridItem newItem) {
            return oldItem.getItem().getId() == newItem.getItem().getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AvatarGridItem oldItem, @NonNull AvatarGridItem newItem) {
            return oldItem.getItem().getId() == newItem.getItem().getId()
                    && oldItem.isOwned() == newItem.isOwned()
                    && oldItem.isEquipped() == newItem.isEquipped();
        }
    };

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_grid, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class AvatarViewHolder extends RecyclerView.ViewHolder {

        private final com.google.android.material.card.MaterialCardView cardView;
        private final View vEquippedTopBar;
        private final FrameLayout layoutAvatarIcon;
        private final View vAvatarBg;
        private final TextView tvEmoji;
        private final TextView tvAvatarName;
        private final TextView tvStatusTag;
        private final LinearLayout layoutPrice;
        private final View vMiniCoin;
        private final TextView tvPrice;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            vEquippedTopBar = itemView.findViewById(R.id.v_equipped_top_bar);
            layoutAvatarIcon = itemView.findViewById(R.id.layout_avatar_icon);
            vAvatarBg = itemView.findViewById(R.id.v_avatar_bg);
            tvEmoji = itemView.findViewById(R.id.tv_emoji);
            tvAvatarName = itemView.findViewById(R.id.tv_avatar_name);
            tvStatusTag = itemView.findViewById(R.id.tv_status_tag);
            layoutPrice = itemView.findViewById(R.id.layout_price);
            vMiniCoin = itemView.findViewById(R.id.v_mini_coin);
            tvPrice = itemView.findViewById(R.id.tv_price);
        }

        public void bind(AvatarGridItem gridItem) {
            ShopItemEntity item = gridItem.getItem();
            boolean isOwned = gridItem.isOwned();
            boolean isEquipped = gridItem.isEquipped();

            tvAvatarName.setText(item.getName());

            // Parse color hex
            int itemColor;
            try {
                itemColor = Color.parseColor(item.getColorHex());
            } catch (Exception e) {
                itemColor = itemView.getContext().getColor(R.color.md_primary);
            }

            int lightColor = lightenColor(itemColor, 0.65f);

            // Avatar icon background
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.RECTANGLE);
            avatarBg.setCornerRadius(12f);
            avatarBg.setColor(lightColor);
            vAvatarBg.setBackground(avatarBg);

            // Emoji
            String emoji = getEmojiForAvatar(item.getIconResName());
            tvEmoji.setText(emoji);

            int accentColor = itemView.getContext().getColor(R.color.md_primary);

            if (isEquipped) {
                // Equipped state: blue border, top bar, "装备中" tag
                cardView.setStrokeColor(accentColor);
                cardView.setStrokeWidth(2);
                vEquippedTopBar.setVisibility(View.VISIBLE);
                vEquippedTopBar.setBackgroundColor(accentColor);

                tvStatusTag.setVisibility(View.VISIBLE);
                tvStatusTag.setText("装备中");
                tvStatusTag.setBackground(itemView.getContext().getDrawable(R.drawable.bg_tag_equipped));
                tvStatusTag.setTextColor(itemView.getContext().getColor(R.color.tag_equipped_text));
                layoutPrice.setVisibility(View.GONE);

                itemView.setOnClickListener(null);
                itemView.setAlpha(1.0f);
            } else if (isOwned) {
                // Owned state: "点击装备" tag
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.md_outline_variant));
                cardView.setStrokeWidth(1);
                vEquippedTopBar.setVisibility(View.GONE);

                tvStatusTag.setVisibility(View.VISIBLE);
                tvStatusTag.setText("点击装备");
                tvStatusTag.setBackground(itemView.getContext().getDrawable(R.drawable.bg_tag_owned));
                tvStatusTag.setTextColor(itemView.getContext().getColor(R.color.tag_owned_text));
                layoutPrice.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onEquipClick(item);
                });
                itemView.setAlpha(1.0f);
            } else {
                // Locked state: "未解锁" tag, price badge, 0.55 opacity
                cardView.setStrokeColor(itemView.getContext().getColor(R.color.md_outline_variant));
                cardView.setStrokeWidth(1);
                vEquippedTopBar.setVisibility(View.GONE);

                tvStatusTag.setVisibility(View.VISIBLE);
                tvStatusTag.setText("未解锁");
                tvStatusTag.setBackground(itemView.getContext().getDrawable(R.drawable.bg_tag_locked));
                tvStatusTag.setTextColor(itemView.getContext().getColor(R.color.tag_locked_text));

                // Price badge
                if (item.getPrice() > 0) {
                    layoutPrice.setVisibility(View.VISIBLE);
                    tvPrice.setText(String.valueOf(item.getPrice()));
                } else {
                    layoutPrice.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onBuyClick(item);
                });
                itemView.setAlpha(0.55f);
            }
        }

        /**
         * Map avatar icon resource names to emoji characters matching the HTML mockup.
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

        /**
         * Lighten a color by mixing it with white.
         * @param factor 0 = no change, 1 = fully white
         */
        private int lightenColor(int color, float factor) {
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            r = Math.round(r + (255 - r) * factor);
            g = Math.round(g + (255 - g) * factor);
            b = Math.round(b + (255 - b) * factor);
            return Color.rgb(r, g, b);
        }
    }
}