/*
 * Copyright (c) 2014
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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;

public class ScheduledVersionCheckService extends WakefulIntentService
{
    // Do not flood update servers. 1 request every 2 seconds max.
    public static final int REQUEST_DELAY = 2000;
    private AppPersistence persistence;

    // This variable is checked by the Activity when it gains the focus to see if it should reload
    // its application list from the database.
    static boolean data_modified = false;

    public ScheduledVersionCheckService()
    {
        super("ScheduledVersionCheckService");
        persistence = new AppPersistence(this, null);
    }

    @Override
    protected void doWakefulWork(Intent intent)
    {
        List<InstalledApp> app_list = persistence.getStoredApps();
        Log.v("ApkTrack", "New update cycle started! (" + app_list.size() + " apps to check)");
        try
        {
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

                VersionGetResult res = new VersionGetTask(app, null, persistence).sync_execute();
                Log.v("ApkTrack", "Play Store check returned: " + res.getStatus());
                if (res.getStatus() == VersionGetResult.Status.ERROR)
                {
                    Log.v("ApkTrack", "Trying AppBrain...");
                    app.setCurrentlyChecking(true);
                    res = new VersionGetTask(app, null, persistence, VersionGetTask.PageUsed.APPBRAIN).sync_execute();
                    Log.v("ApkTrack", "AppBrain check returned: " + res.getStatus());
                }

                if (res.getStatus() == VersionGetResult.Status.UPDATED)
                {
                    Resources r = getResources();
                    // Show a notification for updated apps
                    Notification.Builder b = new Notification.Builder(this);
                    b.setContentTitle(String.format(r.getString(R.string.app_updated_notification), app.getDisplayName()))
                     .setContentText(String.format(r.getString(R.string.app_version_available), app.getLatestVersion()))
                     .setTicker(String.format(r.getString(R.string.app_can_be_updated), app.getDisplayName()))
                     .setSmallIcon(R.drawable.ic_menu_refresh);

                    NotificationManager mgr= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    mgr.notify(app.getDisplayName().hashCode(), b.build()); // One notification per available update.
                    data_modified = true;
                    // TODO: Send an intent to the activity in case it has the focus.
                }

                Thread.sleep(REQUEST_DELAY);
            }
        }
        catch (InterruptedException ignored) {}
    }
}
