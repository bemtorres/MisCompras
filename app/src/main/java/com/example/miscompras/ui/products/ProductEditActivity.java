package com.example.miscompras.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.DataExecutors;
import com.example.miscompras.data.local.AppDatabase;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.local.model.ProductPriceStats;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.example.miscompras.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/** Alta y edición de productos del catálogo. */
public class ProductEditActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_QUICK_ADD = "quick_add";

    private ProductEditViewModel viewModel;
    private long productId;
    private boolean quickAdd;
    private long categoryId = 1L;
    private final List<Category> categories = new ArrayList<>();

    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private MaterialAutoCompleteTextView categoryInput;
    private TextInputEditText quantityInput;
    private MaterialSwitch favoriteSwitch;
    private MaterialSwitch activeSwitch;
    private TextView averageValue;
    private TextView minimumValue;
    private TextView maximumValue;
    private TextView lastValue;
    private View statsCard;
    private MaterialButton deleteButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);
        SystemBars.pad(findViewById(R.id.product_root));

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, 0L);
        quickAdd = getIntent().getBooleanExtra(EXTRA_QUICK_ADD, false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(productId > 0L ? R.string.title_edit_product : R.string.title_new_product);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(ProductEditViewModel.class);
        viewModel.load(productId);

        nameLayout = findViewById(R.id.name_layout);
        nameInput = findViewById(R.id.name_input);
        categoryInput = findViewById(R.id.category_input);
        quantityInput = findViewById(R.id.quantity_input);
        favoriteSwitch = findViewById(R.id.switch_favorite);
        activeSwitch = findViewById(R.id.switch_active);
        averageValue = findViewById(R.id.average_value);
        minimumValue = findViewById(R.id.minimum_value);
        maximumValue = findViewById(R.id.maximum_value);
        lastValue = findViewById(R.id.last_value);
        statsCard = findViewById(R.id.stats_card);
        deleteButton = findViewById(R.id.btn_delete);

        viewModel.getCategories().observe(this, this::bindCategories);
        viewModel.getProduct().observe(this, this::bind);
        viewModel.getMessages().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(findViewById(R.id.product_root), message, Snackbar.LENGTH_SHORT).show();
            }
        });
        if (productId > 0L) {
            viewModel.getStats(productId).observe(this, this::bindStats);
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_new_category).setOnClickListener(v -> createCategory());
        deleteButton.setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btn_price_history).setOnClickListener(v -> {
            Intent intent = new Intent(this, PriceHistoryActivity.class);
            intent.putExtra(PriceHistoryActivity.EXTRA_PRODUCT_ID, productId);
            intent.putExtra(PriceHistoryActivity.EXTRA_PRODUCT_NAME, currentName());
            startActivity(intent);
        });
    }

    private String currentName() {
        return nameInput.getText() == null ? "" : nameInput.getText().toString();
    }

    private void bindCategories(List<Category> values) {
        categories.clear();
        if (values == null) {
            categoryInput.setAdapter(null);
            return;
        }
        categories.addAll(values);
        String[] labels = new String[values.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = values.get(i).name;
        }
        categoryInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        categoryInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categories.size()) {
                    categoryId = categories.get(position).id;
                }
            }
        });
        if (productId <= 0L && !values.isEmpty() && (categoryInput.getText() == null || categoryInput.getText().toString().isEmpty())) {
            categoryId = values.get(0).id;
            categoryInput.setText(values.get(0).name, false);
        } else {
            updateSelectedCategoryInUi();
        }
    }

    private void updateSelectedCategoryInUi() {
        for (Category category : categories) {
            if (category.id == categoryId) {
                categoryInput.setText(category.name, false);
                break;
            }
        }
    }

    private void bind(Product product) {
        if (product == null) {
            return;
        }
        nameInput.setText(product.name);
        quantityInput.setText(Money.raw(product.usualQuantity));
        favoriteSwitch.setChecked(product.isFavorite);
        activeSwitch.setChecked(product.isActive);
        categoryId = product.categoryId;
        updateSelectedCategoryInUi();
        Ui.visible(statsCard, true);
        Ui.visible(deleteButton, true);
    }

    private void bindStats(ProductPriceStats stats) {
        if (stats == null) {
            return;
        }
        boolean hasPrices = stats.count > 0;
        averageValue.setText(hasPrices ? Money.format(stats.average) : "—");
        minimumValue.setText(hasPrices ? Money.format(stats.min) : "—");
        maximumValue.setText(hasPrices ? Money.format(stats.max) : "—");
        lastValue.setText(hasPrices ? Money.format(stats.lastPrice) : "—");
    }

    private double number(TextInputEditText input, double fallback) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        return text.isEmpty() ? fallback : Math.max(0d, Validators.parseMoney(text));
    }

    private void save() {
        Product product = viewModel.getProduct().getValue();
        product = product == null ? new Product() : product;
        product.name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        product.categoryId = categoryId;
        product.unit = "";
        product.estimatedPrice = 0d;
        product.usualQuantity = Math.max(1d, number(quantityInput, 1d));
        product.currentStock = 0d;
        product.minimumStock = 0d;
        product.isFavorite = favoriteSwitch.isChecked();
        product.isActive = activeSwitch.isChecked();
        if (product.icon == null || product.icon.isEmpty()) {
            product.icon = "🛍️";
        }

        nameLayout.setError(null);
        if (product.name.isEmpty()) {
            nameLayout.setError(getString(R.string.msg_name_required));
            return;
        }
        viewModel.save(product, id -> {
            Intent result = new Intent();
            result.putExtra(EXTRA_PRODUCT_ID, id);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_product_title)
                .setMessage(getString(R.string.msg_confirm_delete_product, currentName()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) ->
                        viewModel.delete(productId, deleted -> {
                            if (!deleted) {
                                return;
                            }
                            setResult(RESULT_OK);
                            finish();
                        }))
                .show();
    }

    private void createCategory() {
        View content = getLayoutInflater().inflate(R.layout.dialog_new_category, null);
        TextInputLayout layout = content.findViewById(R.id.category_layout);
        TextInputEditText input = content.findViewById(R.id.category_name_input);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_category)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isEmpty()) {
                        layout.setError(getString(R.string.msg_name_required));
                        return;
                    }
                    Category category = new Category(name, "#0E6B45");
                    DataExecutors.io(() -> AppDatabase.get(getApplicationContext()).categoryDao().insert(category));
                    Snackbar.make(findViewById(R.id.product_root), R.string.msg_category_created, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }
}
