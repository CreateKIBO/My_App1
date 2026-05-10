package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.util.RewardCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RewardAdapter extends ListAdapter<RewardTransactionEntity, RewardAdapter.RewardViewHolder> {

    public RewardAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<RewardTransactionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RewardTransactionEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull RewardTransactionEntity oldItem, @NonNull RewardTransactionEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull RewardTransactionEntity oldItem, @NonNull RewardTransactionEntity newItem) {
            return oldItem.getType().equals(newItem.getType()) && oldItem.getAmount() == newItem.getAmount();
        }
    };

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvIcon;
        private final TextView tvName;
        private final TextView tvDesc;
        private final TextView tvAmount;
        private final TextView tvTime;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_reward_icon);
            tvName = itemView.findViewById(R.id.tv_reward_name);
            tvDesc = itemView.findViewById(R.id.tv_reward_desc);
            tvAmount = itemView.findViewById(R.id.tv_reward_amount);
            tvTime = itemView.findViewById(R.id.tv_reward_time);
        }

        public void bind(RewardTransactionEntity tx) {
            String type = tx.getType();
            int amount = tx.getAmount();

            if (RewardCalculator.TX_XP.equals(type)) {
                // XP type: green icon bg, bolt emoji, green amount
                tvIcon.setText("⚡");
                tvIcon.setBackgroundResource(R.drawable.bg_reward_icon_xp);
                tvAmount.setText("+" + amount + " XP");
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.xp_green));
            } else if (RewardCalculator.TX_COIN.equals(type)) {
                // Coin type: gold icon bg, cent emoji, gold-dark amount
                tvIcon.setText("¢");
                tvIcon.setBackgroundResource(R.drawable.bg_reward_icon_coin);
                tvAmount.setText("+" + amount + " ¢");
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.coin_gold_dark));
            } else if (RewardCalculator.TX_LEVEL_UP.equals(type)) {
                // Level-up type: blue icon bg, arrow emoji, blue amount
                tvIcon.setText("↑");
                tvIcon.setBackgroundResource(R.drawable.bg_reward_icon_level);
                tvAmount.setText("+" + amount + " ¢");
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.coin_gold_dark));
            } else if (RewardCalculator.TX_SPEND.equals(type)) {
                // Spend type: gold icon bg, cent emoji, red amount
                tvIcon.setText("¢");
                tvIcon.setBackgroundResource(R.drawable.bg_reward_icon_coin);
                tvAmount.setText(String.valueOf(amount));
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.md_error));
            } else {
                // Default / bonus: streak icon bg, star emoji, gold amount
                tvIcon.setText("★");
                tvIcon.setBackgroundResource(R.drawable.bg_reward_icon_streak);
                tvAmount.setText("+" + amount + " ¢");
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.coin_gold_dark));
            }

            // Name: use reason as primary text, or fallback
            String reason = tx.getReason();
            tvName.setText(reason != null ? reason : getDefaultName(type));

            // Description: show type-based detail
            tvDesc.setText(getDetailText(type, amount));

            // Time
            tvTime.setText(formatTimestamp(tx.getTimestamp()));
        }

        private String getDefaultName(String type) {
            if (RewardCalculator.TX_XP.equals(type)) return "完成任务 XP";
            if (RewardCalculator.TX_COIN.equals(type)) return "任务金币奖励";
            if (RewardCalculator.TX_LEVEL_UP.equals(type)) return "升级奖励";
            if (RewardCalculator.TX_SPEND.equals(type)) return "消费";
            return "奖励";
        }

        private String getDetailText(String type, int amount) {
            if (RewardCalculator.TX_LEVEL_UP.equals(type)) {
                return "升级奖励金币";
            }
            if (RewardCalculator.TX_SPEND.equals(type)) {
                return "商店购买";
            }
            return "";
        }

        private String formatTimestamp(long timestamp) {
            if (timestamp <= 0) return "";
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            SimpleDateFormat sdf;

            if (diff < 86400000L) {
                // Today
                sdf = new SimpleDateFormat("今天 HH:mm", Locale.CHINA);
            } else if (diff < 172800000L) {
                // Yesterday
                sdf = new SimpleDateFormat("昨天 HH:mm", Locale.CHINA);
            } else {
                // Older
                sdf = new SimpleDateFormat("M月d日", Locale.CHINA);
            }
            return sdf.format(new Date(timestamp));
        }
    }
}
