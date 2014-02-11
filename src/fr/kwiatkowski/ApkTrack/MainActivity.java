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

        persistence = new AppPersistence(getApplicationContext(), getResources());
        installed_apps = getInstalledAps();

        adapter = new AppAdapter(this, installed_apps);
        setListAdapter(adapter);

        WakefulIntentService.scheduleAlarms(new PollReciever(), this);
    }

    // TODO: On resume, refresh app list.

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
        Collections.sort(applist); // Sort applications in alphabetical order.
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
        //TODO : Move out of the main thread
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.check_all_apps:
                for (InstalledApp ia : installed_apps) {
                    performVersionCheck(ia);
                }
                return true;

            case R.id.refresh_apps:
                List<InstalledApp> new_list = refreshInstalledApps(false);

                // Remove the ones we already have. We wouldn't want duplicates
                ArrayList<InstalledApp> uninstalled_apps = new ArrayList<InstalledApp>(installed_apps);
                uninstalled_apps.removeAll(new_list);
                new_list.removeAll(installed_apps);

                Toast t = Toast.makeText(getApplicationContext(),
                                         new_list.size() + " new application(s) detected.\n" +
                                         uninstalled_apps.size() + " application(s) uninstalled.",
                                         Toast.LENGTH_SHORT);
                t.show();

                // Remove uninstalled applications from the list
                if (uninstalled_apps.size() > 0)
                {
                    installed_apps.removeAll(uninstalled_apps);
                    for (InstalledApp app : uninstalled_apps) {
                        persistence.removeFromDatabase(app);
                    }
                    adapter.notifyDataSetChanged();
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
                    adapter.notifyDataSetChanged();
                }

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
            ((AppAdapter) getListAdapter()).notifyDataSetChanged();
            new PlayStoreGetTask(app, adapter, persistence).execute();
        }
    }

    private boolean isSystemPackage(PackageInfo pkgInfo)
    {
        return !(pkgInfo == null || pkgInfo.applicationInfo == null) && ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }
}

