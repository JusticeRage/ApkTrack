/*
 * Copyright (c) 2015
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

package fr.kwiatkowski.apktrack;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.UpdateSource;
import fr.kwiatkowski.apktrack.service.EventBusHelper;
import fr.kwiatkowski.apktrack.service.PollReceiver;
import fr.kwiatkowski.apktrack.service.WebScraperService;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;
import fr.kwiatkowski.apktrack.ui.AppDisplayFragment;
import fr.kwiatkowski.apktrack.ui.SettingsFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = "ApkTrack";
    public static final String APP_FRAGMENT_TAG = "appdisplayfragment";
    private AppDisplayFragment _app_display;

    // --------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportFragmentManager().findFragmentById(R.id.container) == null)
        {
            _app_display = new AppDisplayFragment();
            getSupportFragmentManager().beginTransaction()
                                       .add(R.id.container, _app_display, APP_FRAGMENT_TAG)
                                       .commit();

            // Load update sources
            UpdateSource.initialize_update_sources(this);
        }
        else {
            _app_display = (AppDisplayFragment) getSupportFragmentManager().findFragmentByTag(APP_FRAGMENT_TAG);
        }

        // Schedule automatic version checks.
        WakefulIntentService.scheduleAlarms(new PollReceiver(), this);

        // ApkTrack cannot recieve Intents about its own upgrades. A manual check has to
        // be performed at startup to update its version number if needed.
        new Thread(new Runnable() {
            @Override
            public void run() {
                InstalledApp.detect_new_version(getPackageManager(), getPackageName());
            }
        }).start();

        _handle_intent(getIntent());
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Change the "Show system apps" text depending on the currently selected option.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean show_system = pref.getBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, false);
        if (show_system) {
            menu.findItem(R.id.show_system).setTitle(R.string.hide_system_apps);
        }
        // Disable it if all system apps are ignored.
        if (!InstalledApp.check_system_apps_tracked()) {
            menu.findItem(R.id.show_system).setEnabled(false);
        }

        // Change the change sort type text depending on the currently selected option.
        String sort_type = pref.getString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.ALPHA_SORT);
        if (sort_type.equals(SettingsFragment.ALPHA_SORT)) {
            menu.findItem(R.id.sort_type).setTitle(R.string.sort_type_updated);
        }

        SearchManager seaman = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView sv = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        sv.setSearchableInfo(seaman.getSearchableInfo(getComponentName()));
        sv.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {}

            @Override
            public void onViewDetachedFromWindow(View view) {
                _app_display.restore_apps(); // Unfilter the list when the search bar is closed.
            }
        });

        return true;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Function which handles clicks on the UI.
     * @param item The item which was clicked.
     * @return True.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.settings)
        {
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .setCustomAnimations(R.anim.slide_in_right,
                            R.anim.slide_out_left)
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
            return true;
        }
        else if (id == R.id.show_system) {
            return _toggle_show_system_apps(item);
        }
        else if (id == R.id.sort_type) {
            return _toggle_sort(item);
        }
        else if (id == R.id.check_all_apps)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    List<InstalledApp> apps = InstalledApp.find(InstalledApp.class,
                            "_iscurrentlychecking = 0 and _isignored = 0");
                    InstalledApp.executeQuery("UPDATE installed_app SET _iscurrentlychecking = 1 WHERE " +
                            "_iscurrentlychecking = 0 AND _isignored = 0");
                    for (InstalledApp app : apps)
                    {
                        EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());

                        // Launch an update check
                        Intent i = new Intent(MainActivity.this, WebScraperService.class);
                        i.putExtra(WebScraperService.TARGET_APP_PARAMETER, app.get_package_name());
                        i.putExtra(WebScraperService.SOURCE_PARAMETER, AppDisplayFragment.APP_DISPLAY_FRAGMENT_SOURCE);
                        startService(i);
                    }
                }
            }).start();
        }

        return super.onOptionsItemSelected(item);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    protected void onNewIntent(Intent i) {
        _handle_intent(i);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onBackPressed()
    {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        }
        else {
            super.onBackPressed();
        }
    }

    // --------------------------------------------------------------------------------------------

    private boolean _toggle_show_system_apps(MenuItem item)
    {
        // The user's choice is stored as a preference.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean show_system = pref.getBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, false);
        List<InstalledApp> system_apps = InstalledApp.find(InstalledApp.class, "_isignored = 0 AND _systemapp = 1");
        if (show_system)
        {
            Log.v(TAG, "Hiding system apps.");
            _app_display.remove_apps(system_apps);
            item.setTitle(R.string.show_system_apps);
        }
        else
        {
            Log.v(TAG, "Showing system apps.");
            _app_display.add_apps(system_apps);
            item.setTitle(R.string.hide_system_apps);
        }

        pref.edit().putBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, !show_system).apply(); // Toggle the preference.
        return true;
    }

    // --------------------------------------------------------------------------------------------

    private boolean _toggle_sort(MenuItem item)
    {
        // The user's choice is stored as a preference.
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String sort_type = pref.getString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.ALPHA_SORT);
        if (sort_type.equals(SettingsFragment.ALPHA_SORT))
        {
            Log.v(MainActivity.TAG, "Sorting apps by status.");
            item.setTitle(R.string.sort_type_alpha);
            pref.edit().putString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.STATUS_SORT).apply();
            _app_display.sort();
        }
        else {
            Log.v(MainActivity.TAG, "Sorting apps by alphabetical order.");
            item.setTitle(R.string.sort_type_updated);
            pref.edit().putString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.ALPHA_SORT).apply();
            _app_display.sort();
        }
        return true;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method received the intents passed to the activity and processes them.
     * @param i The intent to handle.
     */
    private void _handle_intent(Intent i)
    {
        // The user wants to filter the list.
        if (Intent.ACTION_SEARCH.equals(i.getAction()))
        {
            Log.v(TAG, "User search: " + i.getStringExtra(SearchManager.QUERY));
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean show_system = pref.getBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, false);

            String query = "%" + i.getStringExtra(SearchManager.QUERY) + "%";
            String where_clause = "(_displayname LIKE ? OR _packagename LIKE ?)";
            if (!show_system) {
                where_clause += " AND _systemapp = 0";
            }
            List<InstalledApp> results = InstalledApp.find(InstalledApp.class, where_clause, query, query);
            Log.v(TAG, results.size() + " application(s) match.");

            if (results.size() == 0) // No apps match the search.
            {
                Toast.makeText(getApplicationContext(), R.string.search_no_result, Toast.LENGTH_SHORT).show();
                return;
            }
            _app_display.filter_apps(results);
        }
        else if (Intent.ACTION_MANAGE_NETWORK_USAGE.equals(i.getAction()))
        {
            // Do not add to the backstack: back should return to the Intent sender.
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
        }
    }
}
