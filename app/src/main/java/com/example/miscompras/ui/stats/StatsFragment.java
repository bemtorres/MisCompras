package com.example.miscompras.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.CategoryStat;
import com.example.miscompras.data.local.model.ProductStat;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** Panel de estadísticas de gasto y hábitos de compra. */
public class StatsFragment extends Fragment {

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    private StatsViewModel viewModel;
    private WeekChartView weekChart;
    private LinearLayout topProducts;
    private LinearLayout categories;
    private LinearLayout notFound;
    private View emptyState;
    private TextView monthValue;
    private TextView monthCaption;
    private TextView monthDelta;
    private TextView ticketValue;
    private TextView purchasesValue;
    private TextView discountsValue;
    private TextView topProductsEmpty;
    private TextView categoriesEmpty;
    private TextView notFoundEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        weekChart = view.findViewById(R.id.week_chart);
        topProducts = view.findViewById(R.id.top_products);
        categories = view.findViewById(R.id.categories);
        notFound = view.findViewById(R.id.not_found);
        emptyState = view.findViewById(R.id.empty_state);
        monthValue = view.findViewById(R.id.month_value);
        monthCaption = view.findViewById(R.id.month_caption);
        monthDelta = view.findViewById(R.id.month_delta);
        ticketValue = view.findViewById(R.id.ticket_value);
        purchasesValue = view.findViewById(R.id.purchases_value);
        discountsValue = view.findViewById(R.id.discounts_value);
        topProductsEmpty = view.findViewById(R.id.top_products_empty);
        categoriesEmpty = view.findViewById(R.id.categories_empty);
        notFoundEmpty = view.findViewById(R.id.not_found_empty);

        viewModel.getStats().observe(getViewLifecycleOwner(), this::bind);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.reload();
    }

    private void bind(StatsSnapshot stats) {
        if (stats == null) {
            return;
        }
        boolean hasHistory = stats.hasHistory();
        emptyState.setVisibility(hasHistory ? View.GONE : View.VISIBLE);

        monthValue.setText(Money.format(stats.monthTotal));
        monthCaption.setText(com.example.miscompras.utils.Dates.capitalize(stats.monthLabel));
        double delta = stats.monthDelta();
        monthDelta.setText(delta == 0d
                ? getString(R.string.stats_no_change)
                : getString(R.string.stats_month_delta, Money.signed(delta),
                        com.example.miscompras.domain.Pricing.formatPercent(stats.monthDeltaPercent())));
        monthDelta.setTextColor(Ui.color(requireContext(), delta <= 0d
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        ticketValue.setText(Money.format(stats.averagePurchase));
        purchasesValue.setText(String.valueOf(stats.completedPurchases));
        discountsValue.setText(Money.format(stats.discountsThisMonth));

        weekChart.setBuckets(stats.weeks);

        topProducts.removeAllViews();
        double maxQuantity = Math.max(1d, stats.maxTopProductQuantity());
        for (ProductStat stat : stats.topProducts) {
            topProducts.addView(statRow(stat.name,
                    getString(R.string.meta_top_product, Money.quantity(stat.quantity), Money.format(stat.total)),
                    stat.quantity / maxQuantity, false));
        }
        topProductsEmpty.setVisibility(stats.topProducts.isEmpty() ? View.VISIBLE : View.GONE);

        categories.removeAllViews();
        double maxCategory = Math.max(1d, stats.maxCategoryTotal());
        for (CategoryStat stat : stats.categories) {
            categories.addView(statRow(stat.name, Money.format(stat.total), stat.total / maxCategory, true));
        }
        categoriesEmpty.setVisibility(stats.categories.isEmpty() ? View.VISIBLE : View.GONE);

        notFound.removeAllViews();
        double maxNotFound = 1d;
        for (ProductStat stat : stats.notFound) {
            maxNotFound = Math.max(maxNotFound, stat.quantity);
        }
        for (ProductStat stat : stats.notFound) {
            notFound.addView(statRow(stat.name,
                    getString(R.string.meta_not_found_times, (int) stat.quantity),
                    stat.quantity / maxNotFound, false));
        }
        notFoundEmpty.setVisibility(stats.notFound.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View statRow(String name, String value, double ratio, boolean tintPrimary) {
        View row = getLayoutInflater().inflate(R.layout.item_stat_row, topProducts, false);
        ((TextView) row.findViewById(R.id.stat_name)).setText(name);
        ((TextView) row.findViewById(R.id.stat_value)).setText(value);
        LinearProgressIndicator progress = row.findViewById(R.id.stat_progress);
        progress.setProgress((int) Math.round(Math.max(0d, Math.min(1d, ratio)) * 100d));
        if (tintPrimary) {
            progress.setIndicatorColor(Ui.color(requireContext(), androidx.appcompat.R.attr.colorPrimary));
        }
        return row;
    }
}
