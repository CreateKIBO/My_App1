package com.example.myapplication.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.TaskEntity;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.RewardCalculator;

public class TaskAdapter extends ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder> {

    private final OnTaskCompleteListener listener;

    public interface OnTaskCompleteListener {
        void onTaskComplete(TaskEntity task);
        void onTaskClicked(TaskEntity task);
    }

    private static final DiffUtil.ItemCallback<TaskEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskEntity oldItem, @NonNull TaskEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull TaskEntity oldItem, @NonNull TaskEntity newItem) {
            return oldItem.isCompleted() == newItem.isCompleted()
                    && oldItem.getTitle().equals(newItem.getTitle());
        }
    };

    public TaskAdapter(OnTaskCompleteListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivCheck;
        private final TextView tvTitle;
        private final TextView tvCategoryBadge;
        private final TextView tvTime;
        private final TextView tvStartTime;
        private final TextView tvCoinsBadge;
        private final LinearLayout layoutCoins;
        private final View viewCatBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvCategoryBadge = itemView.findViewById(R.id.tv_category_badge);
            tvTime = itemView.findViewById(R.id.tv_task_time);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvCoinsBadge = itemView.findViewById(R.id.tv_coins_badge);
            layoutCoins = itemView.findViewById(R.id.layout_coins);
            viewCatBar = itemView.findViewById(R.id.view_cat_bar);
        }

        public void bind(TaskEntity task) {
            boolean done = task.isCompleted();

            // Title
            tvTitle.setText(task.getTitle());
            if (done) {
                tvTitle.setAlpha(0.45f);
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                tvTitle.setAlpha(1f);
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }

            // Checkbox
            if (done) {
                ivCheck.setImageResource(R.drawable.ic_check);
                ivCheck.setImageTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getColor(R.color.md_primary)));
            } else {
                ivCheck.setImageResource(R.drawable.ic_check_circle_outline);
                ivCheck.setImageTintList(null);
            }

            ivCheck.setOnClickListener(v -> {
                if (!done && listener != null) {
                    ivCheck.setEnabled(false);
                    listener.onTaskComplete(task);
                    ivCheck.postDelayed(() -> ivCheck.setEnabled(true), 500);
                }
            });

            // Category badge and color bar
            String catLabel;
            int badgeBgColor, badgeTextColor, barColor;
            switch (task.getCategory()) {
                case RewardCalculator.CAT_WORK:
                    catLabel = "工作";
                    badgeBgColor = 0xFFE8EDFF; badgeTextColor = 0xFF3F51B5;
                    barColor = 0xFF3F51B5;
                    break;
                case RewardCalculator.CAT_STUDY:
                    catLabel = "学习";
                    badgeBgColor = 0xFFF3E8FF; badgeTextColor = 0xFF7C3AED;
                    barColor = 0xFF7C3AED;
                    break;
                case RewardCalculator.CAT_EXERCISE:
                    catLabel = "运动";
                    badgeBgColor = 0xFFDCFCE7; badgeTextColor = 0xFF16A34A;
                    barColor = 0xFF16A34A;
                    break;
                case RewardCalculator.CAT_PERSONAL:
                    catLabel = "个人";
                    badgeBgColor = 0xFFFFF7ED; badgeTextColor = 0xFFEA580C;
                    barColor = 0xFFEA580C;
                    break;
                default:
                    catLabel = "其他";
                    badgeBgColor = 0xFFF5F5F5; badgeTextColor = 0xFF757575;
                    barColor = 0xFF757575;
                    break;
            }
            tvCategoryBadge.setText(catLabel);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(itemView.getContext().getResources().getDisplayMetrics().density * 9999);
            badgeBg.setColor(badgeBgColor);
            tvCategoryBadge.setBackground(badgeBg);
            tvCategoryBadge.setTextColor(badgeTextColor);

            // Category color bar on left
            viewCatBar.setBackgroundColor(barColor);

            // Time range in meta row
            if (task.getStartTime() > 0 && task.getEndTime() > 0) {
                tvTime.setText(DateUtils.minutesToTime(task.getStartTime()) + " – " + DateUtils.minutesToTime(task.getEndTime()));
            } else if (task.getStartTime() > 0) {
                tvTime.setText(DateUtils.minutesToTime(task.getStartTime()));
            } else {
                tvTime.setText("");
            }

            // Start time on right side
            if (task.getStartTime() > 0) {
                tvStartTime.setText(DateUtils.minutesToTime(task.getStartTime()));
            } else {
                tvStartTime.setText("");
            }

            // Coins
            if (done) {
                layoutCoins.setAlpha(0.45f);
                tvCoinsBadge.setText("+" + task.getCoinsEarned());
            } else {
                layoutCoins.setAlpha(1f);
                RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(task.getCategory());
                tvCoinsBadge.setText("+" + reward.coins);
            }

            // Card click for editing
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTaskClicked(task);
            });

            // Dim completed card
            itemView.setAlpha(done ? 0.45f : 1f);
        }
    }
}
