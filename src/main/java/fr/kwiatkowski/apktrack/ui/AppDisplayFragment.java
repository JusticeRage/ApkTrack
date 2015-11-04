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

package fr.kwiatkowski.apktrack.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusException;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.comparator.AlphabeticalComparator;
import fr.kwiatkowski.apktrack.model.comparator.StatusComparator;
import fr.kwiatkowski.apktrack.service.message.CreateToastMessage;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment containing the list of applications installed on the device, and their update status.
 */
public class AppDisplayFragment extends Fragment {

    public static final String APP_DISPLAY_FRAGMENT_SOURCE = "appdisplayfragment";
    public AppDisplayFragment() {}

    private RecyclerView _recycler_view;
    private CoordinatorLayout _coordinator_layout;
    private LinearLayout _spinner;
    private AppAdapter _app_adapter;
    private Comparator<InstalledApp> _comparator;

    // --------------------------------------------------------------------------------------------

    @Override
    public void onResume()
    {
        super.onResume();

        // Register for sticky events in a separate thread.
        // When registering, the latest stiky is also delivered. It may contain many events to
        // process, which is why this is kept out of the UI thread.
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try  {
                    EventBus.getDefault().registerSticky(AppDisplayFragment.this, 1);
                }
                catch (EventBusException ignored) {} // The fragment may already be registered.
            }
        }).start();

    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        _coordinator_layout = (CoordinatorLayout) v.findViewById(R.id.snackbar);
        _spinner = (LinearLayout) v.findViewById(R.id.spinner);
        _recycler_view = (RecyclerView) v.findViewById(R.id.recycler_view);
        _recycler_view.setLayoutManager(new LinearLayoutManager(getActivity()));
        _recycler_view.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        return v;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        // Set the comparator depending on the settings
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(view.getContext());
        String sort_type = pref.getString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.ALPHA_SORT);
        if (sort_type.equals(SettingsFragment.ALPHA_SORT)) {
            _comparator = new AlphabeticalComparator();
        }
        else {
            _comparator = new StatusComparator();
        }

        // Do not freeze the UI while retrieving the application list
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                InstalledApp.executeQuery("UPDATE installed_app SET _iscurrentlychecking = 0");
                List<InstalledApp> installed_apps = _initialize_data();
                Collections.sort(installed_apps, _comparator);
                _app_adapter = new AppAdapter(installed_apps, getContext());
                _run_on_ui_thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        _recycler_view.setAdapter(_app_adapter);
                        // Handle the swipe movement.
                        ItemTouchHelper ith = new ItemTouchHelper(new SwipeHandler(_coordinator_layout, _app_adapter));
                        ith.attachToRecyclerView(_recycler_view);
                        // Handle clicks
                        _spinner.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method is called when a MessageModifiedEvent is posted on the event bus.
     * @param m The message to process.
     */
    public void onEvent(ModelModifiedMessage m)
    {
        if (_app_adapter == null) {
            return;
        }

        List<Pair<ModelModifiedMessage.event_type, String>> events;
        try {
            events = m.get_events();
        }
        catch (ModelModifiedMessage.EventAlreadyProcessedException ignored) {
            return; // Event was already handled.
        }

        for (final Pair<ModelModifiedMessage.event_type, String> event : events)
        {
            // An application was removed
            if (event.first == ModelModifiedMessage.event_type.APP_REMOVED) {
                _app_adapter.remove_app(event.second);
            }

            // An application was added
            else if (event.first == ModelModifiedMessage.event_type.APP_ADDED)
            {
                InstalledApp target = InstalledApp.find_app(event.second);
                if (target == null) {
                    return;
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                // Only add the app if it is not a system app, or if system apps are currently displayed.
                if (!target.is_system() ||
                    (prefs != null && prefs.getBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, false)))
                {
                    _app_adapter.add_app(target);
                }
            }

            // An application was updated.
            else if (event.first == ModelModifiedMessage.event_type.APP_UPDATED){
                _recycler_view.post(new Runnable()
                {
                    @Override
                    public void run() {
                        _app_adapter.notify_app_updated(event.second);
                    }
                });
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method is called when a Service needs the Activity to display a Toast, if it is
     * running.
     * @param m The message containing the text to display.
     */
    public void onEventMainThread(CreateToastMessage m)
    {
        // TODO: Set a timer to prevent toast flood?
        Toast.makeText(getContext(), m.get_message(), Toast.LENGTH_SHORT).show();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Remove apps from an AppDisplayFragment. The call is forwarded to the AppAdapter.
     * @param to_remove A list of apps to remove from the list.
     * @see AppAdapter#remove_apps(List)
     */
    public void remove_apps(List<InstalledApp> to_remove) {
        _app_adapter.remove_apps(to_remove);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Filters apps in an AppDisplayFragment. The call is forwarded to the AppAdapter.
     * @param to_keep A list of apps to keep in the list. All the others will be removed.
     * @see AppAdapter#filter_apps(List)
     */
    public void filter_apps(List<InstalledApp> to_keep) { _app_adapter.filter_apps(to_keep); }

    // --------------------------------------------------------------------------------------------

    /**
     * Add apps to an AppDisplayFragment. The call is forwarded to the AppAdapter.
     * @param to_add A list of apps to add from the list.
     * @see AppAdapter#add_apps(List)
     */
    public void add_apps(List<InstalledApp> to_add) {
        _app_adapter.add_apps(to_add);
    }

    // --------------------------------------------------------------------------------------------

    /**
     *  Restores the app list to its original state.
     *  Any apps filtered out by searches are reinserted, but ignored apps stay ignored.
     */
    public void restore_apps()
    {
        List<InstalledApp> to_reinsert = _initialize_data(); // Get the full app list.
        _app_adapter.add_apps(to_reinsert, false);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Causes the adapter to sort the app list based on the comparator selected in the user
     * preferences.
     */
    public void sort() {
        _app_adapter.sort();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method loads the list of installed applications from the database,
     * or generates it if no data exists.
     */
    private List<InstalledApp> _initialize_data()
    {
        String where_clause = "_isignored = 0";
        // Check whether system apps should be displayed
        Activity activity = getActivity();
        if (activity != null)
        {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
            boolean show_system = pref.getBoolean(SettingsFragment.KEY_PREF_SHOW_SYSTEM, false);
            if (!show_system) {
                where_clause += " and _systemapp = 0";
            }
        }

        List<InstalledApp> installed_apps = InstalledApp.find(InstalledApp.class, where_clause);
        if (installed_apps.size() == 0) // Database is empty
        {
            Log.v(MainActivity.TAG, "Populating database...");
            InstalledApp.generate_applist_from_system(getActivity().getPackageManager());
            installed_apps = InstalledApp.find(InstalledApp.class, "_systemapp = 0 AND _isignored = 0");
            Log.v(MainActivity.TAG, "...database populated. " + InstalledApp.count(InstalledApp.class) + " records created.");
        }
        else {
            Log.v(MainActivity.TAG, installed_apps.size() + " records read from the database.");
        }
        return installed_apps;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Helper function which runs a given Runnable inside the UI Thread.
     * @param r The Runnable to run.
     */
    private void _run_on_ui_thread(Runnable r)
    {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(r);
        }
    }
}