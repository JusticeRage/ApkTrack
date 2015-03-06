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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        InstalledApp app = intent.getParcelableExtra(RequesterService.TARGET_APP_PARAMETER);
        VersionGetResult res = (VersionGetResult) intent.getSerializableExtra(RequesterService.UPDATE_RESULT_PARAMETER);

        if (app == null || res == null)
        {
            Log.v(MainActivity.TAG, "Error: NotificationReceiver received an Intent with missing parameters! "
                    + "app=" + app + " / res=" + res);
            abortBroadcast();
            return;
        }

        Log.v(MainActivity.TAG, "NotificationReciever received " + res.getStatus() + " for " + app.getDisplayName() + ".");

        if (res.getStatus() == VersionGetResult.Status.UPDATED)
        {
            Resources r = context.getResources();
            // Show a notification for updated apps
            Notification.Builder b = new Notification.Builder(context);
            b.setContentTitle(r.getString(R.string.app_updated_notification, app.getDisplayName()))
                    .setContentText(r.getString(R.string.app_version_available, app.getLatestVersion()))
                    .setTicker(r.getString(R.string.app_can_be_updated, app.getDisplayName()))
                    .setSmallIcon(R.drawable.ic_menu_refresh)
                    .setAutoCancel(true); //TODO: Think about launching the search on user click.

            NotificationManager mgr = (NotificationManager) context.getSystemService(Service.NOTIFICATION_SERVICE);
            mgr.notify(app.getDisplayName().hashCode(), b.build()); // One notification per available update.
            MainActivity.data_modified = true;
        }
        else if (res.getStatus() == VersionGetResult.Status.ERROR && app.isLastCheckFatalError()) {
            MainActivity.data_modified = true; // Refresh it there is a new fatal error.
        }
        else if (res.getStatus() == VersionGetResult.Status.SUCCESS) {
            MainActivity.data_modified = true; // TODO: Find a way to set is_currently_checking to false if the source is the Activity.
        }

        abortBroadcast(); // Nobody's listening after this BroadcastReceiver.
    }
}
