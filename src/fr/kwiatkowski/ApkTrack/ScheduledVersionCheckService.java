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

package fr.kwiatkowski.ApkTrack;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;

public class ScheduledVersionCheckService extends WakefulIntentService
{
    public static String SERVICE_SOURCE = "service"; // Tag used to identify the origin of a version check request.

    public ScheduledVersionCheckService()
    {
        super("ScheduledVersionCheckService");
    }

    @Override
    protected void doWakefulWork(Intent intent)
    {
        // Return if the user disabled background checks.
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.KEY_PREF_BACKGROUND_CHECKS, false))
        {
            Log.v(MainActivity.TAG, "Aborting automatic checks due to user preferences.");
            return;
        }

        List<InstalledApp> app_list = AppPersistence.getInstance(getApplicationContext()).getStoredApps();
        Log.v(MainActivity.TAG, "New update cycle started! (" + app_list.size() + " apps to check)");
        for (InstalledApp app : app_list)
        {
            Log.v(MainActivity.TAG, "Service checking updates for " + app.getPackageName());
            Log.d(MainActivity.TAG, String.format("Current version: %s | Latest version: %s", app.getVersion(), app.getLatestVersion()));

            // If we already know that the application is outdated or if the last check resulted in a fatal error, don't look for more updates.
            if (app.isUpdateAvailable() || app.isLastCheckFatalError()) {
                continue;
            }

            Intent i = new Intent(this, RequesterService.class);
            i.putExtra(RequesterService.TARGET_APP_PARAMETER, app);
            i.putExtra(RequesterService.SOURCE_PARAMETER, SERVICE_SOURCE);
            startService(i);
        }
    }
}
