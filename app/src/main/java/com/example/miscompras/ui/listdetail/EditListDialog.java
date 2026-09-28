package com.example.miscompras.ui.listdetail;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.ListType;
import com.example.miscompras.utils.Dates;
import com.example.miscompras.utils.Validators;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

/** Permite cambiar el nombre, la periodicidad y la fecha de una lista. */
public class EditListDialog extends BottomSheetDialogFragment {

    private static final String ARG_LIST_ID = "list_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_TYPE = "type";

    public static EditListDialog newInstance(ListSummary summary) {
        EditListDialog dialog = new EditListDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_LIST_ID, summary == null ? 0L : summary.id);
        args.putString(ARG_NAME, summary == null ? "" : summary.name);
        args.putString(ARG_TYPE, summary == null ? ListType.WEEKLY.name() : summary.type);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_MisCompras_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_edit_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        long listId = args.getLong(ARG_LIST_ID);
        ListType current = ListType.fromName(args.getString(ARG_TYPE));

        TextInputLayout nameLayout = view.findViewById(R.id.name_layout);
        TextInputEditText nameInput = view.findViewById(R.id.name_input);
        MaterialButtonToggleGroup typeToggle = view.findViewById(R.id.type_toggle);
        MaterialButton dateButton = view.findViewById(R.id.date_today);
        long[] plannedDate = {System.currentTimeMillis()};

        nameInput.setText(args.getString(ARG_NAME, ""));
        typeToggle.check(buttonIdFor(current));
        dateButton.setText(Dates.format(plannedDate[0], Dates.NUMERIC));
        dateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(plannedDate[0]);
            new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, 12, 0, 0);
                picked.set(Calendar.MILLISECOND, 0);
                plannedDate[0] = picked.getTimeInMillis();
                dateButton.setText(Dates.format(plannedDate[0], Dates.NUMERIC));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        view.findViewById(R.id.close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (!Validators.isValidName(name)) {
                nameLayout.setError(getString(R.string.msg_name_required));
                return;
            }
            ListType type = typeFor(typeToggle.getCheckedButtonId());
            DataGraph.shopping(requireContext().getApplicationContext())
                    .updateHeader(listId, name, type, plannedDate[0]);
            ListDetailViewModel viewModel = new ViewModelProvider(requireActivity()).get(ListDetailViewModel.class);
            viewModel.notify(getString(R.string.msg_list_updated));
            dismiss();
        });
    }

    private static int buttonIdFor(ListType type) {
        switch (type) {
            case BIWEEKLY:
                return R.id.type_biweekly;
            case MONTHLY:
                return R.id.type_monthly;
            case OTHER:
                return R.id.type_other;
            case WEEKLY:
            default:
                return R.id.type_weekly;
        }
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
