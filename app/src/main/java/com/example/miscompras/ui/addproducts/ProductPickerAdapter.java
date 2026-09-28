package com.example.miscompras.ui.addproducts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.google.android.material.checkbox.MaterialCheckBox;

/** Lista de productos del catálogo con selección y cantidad, para agregar a una lista. */
public class ProductPickerAdapter extends ListAdapter<ProductRow, ProductPickerAdapter.ViewHolder> {

    public interface Listener {
        void onToggle(ProductRow row, boolean selected);

        void onQuantity(ProductRow row, double quantity);
    }

    private final Listener listener;
    private final AddProductsViewModel viewModel;

    public ProductPickerAdapter(Listener listener, AddProductsViewModel viewModel) {
        super(DIFF);
        this.listener = listener;
        this.viewModel = viewModel;
    }

    private static final DiffUtil.ItemCallback<ProductRow> DIFF = new DiffUtil.ItemCallback<ProductRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductRow oldItem, @NonNull ProductRow newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductRow oldItem, @NonNull ProductRow newItem) {
            return oldItem.estimatedPrice == newItem.estimatedPrice
                    && oldItem.currentStock == newItem.currentStock
                    && oldItem.isFavorite == newItem.isFavorite;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox checkBox;
        private final TextView name;
        private final TextView detail;
        private final TextView quantityValue;
        private final View stepper;
        private final TextView avatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.pick_checkbox);
            name = itemView.findViewById(R.id.pick_name);
            detail = itemView.findViewById(R.id.pick_detail);
            quantityValue = itemView.findViewById(R.id.pick_quantity_value);
            stepper = itemView.findViewById(R.id.pick_stepper);
            avatar = itemView.findViewById(R.id.pick_avatar);
        }

        void bind(ProductRow row) {
            boolean selected = viewModel.isSelected(row.id);
            double quantity = viewModel.quantityOf(row.id);

            name.setText(row.name);
            checkBox.setChecked(selected);
            String icon = (row.icon != null && !row.icon.trim().isEmpty()) ? row.icon : (row.name.isEmpty() ? "🛍️" : row.name.substring(0, 1).toUpperCase());
            avatar.setText(icon);
            avatar.setBackground(Ui.circle(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorPrimaryContainer)));
            avatar.setTextColor(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorOnPrimaryContainer));

            detail.setText(row.categoryName != null ? row.categoryName : "");

            Ui.visible(stepper, selected);
            if (selected) {
                quantityValue.setText(Money.quantity(quantity));
            }
            itemView.setOnClickListener(v -> listener.onToggle(row, !selected));
            checkBox.setOnClickListener(v -> listener.onToggle(row, checkBox.isChecked()));
            ImageButton minus = itemView.findViewById(R.id.pick_minus);
            ImageButton plus = itemView.findViewById(R.id.pick_plus);
            minus.setOnClickListener(v -> listener.onQuantity(row, Math.max(0d, quantity - 1d)));
            plus.setOnClickListener(v -> listener.onQuantity(row, quantity + 1d));
        }

        private String getString(int resId, Object arg) {
            return itemView.getContext().getString(resId, arg);
        }
    }
}
