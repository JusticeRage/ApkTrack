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
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;

public class ScheduledVersionCheckService extends WakefulIntentService
{
    public ScheduledVersionCheckService()
    {
        super("ScheduledVersionCheckService");
    }

    @Override
    protected void doWakefulWork(Intent intent)
    {
        List<InstalledApp> app_list = AppPersistence.getInstance(getApplicationContext()).getStoredApps();
        Log.v("ApkTrack", "New update cycle started! (" + app_list.size() + " apps to check)");
        for (InstalledApp app : app_list)
        {
            Log.v("ApkTrack", "Service checking updates for " + app.getPackageName());

            // If we already know that the application is outdated, don't check for more updates.
            if (app.getLatestVersion() != null && !app.getVersion().equals(app.getLatestVersion())) {
                continue;
            }
            // Do not try again if there was an error.
            if (app.isLastCheckFatalError()) {
                continue;
            }

            Intent i = new Intent(this, RequesterService.class);
            i.putExtra(RequesterService.TARGET_APP_PARAMETER, app);
            startService(i);
        }
    }
}
