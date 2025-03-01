package com.example.busmap.Favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.busmap.R;
import java.util.List;

public class GenericFavoriteAdapter<T> extends RecyclerView.Adapter<GenericFavoriteAdapter.ViewHolder> {
    private List<T> itemList;
    private OnFavoriteClickListener<T> onFavoriteClickListener;

    public interface OnFavoriteClickListener<T> {
        void onFavoriteClick(T item, boolean isFavorite);
    }

    public GenericFavoriteAdapter(List<T> itemList, OnFavoriteClickListener<T> listener) {
        this.itemList = itemList;
        this.onFavoriteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        T item = itemList.get(position);
        String name = "";

        if (item instanceof com.example.busmap.entities.route) {
            name = ((com.example.busmap.entities.route) item).getName();
        } else if (item instanceof com.example.busmap.entities.station) {
            name = ((com.example.busmap.entities.station) item).getName();
        }

        holder.tvItemName.setText(name);
        holder.ivFavorite.setOnClickListener(v -> {
            boolean isFavorite = true; // Xử lý logic thay đổi trạng thái yêu thích
            onFavoriteClickListener.onFavoriteClick(item, isFavorite);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        ImageView ivFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
        }
    }
}
