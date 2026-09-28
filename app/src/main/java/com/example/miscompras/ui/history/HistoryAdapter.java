package com.example.miscompras.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** Fila de una compra finalizada. */
public class HistoryAdapter extends ListAdapter<ListSummary, HistoryAdapter.ViewHolder> {

    public interface Listener {
        void onClick(ListSummary summary);
    }

    private final Listener listener;

    public HistoryAdapter(Listener listener) {
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
            return oldItem.realTotal == newItem.realTotal
                    && oldItem.estimatedTotal == newItem.estimatedTotal
                    && oldItem.purchasedCount == newItem.purchasedCount;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView meta;
        private final TextView total;
        private final TextView delta;
        private final Chip statusChip;
        private final LinearProgressIndicator progress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.purchase_name);
            meta = itemView.findViewById(R.id.purchase_meta);
            total = itemView.findViewById(R.id.purchase_total);
            delta = itemView.findViewById(R.id.purchase_delta);
            statusChip = itemView.findViewById(R.id.purchase_chip);
            progress = itemView.findViewById(R.id.purchase_progress);
        }

        void bind(ListSummary summary) {
            name.setText(summary.name);
            long date = summary.completedDate == null ? summary.plannedDate : summary.completedDate;
            meta.setText(itemView.getContext().getString(R.string.meta_purchase_meta,
                    Dates.format(date, Dates.SHORT_DAY_MONTH),
                    summary.itemCount,
                    summary.notFoundCount));
            total.setText(Money.format(summary.realTotal));
            double difference = summary.realTotal - summary.estimatedTotal;
            if (summary.realTotal <= 0d) {
                delta.setText(itemView.getContext().getString(R.string.meta_estimated_total, Money.format(summary.estimatedTotal)));
                delta.setTextColor(Ui.color(itemView.getContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            } else {
                delta.setText(Money.signed(difference));
                delta.setTextColor(Ui.color(itemView.getContext(), difference <= 0d
                        ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurface));
            }
            statusChip.setText(R.string.status_completed);
            progress.setMax(Math.max(summary.itemCount, 1));
            progress.setProgress(summary.purchasedCount + summary.notFoundCount);
            itemView.setOnClickListener(v -> listener.onClick(summary));
        }
    }
}
