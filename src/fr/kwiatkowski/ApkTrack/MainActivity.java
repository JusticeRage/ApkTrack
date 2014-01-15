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
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends ListActivity
{

    BaseAdapter adapter;
    PackageManager pacman;
    AppPersistence persistence;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        persistence = new AppPersistence(getApplicationContext(), getResources());
        List<InstalledApp> applist = getInstalledAps();

        adapter = new AppAdapter(this, applist);
        setListAdapter(adapter);
    }

    /**
     * Retreives the list of applications installed on the device.
     * If no data is present in the database, the list is generated.
     */
    private List<InstalledApp> getInstalledAps()
    {
        List<InstalledApp> applist = persistence.getStoredApps();
        if (applist.size() == 0) {
            applist = refreshInstalledApps();
        }
        Collections.sort(applist); // Sort applications in alphabetical order.
        return applist;
    }

    /**
     * Generates a list of (non-system) applications installed on
     * this device. The data is retrieved from the PackageManager.
     */
    private List<InstalledApp> refreshInstalledApps()
    {
        List<InstalledApp> applist = new ArrayList<InstalledApp>();
        pacman = getPackageManager();
        if (pacman != null)
        {
            List<PackageInfo> list = pacman.getInstalledPackages(0);
            for (PackageInfo pi : list)
            {
                if (isSystemPackage(pi)) {
                    continue; // Ignore system apps
                }

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
                        ai != null ? ai.loadIcon(pacman) : null));
            }
            // Store the data for future use
            for (InstalledApp ia : applist) {
                persistence.persistApp(ia);
            }
        }
        else {
            Log.v("MainActivity", "Could not get application list!");
        }
        return applist;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        InstalledApp app = (InstalledApp) getListView().getItemAtPosition(position);
        if (app != null) {
            new AsyncStoreGet(app, getApplicationContext(), (AppAdapter) getListAdapter(), persistence).execute(app.getPackageName());
        }
    }

    private boolean isSystemPackage(PackageInfo pkgInfo)
    {
        return !(pkgInfo == null || pkgInfo.applicationInfo == null) && ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }
}

