package com.example.miscompras.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Dates {

    public static final String MONTH_YEAR = "MMMM yyyy";
    public static final String SHORT_DAY_MONTH = "d MMM";
    public static final String FULL = "d 'de' MMMM 'de' yyyy";
    public static final String NUMERIC = "dd/MM/yyyy";

    /**
     * La interfaz esta escrita en espanol, por lo que las fechas se formatean siempre en
     * espanol aunque el dispositivo este en otro idioma.
     */
    public static final Locale LOCALE = Locale.forLanguageTag("es-ES");

    private Dates() {
    }

    public static String format(long millis, String pattern) {
        if (millis <= 0L) {
            return "";
        }
        return new SimpleDateFormat(pattern, LOCALE).format(new Date(millis));
    }

    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(LOCALE) + value.substring(1);
    }

    public static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static long startOfMonth(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long endOfMonth(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfMonth(millis));
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.SECOND, -1);
        return calendar.getTimeInMillis();
    }

    public static long daysAgo(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return startOfDay(calendar.getTimeInMillis());
    }

    public static long weeksAgo(int weeks) {
        return daysAgo(weeks * 7);
    }

    /** Inicio de la semana (lunes) de la fecha indicada. */
    public static long startOfWeek(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfDay(millis));
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int diff = (dayOfWeek + 5) % 7; // lunes = 0
        calendar.add(Calendar.DAY_OF_MONTH, -diff);
        return calendar.getTimeInMillis();
    }

    public static boolean isSameDay(long a, long b) {
        return startOfDay(a) == startOfDay(b);
    }

    public static boolean isToday(long millis) {
        return isSameDay(millis, System.currentTimeMillis());
    }

    public static String relativeLabel(long millis) {
        long today = startOfDay(System.currentTimeMillis());
        long day = startOfDay(millis);
        long diff = (today - day) / (24 * 60 * 60 * 1000L);
        if (diff <= 0) {
            return "Hoy";
        }
        if (diff == 1) {
            return "Ayer";
        }
        if (diff < 7) {
            return "Hace " + diff + " días";
        }
        return format(millis, SHORT_DAY_MONTH);
    }
}
