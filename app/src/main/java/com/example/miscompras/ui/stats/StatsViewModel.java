package com.example.miscompras.ui.stats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.miscompras.data.DataGraph;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.data.repository.StatsRepository;

/** Estadísticas: gasto mensual, semanas, productos, categorías y faltantes. */
public class StatsViewModel extends AndroidViewModel {

    private final MutableLiveData<StatsSnapshot> stats = new MutableLiveData<>();

    public StatsViewModel(@NonNull Application application) {
        super(application);
        reload();
    }

    public LiveData<StatsSnapshot> getStats() {
        return stats;
    }

    public void reload() {
        StatsRepository repository = DataGraph.stats(getApplication());
        repository.loadStats(4, stats::setValue);
    }
}
