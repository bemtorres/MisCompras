package com.example.miscompras.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

import com.google.android.material.color.MaterialColors;

public final class Ui {

    private Ui() {
    }

    @ColorInt
    public static int color(Context context, @AttrRes int attr) {
        return MaterialColors.getColor(context, attr, Color.GRAY);
    }

    public static void tint(ImageView view, @ColorInt int color) {
        view.setColorFilter(color);
    }

    public static void setTextColor(TextView view, @ColorInt int color) {
        view.setTextColor(color);
    }

    public static void visible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Disco de color para los avatares de categoría. */
    public static GradientDrawable circle(@ColorInt int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    @ColorInt
    public static int parseColor(String hex, @ColorInt int fallback) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }
}
