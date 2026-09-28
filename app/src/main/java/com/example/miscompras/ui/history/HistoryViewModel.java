package com.example.miscompras.ui.history;

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

/** Historial de compras: listas completadas y resumen del mes. */
public class HistoryViewModel extends AndroidViewModel {

    private final LiveData<List<ListSummary>> purchases;
    private final MutableLiveData<StatsSnapshot> stats = new MutableLiveData<>();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        ShoppingRepository shopping = DataGraph.shopping(application);
        StatsRepository repository = DataGraph.stats(application);
        purchases = shopping.observeCompletedLists();
        repository.loadStats(4, stats::setValue);
    }

    public LiveData<List<ListSummary>> getPurchases() {
        return purchases;
    }

    public LiveData<StatsSnapshot> getStats() {
        return stats;
    }

    public void reload() {
        DataGraph.stats(getApplication()).loadStats(4, stats::setValue);
    }
}
