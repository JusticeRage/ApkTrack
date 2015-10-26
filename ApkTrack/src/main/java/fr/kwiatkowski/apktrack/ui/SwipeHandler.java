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

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.service.EventBusHelper;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;

/**
 * This class is responsible with handling swipe movements in the AppDisplayFragment.
 * Swiped items in the list are removed and become ignored.
 */
public class SwipeHandler extends ItemTouchHelper.SimpleCallback
{
    private CoordinatorLayout _coordinator_layout;
    private AppAdapter _adapter;

    public SwipeHandler(CoordinatorLayout cl, AppAdapter adapter)
    {
        super(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        _coordinator_layout = cl;
        _adapter = adapter;
    }

    @Override
    // Items are not moved around in the list.
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    /**
     * When an app is swiped out of the screen, it is set to ignored: updates won't be
     * checked anymore and it won't be displayed in the application list.
     */
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
    {
        AppViewHolder holder = (AppViewHolder) viewHolder;
        final InstalledApp swiped_app = InstalledApp.find_app(holder.get_package_name());
        if (swiped_app == null) {
            return;
        }
        swiped_app.set_ignored(true);
        swiped_app.save();
        _adapter.remove_app(swiped_app);

        // This click listener restores the app in the list if the undo button is clicked.
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                swiped_app.set_ignored(false);
                swiped_app.save();
                _adapter.add_app(swiped_app);
            }
        };

        Snackbar.make(_coordinator_layout, R.string.app_will_be_ignored, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, listener)
                .show();
    }
}
