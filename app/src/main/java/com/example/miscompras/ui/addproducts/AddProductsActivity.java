package com.example.miscompras.ui.addproducts;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.products.ProductEditActivity;
import com.example.miscompras.utils.Money;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/** Selección de productos del catálogo para sumar a una lista. */
public class AddProductsActivity extends AppCompatActivity implements ProductPickerAdapter.Listener {

    public static final String EXTRA_LIST_ID = "list_id";
    public static final String EXTRA_LIST_NAME = "list_name";

    private AddProductsViewModel viewModel;
    private ProductPickerAdapter adapter;
    private long listId;
    private TextView selectionLabel;
    private MaterialButton confirmButton;
    private View emptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.miscompras.utils.AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_products);
        SystemBars.pad(findViewById(R.id.add_root));

        listId = getIntent().getLongExtra(EXTRA_LIST_ID, 0L);
        String listName = getIntent().getStringExtra(EXTRA_LIST_NAME);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(listName == null ? getString(R.string.title_add_products) : listName);
        toolbar.setSubtitle(R.string.title_add_products);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AddProductsViewModel.class);

        selectionLabel = findViewById(R.id.selection_label);
        confirmButton = findViewById(R.id.confirm_button);
        emptyState = findViewById(R.id.empty_state);

        adapter = new ProductPickerAdapter(this, viewModel);
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        TextInputEditText search = findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setQuery(s == null ? "" : s.toString());
            }
        });

        ChipGroup chips = findViewById(R.id.category_chips);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setCategory(0L);
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            viewModel.setCategory(chip.getTag() == null ? 0L : (long) chip.getTag());
        });

        FloatingActionButton fab = findViewById(R.id.fab_new_product);
        newProductLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == RESULT_OK && data != null) {
                        long productId = data.getLongExtra(ProductEditActivity.EXTRA_PRODUCT_ID, 0L);
                        if (productId > 0L && listId > 0L) {
                            addNewProduct(productId);
                        }
                    }
                });
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductEditActivity.class);
            intent.putExtra(ProductEditActivity.EXTRA_QUICK_ADD, true);
            newProductLauncher.launch(intent);
        });

        confirmButton.setOnClickListener(v -> {
            viewModel.addToList(listId, () -> {
                Snackbar.make(findViewById(R.id.add_root), R.string.msg_item_added, Snackbar.LENGTH_SHORT).show();
                finish();
            });
        });

        viewModel.getProducts().observe(this, this::bindProducts);
        viewModel.getCategories().observe(this, this::bindCategories);
        viewModel.getMessages().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(findViewById(R.id.add_root), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private ActivityResultLauncher<Intent> newProductLauncher;

    /** Agrega a la lista el producto recién creado usando su precio estimado. */
    private void addNewProduct(long productId) {
        DataGraph.products(getApplicationContext()).load(productId, product -> {
            double price = product == null ? 0d : product.estimatedPrice;
            double quantity = product == null || product.usualQuantity <= 0d ? 1d : product.usualQuantity;
            DataGraph.shopping(getApplicationContext())
                    .addItem(listId, productId, quantity, price, result ->
                            Snackbar.make(findViewById(R.id.add_root), R.string.msg_item_added,
                                    Snackbar.LENGTH_SHORT).show());
        });
    }

    private void bindProducts(List<ProductRow> rows) {
        adapter.submitList(rows);
        boolean empty = rows == null || rows.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        findViewById(R.id.recycler).setVisibility(empty ? View.GONE : View.VISIBLE);
        updateSelection();
    }

    private void bindCategories(List<Category> categories) {
        ChipGroup group = findViewById(R.id.category_chips);
        group.removeAllViews();
        group.setSelectionRequired(false);
        if (categories == null || categories.isEmpty()) {
            return;
        }
        Chip all = createChip(getString(R.string.filter_all), 0L);
        all.setChecked(true);
        group.addView(all);
        for (Category category : categories) {
            group.addView(createChip(category.name, category.id));
        }
    }

    private Chip createChip(String label, long categoryId) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(categoryId);
        chip.setChipBackgroundColorResource(R.color.app_chip_filter_background);
        return chip;
    }

    private void updateSelection() {
        int count = viewModel.selectedCount();
        selectionLabel.setText(getString(R.string.meta_selected, count));
        confirmButton.setEnabled(count > 0);
        confirmButton.setText(count == 0
                ? getString(R.string.action_add)
                : getString(R.string.action_add_count, count));
    }

    @Override
    public void onToggle(ProductRow row, boolean selected) {
        viewModel.toggle(row.id, selected);
        adapter.notifyDataSetChanged();
        updateSelection();
    }

    @Override
    public void onQuantity(ProductRow row, double quantity) {
        viewModel.setQuantity(row.id, quantity);
        adapter.notifyDataSetChanged();
        updateSelection();
    }
}
