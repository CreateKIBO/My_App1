package com.example.myapplication.ui.adapter;

import android.graphics.Paint;
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

    public TaskAdapter(OnTaskCompleteListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TaskEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskEntity oldItem, @NonNull TaskEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskEntity oldItem, @NonNull TaskEntity newItem) {
            return oldItem.isCompleted() == newItem.isCompleted()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getStartTime() == newItem.getStartTime()
                    && oldItem.getEndTime() == newItem.getEndTime()
                    && oldItem.getCoinsEarned() == newItem.getCoinsEarned()
                    && oldItem.getXpEarned() == newItem.getXpEarned();
        }
    };

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
        private final TextView tvTaskTitle;
        private final TextView tvTaskTime;
        private final TextView tvTaskCategory;
        private final TextView tvCoinsBadge;
        private final TextView tvXpBadge;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvTaskTime = itemView.findViewById(R.id.tv_task_time);
            tvTaskCategory = itemView.findViewById(R.id.tv_task_category);
            tvCoinsBadge = itemView.findViewById(R.id.tv_coins_badge);
            tvXpBadge = itemView.findViewById(R.id.tv_xp_badge);
        }

        public void bind(TaskEntity task) {
            tvTaskTitle.setText(task.getTitle());
            tvTaskTime.setText(DateUtils.minutesToTime(task.getStartTime()) + " - " + DateUtils.minutesToTime(task.getEndTime()));
            tvTaskCategory.setText(getCategoryLabel(task.getCategory()));

            boolean completed = task.isCompleted();

            if (completed) {
                // Grey out + strikethrough
                tvTaskTitle.setAlpha(0.5f);
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTaskTime.setAlpha(0.5f);
                tvTaskCategory.setAlpha(0.5f);

                // Filled check icon, not clickable
                ivCheck.setImageResource(R.drawable.ic_check);
                ivCheck.setAlpha(1f);
                ivCheck.setClickable(false);
                ivCheck.setOnClickListener(null);

                // Show earned rewards
                tvCoinsBadge.setText("+" + task.getCoinsEarned());
                tvXpBadge.setText("+" + task.getXpEarned());
            } else {
                // Normal appearance
                tvTaskTitle.setAlpha(1f);
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                tvTaskTime.setAlpha(1f);
                tvTaskCategory.setAlpha(1f);

                // Outline check icon, clickable
                ivCheck.setImageResource(R.drawable.ic_check_outline);
                ivCheck.setAlpha(0.3f);
                ivCheck.setClickable(true);
                ivCheck.setOnClickListener(v -> {
                    if (listener != null) listener.onTaskComplete(task);
                });

                // Show potential rewards
                RewardCalculator.Reward reward = RewardCalculator.calculateTaskReward(task.getCategory());
                tvCoinsBadge.setText("+" + reward.coins);
                tvXpBadge.setText("+" + reward.xp);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTaskClicked(task);
            });
        }

        private String getCategoryLabel(String category) {
            if (RewardCalculator.CAT_WORK.equals(category)) return "工作";
            if (RewardCalculator.CAT_STUDY.equals(category)) return "学习";
            if (RewardCalculator.CAT_EXERCISE.equals(category)) return "运动";
            if (RewardCalculator.CAT_PERSONAL.equals(category)) return "个人";
            return "其他";
        }
    }
}