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

import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The role of this asynchrnous task is to request the Play Store page for a given app, and to
 * use a regular expression to get its latest advertised version (when displayed).
 */
public class PlayStoreGetTask extends AsyncTask<Void, Void, PlayStoreGetResult>
{
    private InstalledApp app;
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
     * The role of this task is to request the Play Store page for a given app, and to
     * use a regular expression to get its latest advertised version (when displayed).
     * @param app The application whose version we wish to check.
     * @param la The adapter to notify once the data has been retreived.
     * @param persistence A persistence object to save the new information.
     */
    public PlayStoreGetTask(InstalledApp app, AppAdapter la, AppPersistence persistence)
    {
        super();
        this.app = app;
        this.la = la;
        this.persistence = persistence;
    }

    /**
     * This method performs the task in a synchronous manner.
     * Use @see <code>execute</code> instead if called from the UI thread.
     */
    public PlayStoreGetResult sync_execute()
    {
        PlayStoreGetResult res = get_page();
        process_result(res);
        return res;
    }

    private void process_result(PlayStoreGetResult result)
    {
        app.setCurrentlyChecking(false);

        if (result.getStatus() == PlayStoreGetResult.Status.SUCCESS)
        {
            Matcher m = find_version_pattern.matcher(result.getMessage());
            if (m.find())
            {
                String version = m.group(1).trim();
                app.setLatestVersion(version);

                // It is possible we recieve "Varies depending on the device" here. This is fatal: no need to try again.
                app.setLastCheckFatalError(!check_version_pattern.matcher(version).matches());

                // Update the result object. This data is forwarded to the service during periodic updates.
                if (!app.isLastCheckFatalError() && !app.getVersion().equals(version))
                {
                    result.setMessage(version);
                    result.setStatus(PlayStoreGetResult.Status.UPDATED);
                }
            }
            else
            {
                app.setLastCheckFatalError(true);
            }
        }
        else
        {
            if (!result.isFatal())
            {
                // Error is not fatal, most likely network related.
                // Don't update the app, but try again later.
                return;
            }
            app.setLastCheckFatalError(true);
            app.setLatestVersion(result.getMessage());
        }

        app.setLastCheckDate(String.valueOf(System.currentTimeMillis() / 1000L));
        persistence.updateApp(app);
        if (la != null) {
            la.notifyDataSetChanged();
        }
    }

    private PlayStoreGetResult get_page()
    {
        InputStream conn = null;
        try
        {
            HttpURLConnection huc = (HttpURLConnection) new URL("https://play.google.com/store/apps/details?id=" + app.getPackageName()).openConnection();
            huc.setRequestMethod("GET");
            huc.setReadTimeout(15000); // Timeout : 15s
            huc.connect();
            conn = huc.getInputStream();
            return new PlayStoreGetResult(PlayStoreGetResult.Status.SUCCESS, Misc.readAll(conn, 2048));
        }
        catch (FileNotFoundException e)
        {
            // This error is fatal: do not look for updates automatically anymore.
            return new PlayStoreGetResult(PlayStoreGetResult.Status.ERROR, "Not a Play Store application.", true);
        }
        catch (UnknownHostException e) {
            return new PlayStoreGetResult(PlayStoreGetResult.Status.ERROR, "Connectivity problem.");
        }
        catch (Exception e)
        {
            String err = "https://play.google.com/store/apps/details?id=" + app.getPackageName() + " could not be retrieved! (" +
                    e.getMessage() + ")";
            Log.e("ApkTrack", err);
            e.printStackTrace();

            return new PlayStoreGetResult(PlayStoreGetResult.Status.ERROR, "Could not open Play Store page! (" + e.getMessage() + ")");
        }
        finally
        {
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Request the web page and process its contents.
     * Do not use this function directly!
     * @return The version string read from the Play Store, or an error message.
     */
    @Override
    protected PlayStoreGetResult doInBackground(Void... voids) {
        return get_page();
    }

    @Override
    protected void onPostExecute(PlayStoreGetResult s)
    {
        process_result(s);
    }
}

class PlayStoreGetResult
{
    enum Status {SUCCESS, ERROR, UPDATED}

    private String message;
    private boolean fatal;
    private Status result;

    PlayStoreGetResult(Status status, String message)
    {
        this.message = message;
        this.result = status;
        this.fatal = false;
    }

    PlayStoreGetResult(Status status, String message, boolean fatal)
    {
        this.result = status;
        this.message = message;
        this.fatal = fatal;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFatal() {
        return fatal;
    }

    public Status getStatus() {
        return result;
    }

    public void setStatus(Status result) {
        this.result = result;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}