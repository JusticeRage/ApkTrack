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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.UpdateSource;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;

/**
 * This receiver's role is to listen for ACTION_DOWNLOAD_COMPLETE intents launched by the Download Service, and
 * notify the activity that the app for which an APK was obtained should be updated in the display.
 */
public class DownloadCompletedHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // If the activity was never opened, the update sources need to be initialized.
        UpdateSource.initialize_update_sources(context);

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            if (id == 0) {
                return;
            }

            Log.v(MainActivity.TAG, "Download complete! ID = " + id);
            List<InstalledApp> apps = InstalledApp.find(InstalledApp.class, "_downloadid = ?", String.valueOf(id));
            if (apps.size() < 1) {
                return;
            }
            InstalledApp downloaded_app = apps.get(0);
            EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, downloaded_app.get_package_name());
        } else {
            Log.v(MainActivity.TAG, "DownloadCompletedHandler recieved an unhandled intent: " + intent.getAction());
        }
    }
}
