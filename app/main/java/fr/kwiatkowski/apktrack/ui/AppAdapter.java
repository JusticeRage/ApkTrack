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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.comparator.AlphabeticalComparator;
import fr.kwiatkowski.apktrack.model.comparator.StatusComparator;

import java.util.*;

public class AppAdapter extends RecyclerView.Adapter<AppViewHolder>
{
    private List<InstalledApp> _installed_apps;
    private Context _ctx;

    // --------------------------------------------------------------------------------------------

    public AppAdapter(Context ctx)
    {
        _installed_apps = new ArrayList<InstalledApp>();
        _ctx = ctx;
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.list_item, parent, false);
        return new AppViewHolder(v);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position)
    {
        InstalledApp app = _installed_apps.get(position);
        holder.bind_app(app, _ctx);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public int getItemCount() {
        return _installed_apps.size();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Removes applications from the AppAdapter's list.
     * Although it would be possible to do this without going through the whole list
     * to mark apps for removal, I believe it is the only way to have the nice
     * animation on the UI.
     *
     * @see AppAdapter#add_apps(List)
     *
     * @param to_remove A list of apps to remove from the list.
     */
    public void remove_apps(@NonNull List<InstalledApp> to_remove)
    {
        // Iterate from the end of the list
        ListIterator<InstalledApp> it = _installed_apps.listIterator(_installed_apps.size());
        while (it.hasPrevious())
        {
            InstalledApp current = it.previous();
            if (!to_remove.contains(current)) {
                continue;
            }

            notifyItemRemoved(_installed_apps.indexOf(current));
            it.remove();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Remove a single app from the list.
     * @param app The app to remove.
     */
    public void remove_app(@NonNull InstalledApp app)
    {
        int pos = _installed_apps.indexOf(app);
        if (pos != -1)
        {
            notifyItemRemoved(pos);
            _installed_apps.remove(pos);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Remove a single app from the list based on its package name.
     * This method can be useful when deleting objects which are still displayed, but aren't
     * in the database anymore.
     *
     * @param package_name The package name of the application to remove.
     */
    public void remove_app(@NonNull String package_name)
    {
        for (int i = 0 ; i < _installed_apps.size() ; ++i)
        {
            if (package_name.equals(_installed_apps.get(i).get_package_name()))
            {
                _installed_apps.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Notifies the adapter that an app has been updated in the model.
     * @param package_name The package name of the modifies app.
     */
    public void notify_app_updated(String package_name)
    {
        InstalledApp app = InstalledApp.find_app(package_name);
        if (app == null) {
            return;
        }

        final int pos = _installed_apps.indexOf(app);
        if (pos != -1)
        {
            _installed_apps.remove(pos);
            _installed_apps.add(pos, app);
            notifyItemChanged(pos);
        }

        // TODO: Update sort if we're using the StatusComparator?
        // I'm afraid having apps jumping around in the list after clicking them
        // would be bad user experience.

        // Do not display notifications for this app until a new version is detected,
        // because any current update is now reflected in the UI.
        if (app.is_update_available() && !app.has_notified())
        {
            app.set_has_notified(true);
            app.save();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Leave only the selected apps in the list.
     * @param to_keep The applications to keep.
     */
    public void filter_apps(@NonNull List<InstalledApp> to_keep)
    {
        // Iterate from the end of the list.
        ListIterator<InstalledApp> it = _installed_apps.listIterator(_installed_apps.size());
        while (it.hasPrevious())
        {
            InstalledApp current = it.previous();
            if (to_keep.contains(current)) {
                continue;
            }

            notifyItemRemoved(_installed_apps.indexOf(current));
            it.remove();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Adds applications to the AppAdapter's list.
     *
     * In order to display a nice insertion animation, the new index of each app
     * to insert is computed.
     *
     * @see AppAdapter#remove_apps(List)
     * @see AppAdapter#add_apps(List, boolean)
     * @param to_add A list of applications to add.
     */
    public void add_apps(@NonNull List<InstalledApp> to_add) {
        add_apps(to_add, true);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Adds applications to the AppAdapter's list.
     *
     * In order to display a nice insertion animation, the new index of each app
     * to insert is computed.
     *
     * @see AppAdapter#remove_apps(List)
     * @param to_add A list of applications to add.
     * @param merge_with_existing Whether the existing apps should be merged with
     *                            the provided list. Only set to false if the apps
     *                            contained in the current list are present in
     *                            <code>to_add</code>.
     */
    public void add_apps(@NonNull List<InstalledApp> to_add, boolean merge_with_existing)
    {
        // This test speeds up startup where no animations are needed.
        if (_installed_apps.size() == 0)
        {
            Collections.sort(to_add, _get_comparator());
            _installed_apps = to_add;
            notifyDataSetChanged();
            return;
        }

        if (merge_with_existing)
        {
            for (InstalledApp app : _installed_apps)
            {
                if (!to_add.contains(app)) {
                    to_add.add(app);
                }
            }
        }

        Collections.sort(to_add, _get_comparator());

        for (int i = 0 ; i < to_add.size() ; ++i)
        {
            if (_installed_apps.contains(to_add.get(i))) {
                continue;
            }
            try
            {
                _installed_apps.add(i, to_add.get(i));
                notifyItemInserted(i);
            }
            catch (IndexOutOfBoundsException ignored)
            {
                _installed_apps.add(to_add.get(i));
                notifyItemInserted(_installed_apps.size() - 1);
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Adds a single application to the AppAdapter's list.
     * @param app The application to add.
     */
    public void add_app(InstalledApp app)
    {
        // App is already present
        if (_installed_apps.indexOf(app) != -1) {
            return;
        }

        _installed_apps.add(app);
        Collections.sort(_installed_apps, _get_comparator());
        int pos = _installed_apps.indexOf(app);
        notifyItemInserted(pos);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Sorts the app list using the comparator designated by the user preferences.
     */
    public void sort()
    {
        List<InstalledApp> old_list = new ArrayList<InstalledApp>(_installed_apps);
        Collections.sort(_installed_apps, _get_comparator());
        for (InstalledApp app : _installed_apps) {
            notifyItemMoved(old_list.indexOf(app), _installed_apps.indexOf(app));
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the right comparator based on user preferences.
     * @return A comparator to sort apps in the right order.
     */
    private Comparator<InstalledApp> _get_comparator()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(_ctx);
        String sort_type = pref.getString(SettingsFragment.KEY_PREF_SORT_TYPE, SettingsFragment.ALPHA_SORT);
        if (sort_type.equals(SettingsFragment.ALPHA_SORT)) {
            return new AlphabeticalComparator();
        }
        else {
            return new StatusComparator();
        }
    }
}
