package com.example.miscompras.data;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Ejecución de tareas de base de datos fuera del hilo principal. */
public final class DataExecutors {

    private DataExecutors() {
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "miscompras-db");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void io(Runnable task) {
        IO.execute(task);
    }

    public static void main(Runnable task) {
        MAIN.post(task);
    }
}
