package com.example.miscompras.data.repository;

import com.example.miscompras.data.DataExecutors;
import com.example.miscompras.data.local.AppDatabase;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.data.local.model.MonthTotal;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.domain.Pricing;
import com.example.miscompras.utils.Dates;

import java.util.Calendar;
import java.util.List;

public class StatsRepository {

    public interface Callback<T> {
        void onResult(T value);
    }

    private final AppDatabase database;

    public StatsRepository(AppDatabase database) {
        this.database = database;
    }

    public void loadStats(int weekBuckets, Callback<StatsSnapshot> callback) {
        DataExecutors.io(() -> {
            StatsSnapshot snapshot = new StatsSnapshot();
            List<MonthTotal> months = database.shoppingListDao().monthsSync(12);
            snapshot.months.addAll(months);

            long now = System.currentTimeMillis();
            long currentMonthStart = Dates.startOfMonth(now);
            long previousMonthStart = shiftMonth(currentMonthStart, -1);

            snapshot.monthLabel = Dates.capitalize(Dates.format(currentMonthStart, Dates.MONTH_YEAR));
            snapshot.previousMonthLabel = Dates.capitalize(Dates.format(previousMonthStart, Dates.MONTH_YEAR));
            snapshot.monthTotal = totalFor(months, currentMonthStart);
            snapshot.previousMonthTotal = totalFor(months, previousMonthStart);
            snapshot.discountsThisMonth = database.priceHistoryDao().sumDiscountSync(
                    currentMonthStart, Dates.endOfMonth(now));

            List<ListSummary> completed = database.shoppingListDao().completedSync();
            snapshot.completedPurchases = completed.size();
            if (!completed.isEmpty()) {
                snapshot.lastPurchase = completed.get(0);
                snapshot.averagePurchase = completed.stream().mapToDouble(list -> list.realTotal).average().orElse(0d);
            }
            if (completed.size() > 1) {
                snapshot.previousPurchase = completed.get(1);
            }

            long firstWeek = Dates.startOfWeek(now) - (weekBuckets - 1) * 7L * 24L * 60L * 60L * 1000L;
            for (int i = 0; i < weekBuckets; i++) {
                long start = firstWeek + i * 7L * 24L * 60L * 60L * 1000L;
                snapshot.weeks.add(new StatsSnapshot.Bucket(weekLabel(start), start));
            }
            for (ListSummary list : completed) {
                long date = list.completedDate == null ? list.plannedDate : list.completedDate;
                for (StatsSnapshot.Bucket bucket : snapshot.weeks) {
                    if (date >= bucket.startMillis && date < bucket.startMillis + 7L * 24L * 60L * 60L * 1000L) {
                        bucket.total = Pricing.round(bucket.total + list.realTotal);
                        bucket.estimated = Pricing.round(bucket.estimated + list.estimatedTotal);
                        break;
                    }
                }
            }

            snapshot.topProducts.addAll(database.statsDao().topByQuantitySync(5));
            snapshot.topSpend.addAll(database.statsDao().topBySpendSync(5));
            snapshot.categories.addAll(database.statsDao().byCategorySync(6));
            snapshot.notFound.addAll(database.statsDao().mostNotFoundSync(4));

            DataExecutors.main(() -> callback.onResult(snapshot));
        });
    }

    private static double totalFor(List<MonthTotal> months, long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        String period = String.format(java.util.Locale.getDefault(), "%04d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
        for (MonthTotal month : months) {
            if (month.period.equals(period)) {
                return month.total;
            }
        }
        return 0d;
    }

    private static long shiftMonth(long millis, int delta) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.MONTH, delta);
        return calendar.getTimeInMillis();
    }

    private static String weekLabel(long startMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startMillis);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return Dates.capitalize(Dates.format(startMillis, "d MMM"));
    }
}
