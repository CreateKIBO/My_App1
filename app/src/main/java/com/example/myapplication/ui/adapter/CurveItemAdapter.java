package com.example.myapplication.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.ForgettingCurveItemEntity;
import com.example.myapplication.databinding.ItemCurveBinding;
import com.example.myapplication.util.RewardCalculator;

import java.util.ArrayList;
import java.util.List;

public class CurveItemAdapter extends RecyclerView.Adapter<CurveItemAdapter.ViewHolder> {

    private List<ForgettingCurveItemEntity> items = new ArrayList<>();

    public void submitList(List<ForgettingCurveItemEntity> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCurveBinding binding = ItemCurveBinding.inflate(
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
        private final ItemCurveBinding binding;

        ViewHolder(ItemCurveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ForgettingCurveItemEntity item) {
            binding.tvItemTitle.setText(item.getTitle());

            int totalSteps = RewardCalculator.REVIEW_INTERVALS.length;
            int completed = item.getReviewStep();
            binding.tvItemProgress.setText(completed + "/" + totalSteps);

            // Progress bar
            float ratio = totalSteps > 0 ? completed / (float) totalSteps : 0f;
            binding.vProgressFill.post(() -> {
                int barWidth = binding.vProgressTrack.getWidth();
                if (barWidth > 0) {
                    binding.vProgressFill.getLayoutParams().width = (int) (ratio * barWidth);
                    binding.vProgressFill.requestLayout();
                }
            });

            // Next review date
            if (item.isMastered()) {
                binding.tvNextReview.setText("已掌握 ✓");
                binding.tvNextReview.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.xp_green));
            } else {
                binding.tvNextReview.setText("下次: " + item.getNextReviewDate());
                binding.tvNextReview.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_on_surface_variant));
            }

            // Status tag
            if (item.isMastered()) {
                binding.tvStatusTag.setVisibility(View.VISIBLE);
                binding.tvStatusTag.setText("已掌握");
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.xp_green));
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setShape(GradientDrawable.RECTANGLE);
                tagBg.setCornerRadius(6);
                tagBg.setColor(0xFFDCFCE7);
                binding.tvStatusTag.setBackground(tagBg);
            } else {
                binding.tvStatusTag.setVisibility(View.VISIBLE);
                binding.tvStatusTag.setText("复习中");
                binding.tvStatusTag.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_primary));
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setShape(GradientDrawable.RECTANGLE);
                tagBg.setCornerRadius(6);
                tagBg.setColor(0xFFE8EDFF);
                binding.tvStatusTag.setBackground(tagBg);
            }
        }
    }
}
