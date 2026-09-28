package com.example.miscompras.ui.listdetail;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.MainActivity;
import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.addproducts.AddProductsActivity;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.ui.shopping.SupermarketActivity;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Money;
import com.example.miscompras.utils.ReceiptGenerator;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/** Detalle de una lista: productos, totales, estado y acciones de la compra. */
public class ListDetailActivity extends AppCompatActivity implements ListItemsAdapter.Listener {

    public static final String EXTRA_LIST_ID = "list_id";
    public static final String EXTRA_LIST_NAME = "list_name";

    private ListDetailViewModel viewModel;
    private ListItemsAdapter adapter;
    private ListSummary current;
    private View emptyState;
    private TextView metaLabel;
    private TextView estimatedValue;
    private TextView realValue;
    private TextView differenceValue;
    private TextView differenceCaption;
    private TextView progressLabel;
    private TextView totalBottom;
    private LinearProgressIndicator progress;
    private MaterialButton addButton;
    private MaterialButton bottomAction;
    private MaterialButton shareBottomButton;
    private MaterialButton shareCompletedButton;
    private boolean readOnly;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.miscompras.utils.AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);
        View root = findViewById(R.id.detail_root);
        SystemBars.pad(root);

        long listId = getIntent().getLongExtra(EXTRA_LIST_ID, 0L);
        String listName = getIntent().getStringExtra(EXTRA_LIST_NAME);

        viewModel = new ViewModelProvider(this).get(ListDetailViewModel.class);
        viewModel.init(listId);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(listName == null ? getString(R.string.title_lists) : listName);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_list_detail);
        toolbar.setOnMenuItemClickListener(this::onMenu);

        metaLabel = findViewById(R.id.meta_label);
        estimatedValue = findViewById(R.id.estimated_value);
        realValue = findViewById(R.id.real_value);
        differenceValue = findViewById(R.id.difference_value);
        differenceCaption = findViewById(R.id.difference_caption);
        progress = findViewById(R.id.progress);
        progressLabel = findViewById(R.id.progress_label);
        totalBottom = findViewById(R.id.total_bottom);
        addButton = findViewById(R.id.btn_add);
        bottomAction = findViewById(R.id.bottom_action);
        shareBottomButton = findViewById(R.id.btn_share_bottom);
        shareCompletedButton = findViewById(R.id.btn_share_completed);
        emptyState = findViewById(R.id.empty_state);

        if (shareBottomButton != null) {
            shareBottomButton.setOnClickListener(v -> shareReceipt());
        }
        if (shareCompletedButton != null) {
            shareCompletedButton.setOnClickListener(v -> shareReceipt());
        }

        adapter = new ListItemsAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(v -> openAddProducts());
        bottomAction.setOnClickListener(v -> openShopping());

        viewModel.getList().observe(this, this::bindSummary);
        viewModel.getItems().observe(this, rows -> {
            adapter.submitList(rows);
            boolean empty = rows == null || rows.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.getMessages().observe(this, event -> {
            String message = event == null ? null : event.consume();
            if (message != null) {
                Snackbar.make(findViewById(R.id.detail_root), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSummary(ListSummary summary) {
        if (summary == null) {
            return;
        }
        current = summary;
        ListStatus status = ListStatus.fromName(summary.status);
        readOnly = status == ListStatus.COMPLETED || status == ListStatus.CANCELLED;

        metaLabel.setText(getString(R.string.meta_list,
                Dates.capitalize(Dates.format(summary.plannedDate, Dates.FULL)),
                ListType.fromName(summary.type).getLabel().toLowerCase()));

        estimatedValue.setText(Money.format(summary.estimatedTotal));
        boolean hasReal = summary.realTotal > 0d;
        realValue.setText(hasReal ? Money.format(summary.realTotal) : "—");

        if (hasReal) {
            double delta = Pricing.difference(summary.estimatedTotal, summary.realTotal);
            differenceValue.setText(Money.signed(delta));
            if (delta < 0d) {
                differenceValue.setTextColor(Ui.color(this, androidx.appcompat.R.attr.colorPrimary));
                if (differenceCaption != null) {
                    differenceCaption.setText(R.string.label_savings);
                }
            } else if (delta > 0d) {
                differenceValue.setTextColor(Ui.color(this, androidx.appcompat.R.attr.colorError));
                if (differenceCaption != null) {
                    differenceCaption.setText(R.string.label_more_paid);
                }
            } else {
                differenceValue.setTextColor(Ui.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
                if (differenceCaption != null) {
                    differenceCaption.setText(R.string.label_difference);
                }
            }
        } else {
            differenceValue.setText("—");
            differenceValue.setTextColor(Ui.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
            if (differenceCaption != null) {
                differenceCaption.setText(R.string.label_difference);
            }
        }

        int resolved = summary.purchasedCount + summary.notFoundCount;
        progress.setMax(Math.max(summary.itemCount, 1));
        progress.setProgress(resolved);
        progressLabel.setText(getString(R.string.shopping_progress, resolved, summary.itemCount));

        if (readOnly) {
            totalBottom.setText(getString(R.string.status_completed) + " · "
                    + getString(R.string.meta_progress, resolved, summary.itemCount));
            Ui.visible(addButton, false);
            Ui.visible(bottomAction, false);
            Ui.visible(shareBottomButton, false);
            Ui.visible(shareCompletedButton, true);
            return;
        }

        totalBottom.setText(getString(R.string.meta_bottom_total, Money.format(summary.estimatedTotal)));
        Ui.visible(addButton, true);
        Ui.visible(bottomAction, true);
        Ui.visible(shareBottomButton, true);
        Ui.visible(shareCompletedButton, false);
        if (status == ListStatus.IN_PROGRESS) {
            bottomAction.setText(R.string.action_continue_shop);
        } else {
            bottomAction.setText(R.string.action_shop_mode);
        }
    }

    private boolean onMenu(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_receipt) {
            shareReceipt();
            return true;
        }
        if (id == R.id.action_edit_list) {
            if (current != null) {
                EditListDialog.newInstance(current).show(getSupportFragmentManager(), "edit-list");
            }
            return true;
        }
        if (id == R.id.action_finish) {
            confirmFinish();
            return true;
        }
        if (id == R.id.action_cancel) {
            confirmCancel();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
    }

    public void shareReceipt() {
        if (current == null) {
            Toast.makeText(this, R.string.share_receipt_error, Toast.LENGTH_SHORT).show();
            return;
        }
        List<ItemRow> items = viewModel.getItems() != null && viewModel.getItems().getValue() != null
                ? viewModel.getItems().getValue()
                : (adapter != null ? adapter.getCurrentList() : null);
        ReceiptGenerator.showShareDialog(this, current, items);
    }

    private void openAddProducts() {
        if (current == null) {
            return;
        }
        viewModel.startShopping();
        Intent intent = new Intent(this, AddProductsActivity.class);
        intent.putExtra(AddProductsActivity.EXTRA_LIST_ID, current.id);
        intent.putExtra(AddProductsActivity.EXTRA_LIST_NAME, current.name);
        startActivity(intent);
    }

    private void openShopping() {
        if (current == null) {
            return;
        }
        viewModel.startShopping();
        Intent intent = new Intent(this, SupermarketActivity.class);
        intent.putExtra(SupermarketActivity.EXTRA_LIST_ID, current.id);
        intent.putExtra(SupermarketActivity.EXTRA_LIST_NAME, current.name);
        startActivity(intent);
    }

    private void confirmFinish() {
        if (current == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_finish_title)
                .setMessage(R.string.msg_confirm_finish)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.action_finish, (dialog, which) -> viewModel.countPending(this::finishWith))
                .show();
    }

    private void finishWith(int pending) {
        if (current == null) {
            return;
        }
        int total = current.itemCount;
        int purchased = current.purchasedCount;
        int notFound = current.notFoundCount;
        double discount = current.totalDiscount;
        viewModel.finish(result -> {
            if (result == null || !result.success) {
                return;
            }
            double estimated = current.estimatedTotal;
            double real = current.realTotal;
            double difference = Pricing.difference(estimated, real);
            FinishSummarySheet
                    .create(Money.format(estimated), Money.format(real), Money.format(discount),
                            Money.signed(difference), purchased, total, notFound, pending)
                    .show(getSupportFragmentManager(), "finish-summary");
        });
    }

    private void confirmCancel() {
        if (current == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cancel_list_title)
                .setMessage(R.string.msg_confirm_cancel_list)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.action_cancel, (dialog, which) -> viewModel.cancel(result -> {
                    if (result != null && result.success) {
                        viewModel.notify(getString(R.string.msg_list_cancelled));
                        finish();
                    }
                }))
                .show();
    }

    private void confirmDelete() {
        if (current == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_list_title)
                .setMessage(R.string.msg_confirm_delete_list)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> viewModel.delete(result -> {
                    if (result != null && result.success) {
                        finish();
                    }
                }))
                .show();
    }

    // -------------------------------------------------------------- ListItemsAdapter.Listener

    @Override
    public void onClick(ItemRow row) {
        if (readOnly) {
            ItemEditSheet.newInstance(row.id, viewModel.getListId())
                    .show(getSupportFragmentManager(), "item-edit");
            return;
        }
        viewModel.quickTogglePurchased(row);
    }

    @Override
    public void onEdit(ItemRow row) {
        ItemEditSheet.newInstance(row.id, viewModel.getListId())
                .show(getSupportFragmentManager(), "item-edit");
    }

    @Override
    public void onTogglePurchased(ItemRow row) {
        if (readOnly) {
            return;
        }
        viewModel.quickTogglePurchased(row);
    }

    /** Llamado al cerrar el resumen para volver al listado. */
    public void onPurchaseFinished() {
        finish();
    }

    /** Abre la sección de historial en la pantalla principal. */
    public void openHistoryTab() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_TAB, R.id.nav_history);
        startActivity(intent);
        finish();
    }
}
