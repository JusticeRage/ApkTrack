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
import android.webkit.WebSettings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The role of this asynchronous task is to request the Play Store page or AppBrain for a given app, and to
 * use a regular expression to get its latest advertised version (when displayed).
 */
public class VersionGetTask extends AsyncTask<Void, Void, VersionGetResult>
{
    private InstalledApp app;
    private AppAdapter la;
    private AppPersistence persistence;
    private PageUsed page_used = PageUsed.PLAY_STORE;

    enum PageUsed { PLAY_STORE, APPBRAIN }

    private static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=";
    private static final String APPBRAIN_URL = "https://www.appbrain.com/app/google/";

    /**
     * The regexp to extract the version number for Google's Play Store.
     * May have to be updated as the site changed.
     */
    private static Pattern play_find_version_pattern;

    /**
     * The regexp to extract the version number for AppBrain.
     * May have to be updated as the site changed.
     */
    private static Pattern appbrain_find_version_pattern;

    /**
     * Regexp used to check if a string is a version number, or an error string.
     * For instance, Google Play may return "Version varies depending on the device" and
     * we have to recognize this as an error.
     */
    private static Pattern check_version_pattern;

    static {
        play_find_version_pattern = Pattern.compile("itemprop=\"softwareVersion\">([^<]+?)</div>");
        appbrain_find_version_pattern = Pattern.compile("<div class=\"clDesc\">Version ([^<]+?)</div>");
        check_version_pattern = Pattern.compile("^[0-9a-zA-Z().-]+$");
    }

    /**
     * The role of this task is to request a web page for a given app, and to
     * use a regular expression to get its latest advertised version (when displayed).
     *
     * This constructor defaults the requested page to the Google Play Store.
     *
     * @param app The application whose version we wish to check.
     * @param la The adapter to notify once the data has been retreived.
     * @param persistence A persistence object to save the new information.
     */
    public VersionGetTask(InstalledApp app, AppAdapter la, AppPersistence persistence)
    {
        super();
        this.app = app;
        this.la = la;
        this.persistence = persistence;
        this.page_used = PageUsed.PLAY_STORE;
    }

    /**
     * The role of this task is to request a web page for a given app, and to
     * use a regular expression to get its latest advertised version (when displayed).
     * @param app The application whose version we wish to check.
     * @param la The adapter to notify once the data has been retreived.
     * @param persistence A persistence object to save the new information.
     * @param page The page to check
     */
    public VersionGetTask(InstalledApp app, AppAdapter la, AppPersistence persistence, PageUsed page)
    {
        super();
        this.app = app;
        this.la = la;
        this.persistence = persistence;
        this.page_used = page;
    }

    /**
     * This method performs the task in a synchronous manner.
     * Use @see <code>execute</code> instead if called from the UI thread.
     */
    public VersionGetResult sync_execute()
    {
        VersionGetResult res = null;
        if (page_used == PageUsed.PLAY_STORE) {
            res = get_page(PLAY_STORE_URL);
        }
        else if (page_used == PageUsed.APPBRAIN) {
            res = get_page(APPBRAIN_URL);
        }
        // TODO: User supplied webpage & regexp

        process_result(res);
        return res;
    }

    private void process_result(VersionGetResult result)
    {
        app.setCurrentlyChecking(false);

        if (result.getStatus() == VersionGetResult.Status.SUCCESS)
        {
            Matcher m = null;
            if (page_used == PageUsed.PLAY_STORE) {
                m = play_find_version_pattern.matcher(result.getMessage());
            }
            else if (page_used == PageUsed.APPBRAIN) {
                m = appbrain_find_version_pattern.matcher(result.getMessage());
            }
            // TODO: Support user-specified page & regexp

            if (m != null && m.find())
            {
                String version = m.group(1).trim();
                Log.v("ApkTrack", "Version obtained: " + version);
                app.setLatestVersion(version);

                // Change the status to ERROR if this is not a version number.
                if (!check_version_pattern.matcher(version).matches())
                {
                    Log.v("ApkTrack", "This is not recognized as a version number.");
                    result.setStatus(VersionGetResult.Status.ERROR);
                }
                // Do not perform further auto checks if this is not a version number (i.e. "Varies with the device").
                app.setLastCheckFatalError(!check_version_pattern.matcher(version).matches());

                // Update the result object. This data is forwarded to the service during periodic updates.
                if (!app.isLastCheckFatalError() && !app.getVersion().equals(version))
                {
                    result.setMessage(version);
                    result.setStatus(VersionGetResult.Status.UPDATED);
                }
            }
            else
            {
                Log.v("ApkTrack", "Nothing matched by the regular expression.");
                Log.v("ApkTrack", "Requested page: " + page_used);
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

    private VersionGetResult get_page(String url)
    {
        Log.v("ApkTrack", "Requesting " + url + app.getPackageName());
        InputStream conn = null;
        try
        {
            HttpURLConnection huc = (HttpURLConnection) new URL(url + app.getPackageName()).openConnection();
            // AppBrain doesn't like non-browser user-agents. Use the device's default one.
            huc.setRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(null));
            huc.setRequestMethod("GET");
            huc.setReadTimeout(15000); // Timeout : 15s
            huc.connect();
            conn = huc.getInputStream();
            return new VersionGetResult(VersionGetResult.Status.SUCCESS, Misc.readAll(conn, 2048));
        }
        catch (FileNotFoundException e)
        {
            // This error is fatal: do not look for updates automatically anymore.
            return new VersionGetResult(VersionGetResult.Status.ERROR, "Not a Play Store application", true);
        }
        catch (UnknownHostException e) {
            return new VersionGetResult(VersionGetResult.Status.NETWORK_ERROR, "Connectivity problem");
        }
        catch (Exception e)
        {
            String err = url + app.getPackageName() + " could not be retrieved! (" +
                    e.getMessage() + ")";
            Log.e("ApkTrack", err);
            e.printStackTrace();

            return new VersionGetResult(VersionGetResult.Status.NETWORK_ERROR, "Could not open Play Store page! (" + e.getMessage() + ")");
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
     * @return The version string read from a website, or an error message.
     */
    @Override
    protected VersionGetResult doInBackground(Void... voids)
    {
        Log.v("ApkTrack", app.getDisplayName() + " check started.");
        VersionGetResult res = null;
        if (page_used == PageUsed.PLAY_STORE)
        {
            res = get_page(PLAY_STORE_URL);
            Log.v("ApkTrack", app.getDisplayName() + " check result (Play Store): " + res.getStatus());
        }
        else if (page_used == PageUsed.APPBRAIN)
        {
            res = get_page(APPBRAIN_URL);
            Log.v("ApkTrack", app.getDisplayName() + " check result (AppBrain): " + res.getStatus());
        }
        // TODO: User specified webpage & regexp

        return res;
    }

    @Override
    protected void onPostExecute(VersionGetResult s)
    {
        process_result(s);
        if (s.getStatus() == VersionGetResult.Status.ERROR && page_used == PageUsed.PLAY_STORE)
        {
            Log.v("ApkTrack", "Play Store check failed. Trying AppBrain...");
            app.setCurrentlyChecking(true);
            new VersionGetTask(app, la, persistence, PageUsed.APPBRAIN).execute();
        }
    }
}

class VersionGetResult
{
    enum Status {SUCCESS, ERROR, NETWORK_ERROR, UPDATED}

    private String message;
    private boolean fatal;
    private Status result;

    VersionGetResult(Status status, String message)
    {
        this.message = message;
        this.result = status;
        this.fatal = false;
    }

    VersionGetResult(Status status, String message, boolean fatal)
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