package com.example.miscompras.ui.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.data.repository.ShoppingRepository;
import com.example.miscompras.data.repository.StatsRepository;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private final ShoppingRepository shopping;
    private final StatsRepository stats;

    private final LiveData<ListSummary> activeList;
    private final LiveData<List<ListSummary>> recentPurchases;
    private final MutableLiveData<StatsSnapshot> snapshot = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        shopping = DataGraph.shopping(application);
        stats = DataGraph.stats(application);
        activeList = shopping.observeActiveList();
        recentPurchases = shopping.observeCompletedLists();
        refreshStats();
    }

    public LiveData<ListSummary> getActiveList() {
        return activeList;
    }

    public LiveData<List<ListSummary>> getRecentPurchases() {
        return recentPurchases;
    }

    public LiveData<StatsSnapshot> getSnapshot() {
        return snapshot;
    }

    public void refreshStats() {
        stats.loadStats(8, snapshot::setValue);
    }

    public void startShopping(long listId) {
        shopping.startShopping(listId);
    }
}
