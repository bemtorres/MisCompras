package com.example.miscompras.ui.lists;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;

public class ListsAdapter extends ListAdapter<ListSummary, ListsAdapter.ViewHolder> {

    public interface Listener {
        void onClick(ListSummary summary);

        void onLongClick(ListSummary summary);
    }

    private final Listener listener;

    public ListsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ListSummary> DIFF = new DiffUtil.ItemCallback<ListSummary>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListSummary oldItem, @NonNull ListSummary newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListSummary oldItem, @NonNull ListSummary newItem) {
            return oldItem.status.equals(newItem.status)
                    && oldItem.estimatedTotal == newItem.estimatedTotal
                    && oldItem.realTotal == newItem.realTotal
                    && oldItem.itemCount == newItem.itemCount
                    && oldItem.purchasedCount == newItem.purchasedCount;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView date;
        private final TextView status;
        private final TextView total;
        private final TextView difference;
        private final TextView progressLabel;
        private final TextView typeLabel;
        private final View progressTrack;
        private final View progressFill;
        private final ImageView icon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.list_item_name);
            date = itemView.findViewById(R.id.list_item_date);
            status = itemView.findViewById(R.id.list_item_status);
            total = itemView.findViewById(R.id.list_item_total);
            difference = itemView.findViewById(R.id.list_item_difference);
            progressLabel = itemView.findViewById(R.id.list_item_progress);
            typeLabel = itemView.findViewById(R.id.list_item_type);
            progressTrack = itemView.findViewById(R.id.list_item_progress_track);
            progressFill = itemView.findViewById(R.id.list_item_progress_fill);
            icon = itemView.findViewById(R.id.list_item_icon);
        }

        void bind(ListSummary summary) {
            ListStatus statusValue = ListStatus.fromName(summary.status);
            name.setText(summary.name);
            date.setText(Dates.capitalize(Dates.format(
                    statusValue == ListStatus.COMPLETED && summary.completedDate != null
                            ? summary.completedDate : summary.plannedDate, Dates.FULL)));
            status.setText(statusValue.getLabel());
            typeLabel.setText(ListType.fromName(summary.type).getLabel());
            total.setText(Money.format(statusValue == ListStatus.COMPLETED ? summary.realTotal : summary.estimatedTotal));
            total.setTextColor(Ui.color(itemView.getContext(),
                    statusValue == ListStatus.COMPLETED
                            ? com.google.android.material.R.attr.colorOnSurface
                            : androidx.appcompat.R.attr.colorPrimary));

            if (statusValue == ListStatus.COMPLETED) {
                double delta = Pricing.difference(summary.estimatedTotal, summary.realTotal);
                difference.setText(delta == 0d
                        ? itemView.getContext().getString(R.string.label_difference) + ": 0"
                        : itemView.getContext().getString(R.string.label_difference) + ": " + Money.signed(delta));
                difference.setTextColor(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                difference.setText(itemView.getContext().getString(R.string.label_estimated));
                difference.setTextColor(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            }

            int resolved = summary.purchasedCount + summary.notFoundCount;
            int totalItems = Math.max(summary.itemCount, 1);
            progressLabel.setText(itemView.getContext().getString(R.string.meta_progress, resolved, summary.itemCount));
            android.view.ViewGroup.LayoutParams params = progressFill.getLayoutParams();
            params.width = 0;
            progressFill.setLayoutParams(params);
            progressFill.post(() -> {
                android.view.ViewGroup.LayoutParams layoutParams = progressFill.getLayoutParams();
                int available = progressTrack.getWidth();
                float ratio = Math.min(1f, (float) resolved / (float) totalItems);
                layoutParams.width = Math.max(0, Math.round(available * ratio));
                progressFill.setLayoutParams(layoutParams);
            });
            progressTrack.setVisibility(summary.itemCount == 0 ? View.INVISIBLE : View.VISIBLE);
            icon.setImageResource(statusValue == ListStatus.COMPLETED ? R.drawable.ic_receipt : R.drawable.ic_cart);

            itemView.setOnClickListener(v -> listener.onClick(summary));
            itemView.setOnLongClickListener(v -> {
                listener.onLongClick(summary);
                return true;
            });
        }
    }
}
