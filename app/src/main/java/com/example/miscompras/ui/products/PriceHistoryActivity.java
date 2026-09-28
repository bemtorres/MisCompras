package com.example.miscompras.ui.products;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.PriceRow;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/** Historial de precios de un producto con su precio estimado actual. */
public class PriceHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_PRODUCT_NAME = "product_name";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_history);
        SystemBars.pad(findViewById(R.id.history_root));

        long productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, 0L);
        String name = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(name == null ? getString(R.string.title_price_history) : name);
        toolbar.setSubtitle(R.string.title_price_history);
        toolbar.setNavigationOnClickListener(v -> finish());

        PriceAdapter adapter = new PriceAdapter();
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        View emptyState = findViewById(R.id.empty_state);
        DataGraph.products(getApplicationContext()).observePriceHistory(productId).observe(this, rows -> {
            adapter.submitList(rows);
            boolean empty = rows == null || rows.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private static class PriceAdapter extends ListAdapter<PriceRow, PriceAdapter.ViewHolder> {

        private static final DiffUtil.ItemCallback<PriceRow> DIFF = new DiffUtil.ItemCallback<PriceRow>() {
            @Override
            public boolean areItemsTheSame(@NonNull PriceRow oldItem, @NonNull PriceRow newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull PriceRow oldItem, @NonNull PriceRow newItem) {
                return oldItem.finalPrice == newItem.finalPrice
                        && oldItem.price == newItem.price
                        && oldItem.quantity == newItem.quantity;
            }
        };

        PriceAdapter() {
            super(DIFF);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_price_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView date;
            private final TextView detail;
            private final TextView price;
            private final TextView total;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.price_date);
                detail = itemView.findViewById(R.id.price_detail);
                price = itemView.findViewById(R.id.price_value);
                total = itemView.findViewById(R.id.price_total);
            }

            void bind(PriceRow row) {
                date.setText(Dates.format(row.date, Dates.NUMERIC));
                StringBuilder detailText = new StringBuilder();
                detailText.append(itemView.getContext().getString(R.string.meta_quantity_price,
                        Money.quantity(row.quantity), Money.format(row.price)));
                if (row.discount > 0d) {
                    detailText.append("  ↓").append(Money.format(row.discount));
                }
                detail.setText(detailText);
                price.setText(Money.format(row.finalPrice));
                total.setText(itemView.getContext().getString(R.string.meta_subtotal,
                        Money.format(Pricing.subtotal(row.quantity, row.finalPrice))));
                boolean cheaper = row.finalPrice < row.price;
                price.setTextColor(Ui.color(itemView.getContext(), cheaper
                        ? androidx.appcompat.R.attr.colorPrimary
                        : com.google.android.material.R.attr.colorOnSurface));
            }
        }
    }
}
