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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.UpdateSource;
import fr.kwiatkowski.apktrack.service.EventBusHelper;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;

import java.util.Arrays;

public class UpdateSourceChooser
{
    /**
     * Create a chooser dialog that allows the user to choose his update source.
     * @param app The target application.
     * @param ctx The context of the application.
     */
    public static void show_dialog(final InstalledApp app, final Context ctx)
    {
        if (app == null) {
            return;
        }

        final String[] sources = UpdateSource.get_sources(app);
        if (sources.length == 0) {
            return;
        }
        int checked_item = app.get_update_source() == null ? -1 : Arrays.asList(sources).indexOf(app.get_update_source());

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.available_update_sources);
        final String[] selected = new String[1];
        builder.setSingleChoiceItems(sources, checked_item, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selected[0] = sources[which];
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (selected[0] == null || selected[0].equals(app.get_update_source())) {
                    return; // No change
                }
                app.set_update_source(selected[0]);
                app.set_last_check_date(null); // No check on the new update source
                Log.v(MainActivity.TAG, app.get_display_name() + "'s update source set to " + selected[0]);
                app.save();
                // Tell the AppDisplayFragment to reflect the changes.
                EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            } // Cancel: don't do anything.
        });

        builder.show();
    }
}
