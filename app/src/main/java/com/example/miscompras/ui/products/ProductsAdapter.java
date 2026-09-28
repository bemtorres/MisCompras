package com.example.miscompras.ui.products;

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
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.google.android.material.chip.Chip;

/** Fila del catálogo con precio, stock y favoritos. */
public class ProductsAdapter extends ListAdapter<ProductRow, ProductsAdapter.ViewHolder> {

    public interface Listener {
        void onClick(ProductRow row);

        void onFavorite(ProductRow row);

        void onStock(ProductRow row, double delta);
    }

    private final Listener listener;

    public ProductsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
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
                    && oldItem.minimumStock == newItem.minimumStock
                    && oldItem.isFavorite == newItem.isFavorite
                    && oldItem.isActive == newItem.isActive;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView detail;
        private final TextView price;
        private final TextView stockValue;
        private final Chip stockChip;
        private final ImageButton favoriteButton;
        private final TextView avatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.product_name);
            detail = itemView.findViewById(R.id.product_detail);
            price = itemView.findViewById(R.id.product_price);
            stockValue = itemView.findViewById(R.id.product_stock_value);
            stockChip = itemView.findViewById(R.id.product_stock_chip);
            favoriteButton = itemView.findViewById(R.id.product_favorite);
            avatar = itemView.findViewById(R.id.product_avatar);
        }

        void bind(ProductRow row) {
            name.setText(row.name);
            String icon = (row.icon != null && !row.icon.trim().isEmpty()) ? row.icon : (row.name.isEmpty() ? "🛍️" : row.name.substring(0, 1).toUpperCase());
            avatar.setText(icon);
            avatar.setBackground(Ui.circle(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorPrimaryContainer)));
            avatar.setTextColor(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorOnPrimaryContainer));

            StringBuilder detailText = new StringBuilder();
            if (!row.categoryName.isEmpty()) {
                detailText.append(row.categoryName);
            }
            if (row.purchases > 0) {
                if (detailText.length() > 0) {
                    detailText.append(" · ");
                }
                detailText.append(itemView.getContext().getString(R.string.meta_purchases_times, row.purchases));
            }
            if (row.averagePrice != null) {
                if (detailText.length() > 0) {
                    detailText.append(" · ");
                }
                detailText.append(getString(R.string.meta_average_price, Money.format(row.averagePrice)));
            }
            detail.setText(detailText);

            price.setText(Money.format(row.estimatedPrice) + " / " + row.unit);
            stockValue.setText(Money.quantity(row.currentStock) + " " + row.unit);
            if (row.isLowStock()) {
                stockChip.setVisibility(View.VISIBLE);
                stockChip.setText(R.string.label_restock);
            } else {
                stockChip.setVisibility(View.GONE);
            }
            favoriteButton.setImageResource(row.isFavorite ? R.drawable.ic_star : R.drawable.ic_star_border);
            favoriteButton.setContentDescription(itemView.getContext()
                    .getString(row.isFavorite ? R.string.label_favorite : R.string.action_add));

            itemView.setOnClickListener(v -> listener.onClick(row));
            favoriteButton.setOnClickListener(v -> listener.onFavorite(row));
            itemView.findViewById(R.id.product_minus).setOnClickListener(v -> listener.onStock(row, -1d));
            itemView.findViewById(R.id.product_plus).setOnClickListener(v -> listener.onStock(row, 1d));
        }

        private String getString(int resId, Object arg) {
            return itemView.getContext().getString(resId, arg);
        }
    }
}
