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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.UpdateSource;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;

public class BroadcastHandler extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // If the activity was never opened, the update sources need to be initialized.
        UpdateSource.initialize_update_sources(context);

        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) ||
            Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction()))
        {
            _handle_model_modification_intent(intent, context);
        }
        else {
            Log.v(MainActivity.TAG, "BroadcastHandler recieved an unhandled intent: " + intent.getAction());
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method is called to handle ACTION_PACKAGE_ADDED and ACTION_PACKAGE_REMOVED
     * intents. It notifies the AppDisplayFragment with a sticky intent which contains
     * the detected information.
     *
     * ACTION_PACKAGE_REPLACED isn't taken into account anymore. This is because when
     * android replaces a package, it removes the old one (ACTION_PACKAGE_ADDED is fired),
     * then adds the new one (ACTION_PACKAGE_ADDED is also fired) and only then is the
     * ACTION_PACKAGE_REPLACED intent sent.
     * If this is a replacement, this method can detect it based on the first two intents
     * so there is no need to wait for the third one.
     *
     * @param i The Intent to process.
     */
    private void _handle_model_modification_intent(Intent i, Context ctx)
    {
        String package_name = i.getDataString();
        package_name = package_name.substring(package_name.indexOf(":") + 1);

        ModelModifiedMessage.event_type type = null;
        if (Intent.ACTION_PACKAGE_ADDED.equals(i.getAction()))
        {
            if (InstalledApp.find_app(package_name) == null)
            {
                type = ModelModifiedMessage.event_type.APP_ADDED;
                InstalledApp.create_app(ctx.getPackageManager(), package_name);
            }
            else // The app was "added", but already exists. This is actually a replacement.
            {
                if (!InstalledApp.detect_new_version(ctx.getPackageManager(), package_name)) {
                    return; // Do not send an event if the version number hasn't changed.
                }
                type = ModelModifiedMessage.event_type.APP_UPDATED;
            }
        }
        else if (Intent.ACTION_PACKAGE_REMOVED.equals(i.getAction()))
        {
            /* If this is a replacement, the intent will be followed by a ACTION_PACKAGE_REPLACED
            immediately after, so we can safely disregard this one. */
            if (i.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                return;
            }
            type = ModelModifiedMessage.event_type.APP_REMOVED;
            InstalledApp.delete_app(package_name); // Remove the app from the model.
        }

        Log.v(MainActivity.TAG, "Received " + type + " (" + package_name + "). The activity will be informed.");

        // Check whether a sticky event already exists and wasn't processed yet.
        EventBusHelper.post_sticky(type, package_name);
    }

}
