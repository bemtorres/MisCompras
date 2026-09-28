package com.example.miscompras.ui.shopping;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.NotFoundReason;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.ui.listdetail.ItemEditSheet;
import com.example.miscompras.utils.Money;
import com.example.miscompras.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/** Modo compra: producto por producto con precio real, descuento y sustitutos. */
public class SupermarketActivity extends AppCompatActivity {

    public static final String EXTRA_LIST_ID = "list_id";
    public static final String EXTRA_LIST_NAME = "list_name";

    private SupermarketViewModel viewModel;
    private List<ItemRow> rows = new java.util.ArrayList<>();
    private ListSummary summary;
    private ItemRow current;

    private TextView counterLabel;
    private TextView nameView;
    private TextView categoryView;
    private TextView estimatedView;
    private TextView quantityView;
    private TextView subtotalView;
    private TextView totalLabel;
    private TextView emptyView;
    private TextInputLayout priceLayout;
    private TextInputEditText priceInput;
    private TextInputLayout discountLayout;
    private TextInputEditText discountInput;
    private MaterialButtonToggleGroup discountToggle;
    private LinearProgressIndicator progress;
    private MaterialButton buyButton;
    private MaterialButton estimateButton;
    private MaterialButton notFoundButton;
    private View productCard;
    private View emptyCard;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.miscompras.utils.AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supermarket);
        SystemBars.pad(findViewById(R.id.shopping_root));

        long listId = getIntent().getLongExtra(EXTRA_LIST_ID, 0L);
        String listName = getIntent().getStringExtra(EXTRA_LIST_NAME);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.mode_shopping);
        toolbar.setSubtitle(listName == null ? "" : listName);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_supermarket);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_finish) {
                confirmFinish();
                return true;
            }
            if (id == R.id.action_add_product) {
                openAddProducts();
                return true;
            }
            return false;
        });

        viewModel = new ViewModelProvider(this).get(SupermarketViewModel.class);
        viewModel.init(listId);

        counterLabel = findViewById(R.id.counter_label);
        nameView = findViewById(R.id.product_name);
        categoryView = findViewById(R.id.product_category);
        estimatedView = findViewById(R.id.estimated_label);
        quantityView = findViewById(R.id.quantity_value);
        subtotalView = findViewById(R.id.subtotal_value);
        totalLabel = findViewById(R.id.total_value);
        emptyView = findViewById(R.id.empty_message);
        priceLayout = findViewById(R.id.price_layout);
        priceLayout.setPrefixText(Money.getCurrencySymbol().trim());
        priceInput = findViewById(R.id.price_input);
        discountLayout = findViewById(R.id.discount_layout);
        discountInput = findViewById(R.id.discount_input);
        discountToggle = findViewById(R.id.discount_toggle);
        progress = findViewById(R.id.progress);
        buyButton = findViewById(R.id.btn_buy);
        estimateButton = findViewById(R.id.btn_estimate);
        notFoundButton = findViewById(R.id.btn_not_found);
        productCard = findViewById(R.id.product_card);
        emptyCard = findViewById(R.id.empty_card);

        ImageButton minus = findViewById(R.id.quantity_minus);
        ImageButton plus = findViewById(R.id.quantity_plus);
        minus.setOnClickListener(v -> {
            if (current != null) {
                viewModel.setQuantity(current.id, Math.max(0d, current.quantity - 1d));
            }
        });
        plus.setOnClickListener(v -> {
            if (current != null) {
                viewModel.setQuantity(current.id, current.quantity + 1d);
            }
        });
        estimateButton.setOnClickListener(v -> {
            if (current != null) {
                viewModel.useEstimate(current);
            }
        });
        buyButton.setOnClickListener(v -> registerPurchase());
        findViewById(R.id.btn_price_later).setOnClickListener(v -> {
            if (current != null) {
                // Registra que el producto se lleva (cantidad confirmada) manteniendo el precio estimado o 0 para completar el precio real después
                double fallback = current.estimatedUnitPrice > 0d ? current.estimatedUnitPrice : 0d;
                viewModel.registerPurchase(current, fallback, DiscountType.NONE, 0d);
            }
        });
        notFoundButton.setOnClickListener(v -> askNotFound());
        findViewById(R.id.btn_substitute).setOnClickListener(v -> askSubstitute());
        findViewById(R.id.btn_details).setOnClickListener(v -> {
            if (current != null) {
                ItemEditSheet.newInstance(current.id, listId).show(getSupportFragmentManager(), "item_edit");
            }
        });
        findViewById(R.id.btn_finish).setOnClickListener(v -> confirmFinish());
        findViewById(R.id.btn_add_products).setOnClickListener(v -> openAddProducts());
        findViewById(R.id.btn_history).setOnClickListener(v -> {
            startActivity(new Intent(this, ListDetailActivity.class)
                    .putExtra(ListDetailActivity.EXTRA_LIST_ID, listId)
                    .putExtra(ListDetailActivity.EXTRA_LIST_NAME, listName));
            finish();
        });

        priceInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshTotals();
            }
        });
        discountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshTotals();
            }
        });
        discountToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            Ui.visible(discountLayout, isChecked && checkedId != R.id.discount_none);
            refreshTotals();
        });

        viewModel.getList().observe(this, this::bindList);
        viewModel.getItems().observe(this, this::bindItems);
        viewModel.getMessages().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(findViewById(R.id.shopping_root), message, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.getFinished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) {
                startActivity(new Intent(this, ListDetailActivity.class)
                        .putExtra(ListDetailActivity.EXTRA_LIST_ID, listId)
                        .putExtra(ListDetailActivity.EXTRA_LIST_NAME, listName));
                finish();
            }
        });
    }

    private void bindList(ListSummary value) {
        summary = value;
        if (value == null) {
            return;
        }
        viewModel.rememberTotal(value);
        int total = Math.max(value.itemCount, 1);
        progress.setMax(total);
        progress.setProgress(value.purchasedCount + value.notFoundCount);
        counterLabel.setText(getString(R.string.shopping_progress,
                value.purchasedCount + value.notFoundCount, value.itemCount));
        totalLabel.setText(Money.format(value.realTotal > 0d ? value.realTotal : value.estimatedTotal));
        boolean done = value.purchasedCount + value.notFoundCount >= value.itemCount;
        findViewById(R.id.btn_finish).setEnabled(!done || value.realTotal > 0d);
    }

    private void bindItems(List<ItemRow> items) {
        rows = items == null ? new java.util.ArrayList<>() : items;
        current = null;
        for (ItemRow row : rows) {
            if (!row.isPurchased && !row.notFound) {
                current = row;
                break;
            }
        }
        boolean hasCurrent = current != null;
        productCard.setVisibility(hasCurrent ? View.VISIBLE : View.GONE);
        emptyCard.setVisibility(hasCurrent ? View.GONE : View.VISIBLE);
        if (!hasCurrent) {
            emptyView.setText(R.string.shopping_all_done);
            return;
        }
        String icon = (current.productIcon != null && !current.productIcon.trim().isEmpty()) ? (current.productIcon + " ") : "";
        nameView.setText(icon + current.productName);
        String meta = "Cantidad: " + Money.quantity(current.quantity);
        categoryView.setText(current.categoryName.isEmpty() ? meta : meta + " · " + current.categoryName);
        if (current.estimatedUnitPrice > 0d) {
            estimatedView.setText(getString(R.string.shopping_use_estimate, Money.format(current.estimatedUnitPrice)));
            estimatedView.setVisibility(View.VISIBLE);
            estimateButton.setVisibility(View.VISIBLE);
        } else {
            estimatedView.setVisibility(View.GONE);
            estimateButton.setVisibility(View.GONE);
        }
        quantityView.setText(Money.quantity(current.quantity));

        DiscountType type = DiscountType.fromName(current.discountType);
        discountToggle.check(type == DiscountType.PERCENT ? R.id.discount_percent
                : (type == DiscountType.FIXED ? R.id.discount_amount : R.id.discount_none));
        discountInput.setText(current.discountValue > 0d ? Money.raw(current.discountValue) : "");
        Ui.visible(discountLayout, type != DiscountType.NONE);
        double real = current.realUnitPrice == null || current.realUnitPrice <= 0d
                ? (current.estimatedUnitPrice > 0d ? current.estimatedUnitPrice : 0d)
                : current.realUnitPrice;
        priceInput.setText(real > 0d ? Money.raw(real) : "");
        priceLayout.setError(null);
        refreshTotals();
    }

    private DiscountType discountType() {
        int checked = discountToggle.getCheckedButtonId();
        if (checked == R.id.discount_percent) {
            return DiscountType.PERCENT;
        }
        if (checked == R.id.discount_amount) {
            return DiscountType.FIXED;
        }
        return DiscountType.NONE;
    }

    private double readPrice(double fallback) {
        String text = priceInput.getText() == null ? "" : priceInput.getText().toString().trim();
        return text.isEmpty() ? fallback : Validators.parseMoney(text);
    }

    private double readDiscount() {
        String text = discountInput.getText() == null ? "" : discountInput.getText().toString().trim();
        return text.isEmpty() ? 0d : Math.max(0d, Validators.parseMoney(text));
    }

    private void refreshTotals() {
        if (current == null) {
            return;
        }
        DiscountType type = discountType();
        double discount = type == DiscountType.NONE ? 0d : readDiscount();
        double real = readPrice(current.estimatedUnitPrice > 0d ? current.estimatedUnitPrice : 0d);
        double finalPrice = Pricing.finalUnitPrice(real, type, discount);
        subtotalView.setText(Money.format(Pricing.subtotal(current.quantity, finalPrice)));
        priceLayout.setError(null);
        discountLayout.setError(null);
        if (finalPrice < 0d) {
            discountLayout.setError(getString(R.string.msg_discount_too_big));
        }
        boolean cheaper = current.estimatedUnitPrice > 0d && finalPrice < current.estimatedUnitPrice;
        subtotalView.setTextColor(Ui.color(this, cheaper
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurface));
    }

    private void registerPurchase() {
        if (current == null) {
            return;
        }
        DiscountType type = discountType();
        double discount = type == DiscountType.NONE ? 0d : readDiscount();
        if (type == DiscountType.PERCENT && discount > 100d) {
            discountLayout.setError(getString(R.string.msg_discount_invalid));
            return;
        }
        double real = readPrice(current.estimatedUnitPrice > 0d ? current.estimatedUnitPrice : 0d);
        if (real < 0d) {
            priceLayout.setError(getString(R.string.msg_price_invalid));
            return;
        }
        viewModel.registerPurchase(current, real, type, discount);
    }

    private void askNotFound() {
        if (current == null) {
            return;
        }
        String[] reasons = new String[NotFoundReason.values().length];
        for (int i = 0; i < reasons.length; i++) {
            reasons[i] = NotFoundReason.values()[i].getLabel();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.shopping_reason_title)
                .setItems(reasons, (dialog, which) ->
                        viewModel.registerNotFound(current.id, NotFoundReason.values()[which].name()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void askSubstitute() {
        if (current == null) {
            return;
        }
        View content = getLayoutInflater().inflate(R.layout.dialog_new_category, null);
        TextInputLayout layout = content.findViewById(R.id.category_layout);
        TextInputEditText input = content.findViewById(R.id.category_name_input);
        input.setText(current.substituteName);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_substitute_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isEmpty()) {
                        layout.setError(getString(R.string.msg_name_required));
                        return;
                    }
                    viewModel.setSubstitute(current.id, name);
                })
                .show();
    }

    private void confirmFinish() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_finish_title)
                .setMessage(R.string.msg_confirm_finish)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.action_finish, (dialog, which) -> viewModel.finish(result -> finish()))
                .show();
    }

    private void openAddProducts() {
        long listId = getIntent().getLongExtra(EXTRA_LIST_ID, 0L);
        String listName = getIntent().getStringExtra(EXTRA_LIST_NAME);
        startActivity(new Intent(this, com.example.miscompras.ui.addproducts.AddProductsActivity.class)
                .putExtra(com.example.miscompras.ui.addproducts.AddProductsActivity.EXTRA_LIST_ID, listId)
                .putExtra(com.example.miscompras.ui.addproducts.AddProductsActivity.EXTRA_LIST_NAME, listName));
    }
}
