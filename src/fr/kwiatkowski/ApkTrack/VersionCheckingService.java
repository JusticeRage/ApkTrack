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
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

public class VersionCheckingService extends Service
{
    /**
     * The minimum delay (in milliseconds) between two requests. We don't want to flood the Play Store and get banned.
     */
    public static int REQUEST_DELAY = 5000;

    /**
     * The delay (in milliseconds) between two version checks for a single application.
     */
    public static int CHECK_INTERVAL = 1000 * 60 * 60 * 24; // 1 day

    private NotificationManager nm;

    @Override
    public void onCreate()
    {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(getApplicationContext())
            .setContentTitle("ApkTrack")
            .setContentText("Service has been launched :)")
            .setSmallIcon(R.drawable.ic_menu_refresh)
            .build();
        nm.notify(0, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ApkTrackServiceBinder();
    }

    /**
     * Begin checking versions in the background
     */
    public void start(List<InstalledApp> apps, AppPersistence persistence)
    {
        Thread t = new Thread(new VersionCheckTask(apps, persistence));
        t.start();
    }

    class ApkTrackServiceBinder extends Binder
    {
        VersionCheckingService getService() {
            return VersionCheckingService.this;
        }
    }

}

class VersionCheckTask implements Runnable
{
    private List<InstalledApp> app_list;
    private AppPersistence persistence;

    public VersionCheckTask(List<InstalledApp> app_list, AppPersistence persistence)
    {
        this.app_list = app_list;
        this.persistence = persistence;
    }

    @Override
    public void run()
    {
        while (true)
        {
            for (InstalledApp app : app_list)
            {
                // If we already know that the application is outdated, don't check for more updates.
                if (!app.getVersion().equals(app.getLatestVersion())) {
                    continue;
                }
                // Do not try again if there was an error.
                // TODO: Deal with network issues.
                if (app.isLastCheckError()) {
                    continue;
                }

                new AsyncStoreGet(app, null, persistence).execute();
                Log.v("ApkTrack", "Service checking updates for " + app.getPackageName());
                SystemClock.sleep(VersionCheckingService.REQUEST_DELAY);
            }
            SystemClock.sleep(VersionCheckingService.CHECK_INTERVAL);
        }
    }
}