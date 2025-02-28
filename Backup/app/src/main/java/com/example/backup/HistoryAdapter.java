package com.example.backup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false); // Thay tên layout phù hợp
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.historyImageView.setImageResource(item.getImageResource());
        holder.historyTitleTextView.setText(item.getTitle());
        holder.historyDescriptionTextView.setText(item.getDescription());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView historyImageView;
        TextView historyTitleTextView;
        TextView historyDescriptionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            historyImageView = itemView.findViewById(R.id.historyImageView);
            historyTitleTextView = itemView.findViewById(R.id.historyTitleTextView);
            historyDescriptionTextView = itemView.findViewById(R.id.historyDescriptionTextView);
        }
    }
}
