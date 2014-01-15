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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The role of this asynchrnous task is to request the Play Store page for a given app, and to
 * use a regular expression to get its latest advertised version (when displayed).
 */
public class AsyncStoreGet extends AsyncTask<String, Void, String>
{
    private InstalledApp app;
    private Context ctx;
    private BaseAdapter la;
    private AppPersistence persistence;

    /**
     * The regexp to extract the version number for Google's Play Store.
     * May have to be updated as the site changed.
     */
    private static Pattern find_version_pattern;
    /**
     * Regexp used to check if a string is a version number, or an error string.
     * For instance, Google Play may return "Version varies depending on the device" and
     * we have to recognize this as an error.
     */
    private static Pattern check_version_pattern;

    static {
        find_version_pattern = Pattern.compile("itemprop=\"softwareVersion\">([^<]+?)</div>");
        check_version_pattern = Pattern.compile("^[0-9a-z.]+$");
    }

    /**
     * The role of this asynchrnous task is to request the Play Store page for a given app, and to
     * use a regular expression to get its latest advertised version (when displayed).
     * @param app The application whose version we wish to check.
     * @param ctx The context of the application.
     * @param la The adapter to notify once the data has been retreived.
     * @param persistence A persistence object to save the new information.
     */
    public AsyncStoreGet(InstalledApp app, Context ctx, AppAdapter la, AppPersistence persistence)
    {
        super();
        this.app = app;
        this.ctx = ctx;
        this.la = la;
        this.persistence = persistence;
    }

    /**
     * Request the web page and process its contents.
     * Do not use this function directly!
     * @param package_names The package name of the application to check. Only the first one is used.
     * @return The version string read from the Play Store, or an error message.
     */
    @Override
    protected String doInBackground(String... package_names)
    {
        String package_name = package_names[0];
        try
        {
            InputStream conn = new URL("https://play.google.com/store/apps/details?id=" + package_name).openStream();
            return Misc.readAll(conn, 2048);
        }
        catch (FileNotFoundException e)
        {
            return "Error: Not a Play Store application.";
        }
        catch (Exception e)
        {
            String err = "https://play.google.com/store/apps/details?id=" + package_name + " could not be retrieved! (" +
                    e.getMessage() + ")";
            Log.e("ApkTrack", err);
            e.printStackTrace();

            return "Error:Could not get open Play Store page! (" + e.getMessage() + ")";
        }
    }

    @Override
    protected void onPostExecute(String s)
    {
        if (!s.startsWith("Error:"))
        {
            Matcher m = find_version_pattern.matcher(s);
            if (m.find())
            {
                String version = m.group(1).trim();
                app.setLatestVersion(version);
                app.setLastCheckError(!check_version_pattern.matcher(version).matches());
            }
            else
            {
                Toast toast = Toast.makeText(ctx, "Version not found.", Toast.LENGTH_LONG);
                toast.show();
                app.setLastCheckError(true);
            }
        }
        else
        {
            app.setLastCheckError(true);
            app.setLatestVersion(s.substring(7));
        }

        app.setLastCheckDate(String.valueOf(System.currentTimeMillis() / 1000L));
        persistence.persistApp(app);
        if (la != null) {
            la.notifyDataSetChanged();
        }
    }
}
