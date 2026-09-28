package com.example.miscompras.data.model;

import com.example.miscompras.data.local.model.CategoryStat;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.local.model.MonthTotal;
import com.example.miscompras.data.local.model.ProductStat;

import java.util.ArrayList;
import java.util.List;

/** Fotografía de las estadísticas calculadas a partir del historial. */
public class StatsSnapshot {

    public double monthTotal;
    public double previousMonthTotal;
    public String monthLabel = "";
    public String previousMonthLabel = "";

    public double discountsThisMonth;
    public double averagePurchase;
    public int completedPurchases;

    public final List<MonthTotal> months = new ArrayList<>();
    public final List<Bucket> weeks = new ArrayList<>();
    public final List<ProductStat> topProducts = new ArrayList<>();
    public final List<ProductStat> topSpend = new ArrayList<>();
    public final List<CategoryStat> categories = new ArrayList<>();
    public final List<ProductStat> notFound = new ArrayList<>();

    public ListSummary lastPurchase;
    public ListSummary previousPurchase;

    /** Barra de gasto por semana. */
    public static class Bucket {
        public final String label;
        public final long startMillis;
        public double total;
        public double estimated;

        public Bucket(String label, long startMillis) {
            this.label = label;
            this.startMillis = startMillis;
        }
    }

    public boolean hasHistory() {
        return completedPurchases > 0;
    }

    public double monthDelta() {
        return monthTotal - previousMonthTotal;
    }

    public double monthDeltaPercent() {
        if (previousMonthTotal <= 0d) {
            return 0d;
        }
        return (monthTotal - previousMonthTotal) / previousMonthTotal * 100d;
    }

    public double lastPurchaseDelta() {
        if (lastPurchase == null || previousPurchase == null) {
            return 0d;
        }
        return lastPurchase.realTotal - previousPurchase.realTotal;
    }

    public double maxWeekTotal() {
        double max = 0d;
        for (Bucket bucket : weeks) {
            max = Math.max(max, bucket.total);
        }
        return max;
    }

    public double maxCategoryTotal() {
        double max = 0d;
        for (CategoryStat stat : categories) {
            max = Math.max(max, stat.total);
        }
        return max;
    }

    public double maxTopProductQuantity() {
        double max = 0d;
        for (ProductStat stat : topProducts) {
            max = Math.max(max, stat.quantity);
        }
        return max;
    }
}
