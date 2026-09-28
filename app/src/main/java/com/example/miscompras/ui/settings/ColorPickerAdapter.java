package com.example.miscompras.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.utils.AppPreferences;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder> {

    public interface OnColorSelectedListener {
        void onColorSelected(AppPreferences.ColorThemeOption option);
    }

    private final List<AppPreferences.ColorThemeOption> items;
    private final OnColorSelectedListener listener;
    private String selectedKey;

    public ColorPickerAdapter(List<AppPreferences.ColorThemeOption> items, String selectedKey, OnColorSelectedListener listener) {
        this.items = items;
        this.selectedKey = selectedKey;
        this.listener = listener;
    }

    public void setSelectedKey(String key) {
        this.selectedKey = key;
        notifyDataSetChanged();
    }

    public String getSelectedKey() {
        return selectedKey;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_circle, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        AppPreferences.ColorThemeOption option = items.get(position);
        holder.bind(option, option.key.equalsIgnoreCase(selectedKey), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView outerCard;
        private final MaterialCardView innerCard;
        private final ImageView checkIcon;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            outerCard = itemView.findViewById(R.id.theme_circle_outer);
            innerCard = itemView.findViewById(R.id.theme_circle_inner);
            checkIcon = itemView.findViewById(R.id.theme_check_icon);
        }

        void bind(AppPreferences.ColorThemeOption option, boolean isSelected, OnColorSelectedListener listener) {
            Context context = itemView.getContext();
            int color = ContextCompat.getColor(context, option.colorRes);

            innerCard.setCardBackgroundColor(color);

            if (isSelected) {
                outerCard.setStrokeWidth((int) (3 * context.getResources().getDisplayMetrics().density));
                outerCard.setStrokeColor(color);
                outerCard.setCardBackgroundColor(Color.TRANSPARENT);
                checkIcon.setVisibility(View.VISIBLE);
            } else {
                outerCard.setStrokeWidth(0);
                outerCard.setCardBackgroundColor(Color.TRANSPARENT);
                checkIcon.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(option);
                }
            });
        }
    }
}
