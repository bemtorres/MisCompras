package com.example.miscompras;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.dashboard.DashboardFragment;
import com.example.miscompras.ui.history.HistoryFragment;
import com.example.miscompras.ui.lists.ListsFragment;
import com.example.miscompras.ui.products.ProductsFragment;
import com.example.miscompras.ui.stats.StatsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.LinkedHashMap;
import java.util.Map;

/** Contenedor principal con navegación inferior entre las cinco secciones de la app. */
public class MainActivity extends AppCompatActivity {

    public static final String TAG_HOME = "home";
    public static final String TAG_LISTS = "lists";
    public static final String TAG_HISTORY = "history";
    public static final String TAG_STATS = "stats";
    public static final String EXTRA_TAB = "open_tab";

    private final Map<String, Fragment> fragments = new LinkedHashMap<>();
    private String currentTag = TAG_HOME;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        com.example.miscompras.utils.AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);

        com.example.miscompras.utils.Money.updateCurrency(this);

        if (!com.example.miscompras.utils.AppPreferences.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, com.example.miscompras.ui.onboarding.OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main_root);
        SystemBars.pad(root);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_lists) {
                return show(TAG_LISTS);
            }
            if (id == R.id.nav_history) {
                return show(TAG_HISTORY);
            }
            if (id == R.id.nav_stats) {
                return show(TAG_STATS);
            }
            return show(TAG_HOME);
        });
        bottomNavigation.setOnItemReselectedListener(item -> {
            // sin acción: se mantiene la pantalla actual
        });

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            show(TAG_HOME);
        } else {
            currentTag = savedInstanceState.getString("current_tag", TAG_HOME);
            show(currentTag);
        }
        applyRequestedTab(getIntent());
    }

    private String lastThemeKey = null;

    @Override
    protected void onResume() {
        super.onResume();
        com.example.miscompras.utils.Money.updateCurrency(this);
        String currentTheme = com.example.miscompras.utils.AppPreferences.getAppTheme(this);
        if (lastThemeKey == null) {
            lastThemeKey = currentTheme;
        } else if (!lastThemeKey.equalsIgnoreCase(currentTheme)) {
            lastThemeKey = currentTheme;
            recreate();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyRequestedTab(intent);
    }

    private void applyRequestedTab(@Nullable Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_TAB)) {
            return;
        }
        int tab = intent.getIntExtra(EXTRA_TAB, R.id.nav_home);
        intent.removeExtra(EXTRA_TAB);
        navigateTo(tab);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_tag", currentTag);
    }

    private boolean show(String tag) {
        if (tag.equals(currentTag) && fragments.containsKey(tag)) {
            return true;
        }
        Fragment target = fragments.get(tag);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        for (Fragment fragment : fragments.values()) {
            if (fragment.isAdded() && fragment != target) {
                transaction.hide(fragment);
            }
        }
        if (target == null) {
            target = create(tag);
            fragments.put(tag, target);
            transaction.add(R.id.nav_container, target, tag);
        } else {
            transaction.show(target);
        }
        transaction.commit();
        currentTag = tag;
        return true;
    }

    private Fragment create(String tag) {
        switch (tag) {
            case TAG_LISTS:
                return new ListsFragment();
            case TAG_HISTORY:
                return new HistoryFragment();
            case TAG_STATS:
                return new StatsFragment();
            case TAG_HOME:
            default:
                return new DashboardFragment();
        }
    }

    /** Permite navegar a una sección concreta desde cualquier pantalla. */
    public void navigateTo(int menuItemId) {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation.getSelectedItemId() != menuItemId) {
            bottomNavigation.setSelectedItemId(menuItemId);
        } else {
            show(tagFor(menuItemId));
        }
    }

    public static String tagFor(int menuItemId) {
        if (menuItemId == R.id.nav_lists) {
            return TAG_LISTS;
        }
        if (menuItemId == R.id.nav_history) {
            return TAG_HISTORY;
        }
        if (menuItemId == R.id.nav_stats) {
            return TAG_STATS;
        }
        return TAG_HOME;
    }
}
