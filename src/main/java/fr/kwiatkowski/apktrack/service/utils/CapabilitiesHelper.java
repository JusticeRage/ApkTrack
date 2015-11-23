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

package fr.kwiatkowski.apktrack.service.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

/**
 * This class centralizes the tests about device capabilities.
 * Values are cached for performance reasons, but it may happen that in some fringe cases, this is a problem
 * (i.e. if the user installs a browser after seeing an error message).
 */
public class CapabilitiesHelper
{
    /**
     * Checks whether a browser if available on the device to handle ACTION_VIEW intents.
     * @param ctx The context of the application.
     * @return Whether ACTION_VIEW intents will resolve.
     */
    public static boolean check_browser_available(Context ctx)
    {
        if (_BROWSER_AVAILABLE != null) {
            return _BROWSER_AVAILABLE;
        }
        Intent browser_intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://apktrack.kwiatkowski.fr/"));
        _BROWSER_AVAILABLE = browser_intent.resolveActivity(ctx.getPackageManager()) != null;
        return _BROWSER_AVAILABLE;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Checks whether the download service is present on the device.
     * @param ctx The context of the application.
     * @return True if the download service can be used to download files.
     */
    public static boolean check_download_service(Context ctx)
    {
        if (_DOWNLOAD_SERVICE_AVAILABLE != null) {
            return _DOWNLOAD_SERVICE_AVAILABLE;
        }
        File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!(f.mkdirs() || f.isDirectory())) { // Make sure the downloads folder exists.
            return false;
        }
        _DOWNLOAD_SERVICE_AVAILABLE = ctx.getSystemService(Context.DOWNLOAD_SERVICE) != null;
        return _DOWNLOAD_SERVICE_AVAILABLE;
    }

    // --------------------------------------------------------------------------------------------

    // Cached values.
    private static Boolean _BROWSER_AVAILABLE = null;
    private static Boolean _DOWNLOAD_SERVICE_AVAILABLE = null;
}
