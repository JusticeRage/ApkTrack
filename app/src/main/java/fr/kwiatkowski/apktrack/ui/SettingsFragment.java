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
import android.widget.Toast;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.service.EventBusHelper;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;
import fr.kwiatkowski.apktrack.service.utils.CapabilitiesHelper;
import fr.kwiatkowski.apktrack.service.utils.ProxyHelper;

import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat
{
    public final static String KEY_PREF_SEARCH_ENGINE = "pref_search_engine";
    public final static String KEY_PREF_SORT_TYPE = "pref_sort_type";
    public final static String KEY_PREF_BACKGROUND_CHECKS = "pref_background_checks";
    public final static String KEY_PREF_WIFI_ONLY = "pref_wifi_only";
    public final static String KEY_PREF_DOWNLOAD_APKS = "pref_automatic_downloads";
    public final static String KEY_PREF_PROXY_TYPE = "pref_proxy_type";
    public final static String KEY_PREF_PROXY_ADDRESS = "pref_proxy_address";
    public final static String KEY_PREF_PROXY_WARNING = "pref_proxy_warning";
    public final static String KEY_PREF_CLEAN_APKS = "action_clean_downloads";

    public final static String ALPHA_SORT = "alpha";
    public final static String STATUS_SORT = "status";

    // Not displayed in the settings screen: the user interacts with it from the top menu.
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
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.preferences);

        final Preference reset = findPreference("pref_reset_ignored_apps");
        final Preference ignore_system = findPreference("pref_ignore_system_apps");
        final Preference ignore_xposed = findPreference("pref_ignore_xposed_apps");
        final Preference privacy = findPreference("action_privacy_policy");
        final Preference clean_apks = findPreference(KEY_PREF_CLEAN_APKS);
        final Preference proxy_type = findPreference(KEY_PREF_PROXY_TYPE);
        final Preference proxy_address = findPreference(KEY_PREF_PROXY_ADDRESS);
        if (reset == null || privacy == null || ignore_system == null || ignore_xposed == null ||
            proxy_type == null || proxy_address == null || clean_apks == null)
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

        // Add a click listener to ignore system apps.
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

        // Add a click listener to ignore xposed apps.
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

        // Set the right summary and enabled status for proxy options
        render_proxy_type_preference(null);
        set_proxy_address_summary(null);
        // Add a change listener to disable so this function is called when the value is updated.
        proxy_type.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                render_proxy_type_preference(newValue.toString());
                return true;
            }
        });
        proxy_address.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                // Assert that the input value is valid.
                if (!ProxyHelper.test_proxy_address(newValue.toString()))
                {
                    Toast.makeText(getContext(), R.string.invalid_proxy_address, Toast.LENGTH_SHORT).show();
                    return false;
                }

                set_proxy_address_summary(newValue.toString());
                return true;
            }
        });

        // Setup the description and click listener for the "Clean APK" setting.
        final List<InstalledApp> downloaded = InstalledApp.find(InstalledApp.class, "_downloadid != 0");
        clean_apks.setSummary(getResources().getString(R.string.clean_downloads_description, downloaded.size()));
        if (downloaded.size() == 0) {
            clean_apks.setEnabled(false);
        }
        else {
            clean_apks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    for (InstalledApp app : downloaded)
                    {
                        app.clean_downloads(getContext());
                        EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());
                    }
                    clean_apks.setSummary(getResources().getString(R.string.clean_downloads_description, 0));
                    clean_apks.setEnabled(false);
                    return true;
                }
            });
        }
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

    // --------------------------------------------------------------------------------------------

    /**
     * This function is used to set the right summary for the proxy type setting, and
     * disable the proxy address setting if no proxy type is selected.
     * @param new_type The new value for this setting (may be null if we're initializing and not
     *                 updating).
     */
    private void render_proxy_type_preference(String new_type)
    {
        Preference proxy_type = findPreference(KEY_PREF_PROXY_TYPE);
        Preference proxy_address = findPreference(KEY_PREF_PROXY_ADDRESS);
        Preference proxy_warning = findPreference(KEY_PREF_PROXY_WARNING);
        if (proxy_type == null || proxy_address == null || proxy_warning == null)
        {
            Log.v(MainActivity.TAG, "The preferences are malformed!");
            return;
        }

        // new_type will be set if this function is called from the PropertyChangeListener, but
        // will be null when called from onCreatePreferences. Get the stored value in that case.
        if (new_type == null) {
            new_type = proxy_type.getSharedPreferences().getString(KEY_PREF_PROXY_TYPE, "DIRECT");
        }
        switch (new_type)
        {
            case "DIRECT":
                proxy_type.setSummary(R.string.no_proxy_summary);
                proxy_address.setEnabled(false);
                proxy_warning.setEnabled(false);
                break;
            case "HTTP":
                proxy_type.setSummary(R.string.http_proxy_summary);
                proxy_address.setEnabled(true);
                proxy_warning.setEnabled(true);
                break;
            case "SOCKS":
                proxy_type.setSummary(R.string.socks_proxy_summary);
                proxy_address.setEnabled(true);
                proxy_warning.setEnabled(true);
                break;
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This function is used to set the right summary for the proxy address setting.
     * The following code would not exist if "%s" were supported by EditTextPreference.
     * @param new_value The new value for this setting (may be null if we're initializing and not
     *                  updating).
     */
    private void set_proxy_address_summary(String new_value)
    {
        Preference proxy_address = findPreference(KEY_PREF_PROXY_ADDRESS);
        if (proxy_address == null)
        {
            Log.v(MainActivity.TAG, "The preferences are malformed!");
            return;
        }
        // Set the summary manually, since "%s" is not supported for EditTextPreferences.
        String address = new_value;
        if (address == null) {
            address = proxy_address.getSharedPreferences().getString(KEY_PREF_PROXY_ADDRESS, "127.0.0.1:9050");
        }
        proxy_address.setSummary(address);
    }
}
