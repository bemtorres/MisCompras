package com.example.miscompras.ui.lists;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.ui.listdetail.ListDetailActivity;
import com.example.miscompras.ui.newlist.NewListActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Todas las listas de compra: activas, completadas e historial rápido. */
public class ListsFragment extends Fragment implements ListsAdapter.Listener {

    private ListsViewModel viewModel;
    private ListsAdapter adapter;
    private View emptyState;
    private TextView countLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ListsViewModel.class);
        emptyState = view.findViewById(R.id.empty_state);
        countLabel = view.findViewById(R.id.count_label);

        adapter = new ListsAdapter(this);
        androidx.recyclerview.widget.RecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);

        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipActive = view.findViewById(R.id.chip_active);
        Chip chipCompleted = view.findViewById(R.id.chip_completed);
        chipAll.setOnClickListener(v -> {
            viewModel.setFilter(ListsViewModel.FILTER_ALL);
            updateChips(chipAll, chipActive, chipCompleted);
        });
        chipActive.setOnClickListener(v -> {
            viewModel.setFilter(ListsViewModel.FILTER_ACTIVE);
            updateChips(chipActive, chipAll, chipCompleted);
        });
        chipCompleted.setOnClickListener(v -> {
            viewModel.setFilter(ListsViewModel.FILTER_COMPLETED);
            updateChips(chipCompleted, chipAll, chipActive);
        });

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_new_list);
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), NewListActivity.class)));
        view.findViewById(R.id.empty_new_list).setOnClickListener(v -> startActivity(new Intent(requireContext(), NewListActivity.class)));

        viewModel.getLists().observe(getViewLifecycleOwner(), this::bind);
    }

    private void updateChips(Chip selected, Chip... others) {
        for (Chip chip : others) {
            chip.setChecked(false);
        }
        selected.setChecked(true);
    }

    private void bind(List<ListSummary> lists) {
        adapter.submitList(lists);
        boolean empty = lists == null || lists.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        countLabel.setText(getString(R.string.meta_products, lists == null ? 0 : lists.size()));
    }

    @Override
    public void onClick(ListSummary summary) {
        Intent intent = new Intent(requireContext(), ListDetailActivity.class);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, summary.id);
        intent.putExtra(ListDetailActivity.EXTRA_LIST_NAME, summary.name);
        startActivity(intent);
    }

    @Override
    public void onLongClick(ListSummary summary) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_list_title)
                .setMessage(R.string.msg_confirm_delete_list)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> viewModel.delete(summary.id, result -> {
                    if (result != null && result.success) {
                        Snackbar.make(requireView(), R.string.msg_list_deleted, Snackbar.LENGTH_SHORT).show();
                    }
                }))
                .show();
    }
}
