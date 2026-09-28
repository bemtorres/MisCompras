package com.example.miscompras.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;

/** Historial de compras finalizadas con su detalle. */
public class HistoryFragment extends Fragment implements HistoryAdapter.Listener {

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private View emptyState;
    private TextView monthValue;
    private TextView monthCaption;
    private TextView monthDelta;
    private TextView ticketValue;
    private TextView purchasesValue;
    private TextView discountsValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        emptyState = view.findViewById(R.id.empty_state);
        monthValue = view.findViewById(R.id.month_value);
        monthCaption = view.findViewById(R.id.month_caption);
        monthDelta = view.findViewById(R.id.month_delta);
        ticketValue = view.findViewById(R.id.ticket_value);
        purchasesValue = view.findViewById(R.id.purchases_value);
        discountsValue = view.findViewById(R.id.discounts_value);

        adapter = new HistoryAdapter(this);
        RecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getPurchases().observe(getViewLifecycleOwner(), rows -> {
            adapter.submitList(rows);
            boolean empty = rows == null || rows.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.getStats().observe(getViewLifecycleOwner(), this::bindStats);
    }

    private void bindStats(StatsSnapshot stats) {
        if (stats == null) {
            return;
        }
        monthValue.setText(Money.format(stats.monthTotal));
        monthCaption.setText(Dates.capitalize(stats.monthLabel));
        double delta = stats.monthDelta();
        monthDelta.setText(delta == 0d
                ? getString(R.string.stats_no_change)
                : getString(R.string.stats_month_delta, Money.signed(delta),
                        Pricing.formatPercent(stats.monthDeltaPercent())));
        monthDelta.setTextColor(Ui.color(requireContext(), delta <= 0d
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        ticketValue.setText(Money.format(stats.averagePurchase));
        purchasesValue.setText(String.valueOf(stats.completedPurchases));
        discountsValue.setText(Money.format(stats.discountsThisMonth));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.reload();
    }

    @Override
    public void onClick(ListSummary summary) {
        Intent intent = new Intent(requireContext(), ListDetailActivity.class);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, summary.id);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_NAME, summary.name);
        startActivity(intent);
    }
}
