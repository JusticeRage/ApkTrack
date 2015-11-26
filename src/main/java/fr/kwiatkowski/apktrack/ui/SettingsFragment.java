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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.service.utils.CapabilitiesHelper;

public class SettingsFragment extends PreferenceFragmentCompat
{
    public final static String KEY_PREF_SEARCH_ENGINE = "pref_search_engine";
    public final static String KEY_PREF_SORT_TYPE = "pref_sort_type";
    public final static String KEY_PREF_BACKGROUND_CHECKS = "pref_background_checks";
    public final static String KEY_PREF_WIFI_ONLY = "pref_wifi_only";
    public final static String KEY_PREF_DOWNLOAD_APKS = "pref_automatic_downloads";

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
        _enable_buttons();
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onCreatePreferences(Bundle bundle, String s)
    {
        addPreferencesFromResource(R.xml.preferences);

        final Preference reset = findPreference("pref_reset_ignored_apps");
        final Preference ignore_system = findPreference("pref_ignore_system_apps");
        final Preference ignore_xposed = findPreference("pref_ignore_xposed_apps");
        final Preference privacy = findPreference("action_privacy_policy");
        if (reset == null || privacy == null || ignore_system == null || ignore_xposed == null)
        {
            Log.v(MainActivity.TAG, "The preferences are malformed!");
            return;
        }

        _enable_buttons();

        // Add a click listener to unignore apps.
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
                                _enable_buttons();
                                // Re-enable the "show system apps" button if it was disabled
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.invalidateOptionsMenu();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, null).show();
                _enable_buttons();
                return false;
            }
        });

        // Add a click listened to ignore system apps.
        ignore_system.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                InstalledApp.executeQuery("UPDATE installed_app SET _isignored = 1 WHERE " +
                        "_systemapp = 1");

                // Disable the "show system apps" button
                Activity activity = getActivity();
                if (activity != null) {
                    activity.invalidateOptionsMenu();
                }

                _enable_buttons();
                return false;
            }
        });

        // Add a click listened to ignore xposed apps.
        ignore_xposed.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                InstalledApp.executeQuery("UPDATE installed_app SET _isignored = 1 WHERE " +
                        "_updatesource LIKE 'Xposed%'");
                _enable_buttons();
                return false;
            }
        });

        // Add a click listener to open the privacy policy
        privacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                if (CapabilitiesHelper.check_browser_available(getContext())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://apktrack.kwiatkowski.fr/privacy")));
                }
                else // No browser is present on the device.
                {
                    privacy.setSummary(getContext().getString(R.string.cant_handle_view));
                    privacy.setEnabled(false);
                }
                return false;
            }
        });
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Called to determine whether the "ignore [category of apps]" actions should be
     * enabled or disabled, depending on whether there are actually apps to be
     * ignored in the category.
     */
    private void _enable_buttons()
    {
        final Preference reset = findPreference("pref_reset_ignored_apps");
        final Preference ignore_system = findPreference("pref_ignore_system_apps");
        final Preference ignore_xposed = findPreference("pref_ignore_xposed_apps");

        if (reset != null)
        {
            long ignored_apps = InstalledApp.count(InstalledApp.class, "_isignored = 1", null);
            reset.setEnabled(ignored_apps != 0);
        }
        if (ignore_xposed != null)
        {
            long xposed_apps = InstalledApp.count(InstalledApp.class,
                                                  "_updatesource LIKE 'Xposed%' AND _isignored = 0",
                                                  null);
            ignore_xposed.setEnabled(xposed_apps != 0);
        }
        if (ignore_system != null) {
            ignore_system.setEnabled(InstalledApp.check_system_apps_tracked());
        }
    }

}
