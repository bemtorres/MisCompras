package com.example.miscompras.ui.stats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.miscompras.R;
import com.example.miscompras.data.model.StatsSnapshot;
import com.example.miscompras.utils.Money;

import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras simple para el gasto por semana. */
public class WeekChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint estimatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<StatsSnapshot.Bucket> buckets = new ArrayList<>();
    private double max = 1d;

    public WeekChartView(Context context) {
        this(context, null);
    }

    public WeekChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(ContextCompat.getColor(context, R.color.app_primary));
        estimatePaint.setColor(ContextCompat.getColor(context, R.color.app_estimate_container));
        textPaint.setColor(ContextCompat.getColor(context, R.color.app_on_surface_variant));
        textPaint.setTextSize(28f);
    }

    public void setBuckets(List<StatsSnapshot.Bucket> values) {
        buckets.clear();
        if (values != null) {
            buckets.addAll(values);
        }
        max = 1d;
        for (StatsSnapshot.Bucket bucket : buckets) {
            max = Math.max(max, Math.max(bucket.total, bucket.estimated));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (buckets.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getContext().getString(R.string.stats_empty), getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }
        float labelHeight = 40f;
        float chartHeight = getHeight() - labelHeight;
        float available = getWidth() - getPaddingLeft() - getPaddingRight();
        float slot = available / buckets.size();
        float barWidth = Math.min(slot * 0.52f, 72f);
        for (int i = 0; i < buckets.size(); i++) {
            StatsSnapshot.Bucket bucket = buckets.get(i);
            float centerX = getPaddingLeft() + slot * i + slot / 2f;
            float estimatedHeight = (float) (bucket.estimated / max * chartHeight);
            if (bucket.estimated > 0d) {
                rect.set(centerX - barWidth / 2f, chartHeight - estimatedHeight,
                        centerX + barWidth / 2f, chartHeight);
                canvas.drawRoundRect(rect, barWidth / 3f, barWidth / 3f, estimatePaint);
            }
            float totalHeight = (float) (bucket.total / max * chartHeight);
            rect.set(centerX - barWidth / 3f, chartHeight - totalHeight,
                    centerX + barWidth / 3f, chartHeight);
            canvas.drawRoundRect(rect, barWidth / 4f, barWidth / 4f, barPaint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(bucket.label, centerX, getHeight() - 10f, textPaint);
            if (bucket.total > 0d) {
                String total = Money.compact(bucket.total);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTextSize(fitTextSize(total, slot));
                canvas.drawText(total, centerX, chartHeight - totalHeight - 10f, textPaint);
                textPaint.setTextSize(28f);
            }
        }
    }

    /** Reduce el tamaño del texto hasta que quepa en el espacio disponible. */
    private float fitTextSize(String text, float available) {
        float size = 22f;
        textPaint.setTextSize(size);
        while (size > 10f && textPaint.measureText(text) > available - 6f) {
            size -= 1f;
            textPaint.setTextSize(size);
        }
        return size;
    }
}
