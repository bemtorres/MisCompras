package com.example.miscompras.ui.listdetail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.miscompras.R;
import com.example.miscompras.ui.common.Ui;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Resumen final de la compra: estimado, real, descuentos y diferencia. */
public class FinishSummarySheet extends BottomSheetDialogFragment {

    private static final String ARG_ESTIMATED = "estimated";
    private static final String ARG_REAL = "real";
    private static final String ARG_DISCOUNT = "discount";
    private static final String ARG_DIFFERENCE = "difference";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_PURCHASED = "purchased";
    private static final String ARG_TOTAL = "total";
    private static final String ARG_PENDING = "pending";
    private static final String ARG_NOT_FOUND = "not_found";

    public static FinishSummarySheet create(String estimated, String real, String discount, String difference,
                                            int purchased, int total, int notFound, int pending) {
        FinishSummarySheet sheet = new FinishSummarySheet();
        Bundle args = new Bundle();
        args.putString(ARG_ESTIMATED, estimated);
        args.putString(ARG_REAL, real);
        args.putString(ARG_DISCOUNT, discount);
        args.putString(ARG_DIFFERENCE, difference);
        args.putInt(ARG_PURCHASED, purchased);
        args.putInt(ARG_TOTAL, total);
        args.putInt(ARG_NOT_FOUND, notFound);
        args.putInt(ARG_PENDING, pending);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_MisCompras_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_finish_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        int purchased = args.getInt(ARG_PURCHASED);
        int total = args.getInt(ARG_TOTAL);
        int notFound = args.getInt(ARG_NOT_FOUND);
        int pending = args.getInt(ARG_PENDING);
        String difference = args.getString(ARG_DIFFERENCE, "");

        ((TextView) view.findViewById(R.id.summary_items)).setText(getString(R.string.summary_items, purchased, total));
        ((TextView) view.findViewById(R.id.summary_estimated)).setText(args.getString(ARG_ESTIMATED, "—"));
        ((TextView) view.findViewById(R.id.summary_real)).setText(args.getString(ARG_REAL, "—"));
        ((TextView) view.findViewById(R.id.summary_discount)).setText(args.getString(ARG_DISCOUNT, "—"));
        TextView differenceView = view.findViewById(R.id.summary_difference);
        differenceView.setText(difference.startsWith("-") ? getString(R.string.label_savings) + " " + difference.substring(1)
                : difference.isEmpty() ? "—" : getString(R.string.label_more_paid) + " " + difference);
        differenceView.setTextColor(Ui.color(requireContext(), difference.startsWith("-")
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurface));

        TextView pendingView = view.findViewById(R.id.summary_pending);
        if (pending > 0) {
            StringBuilder text = new StringBuilder(getString(R.string.summary_pending, pending));
            if (notFound > 0) {
                text.append(" · ").append(getString(R.string.summary_not_found, notFound));
            }
            pendingView.setText(text.toString());
            pendingView.setVisibility(View.VISIBLE);
        } else if (notFound > 0) {
            pendingView.setText(getString(R.string.summary_not_found, notFound));
            pendingView.setVisibility(View.VISIBLE);
        } else {
            pendingView.setVisibility(View.GONE);
        }

        View shareButton = view.findViewById(R.id.summary_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                if (getActivity() instanceof ListDetailActivity) {
                    ((ListDetailActivity) getActivity()).shareReceipt();
                }
            });
        }

        view.findViewById(R.id.summary_history).setOnClickListener(v -> {
            if (getActivity() instanceof ListDetailActivity) {
                ((ListDetailActivity) getActivity()).openHistoryTab();
            }
            dismiss();
        });
        view.findViewById(R.id.summary_close).setOnClickListener(v -> {
            if (getActivity() instanceof ListDetailActivity) {
                ((ListDetailActivity) getActivity()).onPurchaseFinished();
            }
            dismiss();
        });
    }
}
