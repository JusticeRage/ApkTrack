package fr.kwiatkowski.ApkTrack;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>
{

    BaseAdapter adapter;
    PackageManager pacman;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        List<InstalledApp> applist = getInstalledAps();

        adapter = new AppAdapter(this, applist);
        setListAdapter(adapter);
    }

    private List<InstalledApp> getInstalledAps()
    {

        ArrayList<InstalledApp> applist = new ArrayList<InstalledApp>();
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
            Collections.sort(applist); // Sort applications by alphabetical order.

            // TODO: Persistence test
            AppPersistence p = new AppPersistence(getApplicationContext());
            for (InstalledApp ia : applist) {
                p.persistApp(ia);
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
            new AsyncStoreGet(app, getApplicationContext(), (AppAdapter) getListAdapter()).execute(app.getPackageName());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private boolean isSystemPackage(PackageInfo pkgInfo)
    {
        return !(pkgInfo == null || pkgInfo.applicationInfo == null) && ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }
}

