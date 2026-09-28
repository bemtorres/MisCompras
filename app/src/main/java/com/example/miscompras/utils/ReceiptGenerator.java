package com.example.miscompras.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.FileProvider;

import com.example.miscompras.R;
import com.example.miscompras.data.local.model.ItemRow;
import com.example.miscompras.data.local.model.ListSummary;
import com.example.miscompras.domain.ListStatus;
import com.example.miscompras.domain.Pricing;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Generador de imagen de comprobante / recibo para listas de compras,
 * optimizado para compartir en WhatsApp u otras aplicaciones con el logo
 * y branding oficial de "Mis Compras".
 */
public final class ReceiptGenerator {

    private static final String TAG = "ReceiptGenerator";

    private ReceiptGenerator() {
    }

    /**
     * Muestra un diálogo ergonómico para compartir el comprobante por WhatsApp u otras aplicaciones.
     */
    public static void showShareDialog(Context context, ListSummary summary, List<ItemRow> items) {
        if (context == null || summary == null) {
            return;
        }

        CharSequence[] options = new CharSequence[]{
                context.getString(R.string.share_receipt_wsp),
                context.getString(R.string.share_receipt_other)
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.share_receipt_dialog_title)
                .setMessage(R.string.share_receipt_dialog_desc)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        shareToWhatsApp(context, summary, items);
                    } else {
                        shareGeneral(context, summary, items);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    /**
     * Comparte el comprobante directamente a WhatsApp (o WhatsApp Business) si está instalado,
     * o muestra el selector general automáticamente.
     */
    public static void shareToWhatsApp(Context context, ListSummary summary, List<ItemRow> items) {
        Uri uri = generateReceiptUri(context, summary, items);
        if (uri == null) {
            Toast.makeText(context, R.string.share_receipt_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = buildCaptionText(context, summary);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, caption);
        intent.setClipData(ClipData.newRawUri("comprobante", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Conceder permisos explícitos para WhatsApp normal y Business
        grantUriPermissionSafe(context, "com.whatsapp", uri);
        grantUriPermissionSafe(context, "com.whatsapp.w4b", uri);

        // 1. Probar WhatsApp estándar
        intent.setPackage("com.whatsapp");
        try {
            context.startActivity(intent);
            return;
        } catch (Exception e1) {
            Log.d(TAG, "WhatsApp normal no disponible, intentando WhatsApp Business: " + e1.getMessage());
        }

        // 2. Probar WhatsApp Business
        intent.setPackage("com.whatsapp.w4b");
        try {
            context.startActivity(intent);
            return;
        } catch (Exception e2) {
            Log.d(TAG, "WhatsApp Business no disponible, abriendo selector general: " + e2.getMessage());
        }

        // 3. Fallback a selector nativo general
        shareGeneralWithUri(context, uri, caption);
    }

    /**
     * Abre el selector estándar de Android para compartir el comprobante de imagen.
     */
    public static void shareGeneral(Context context, ListSummary summary, List<ItemRow> items) {
        Uri uri = generateReceiptUri(context, summary, items);
        if (uri == null) {
            Toast.makeText(context, R.string.share_receipt_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = buildCaptionText(context, summary);
        shareGeneralWithUri(context, uri, caption);
    }

    private static void shareGeneralWithUri(Context context, Uri uri, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, caption);
        intent.setClipData(ClipData.newRawUri("comprobante", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        grantPermissions(context, intent, uri);

        Intent chooser = Intent.createChooser(intent, context.getString(R.string.share_receipt_title));
        chooser.setClipData(ClipData.newRawUri("comprobante", uri));
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            context.startActivity(chooser);
        } catch (Exception ex) {
            Log.e(TAG, "Error iniciando chooser de compartición", ex);
            Toast.makeText(context, R.string.share_receipt_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Genera la imagen del comprobante y retorna su URI segura de FileProvider.
     */
    public static Uri generateReceiptUri(Context context, ListSummary summary, List<ItemRow> items) {
        try {
            Bitmap bitmap = createReceiptBitmap(context, summary, items);
            if (bitmap == null) {
                Log.e(TAG, "Bitmap generado es nulo");
                return null;
            }

            File cacheDir = new File(context.getCacheDir(), "receipts");
            if (!cacheDir.exists()) {
                boolean created = cacheDir.mkdirs();
                if (!created && !cacheDir.exists()) {
                    cacheDir = context.getCacheDir();
                }
            }

            File file = new File(cacheDir, "comprobante_miscompras_" + summary.id + ".png");
            if (file.exists()) {
                file.delete();
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            }

            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (Throwable t) {
            Log.e(TAG, "Error en generateReceiptUri", t);
            return null;
        }
    }

    /**
     * Construye y dibuja la vista del comprobante a un Bitmap de alta resolución.
     */
    public static Bitmap createReceiptBitmap(Context context, ListSummary summary, List<ItemRow> items) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View ticketView = inflater.inflate(R.layout.view_receipt_ticket, null, false);

            // Ancho fijo de 800px para máxima nitidez en WhatsApp
            int widthPx = 800;
            ticketView.setLayoutParams(new ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Logo
            ImageView logoView = ticketView.findViewById(R.id.receipt_logo);
            try {
                Drawable logoDrawable = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher_mc);
                if (logoDrawable != null) {
                    logoView.setImageDrawable(logoDrawable);
                } else {
                    logoView.setImageResource(R.drawable.ic_cart);
                }
            } catch (Throwable t) {
                try {
                    logoView.setImageResource(R.drawable.ic_cart);
                } catch (Throwable ignored) {
                }
            }

            // Estado y fecha
            TextView statusBadge = ticketView.findViewById(R.id.receipt_status_badge);
            boolean isCompleted = ListStatus.COMPLETED.name().equalsIgnoreCase(summary.status);
            if (isCompleted) {
                statusBadge.setText(R.string.status_completed);
                statusBadge.setTextColor(0xFF15803D);
                statusBadge.setBackgroundColor(0xFFDCFCE7);
            } else {
                statusBadge.setText(R.string.status_in_progress);
                statusBadge.setTextColor(0xFF1D4ED8);
                statusBadge.setBackgroundColor(0xFFDBEAFE);
            }

            TextView dateView = ticketView.findViewById(R.id.receipt_date);
            long dateToUse = summary.completedDate != null && summary.completedDate > 0
                    ? summary.completedDate
                    : (summary.plannedDate > 0 ? summary.plannedDate : System.currentTimeMillis());
            dateView.setText(Dates.format(dateToUse, "dd/MM/yyyy · HH:mm"));

            // Información de la lista
            TextView listNameView = ticketView.findViewById(R.id.receipt_list_name);
            listNameView.setText(summary.name != null && !summary.name.trim().isEmpty() ? summary.name : "Lista de compras");

            TextView listTypeView = ticketView.findViewById(R.id.receipt_list_type);
            listTypeView.setText(formatListType(context, summary.type) + " · "
                    + context.getString(R.string.meta_products, summary.itemCount));

            // Totales financieros
            TextView totalLabel = ticketView.findViewById(R.id.receipt_total_label);
            TextView totalValue = ticketView.findViewById(R.id.receipt_total_value);
            TextView estimatedValue = ticketView.findViewById(R.id.receipt_estimated_value);
            TextView discountValue = ticketView.findViewById(R.id.receipt_discount_value);
            TextView diffLabel = ticketView.findViewById(R.id.receipt_diff_label);
            TextView diffValue = ticketView.findViewById(R.id.receipt_diff_value);

            estimatedValue.setText(Money.format(summary.estimatedTotal));
            discountValue.setText(summary.totalDiscount > 0 ? "−" + Money.format(summary.totalDiscount) : "$0");

            if (isCompleted || summary.realTotal > 0) {
                totalLabel.setText(R.string.summary_real);
                totalValue.setText(Money.format(summary.realTotal));

                double delta = Pricing.difference(summary.estimatedTotal, summary.realTotal);
                if (delta < 0) {
                    diffLabel.setText(R.string.label_savings);
                    diffValue.setText("+" + Money.format(-delta));
                    diffValue.setTextColor(0xFF16A34A);
                } else if (delta > 0) {
                    diffLabel.setText(R.string.label_more_paid);
                    diffValue.setText("−" + Money.format(delta));
                    diffValue.setTextColor(0xFFDC2626);
                } else {
                    diffLabel.setText(R.string.label_difference);
                    diffValue.setText(Money.format(0));
                    diffValue.setTextColor(0xFF475569);
                }
            } else {
                totalLabel.setText(R.string.summary_estimated);
                totalValue.setText(Money.format(summary.estimatedTotal));
                diffLabel.setText(R.string.label_difference);
                diffValue.setText("—");
                diffValue.setTextColor(0xFF475569);
            }

            // Filas dinámicas de productos
            LinearLayout itemsContainer = ticketView.findViewById(R.id.receipt_items_container);
            itemsContainer.removeAllViews();

            if (items != null) {
                for (ItemRow row : items) {
                    View rowView = inflater.inflate(R.layout.view_receipt_item_row, itemsContainer, false);

                    TextView iconView = rowView.findViewById(R.id.receipt_item_icon);
                    TextView nameView = rowView.findViewById(R.id.receipt_item_name);
                    TextView noteView = rowView.findViewById(R.id.receipt_item_note);
                    TextView qtyView = rowView.findViewById(R.id.receipt_item_qty);
                    TextView unitPriceView = rowView.findViewById(R.id.receipt_item_unit_price);
                    TextView totalItemView = rowView.findViewById(R.id.receipt_item_total);
                    TextView statusView = rowView.findViewById(R.id.receipt_item_status);

                    iconView.setText(row.productIcon != null && !row.productIcon.isEmpty() ? row.productIcon : "🛍️");
                    nameView.setText(row.productName != null ? row.productName : "");

                    if (row.notFound) {
                        noteView.setText(R.string.status_not_found);
                        noteView.setTextColor(0xFFDC2626);
                        noteView.setVisibility(View.VISIBLE);
                    } else if (row.discountValue > 0) {
                        noteView.setText(context.getString(R.string.summary_discount) + ": " + Money.format(row.discountValue));
                        noteView.setTextColor(0xFF16A34A);
                        noteView.setVisibility(View.VISIBLE);
                    } else {
                        noteView.setVisibility(View.GONE);
                    }

                    qtyView.setText("x " + Money.quantity(row.quantity));

                    double unitPrice = row.computeEffectiveUnit();
                    unitPriceView.setText(Money.format(unitPrice));

                    double subtotal = (row.isPurchased && row.realSubtotal > 0) ? row.realSubtotal : row.estimatedSubtotal;
                    totalItemView.setText(Money.format(subtotal));

                    if (row.isPurchased) {
                        statusView.setText("✓");
                        statusView.setTextColor(0xFF16A34A);
                    } else if (row.notFound) {
                        statusView.setText("✕");
                        statusView.setTextColor(0xFFDC2626);
                    } else {
                        statusView.setText("⏳");
                        statusView.setTextColor(0xFF94A3B8);
                    }

                    itemsContainer.addView(rowView);
                }
            }

            // Resumen inferior de conteos
            TextView summaryCountsView = ticketView.findViewById(R.id.receipt_summary_counts);
            StringBuilder countText = new StringBuilder();
            countText.append("✓ ").append(context.getString(R.string.meta_progress, summary.purchasedCount, summary.itemCount))
                    .append(" ").append(context.getString(R.string.status_purchased).toLowerCase());
            if (summary.notFoundCount > 0) {
                countText.append(" · ").append(context.getString(R.string.summary_not_found, summary.notFoundCount));
            }
            summaryCountsView.setText(countText.toString());

            // Código de ticket digital
            TextView codeView = ticketView.findViewById(R.id.receipt_ticket_code);
            codeView.setText(context.getString(R.string.receipt_ticket_hash, summary.id));

            // Medición y renderizado
            ticketView.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int heightPx = Math.max(ticketView.getMeasuredHeight(), 400);
            ticketView.layout(0, 0, widthPx, heightPx);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            ticketView.draw(canvas);

            return bitmap;
        } catch (Throwable t) {
            Log.e(TAG, "Error en createReceiptBitmap", t);
            return null;
        }
    }

    private static String formatListType(Context context, String type) {
        if ("WEEKLY".equalsIgnoreCase(type)) {
            return context.getString(R.string.type_weekly);
        } else if ("BIWEEKLY".equalsIgnoreCase(type)) {
            return context.getString(R.string.type_biweekly);
        } else if ("MONTHLY".equalsIgnoreCase(type)) {
            return context.getString(R.string.type_monthly);
        }
        return context.getString(R.string.type_other);
    }

    private static String buildCaptionText(Context context, ListSummary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *Mis Compras · Comprobante de Compra*\n");
        sb.append("📋 *Lista:* ").append(summary.name != null ? summary.name : "Lista").append("\n");

        if (ListStatus.COMPLETED.name().equalsIgnoreCase(summary.status) || summary.realTotal > 0) {
            sb.append("💰 *Total Pagado:* ").append(Money.format(summary.realTotal)).append("\n");
            double delta = Pricing.difference(summary.estimatedTotal, summary.realTotal);
            if (delta < 0) {
                sb.append("🎉 *Ahorro:* ").append(Money.format(-delta)).append("\n");
            }
        } else {
            sb.append("💰 *Total Estimado:* ").append(Money.format(summary.estimatedTotal)).append("\n");
        }

        sb.append("✅ *Comprados:* ").append(summary.purchasedCount).append(" de ").append(summary.itemCount).append("\n");
        sb.append("\n_Generado con la app Mis Compras_");
        return sb.toString();
    }

    private static void grantUriPermissionSafe(Context context, String packageName, Uri uri) {
        try {
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable t) {
            Log.d(TAG, "No se pudo otorgar permiso explícito a " + packageName + ": " + t.getMessage());
        }
    }

    private static void grantPermissions(Context context, Intent intent, Uri uri) {
        try {
            grantUriPermissionSafe(context, "com.whatsapp", uri);
            grantUriPermissionSafe(context, "com.whatsapp.w4b", uri);

            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> resInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                if (resolveInfo.activityInfo != null) {
                    grantUriPermissionSafe(context, resolveInfo.activityInfo.packageName, uri);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error otorgando permisos de URI", t);
        }
    }
}
