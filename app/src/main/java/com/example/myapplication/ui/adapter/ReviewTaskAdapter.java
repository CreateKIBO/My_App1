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
import com.example.myapplication.data.local.ReviewTaskEntity;
import com.example.myapplication.databinding.ItemReviewTaskBinding;
import com.example.myapplication.util.AnimUtils;

import java.util.ArrayList;
import java.util.List;

public class ReviewTaskAdapter extends RecyclerView.Adapter<ReviewTaskAdapter.ViewHolder> {

    public interface OnReviewClickListener {
        void onReviewClick(ReviewTaskEntity item);
    }

    private List<ReviewTaskEntity> items = new ArrayList<>();
    private final OnReviewClickListener listener;

    public ReviewTaskAdapter(OnReviewClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ReviewTaskEntity> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewTaskBinding binding = ItemReviewTaskBinding.inflate(
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
        private final ItemReviewTaskBinding binding;

        ViewHolder(ItemReviewTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            AnimUtils.scaleOnClick(itemView);
        }

        void bind(ReviewTaskEntity item) {
            binding.tvReviewTitle.setText(item.getTitle());
            binding.tvReviewStep.setText("第" + item.getStep() + "次复习");

            GradientDrawable stepBg = new GradientDrawable();
            stepBg.setShape(GradientDrawable.RECTANGLE);
            stepBg.setCornerRadius(9999);
            stepBg.setColor(0xFFE8EDFF);
            binding.tvReviewStep.setBackground(stepBg);

            binding.btnReviewNow.setOnClickListener(v -> listener.onReviewClick(item));
            AnimUtils.scaleOnClick(binding.btnReviewNow);
        }
    }
}
