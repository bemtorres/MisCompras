package com.example.miscompras.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.entity.Category;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.local.model.ProductRow;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.ui.shopping.SupermarketActivity;
import com.example.miscompras.utils.Money;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Catálogo de productos con búsqueda, filtros y control de stock. */
public class ProductsFragment extends Fragment implements ProductsAdapter.Listener {

    public static ProductsFragment newInstance() {
        return new ProductsFragment();
    }

    private ProductsViewModel viewModel;
    private ProductsAdapter adapter;
    private final List<ProductRow> allProducts = new ArrayList<>();
    private long categoryId;
    private boolean onlyFavorites;
    private boolean onlyLowStock;
    private View emptyState;
    private TextView emptyTitle;
    private TextView emptyBody;
    private View activeBanner;
    private TextView activeName;
    private TextView activeMeta;
    private TextView activeTotal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductsViewModel.class);

        emptyState = view.findViewById(R.id.empty_state);
        emptyTitle = view.findViewById(R.id.empty_title);
        emptyBody = view.findViewById(R.id.empty_body);
        activeBanner = view.findViewById(R.id.active_banner);
        activeName = view.findViewById(R.id.active_name);
        activeMeta = view.findViewById(R.id.active_meta);
        activeTotal = view.findViewById(R.id.active_total);

        adapter = new ProductsAdapter(this);
        RecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        TextInputEditText search = view.findViewById(R.id.search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilters(s == null ? "" : s.toString());
            }
        });

        ChipGroup categoryChips = view.findViewById(R.id.category_chips);
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            categoryId = 0L;
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                categoryId = chip.getTag() == null ? 0L : (long) chip.getTag();
            }
            applyFilters(search.getText() == null ? "" : search.getText().toString());
        });

        ChipGroup filterChips = view.findViewById(R.id.filter_chips);
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            onlyFavorites = checkedIds.contains(R.id.chip_favorites);
            onlyLowStock = checkedIds.contains(R.id.chip_low_stock);
            applyFilters(search.getText() == null ? "" : search.getText().toString());
        });

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_new);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ProductEditActivity.class);
            startActivity(intent);
        });

        activeBanner.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ListDetailActivity.class);
            intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, activeBanner.getTag() instanceof Long ? (long) activeBanner.getTag() : 0L);
            intent.putExtra(ListDetailActivity.EXTRA_LIST_NAME, activeName.getText().toString());
            startActivity(intent);
        });
        view.findViewById(R.id.active_shop).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SupermarketActivity.class);
            intent.putExtra(SupermarketActivity.EXTRA_LIST_ID, activeBanner.getTag() instanceof Long ? (long) activeBanner.getTag() : 0L);
            intent.putExtra(SupermarketActivity.EXTRA_LIST_NAME, activeName.getText().toString());
            startActivity(intent);
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), this::bindCategories);
        viewModel.getProducts().observe(getViewLifecycleOwner(), rows -> {
            allProducts.clear();
            if (rows != null) {
                allProducts.addAll(rows);
            }
            applyFilters(search.getText() == null ? "" : search.getText().toString());
        });
        viewModel.getMessages().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
            }
        });
        DataGraph.shopping(requireContext().getApplicationContext())
                .observeActiveList()
                .observe(getViewLifecycleOwner(), this::bindActive);
    }

    private void bindActive(ListSummary summary) {
        if (summary == null) {
            activeBanner.setVisibility(View.GONE);
            return;
        }
        activeBanner.setVisibility(View.VISIBLE);
        activeBanner.setTag(summary.id);
        activeName.setText(summary.name);
        activeMeta.setText(getString(R.string.meta_progress,
                summary.purchasedCount + summary.notFoundCount, summary.itemCount));
        activeTotal.setText(Money.format(summary.estimatedTotal));
    }

    private void bindCategories(List<Category> categories) {
        ChipGroup group = requireView().findViewById(R.id.category_chips);
        group.removeAllViews();
        group.setSelectionRequired(false);
        if (categories == null || categories.isEmpty()) {
            return;
        }
        Chip all = new Chip(requireContext());
        all.setText(R.string.filter_all);
        all.setCheckable(true);
        all.setTag(0L);
        all.setChecked(true);
        group.addView(all);
        for (Category category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category.name);
            chip.setCheckable(true);
            chip.setTag(category.id);
            group.addView(chip);
        }
    }

    private void applyFilters(String text) {
        String query = text == null ? "" : text.trim().toLowerCase(Locale.getDefault());
        List<ProductRow> result = new ArrayList<>();
        for (ProductRow row : allProducts) {
            if (categoryId > 0L && row.categoryId != categoryId) {
                continue;
            }
            if (onlyFavorites && !row.isFavorite) {
                continue;
            }
            if (onlyLowStock && !row.isLowStock()) {
                continue;
            }
            if (!query.isEmpty() && !row.name.toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            result.add(row);
        }
        adapter.submitList(result);
        boolean empty = result.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.recycler).setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            emptyTitle.setText(R.string.empty_products_title);
            emptyBody.setText(R.string.empty_products_body);
        }
    }

    @Override
    public void onClick(ProductRow row) {
        Intent intent = new Intent(requireContext(), ProductEditActivity.class);
        intent.putExtra(ProductEditActivity.EXTRA_PRODUCT_ID, row.id);
        startActivity(intent);
    }

    @Override
    public void onFavorite(ProductRow row) {
        viewModel.toggleFavorite(row);
    }

    @Override
    public void onStock(ProductRow row, double delta) {
        viewModel.changeStock(row, delta);
    }
}
