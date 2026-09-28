package com.example.miscompras.ui.listdetail;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.domain.DiscountType;
import com.example.miscompras.domain.NotFoundReason;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.Money;
import com.example.miscompras.utils.Validators;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Hoja de edición de un ítem: cantidad, estimado, precio real, descuento, sustituto y estado. */
public class ItemEditSheet extends BottomSheetDialogFragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_LIST_ID = "list_id";

    public static ItemEditSheet newInstance(long itemId, long listId) {
        ItemEditSheet sheet = new ItemEditSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, itemId);
        args.putLong(ARG_LIST_ID, listId);
        sheet.setArguments(args);
        return sheet;
    }

    private long itemId;
    private long listId;
    private ItemRow row;
    private String reasonValue;

    private TextInputLayout quantityLayout;
    private TextInputEditText quantityInput;
    private TextInputLayout realLayout;
    private TextInputEditText realInput;
    private TextInputLayout discountLayout;
    private TextInputEditText discountInput;
    private MaterialButtonToggleGroup discountToggle;
    private MaterialSwitch purchasedSwitch;
    private MaterialSwitch notFoundSwitch;
    private View reasonGroup;
    private MaterialAutoCompleteTextView reasonInput;
    private TextInputLayout substituteLayout;
    private TextInputEditText substituteInput;
    private TextInputLayout productLayout;
    private com.google.android.material.textfield.TextInputEditText productNameInput;

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_MisCompras_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_item_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        itemId = requireArguments().getLong(ARG_ITEM_ID);
        listId = requireArguments().getLong(ARG_LIST_ID);

        quantityLayout = view.findViewById(R.id.quantity_layout);
        quantityInput = view.findViewById(R.id.quantity_input);
        realLayout = view.findViewById(R.id.real_layout);
        realLayout.setPrefixText(Money.getCurrencySymbol().trim());
        realInput = view.findViewById(R.id.real_input);
        discountLayout = view.findViewById(R.id.discount_layout);
        discountInput = view.findViewById(R.id.discount_input);
        discountToggle = view.findViewById(R.id.discount_toggle);
        purchasedSwitch = view.findViewById(R.id.switch_purchased);
        notFoundSwitch = view.findViewById(R.id.switch_not_found);
        reasonGroup = view.findViewById(R.id.reason_group);
        reasonInput = view.findViewById(R.id.reason_input);
        substituteLayout = view.findViewById(R.id.substitute_layout);
        substituteInput = view.findViewById(R.id.substitute_input);
        productLayout = view.findViewById(R.id.product_layout);
        productNameInput = view.findViewById(R.id.product_name_input);

        String[] reasons = new String[NotFoundReason.values().length];
        for (int i = 0; i < reasons.length; i++) {
            reasons[i] = NotFoundReason.values()[i].getLabel();
        }
        reasonInput.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, reasons));
        reasonInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < NotFoundReason.values().length) {
                    reasonValue = NotFoundReason.values()[position].name();
                }
            }
        });

        view.findViewById(R.id.close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> save());
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());

        purchasedSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                notFoundSwitch.setChecked(false);
            }
            refreshState();
        });
        notFoundSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                purchasedSwitch.setChecked(false);
            }
            refreshState();
        });
        discountToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> refreshState());
        realInput.addTextChangedListener(new SimpleWatcher(this::refreshState));
        discountInput.addTextChangedListener(new SimpleWatcher(this::refreshState));
        quantityInput.addTextChangedListener(new SimpleWatcher(this::refreshState));

        DataGraph.shopping(requireContext().getApplicationContext())
                .loadItem(itemId, loaded -> {
                    if (loaded == null) {
                        dismiss();
                        return;
                    }
                    bind(loaded);
                });
    }

    private void bind(ItemRow value) {
        row = value;
        productNameInput.setText(value.productName);
        productNameInput.setEnabled(false);
        quantityInput.setText(Money.raw(value.quantity));
        realInput.setText(value.realUnitPrice == null ? "" : Money.raw(value.realUnitPrice));
        discountInput.setText(value.discountValue > 0 ? Money.raw(value.discountValue) : "");
        DiscountType type = DiscountType.fromName(value.discountType);
        discountToggle.check(type == DiscountType.PERCENT ? R.id.discount_percent
                : (type == DiscountType.FIXED ? R.id.discount_amount : R.id.discount_none));
        substituteInput.setText(value.substituteName);
        purchasedSwitch.setChecked(value.isPurchased);
        notFoundSwitch.setChecked(value.notFound);
        NotFoundReason reason = NotFoundReason.fromName(value.notFoundReason.isEmpty() ? null : value.notFoundReason);
        reasonValue = (reason == null ? NotFoundReason.OTHER : reason).name();
        reasonInput.setText((reason == null ? NotFoundReason.OTHER : reason).getLabel(), false);
        refreshState();
    }

    private void refreshState() {
        boolean purchased = purchasedSwitch.isChecked();
        boolean notFound = notFoundSwitch.isChecked();
        Ui.visible(realLayout, purchased);
        Ui.visible(discountLayout, purchased && discountType() != DiscountType.NONE);
        Ui.visible(reasonGroup, notFound);
        Ui.visible(substituteLayout, purchased);
        if (row == null) {
            return;
        }
        double quantity = readNumber(quantityInput, row.quantity);
        double real = purchased ? readNumber(realInput, 0d) : 0d;
        double discount = discountType() == DiscountType.NONE ? 0d : readNumber(discountInput, 0d);
        if (discountType() == DiscountType.PERCENT && (discount < 0d || discount > 100d)) {
            discount = Math.max(0d, Math.min(100d, discount));
        }
        double finalPrice = Pricing.finalUnitPrice(real, discountType(), discount);
        quantityLayout.setError(null);
        realLayout.setError(null);
        discountLayout.setError(null);
        if (finalPrice < 0d) {
            realLayout.setError(getString(R.string.msg_price_invalid));
        }
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

    private static double readNumber(TextInputEditText input, double fallback) {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (value.isEmpty()) {
            return fallback;
        }
        double parsed = Validators.parseMoney(value);
        return parsed < 0d ? fallback : parsed;
    }

    private void save() {
        if (row == null) {
            return;
        }
        double quantity = readNumber(quantityInput, row.quantity);
        if (quantity <= 0d) {
            quantityLayout.setError(getString(R.string.msg_quantity_invalid));
            return;
        }
        boolean purchased = purchasedSwitch.isChecked();
        boolean notFound = notFoundSwitch.isChecked();
        Double real = purchased ? readNumber(realInput, 0d) : null;
        DiscountType type = discountType();
        double discount = type == DiscountType.NONE ? 0d : readNumber(discountInput, 0d);
        if (type == DiscountType.PERCENT && discount > 100d) {
            discountLayout.setError(getString(R.string.msg_discount_invalid));
            return;
        }
        if (!Validators.isValidQuantity(quantity)) {
            quantityLayout.setError(getString(R.string.msg_quantity_invalid));
            return;
        }
        String substitute = substituteInput.getText() == null ? "" : substituteInput.getText().toString();
        String reason = notFound ? reasonValue : null;

        double estimated = row.estimatedUnitPrice;
        ListDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ListDetailViewModel.class);
        viewModel.saveItem(itemId, quantity, estimated, real, type, discount, purchased, notFound,
                reason, substitute);
        dismiss();
    }

    private void confirmDelete() {
        if (row == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_item_title)
                .setMessage(getString(R.string.msg_confirm_delete_item, row.productName))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    ListDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ListDetailViewModel.class);
                    viewModel.deleteItem(itemId);
                    dismiss();
                })
                .show();
    }

    private static class SimpleWatcher implements TextWatcher {
        private final Runnable action;

        SimpleWatcher(Runnable action) {
            this.action = action;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            action.run();
        }
    }
}
