package com.example.miscompras.ui.lists;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.domain.ListStatus;

import java.util.ArrayList;
import java.util.List;

public class ListsViewModel extends AndroidViewModel {

    public static final int FILTER_ACTIVE = 0;
    public static final int FILTER_COMPLETED = 1;
    public static final int FILTER_ALL = 2;

    private final ShoppingRepository shopping;
    private final LiveData<List<ListSummary>> allLists;
    private final MediatorLiveData<List<ListSummary>> filtered = new MediatorLiveData<>();
    private int filter = FILTER_ALL;

    public ListsViewModel(@NonNull Application application) {
        super(application);
        shopping = DataGraph.shopping(application);
        allLists = shopping.observeAllLists();
        filtered.addSource(allLists, value -> publish());
    }

    public LiveData<List<ListSummary>> getLists() {
        return filtered;
    }

    public void setFilter(int filter) {
        if (this.filter != filter) {
            this.filter = filter;
            publish();
        }
    }

    private void publish() {
        List<ListSummary> result = new ArrayList<>();
        List<ListSummary> source = allLists.getValue();
        if (source != null) {
            for (ListSummary summary : source) {
                if (matches(summary)) {
                    result.add(summary);
                }
            }
        }
        filtered.setValue(result);
    }

    private boolean matches(ListSummary summary) {
        ListStatus status = ListStatus.fromName(summary.status);
        switch (filter) {
            case FILTER_ACTIVE:
                return status == ListStatus.PLANNED || status == ListStatus.IN_PROGRESS;
            case FILTER_COMPLETED:
                return status == ListStatus.COMPLETED;
            case FILTER_ALL:
            default:
                return status != ListStatus.CANCELLED;
        }
    }

    public void delete(long listId, ShoppingRepository.Callback<ShoppingRepository.Result> callback) {
        shopping.deleteList(listId, callback);
    }
}
