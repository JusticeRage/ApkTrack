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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;

import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat
{
    public final static String KEY_PREF_SEARCH_ENGINE = "pref_search_engine";
    public final static String KEY_PREF_SORT_TYPE = "pref_sort_type";
    public final static String KEY_PREF_BACKGROUND_CHECKS = "pref_background_checks";
    public final static String KEY_PREF_WIFI_ONLY = "pref_wifi_only";

    public final static String ALPHA_SORT = "alpha";
    public final static String STATUS_SORT = "status";

    // Not used displayed in the SettingsFragment: the user interacts with it from the top menu.
    public final static String KEY_PREF_SHOW_SYSTEM = "pref_show_system";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onResume()
    {
        super.onResume();
        Preference reset = findPreference("reset_ignored_apps");
        if (reset == null) {
            return;
        }

        List<InstalledApp> installed_apps = InstalledApp.find(InstalledApp.class, "_isignored = 1");
        reset.setEnabled(installed_apps.size() != 0); // Only enable if there are apps to unignore.
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.preferences);
        Preference reset = findPreference("reset_ignored_apps");
        if (reset == null)
        {
            Log.v(MainActivity.TAG, "Could not attach listener to the reset item!");
            return;
        }
        reset.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference)
            {
                new AlertDialog.Builder(preference.getContext())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.confirm_reset_ignored)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                InstalledApp.executeQuery("UPDATE installed_app SET _isignored = 0");
                                preference.setEnabled(false);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null).show();
                return false;
            }
        });
    }

}
