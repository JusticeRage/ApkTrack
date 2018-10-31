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

package fr.kwiatkowski.apktrack.service;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;

import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.ui.SettingsFragment;

public class ScheduledCheckService extends WakefulIntentService {
    public static String SERVICE_SOURCE = "service"; // Tag used to identify the origin of a version check request.

    public ScheduledCheckService() {
        super("ScheduledVersionCheckService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        // Return if the user disabled background checks.
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.KEY_PREF_BACKGROUND_CHECKS, false)) {
            Log.v(MainActivity.TAG, "Aborting automatic checks due to user preferences.");
            return;
        }

        // Perform a refresh in case updates were performed and the activity hasn't gained focus yet.
        List<InstalledApp> app_list = InstalledApp.find(InstalledApp.class,
                "_isignored = 0 AND _iscurrentlychecking = 0");

        Log.v(MainActivity.TAG, "New update cycle started! (" + app_list.size() + " apps to check)");
        for (InstalledApp app : app_list) {
            // If we already know that the application is outdated or if the last check resulted in a fatal error, don't look for more updates.
            if (app.is_update_available() || app.is_last_ckeck_error()) {
                continue;
            }

            // Launch an update check
            Intent i = new Intent(this, WebService.class);
            i.putExtra(WebService.TARGET_APP_PARAMETER, app.get_package_name());
            i.putExtra(WebService.SOURCE_PARAMETER, SERVICE_SOURCE);
            i.putExtra(WebService.ACTION, WebService.ACTION_VERSION_CHECK);
            startService(i);
        }
    }
}
