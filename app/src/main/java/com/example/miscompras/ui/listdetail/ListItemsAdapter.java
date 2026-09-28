package com.example.miscompras.ui.listdetail;

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
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.NotFoundReason;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;

/** Fila de producto dentro del detalle de una lista. */
public class ListItemsAdapter extends ListAdapter<ItemRow, ListItemsAdapter.ViewHolder> {

    public interface Listener {
        void onClick(ItemRow row);

        void onEdit(ItemRow row);

        void onTogglePurchased(ItemRow row);
    }

    private final Listener listener;

    public ListItemsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ItemRow> DIFF = new DiffUtil.ItemCallback<ItemRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull ItemRow oldItem, @NonNull ItemRow newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ItemRow oldItem, @NonNull ItemRow newItem) {
            return oldItem.isPurchased == newItem.isPurchased
                    && oldItem.notFound == newItem.notFound
                    && oldItem.quantity == newItem.quantity
                    && oldItem.estimatedUnitPrice == newItem.estimatedUnitPrice
                    && sameNullable(oldItem.realUnitPrice, newItem.realUnitPrice)
                    && oldItem.discountType.equals(newItem.discountType)
                    && oldItem.discountValue == newItem.discountValue
                    && oldItem.substituteName.equals(newItem.substituteName)
                    && oldItem.notFoundReason.equals(newItem.notFoundReason)
                    && oldItem.position == newItem.position;
        }

        private boolean sameNullable(Double first, Double second) {
            if (first == null || second == null) {
                return first == second;
            }
            return first.doubleValue() == second.doubleValue();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox checkBox;
        private final TextView avatar;
        private final TextView name;
        private final TextView detail;
        private final TextView subtotal;
        private final TextView statusChip;
        private final TextView realPrice;
        private final ImageButton edit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.item_checkbox);
            avatar = itemView.findViewById(R.id.item_avatar);
            name = itemView.findViewById(R.id.item_name);
            detail = itemView.findViewById(R.id.item_detail);
            subtotal = itemView.findViewById(R.id.item_subtotal);
            statusChip = itemView.findViewById(R.id.item_status_chip);
            realPrice = itemView.findViewById(R.id.item_real_price);
            edit = itemView.findViewById(R.id.item_edit);
        }

        void bind(ItemRow row) {
            name.setText(row.productName);
            String icon = (row.productIcon != null && !row.productIcon.trim().isEmpty()) ? row.productIcon : (row.productName.isEmpty() ? "🛍️" : row.productName.substring(0, 1).toUpperCase());
            avatar.setText(icon);
            avatar.setBackground(Ui.circle(Ui.color(itemView.getContext(),
                    com.google.android.material.R.attr.colorPrimaryContainer)));
            avatar.setTextColor(Ui.color(itemView.getContext(),
                    com.google.android.material.R.attr.colorOnPrimaryContainer));

            StringBuilder detailText = new StringBuilder();
            detailText.append(Money.quantity(row.quantity)).append(" ");
            if (!row.categoryName.isEmpty()) {
                detailText.append("· ").append(row.categoryName);
            }
            detail.setText(detailText);

            boolean registered = row.isPurchased || row.notFound;
            checkBox.setChecked(registered);
            if (row.isPurchased && row.realSubtotal > 0d) {
                subtotal.setText(Money.format(row.realSubtotal));
                subtotal.setVisibility(View.VISIBLE);
            } else {
                subtotal.setVisibility(View.GONE);
            }

            if (row.isPurchased) {
                statusChip.setText(R.string.status_purchased);
                statusChip.setBackgroundResource(R.drawable.bg_badge_success);
                statusChip.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.app_on_success_container));
                Double finalPrice = row.finalUnitPrice == null ? row.realUnitPrice : row.finalUnitPrice;
                StringBuilder priceText = new StringBuilder(Money.format(finalPrice == null ? 0d : finalPrice));
                DiscountType type = DiscountType.fromName(row.discountType);
                if (row.discountValue > 0d && type != DiscountType.NONE) {
                    priceText.append("  ↓")
                            .append(type == DiscountType.PERCENT
                                     ? Money.plain(row.discountValue) + "%"
                                     : Money.format(row.discountValue));
                }
                if (!row.substituteName.isEmpty()) {
                    priceText.append("  → ").append(row.substituteName);
                }
                realPrice.setText(priceText);
                realPrice.setVisibility(View.VISIBLE);
            } else if (row.notFound) {
                NotFoundReason reason = NotFoundReason.fromName(
                        row.notFoundReason.isEmpty() ? null : row.notFoundReason);
                statusChip.setText(reason == null
                        ? itemView.getContext().getString(R.string.status_not_found)
                        : reason.getLabel());
                statusChip.setBackgroundResource(R.drawable.bg_badge_error);
                statusChip.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.app_on_danger_container));
                realPrice.setVisibility(View.GONE);
            } else {
                statusChip.setText(R.string.status_pending);
                statusChip.setBackgroundResource(R.drawable.bg_badge_pending);
                statusChip.setTextColor(Ui.color(itemView.getContext(),
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
                Ui.visible(realPrice, row.discountValue > 0d || !row.substituteName.isEmpty());
                if (realPrice.getVisibility() == View.VISIBLE) {
                    StringBuilder priceText = new StringBuilder();
                    if (row.discountValue > 0d) {
                        priceText.append("↓").append(Money.format(row.discountValue));
                    }
                    if (!row.substituteName.isEmpty()) {
                        if (priceText.length() > 0) {
                            priceText.append("  ");
                        }
                        priceText.append("→ ").append(row.substituteName);
                    }
                    realPrice.setText(priceText);
                }
            }

            itemView.setOnClickListener(v -> listener.onClick(row));
            checkBox.setOnClickListener(v -> listener.onTogglePurchased(row));
            edit.setOnClickListener(v -> listener.onEdit(row));
        }
    }
}
