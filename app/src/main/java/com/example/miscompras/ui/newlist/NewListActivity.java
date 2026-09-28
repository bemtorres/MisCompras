package com.example.miscompras.ui.newlist;

import android.app.Application;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.entity.Product;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.ui.addproducts.AddProductsActivity;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Validators;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Creación de una lista: nombre, periodicidad, fecha y productos sugeridos. */
public class NewListActivity extends AppCompatActivity {

    private NewListViewModel viewModel;
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private MaterialButtonToggleGroup typeToggle;
    private MaterialButton dateButton;
    private ChipGroup suggestionChips;
    private long plannedDate = System.currentTimeMillis();
    private long lastCreatedListId;
    private List<Product> suggestions = new ArrayList<>();

    public static class NewListViewModel extends AndroidViewModel {

        private final ShoppingRepository shopping;
        private final MutableLiveData<List<Product>> suggestions = new MutableLiveData<>();
        private final MutableLiveData<String> messages = new MutableLiveData<>();

        public NewListViewModel(@NonNull Application application) {
            super(application);
            shopping = DataGraph.shopping(application);
            loadSuggestions();
        }

        public LiveData<List<Product>> getSuggestions() {
            return suggestions;
        }

        public LiveData<String> getMessages() {
            return messages;
        }

        private void loadSuggestions() {
            shopping.suggestProductsForNewList(suggestions::setValue);
        }

        public void create(String name, ListType type, long date, ShoppingRepository.Callback<Long> callback) {
            if (!Validators.isValidName(name)) {
                messages.setValue("Escribe un nombre");
                return;
            }
            shopping.createList(name, type, date, null, id -> {
                messages.setValue("Lista creada");
                callback.onResult(id);
            });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.miscompras.utils.AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_list);
        SystemBars.pad(findViewById(R.id.new_list_root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_new_list);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(NewListViewModel.class);

        nameLayout = findViewById(R.id.name_layout);
        nameInput = findViewById(R.id.name_input);
        typeToggle = findViewById(R.id.type_toggle);
        dateButton = findViewById(R.id.date_button);
        suggestionChips = findViewById(R.id.suggestion_chips);

        dateButton.setText(Dates.format(plannedDate, Dates.NUMERIC));
        dateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(plannedDate);
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, day, 12, 0, 0);
                picked.set(Calendar.MILLISECOND, 0);
                plannedDate = picked.getTimeInMillis();
                dateButton.setText(Dates.format(plannedDate, Dates.NUMERIC));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        findViewById(R.id.btn_create).setOnClickListener(v -> create());
        findViewById(R.id.btn_create_empty).setOnClickListener(v -> {
            createThenAdd();
        });

        viewModel.getSuggestions().observe(this, this::bindSuggestions);
        viewModel.getMessages().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(findViewById(R.id.new_list_root), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void bindSuggestions(List<Product> products) {
        suggestionChips.removeAllViews();
        suggestions = products == null ? new ArrayList<>() : products;
        if (suggestions.isEmpty()) {
            suggestionChips.setVisibility(View.GONE);
            return;
        }
        suggestionChips.setVisibility(View.VISIBLE);
        for (Product product : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(product.name);
            chip.setCheckable(true);
            suggestionChips.addView(chip);
        }
    }

    private void create() {
        submit(false);
    }

    private void createThenAdd() {
        submit(true);
    }

    private void submit(boolean openPicker) {
        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (!Validators.isValidName(name)) {
            nameLayout.setError(getString(R.string.msg_name_required));
            return;
        }
        nameLayout.setError(null);
        ListType type = typeFor(typeToggle.getCheckedButtonId());
        viewModel.create(name, type, plannedDate, listId -> {
            lastCreatedListId = listId;
            if (openPicker) {
                openPicker(listId, name);
            } else {
                addSuggestions(listId, selectedSuggestions(), 0);
            }
        });
    }

    private List<Product> selectedSuggestions() {
        List<Product> selected = new ArrayList<>();
        for (int id : suggestionChips.getCheckedChipIds()) {
            View chip = suggestionChips.findViewById(id);
            if (chip == null || !(chip instanceof Chip)) {
                continue;
            }
            String text = ((Chip) chip).getText().toString();
            for (Product product : suggestions) {
                if (product.name.equals(text) && !selected.contains(product)) {
                    selected.add(product);
                }
            }
        }
        return selected;
    }

    private void addSuggestions(long listId, List<Product> selected, int index) {
        if (index >= selected.size()) {
            openDetail();
            return;
        }
        Product product = selected.get(index);
        DataGraph.shopping(getApplicationContext())
                .addItem(listId, product.id, Math.max(1d, product.usualQuantity), product.estimatedPrice,
                        result -> addSuggestions(listId, selected, index + 1));
    }

    private void openPicker(long listId, String name) {
        Intent intent = new Intent(this, AddProductsActivity.class);
        intent.putExtra(AddProductsActivity.EXTRA_LIST_ID, listId);
        intent.putExtra(AddProductsActivity.EXTRA_LIST_NAME, name);
        startActivity(intent);
        finish();
    }

    private void openDetail() {
        Intent intent = new Intent(this, ListDetailActivity.class);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, lastCreatedListId);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_NAME, nameInput.getText() == null
                ? "" : nameInput.getText().toString().trim());
        startActivity(intent);
        finish();
    }

    private static ListType typeFor(int buttonId) {
        if (buttonId == R.id.type_biweekly) {
            return ListType.BIWEEKLY;
        }
        if (buttonId == R.id.type_monthly) {
            return ListType.MONTHLY;
        }
        if (buttonId == R.id.type_other) {
            return ListType.OTHER;
        }
        return ListType.WEEKLY;
    }
}
