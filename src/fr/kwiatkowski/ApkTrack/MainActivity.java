/*
 * Copyright (c) 2014
 *
 * ApkTrack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ApkTrack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ApkTrack.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.kwiatkowski.ApkTrack;

import android.app.ListActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends ListActivity
{
    AppAdapter adapter;
    PackageManager pacman;
    AppPersistence persistence;
    List<InstalledApp> installed_apps;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // New thread to load the data without hanging the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                persistence = new AppPersistence(getApplicationContext(), getResources());
                installed_apps = getInstalledAps();
                adapter = new AppAdapter(MainActivity.this, installed_apps);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.setListAdapter(adapter);
                    }
                });

                // Hide the spinner now
                final LinearLayout ll = (LinearLayout) findViewById(R.id.spinner);
                ll.post(new Runnable() {
                    @Override
                    public void run() {
                        ll.setVisibility(View.GONE);
                    }
                });
            }
        }).start();

        WakefulIntentService.scheduleAlarms(new PollReciever(), this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (!hasFocus || installed_apps == null || adapter == null) {
            return;
        }

        // Nothing to do if the service hasn't detected any updates.
        if (!ScheduledVersionCheckService.data_modified) {
            return;
        }

        // When focus is gained, refresh the application list. It may have been changed by the
        // background service.
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                // Show spinner
                final LinearLayout ll = (LinearLayout) findViewById(R.id.spinner);
                ll.post(new Runnable() {
                    @Override
                    public void run() {
                        ll.setVisibility(View.VISIBLE);
                    }
                });

                installed_apps.clear();
                installed_apps.addAll(getInstalledAps());
                notifyAdapterInUIThread();

                // Hide spinner
                ll.post(new Runnable() {
                    @Override
                    public void run() {
                        ll.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
        ScheduledVersionCheckService.data_modified = false;
    }

    /**
     * Retreives the list of applications installed on the device.
     * If no data is present in the database, the list is generated.
     */
    private List<InstalledApp> getInstalledAps()
    {
        List<InstalledApp> applist = persistence.getStoredApps();
        if (applist.size() == 0) {
            applist = refreshInstalledApps(true);
        }

        if (adapter == null || adapter.isShowSystem())
        {
            Collections.sort(applist);
        }
        else {
            Collections.sort(applist, InstalledApp.system_comparator);
        }

        return applist;
    }


    /**
     * Generates a list of applications installed on
     * this device. The data is retrieved from the PackageManager.
     *
     * @param overwrite_database If true, the data already present in ApkTrack's SQLite database will be
     *                           overwritten by the new data.
     */
    private List<InstalledApp> refreshInstalledApps(boolean overwrite_database)
    {
        List<InstalledApp> applist = new ArrayList<InstalledApp>();
        pacman = getPackageManager();
        if (pacman != null)
        {
            List<PackageInfo> list = pacman.getInstalledPackages(0);
            for (PackageInfo pi : list)
            {
                ApplicationInfo ai;
                try {
                    ai = pacman.getApplicationInfo(pi.packageName, 0);
                }
                catch (final PackageManager.NameNotFoundException e) {
                    ai = null;
                }
                String applicationName = (String) (ai != null ? pacman.getApplicationLabel(ai) : null);
                applist.add(new InstalledApp(pi.packageName,
                        pi.versionName,
                        applicationName,
                        isSystemPackage(pi),
                        ai != null ? ai.loadIcon(pacman) : null));
            }

            if (overwrite_database)
            {
                for (InstalledApp ia : applist) {
                    persistence.insertApp(ia);
                }
            }
            else
            {
                for (InstalledApp ia : applist)
                {
                    InstalledApp previous = persistence.getStoredApp(ia.getPackageName());
                    if (previous != null && !previous.getVersion().equals(ia.getVersion())) { // Application has been updated
                        persistence.insertApp(ia); // Store the update in the database.
                        Log.v("ApkTrack", previous.getDisplayName() + " has been updated to version " + previous.getVersion());
                    }
                }
            }
        }
        else {
            Log.e("ApkTrack", "Could not get application list!");
        }
        return applist;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.action_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This function handles user input through the action bar.
     * Three buttons exist as of yet:
     * - Get the latest version for all installed apps
     * - Regenerate the list of installed applications
     * - Hide / show system applications
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.check_all_apps:
                for (InstalledApp ia : installed_apps) {
                    performVersionCheck(ia);
                }
                return true;

            case R.id.refresh_apps:
                item.setEnabled(false);
                item.getIcon().setAlpha(130);

                // Do this in a separate thread, or the UI hangs.
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        onRefreshAppsClicked();
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run() {
                                item.setEnabled(true);
                                item.getIcon().setAlpha(255);
                            }
                        });
                    }
                }).start();
                return true;

            case R.id.show_system:
                if (!adapter.isShowSystem())
                {
                    adapter.showSystemApps();
                    item.setTitle("Hide system applications");
                }
                else
                {
                    adapter.hideSystemApps();
                    item.setTitle("Show system applications");
                }
                adapter.notifyDataSetChanged();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        InstalledApp app = (InstalledApp) getListView().getItemAtPosition(position);
        performVersionCheck(app);
    }

    private void performVersionCheck(InstalledApp app)
    {
        if (app != null && !app.isCurrentlyChecking())
        {
            // The loader icon will be displayed from here on
            app.setCurrentlyChecking(true);
            notifyAdapterInUIThread();
            new VersionGetTask(app, adapter, persistence).execute();
        }
    }

    private void onRefreshAppsClicked()
    {
        final List<InstalledApp> new_list = refreshInstalledApps(false);

        // Remove the ones we already have. We wouldn't want duplicates
        final ArrayList<InstalledApp> uninstalled_apps = new ArrayList<InstalledApp>(installed_apps);
        uninstalled_apps.removeAll(new_list);

        // Check for updated apps
        int updated_count = 0;
        for (InstalledApp ai : new_list)
        {
            if (installed_apps.contains(ai) &&
                    !ai.getVersion().equals(installed_apps.get(installed_apps.indexOf(ai)).getVersion()))
            {
                updated_count += 1;
                // The following lines may look strange, but it works because of the equality operation
                // override for InstalledApp: objects are matched on their package name alone.
                installed_apps.remove(ai); // Removes the app with the same package name as ai
                installed_apps.add(ai);    // Adds the new app
                if (adapter.isShowSystem()) {
                    Collections.sort(installed_apps);
                }
                else {
                    Collections.sort(installed_apps, InstalledApp.system_comparator);
                }
            }
        }
        if (updated_count > 0) {
            notifyAdapterInUIThread();
        }

        // Keep the new applications
        new_list.removeAll(installed_apps);

        final int final_updated_count = updated_count;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast t = Toast.makeText(getApplicationContext(),
                        new_list.size() + " new application(s) detected.\n" +
                                final_updated_count + " application(s) updated.\n" +
                                uninstalled_apps.size() + " application(s) uninstalled.",
                        Toast.LENGTH_SHORT);
                t.show();
            }
        });

        // Remove uninstalled applications from the list
        if (uninstalled_apps.size() > 0)
        {
            installed_apps.removeAll(uninstalled_apps);
            for (InstalledApp app : uninstalled_apps) {
                persistence.removeFromDatabase(app);
            }
            notifyAdapterInUIThread();
        }

        // Add new applications
        if (new_list.size() > 0)
        {
            for (InstalledApp app : new_list)
            {
                // Save the newly detected applications in the database.
                persistence.insertApp(app);
                installed_apps.add(app);
            }

            if (adapter.isShowSystem())
            {
                Collections.sort(installed_apps);
            }
            else {
                Collections.sort(installed_apps, InstalledApp.system_comparator);
            }
            notifyAdapterInUIThread();
        }
    }

    private boolean isSystemPackage(PackageInfo pkgInfo)
    {
        return !(pkgInfo == null || pkgInfo.applicationInfo == null) && ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    private void notifyAdapterInUIThread()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }
}

