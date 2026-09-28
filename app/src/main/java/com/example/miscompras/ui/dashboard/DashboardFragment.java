package com.example.miscompras.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.MainActivity;
import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.ui.newlist.NewListActivity;
import com.example.miscompras.ui.shopping.SupermarketActivity;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** Panel de inicio: estado de la compra actual, resumen del mes y accesos rápidos. */
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    private View emptyState;
    private View activeState;
    private TextView listName;
    private TextView listMeta;
    private TextView statusChip;
    private TextView estimatedValue;
    private TextView realValue;
    private TextView differenceValue;
    private TextView differenceCaption;
    private TextView progressLabel;
    private LinearProgressIndicator progress;
    private MaterialButton primaryAction;
    private MaterialButton secondaryAction;
    private LinearLayout recentContainer;
    private TextView monthAmount;
    private TextView monthCaption;
    private TextView discountAmount;
    private TextView averageAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        ((TextView) view.findViewById(R.id.today_label)).setText(
                Dates.capitalize(Dates.format(System.currentTimeMillis(), "d MMM")));

        emptyState = view.findViewById(R.id.empty_state);
        activeState = view.findViewById(R.id.active_card);
        listName = view.findViewById(R.id.list_name);
        listMeta = view.findViewById(R.id.list_meta);
        statusChip = view.findViewById(R.id.status_chip);
        estimatedValue = view.findViewById(R.id.estimated_value);
        realValue = view.findViewById(R.id.real_value);
        differenceValue = view.findViewById(R.id.difference_value);
        differenceCaption = view.findViewById(R.id.difference_caption);
        progressLabel = view.findViewById(R.id.progress_label);
        progress = view.findViewById(R.id.progress);
        primaryAction = view.findViewById(R.id.primary_action);
        secondaryAction = view.findViewById(R.id.secondary_action);
        recentContainer = view.findViewById(R.id.recent_container);
        monthAmount = view.findViewById(R.id.month_amount);
        monthCaption = view.findViewById(R.id.month_caption);
        discountAmount = view.findViewById(R.id.discount_amount);
        averageAmount = view.findViewById(R.id.average_amount);

        view.findViewById(R.id.action_new_list_small).setOnClickListener(v -> openNewList());
        View quickNewList = view.findViewById(R.id.action_new_list_small);
        bindQuickAction(quickNewList, R.drawable.ic_add, R.string.action_new_list);
        bindQuickAction(view.findViewById(R.id.action_history), R.drawable.ic_history, R.string.title_history);
        bindQuickAction(view.findViewById(R.id.action_stats), R.drawable.ic_stats, R.string.title_stats);
        view.findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), com.example.miscompras.ui.settings.SettingsActivity.class));
        });
        view.findViewById(R.id.action_history).setOnClickListener(v -> navigate(R.id.nav_history));
        view.findViewById(R.id.action_stats).setOnClickListener(v -> navigate(R.id.nav_stats));
        view.findViewById(R.id.see_all_history).setOnClickListener(v -> navigate(R.id.nav_history));
        view.findViewById(R.id.empty_new_list).setOnClickListener(v -> openNewList());
        view.findViewById(R.id.empty_continue).setOnClickListener(v -> {
            ListSummary active = viewModel.getActiveList().getValue();
            if (active != null) {
                startSupermarket(active);
            }
        });

        primaryAction.setOnClickListener(v -> {
            ListSummary active = viewModel.getActiveList().getValue();
            if (active == null) {
                return;
            }
            ListStatus status = ListStatus.fromName(active.status);
            if (status == ListStatus.IN_PROGRESS) {
                startSupermarket(active);
            } else {
                openListDetail(active);
            }
        });

        secondaryAction.setOnClickListener(v -> {
            ListSummary active = viewModel.getActiveList().getValue();
            if (active != null) {
                openListDetail(active);
            }
        });

        viewModel.getActiveList().observe(getViewLifecycleOwner(), this::bindActiveList);
        viewModel.getRecentPurchases().observe(getViewLifecycleOwner(), this::bindRecent);
        viewModel.getSnapshot().observe(getViewLifecycleOwner(), this::bindSnapshot);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshStats();
    }

    private void bindActiveList(ListSummary summary) {
        boolean hasList = summary != null;
        emptyState.setVisibility(hasList ? View.GONE : View.VISIBLE);
        activeState.setVisibility(hasList ? View.VISIBLE : View.GONE);
        if (!hasList) {
            return;
        }
        ListStatus status = ListStatus.fromName(summary.status);
        listName.setText(summary.name);
        String typeLabel = ListType.fromName(summary.type).getLabel();
        listMeta.setText(getString(R.string.meta_list, Dates.capitalize(Dates.format(summary.plannedDate, Dates.FULL)),
                typeLabel.toLowerCase()));
        statusChip.setText(status.getLabel());
        estimatedValue.setText(Money.format(summary.estimatedTotal));
        boolean hasReal = summary.realTotal > 0d;
        realValue.setText(hasReal ? Money.format(summary.realTotal) : "—");

        double difference = Pricing.difference(summary.estimatedTotal, summary.realTotal);
        if (hasReal) {
            differenceValue.setText(Money.signed(difference));
            if (difference < 0) {
                differenceValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_success));
                differenceCaption.setText(R.string.label_savings);
            } else if (difference > 0) {
                differenceValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_danger));
                differenceCaption.setText(R.string.label_more_paid);
            } else {
                differenceValue.setTextColor(Ui.color(requireContext(), com.google.android.material.R.attr.colorOnSurface));
                differenceCaption.setText(R.string.label_difference);
            }
        } else {
            differenceValue.setText("—");
            differenceValue.setTextColor(Ui.color(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            differenceCaption.setText(R.string.label_not_registered);
        }

        int resolved = summary.purchasedCount + summary.notFoundCount;
        int total = Math.max(summary.itemCount, 1);
        progress.setMax(total);
        progress.setProgress(resolved);
        progressLabel.setText(getString(R.string.meta_progress, resolved, summary.itemCount));

        boolean inProgress = status == ListStatus.IN_PROGRESS;
        primaryAction.setText(inProgress ? R.string.action_continue_shop : R.string.action_open_list);
        primaryAction.setIcon(ContextCompat.getDrawable(requireContext(),
                inProgress ? R.drawable.ic_cart : R.drawable.ic_list));
    }

    private void bindSnapshot(StatsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        monthAmount.setText(Money.format(snapshot.monthTotal));
        monthCaption.setText(getString(R.string.meta_month, snapshot.monthLabel));
        discountAmount.setText(Money.format(snapshot.discountsThisMonth));
        averageAmount.setText(Money.format(snapshot.averagePurchase));
    }

    private void bindRecent(java.util.List<ListSummary> lists) {
        recentContainer.removeAllViews();
        View header = requireView().findViewById(R.id.recent_header);
        if (lists == null || lists.isEmpty()) {
            header.setVisibility(View.GONE);
            return;
        }
        header.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int limit = Math.min(3, lists.size());
        for (int i = 0; i < limit; i++) {
            ListSummary summary = lists.get(i);
            View row = inflater.inflate(R.layout.item_recent_purchase, recentContainer, false);
            ((TextView) row.findViewById(R.id.recent_name)).setText(summary.name);
            ((TextView) row.findViewById(R.id.recent_date)).setText(
                    Dates.capitalize(Dates.format(summary.completedDate == null ? summary.plannedDate : summary.completedDate,
                            Dates.FULL)));
            ((TextView) row.findViewById(R.id.recent_total)).setText(Money.format(summary.realTotal));
            ((TextView) row.findViewById(R.id.recent_meta)).setText(
                    getString(R.string.meta_products, summary.purchasedCount));
            ((ImageView) row.findViewById(R.id.recent_icon)).setImageResource(R.drawable.ic_receipt);
            row.setOnClickListener(v -> {
                if (ListStatus.fromName(summary.status) == ListStatus.COMPLETED) {
                    openListDetail(summary);
                }
            });
            recentContainer.addView(row);
        }
    }

    private void openListDetail(ListSummary summary) {
        Intent intent = new Intent(requireContext(), ListDetailActivity.class);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, summary.id);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_NAME, summary.name);
        startActivity(intent);
    }

    private void startSupermarket(ListSummary summary) {
        viewModel.startShopping(summary.id);
        Intent intent = new Intent(requireContext(), SupermarketActivity.class);
        intent.putExtra(SupermarketActivity.EXTRA_LIST_ID, summary.id);
        intent.putExtra(SupermarketActivity.EXTRA_LIST_NAME, summary.name);
        startActivity(intent);
    }

    private void bindQuickAction(View card, int iconRes, int labelRes) {
        ((ImageView) card.findViewById(R.id.quick_action_icon)).setImageResource(iconRes);
        ((TextView) card.findViewById(R.id.quick_action_label)).setText(labelRes);
    }

    private void openNewList() {
        startActivity(new Intent(requireContext(), NewListActivity.class));
    }

    private void navigate(int menuItemId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(menuItemId);
        }
    }
}
