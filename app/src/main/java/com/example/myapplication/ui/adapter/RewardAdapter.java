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
import com.example.myapplication.data.local.RewardTransactionEntity;
import com.example.myapplication.util.RewardCalculator;

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

        private final ImageView ivIcon;
        private final TextView tvAmount;
        private final TextView tvReason;

        public RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_reward_icon);
            tvAmount = itemView.findViewById(R.id.tv_reward_amount);
            tvReason = itemView.findViewById(R.id.tv_reward_reason);
        }

        public void bind(RewardTransactionEntity tx) {
            String type = tx.getType();
            int amount = tx.getAmount();

            if (RewardCalculator.TX_COIN.equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_coin);
                tvAmount.setText("+" + amount);
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.coin_gold));
            } else if (RewardCalculator.TX_XP.equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_xp);
                tvAmount.setText("+" + amount + " XP");
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.xp_green));
            } else if (RewardCalculator.TX_LEVEL_UP.equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_streak);
                tvAmount.setText("+" + amount);
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.streak_orange));
            } else if (RewardCalculator.TX_SPEND.equals(type)) {
                ivIcon.setImageResource(R.drawable.ic_coin);
                tvAmount.setText(String.valueOf(amount));
                tvAmount.setTextColor(itemView.getContext().getColor(R.color.md_error));
            } else {
                ivIcon.setImageResource(R.drawable.ic_coin);
                tvAmount.setText(String.valueOf(amount));
            }

            tvReason.setText(tx.getReason() != null ? tx.getReason() : "");
        }
    }
}