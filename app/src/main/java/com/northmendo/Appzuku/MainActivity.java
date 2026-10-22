package com.northmendo.Appzuku;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.northmendo.Appzuku.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import android.widget.PopupMenu;
import android.widget.Toast;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;

import rikka.shizuku.Shizuku;

import static com.northmendo.Appzuku.PreferenceKeys.*;
import static com.northmendo.Appzuku.AppConstants.*;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 1;
    private static final String FORK_URL = AppConstants.FORK_URL;
    private static final String REPLACEMENT_NOTICE_VERSION = "1.3.17-fork.2";

    private ActivityMainBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ShellManager shellManager;
    private BackgroundAppManager appManager;
    private RamMonitor ramMonitor;
    private BackgroundAppsRecyclerViewAdapter listAdapter;
    private final List<AppModel> appsDataList = new ArrayList<>();
    private final List<AppModel> fullAppsList = new ArrayList<>();
    private String currentSearchQuery = "";
    private int currentSortMode = AppConstants.SORT_MODE_DEFAULT;
    private MenuItem selectAllItem;
    private MenuItem unselectAllItem;

    // Handle Shizuku permission results
    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener = (requestCode, grantResult) -> {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            loadBackgroundApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        NOTIFICATION_PERMISSION_CODE);
            }
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar with colors from resources
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(Color.WHITE);

        // Initialize components
        shellManager = new ShellManager(this, handler, executor);
        appManager = new BackgroundAppManager(this, handler, executor, shellManager);
        ramMonitor = new RamMonitor(handler, binding.ramUsage, binding.ramUsageText);

        listAdapter = new BackgroundAppsRecyclerViewAdapter(this);
        binding.recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        binding.recyclerView.setAdapter(listAdapter);

        // Configure listeners
        setupListeners();

        // Initialize SharedPreferences and load settings
        loadSettingsAndApplyToManager();

        // Initialize Shizuku and load apps
        shellManager.setShizukuPermissionListener(shizukuPermissionListener);
        shellManager.checkShellPermissions();
        loadBackgroundApps();
        updatePermissionNotice();
        handler.postDelayed(this::updatePermissionNotice, 1500);
        ramMonitor.startMonitoring();
        maybeShowOnboarding();
        maybeShowReplacementNotice();
    }

    /** La intro se muestra una sola vez, antes de que la gente se pierda con la lista vacia. */
    private void maybeShowOnboarding() {
        if (sharedPreferences.getBoolean(KEY_ONBOARDING_SHOWN, false)) {
            return;
        }
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply();
        startActivity(new Intent(this, OnboardingActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload settings in case they changed in SettingsActivity
        loadSettingsAndApplyToManager();
        loadBackgroundApps();
        updatePermissionNotice();
    }

    private void loadSettingsAndApplyToManager() {
        boolean showSystemApps = sharedPreferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false);
        boolean showPersistentApps = sharedPreferences.getBoolean(KEY_SHOW_PERSISTENT_APPS, false);
        currentSortMode = sharedPreferences.getInt(KEY_SORT_MODE, AppConstants.SORT_MODE_DEFAULT);
        appManager.setShowSystemApps(showSystemApps);
        appManager.setShowPersistentApps(showPersistentApps);
    }

    // Setup event listeners
    private void setupListeners() {
        binding.swiperefreshlayout1.setOnRefreshListener(this::loadBackgroundApps);
        binding.fab.setOnClickListener(view -> killSelectedApps());
        binding.permissionNoticeButton.setOnClickListener(v -> shellManager.checkShellPermissions());
        binding.permissionNoticeHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        listAdapter.setOnAppActionListener(new BackgroundAppsRecyclerViewAdapter.OnAppActionListener() {
            @Override
            public void onKillApp(AppModel app, int position) {
                appManager.killApp(app.getPackageName(), MainActivity.this::loadBackgroundApps);
            }

            @Override
            public void onToggleWhitelist(AppModel app, int position) {
                boolean isNowWhitelisted = appManager.toggleWhitelist(app.getPackageName());
                app.setWhitelisted(isNowWhitelisted);
                listAdapter.notifyItemChanged(position);

                Toast.makeText(MainActivity.this, isNowWhitelisted
                        ? R.string.toast_added_whitelist
                        : R.string.toast_removed_whitelist, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAppClick(AppModel app, int position) {
                if (app.isProtected() || app.isWhitelisted()) {
                    return;
                }
                app.setSelected(!app.isSelected());
                listAdapter.notifyItemChanged(position);
                updateSelectMenuVisibility();
            }

            @Override
            public void onOverflowClick(AppModel app, View anchor) {
                showAppOptionsMenu(app, anchor);
            }
        });
    }

    private void showAppOptionsMenu(AppModel app, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_app_options, popup.getMenu());

        // Hide uninstall for system apps
        if (app.isSystemApp()) {
            popup.getMenu().findItem(R.id.action_uninstall).setVisible(false);
        }

        // Set checkmarks for current list memberships
        String packageName = app.getPackageName();
        popup.getMenu().findItem(R.id.action_whitelist).setChecked(
                appManager.getWhitelistedApps().contains(packageName));
        popup.getMenu().findItem(R.id.action_blacklist).setChecked(
                appManager.getBlacklistedApps().contains(packageName));
        popup.getMenu().findItem(R.id.action_hidden).setChecked(
                appManager.getHiddenApps().contains(packageName));
        MenuItem restrictionItem = popup.getMenu().findItem(R.id.action_background_restriction);
        restrictionItem.setVisible(appManager.supportsBackgroundRestriction());
        restrictionItem.setChecked(app.isBackgroundRestrictionDesired());
        restrictionItem.setTitle(getBackgroundRestrictionMenuTitle(app));

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_app_info) {
                openAppInfo(packageName);
                return true;
            } else if (id == R.id.action_uninstall) {
                showUninstallConfirmation(app);
                return true;
            } else if (id == R.id.action_whitelist) {
                toggleListMembership(app, "whitelist");
                return true;
            } else if (id == R.id.action_blacklist) {
                toggleListMembership(app, "blacklist");
                return true;
            } else if (id == R.id.action_hidden) {
                toggleListMembership(app, "hidden");
                return true;
            } else if (id == R.id.action_background_restriction) {
                toggleBackgroundRestriction(app);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void openAppInfo(String packageName) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_app_info_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showUninstallConfirmation(AppModel app) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.uninstall_dialog_title, app.getAppName()))
                .setMessage(R.string.uninstall_dialog_body)
                .setPositiveButton(R.string.opt_uninstall, (dialog, which) -> {
                    appManager.uninstallPackage(app.getPackageName(), this::loadBackgroundApps);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void maybeShowReplacementNotice() {
        String shownVersion = sharedPreferences.getString(KEY_REPLACEMENT_NOTICE_SHOWN_VERSION, null);
        if (REPLACEMENT_NOTICE_VERSION.equals(shownVersion)) {
            return;
        }

        sharedPreferences.edit()
                .putString(KEY_REPLACEMENT_NOTICE_SHOWN_VERSION, REPLACEMENT_NOTICE_VERSION)
                .apply();

        new AlertDialog.Builder(this)
                .setTitle(R.string.replacement_notice_title)
                .setMessage(R.string.replacement_notice_message)
                .setPositiveButton(R.string.replacement_notice_action_view, (dialog, which) -> {
                    openUrl(FORK_URL);
                })
                .setNegativeButton(R.string.replacement_notice_action_dismiss, null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.url_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleListMembership(AppModel app, String listType) {
        String packageName = app.getPackageName();
        Set<String> currentSet;
        int addedMsg, removedMsg;

        switch (listType) {
            case "whitelist":
                currentSet = appManager.getWhitelistedApps();
                addedMsg = R.string.toast_added_whitelist;
                removedMsg = R.string.toast_removed_whitelist;
                break;
            case "blacklist":
                currentSet = appManager.getBlacklistedApps();
                addedMsg = R.string.toast_added_blacklist;
                removedMsg = R.string.toast_removed_blacklist;
                break;
            case "hidden":
                currentSet = appManager.getHiddenApps();
                addedMsg = R.string.toast_app_hidden;
                removedMsg = R.string.toast_app_unhidden;
                break;
            default:
                return;
        }

        boolean wasInList = currentSet.contains(packageName);
        if (wasInList) {
            currentSet.remove(packageName);
        } else {
            currentSet.add(packageName);
        }

        // Save the updated set
        switch (listType) {
            case "whitelist":
                appManager.saveWhitelistedApps(currentSet);
                app.setWhitelisted(!wasInList);
                break;
            case "blacklist":
                appManager.saveBlacklistedApps(currentSet);
                break;
            case "hidden":
                appManager.saveHiddenApps(currentSet);
                break;
        }

        listAdapter.submitList(new ArrayList<>(appsDataList));
        Toast.makeText(this, wasInList ? removedMsg : addedMsg, Toast.LENGTH_SHORT).show();
    }

    private void toggleBackgroundRestriction(AppModel app) {
        if (app.needsBackgroundRestrictionReapply()) {
            showOutOfSyncRestrictionDialog(app);
            return;
        }
        if (app.isBackgroundRestrictionExternal()) {
            showExternalRestrictionDialog(app);
            return;
        }
        boolean enableRestriction = !app.isBackgroundRestrictionDesired();
        if (enableRestriction && app.isSystemApp()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.sysapp_restriction_title)
                    .setMessage(R.string.sysapp_restriction_body)
                    .setPositiveButton(R.string.action_apply, (dialog, which) -> applyBackgroundRestriction(app, true))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }
        applyBackgroundRestriction(app, enableRestriction);
    }

    private void showOutOfSyncRestrictionDialog(AppModel app) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.desync_restriction_title)
                .setMessage(app.getAppName()
                        + " is still on Appzuku's saved restriction list, but Android is not currently restricting it.\n\nRe-apply the restriction or remove it from Appzuku's list?")
                .setPositiveButton(R.string.action_reapply, (dialog, which) -> applyBackgroundRestriction(app, true))
                .setNeutralButton(R.string.action_remove_from_list, (dialog, which) -> applyBackgroundRestriction(app, false))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showExternalRestrictionDialog(AppModel app) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.external_restriction_title)
                .setMessage(app.getAppName()
                        + " is currently restricted by Android, but it is not on Appzuku's saved list.\n\nAdd it to Appzuku or remove the current restriction?")
                .setPositiveButton(R.string.action_add_to_frenapps, (dialog, which) -> applyBackgroundRestriction(app, true))
                .setNeutralButton(R.string.action_remove_restriction, (dialog, which) -> applyBackgroundRestriction(app, false))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void applyBackgroundRestriction(AppModel app, boolean enableRestriction) {
        appManager.setBackgroundRestricted(app.getPackageName(), enableRestriction, this::loadBackgroundApps);
    }

    private String getBackgroundRestrictionMenuTitle(AppModel app) {
        if (app.needsBackgroundRestrictionReapply()) {
            return getString(R.string.opt_restriction_out_of_sync);
        }
        if (app.isBackgroundRestrictionExternal()) {
            return getString(R.string.opt_restriction_external);
        }
        if (app.isBackgroundRestrictionDesired() && !app.isBackgroundRestrictionActualKnown()) {
            return getString(R.string.opt_restriction_saved);
        }
        return getString(R.string.opt_background_restriction);
    }

    // Load background apps with selection preservation
    private void loadBackgroundApps() {
        if (binding == null) {
            return;
        }
        binding.swiperefreshlayout1.setRefreshing(true);

        // Save currently selected packages before reload
        final Set<String> selectedPackages = fullAppsList.stream()
                .filter(AppModel::isSelected)
                .map(AppModel::getPackageName)
                .collect(Collectors.toSet());

        appManager.loadBackgroundApps(result -> {
            // El escaneo tarda y puede volver cuando la actividad ya se destruyo (rotar la
            // pantalla o salir); binding queda null y aca reventaba con un NPE.
            if (binding == null || !isUiUsable()) {
                return;
            }
            fullAppsList.clear();
            fullAppsList.addAll(result);

            // Restore selection state for apps that are still in the list
            for (AppModel app : fullAppsList) {
                if (selectedPackages.contains(app.getPackageName()) && !app.isProtected()) {
                    app.setSelected(true);
                }
            }

            filterApps(currentSearchQuery);
            binding.runningApps.setText(getString(R.string.running_apps_count, fullAppsList.size()));
            binding.swiperefreshlayout1.setRefreshing(false);
            updatePermissionNotice();
        });
    }

    /** Sin Root ni Shizuku la lista queda vacia sin mas; el aviso explica por que. */
    private void updatePermissionNotice() {
        if (binding == null || !isUiUsable()) {
            return;
        }
        binding.permissionNotice.setVisibility(
                shellManager.hasAnyShellPermission() ? View.GONE : View.VISIBLE);
    }

    private void filterApps(String query) {
        currentSearchQuery = query;
        appsDataList.clear();
        if (query == null || query.isEmpty()) {
            appsDataList.addAll(fullAppsList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppModel app : fullAppsList) {
                if (app.getAppName().toLowerCase().contains(lowerQuery) ||
                        app.getPackageName().toLowerCase().contains(lowerQuery)) {
                    appsDataList.add(app);
                }
            }
        }
        // Apply current sort mode
        appManager.sortAppList(appsDataList, currentSortMode);
        listAdapter.submitList(new ArrayList<>(appsDataList));
        updateSelectMenuVisibility();
    }

    // Kill selected apps and manage FAB visibility
    private void killSelectedApps() {
        List<String> packagesToKill = fullAppsList.stream()
                .filter(AppModel::isSelected)
                .map(AppModel::getPackageName)
                .collect(Collectors.toList());

        // Hide FAB
        binding.fab.hide();

        // Clear selection
        for (AppModel app : fullAppsList) {
            app.setSelected(false);
        }
        listAdapter.submitList(new ArrayList<>(appsDataList));

        // Kill apps and show FAB on completion
        appManager.killPackages(packagesToKill, () -> {
            if (binding == null || !isUiUsable()) {
                return;
            }
            loadBackgroundApps();
            binding.fab.show();
        });
    }

    // Toggle between "Select All" and "Unselect All" based on whether any item is
    // selected
    private void updateSelectMenuVisibility() {
        boolean hasSelection = fullAppsList.stream().anyMatch(AppModel::isSelected);
        if (hasSelection) {
            binding.fab.show();
        } else {
            binding.fab.hide();
        }
        if (selectAllItem != null && unselectAllItem != null) {
            selectAllItem.setVisible(!hasSelection);
            unselectAllItem.setVisible(hasSelection);
        }
    }

    private void selectAll() {
        for (AppModel app : fullAppsList) {
            if (!app.isProtected() && !app.isWhitelisted()) {
                app.setSelected(true);
            }
        }
        listAdapter.submitList(new ArrayList<>(appsDataList));
        updateSelectMenuVisibility();
    }

    private void unselectAll() {
        for (AppModel app : fullAppsList) {
            app.setSelected(false);
        }
        listAdapter.submitList(new ArrayList<>(appsDataList));
        updateSelectMenuVisibility();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        selectAllItem = menu.findItem(R.id.action_select_all);
        unselectAllItem = menu.findItem(R.id.action_unselect_all);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterApps(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });

        View selectView = selectAllItem.getActionView();
        View unselectView = unselectAllItem.getActionView();
        unselectAllItem.setVisible(false);

        if (selectView != null) {
            ImageButton selectBtn = selectView.findViewById(R.id.select_all_action);
            selectBtn.setOnClickListener(v -> selectAll());
        }

        if (unselectView != null) {
            ImageButton unselectBtn = unselectView.findViewById(R.id.unselect_all_action);
            unselectBtn.setOnClickListener(v -> unselectAll());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (itemId == R.id.action_select_all) {
            selectAll();
            return true;
        } else if (itemId == R.id.action_unselect_all) {
            unselectAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sort, null);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.sort_radio_group);

        // Set current selection
        int selectedRadioId;
        switch (currentSortMode) {
            case AppConstants.SORT_MODE_RAM_DESC:
                selectedRadioId = R.id.sort_ram_desc;
                break;
            case AppConstants.SORT_MODE_RAM_ASC:
                selectedRadioId = R.id.sort_ram_asc;
                break;
            case AppConstants.SORT_MODE_NAME_ASC:
                selectedRadioId = R.id.sort_name_asc;
                break;
            case AppConstants.SORT_MODE_NAME_DESC:
                selectedRadioId = R.id.sort_name_desc;
                break;
            case AppConstants.SORT_MODE_DEFAULT:
            default:
                selectedRadioId = R.id.sort_default;
                break;
        }
        radioGroup.check(selectedRadioId);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.action_apply, (dialog, which) -> {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    int newSortMode = AppConstants.SORT_MODE_DEFAULT;

                    if (checkedId == R.id.sort_ram_desc) {
                        newSortMode = AppConstants.SORT_MODE_RAM_DESC;
                    } else if (checkedId == R.id.sort_ram_asc) {
                        newSortMode = AppConstants.SORT_MODE_RAM_ASC;
                    } else if (checkedId == R.id.sort_name_asc) {
                        newSortMode = AppConstants.SORT_MODE_NAME_ASC;
                    } else if (checkedId == R.id.sort_name_desc) {
                        newSortMode = AppConstants.SORT_MODE_NAME_DESC;
                    }

                    currentSortMode = newSortMode;
                    sharedPreferences.edit().putInt(KEY_SORT_MODE, newSortMode).apply();
                    filterApps(currentSearchQuery); // Re-filter and sort
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        shellManager.removeShizukuPermissionListener();
        executor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        ramMonitor.stopMonitoring();
        binding = null;
    }
}
