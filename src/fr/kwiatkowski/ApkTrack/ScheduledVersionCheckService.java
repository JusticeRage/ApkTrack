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
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;

public class ScheduledVersionCheckService extends WakefulIntentService
{
    // Do not flood update servers. 1 request every 5 seconds max.
    public static final int REQUEST_DELAY = 5000;
    private AppPersistence persistence;

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

                PlayStoreGetResult res = new PlayStoreGetTask(app, null, persistence).sync_execute();
                Log.v("ApkTrack", "Update returned: " + res.getStatus() + " / " + res.getMessage());

                if (res.getStatus() == PlayStoreGetResult.Status.UPDATED)
                {
                    // Show a notification for updated apps
                    Notification.Builder b = new Notification.Builder(this);
                    b.setContentTitle(app.getDisplayName() + "updated.")
                     .setContentText("Version " + app.getLatestVersion() + " is available!")
                     .setTicker(app.getDisplayName() + " was updated!")
                     .setSmallIcon(R.drawable.ic_menu_refresh);

                    NotificationManager mgr= (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    mgr.notify(1, b.build());
                }

                Thread.sleep(REQUEST_DELAY);
            }
        }
        catch (InterruptedException ignored) {}
    }
}
