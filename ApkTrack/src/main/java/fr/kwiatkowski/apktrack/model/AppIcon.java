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

package fr.kwiatkowski.apktrack.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.orm.SugarRecord;
import fr.kwiatkowski.apktrack.MainActivity;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * This class represents the icon of an application.
 * Since 2.0, they are stored outside of InstalledApp in order to avoid loading
 * 50 Mb of images at once.
 *
 * This actually used to cause memory exhaustion shutdowns for people who have
 * 500+ apps installed on their device.
 *
 * The AppAdapter now fetches the necessary icons only when needed, and they can
 * also be freed as soon as they aren't displayed anymore.
 */
public class AppIcon extends SugarRecord
{
    private byte[] _raw_image;
    private String _owner; // Used as a foreign key into InstalledApp records.

    // --------------------------------------------------------------------------------------------

    public AppIcon() {} // Default constructor for SugarORM

    // --------------------------------------------------------------------------------------------

    public AppIcon(InstalledApp owner, BitmapDrawable icon)
    {
        Bitmap bmp = icon.getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        _raw_image = baos.toByteArray();
        _owner = owner.get_package_name();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the icon associated to a given app.
     * @param app The app for which we want an icon.
     * @return A BitmapDrawable of the icon if one was found, null otherwise.
     */
    public static Drawable get_icon(InstalledApp app, Context ctx)
    {
        List<AppIcon> icons = AppIcon.find(AppIcon.class, "_owner = ?", app.get_package_name());
        if (icons.size() == 0) {
            return null;
        }
        return icons.get(0)._make_drawable(ctx);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Helper function which creates a BitmapDrawable from a byte array.
     * @return A BitmapDrawable object based on the input data.
     */
    private BitmapDrawable _make_drawable(Context ctx)
    {
        if (ctx == null || ctx.getResources() == null)
        {
            Log.e(MainActivity.TAG, "[AppIcon] make_drawable called with a null context or " +
                    "unable to obtain resources.");
            return null;
        }
        if (_raw_image == null)
        {
            Log.e(MainActivity.TAG, "[AppIcon] make_drawable called with an empty byte array!");
            return null;
        }

        Bitmap bmp = BitmapFactory.decodeByteArray(_raw_image, 0, _raw_image.length);
        return new BitmapDrawable(ctx.getResources(), bmp);
    }

    // --------------------------------------------------------------------------------------------
}
