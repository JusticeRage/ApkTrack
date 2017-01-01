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

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.NoSubscriberEvent;
import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.model.UpdateSource;
import fr.kwiatkowski.apktrack.model.UpdateSourceEntry;
import fr.kwiatkowski.apktrack.service.message.CreateToastMessage;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;
import fr.kwiatkowski.apktrack.service.message.StickyUpdatedMessage;
import fr.kwiatkowski.apktrack.service.utils.SSLHelper;
import fr.kwiatkowski.apktrack.ui.AppDisplayFragment;
import fr.kwiatkowski.apktrack.ui.SettingsFragment;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service performs web requests to obtain the latest version of a given app.
 * An IntentService is used instead of relying on EventBus, because it allows
 * intents to be bufferred. The "check updates for all apps" button would cause
 * the creation of many simultaneous threads with EventBus.
 */
public class WebService extends IntentService
{
    public static final String TARGET_APP_PARAMETER = "target_app";
    public static final String SOURCE_PARAMETER = "source";
    public static final String ACTION = "action";
    public static final String ACTION_VERSION_CHECK = "version_check";
    public static final String ACTION_DOWNLOAD_APK = "download_apk";

    private static final String _nexus_5_user_agent =
            "Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/BuildID) AppleWebKit/537.36" +
            " (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36";
    private static Pattern _fdroid_not_found;
    private static Pattern _appbrain_not_found;
    private static Pattern _play_store_not_a_version;
    static
    {
        _fdroid_not_found = Pattern.compile("<p>Application not found</p>");
        _appbrain_not_found = Pattern.compile("Got a 200 OK, but nothing matched by the regex.");
        _play_store_not_a_version = Pattern.compile("^([^ ]| \\()*$");
    }

    // --------------------------------------------------------------------------------------------

    public WebService()
    {
        super("WebService");
        EventBus.getDefault().register(this);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    protected void onHandleIntent(Intent intent)
    {
        String package_name = intent.getStringExtra(TARGET_APP_PARAMETER);
        String request_source = intent.getStringExtra(SOURCE_PARAMETER);
        String action = intent.getStringExtra(ACTION);

        InstalledApp app = InstalledApp.find_app(package_name);
        if (!_check_arguments(app, request_source, action)) {
            return;
        }

        if (action.equals(ACTION_VERSION_CHECK)) {
            _perform_version_check(app, request_source);
        }
        else if (action.equals(ACTION_DOWNLOAD_APK)) {
            _download_apk(app, request_source);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Performs a web request to obtain the contents of the source's page related to the app.
     * @param source The <code>UpdateSource</code> used by the app. It contains the template of
     *               the URL to query.
     * @param app The app for which the page should be requested.
     * @return An object which represents the contents of the web request, and contains the raw
     * HTML data in case of success.
     */
    private GetResult get_page(UpdateSource source, InstalledApp app)
    {
        String url = String.format(source.get_url(), app.get_package_name());

        Log.v(MainActivity.TAG, "Requesting " + url);
        InputStream conn = null;
        try
        {
            URL target = new URL(url);
            HttpURLConnection huc = (HttpURLConnection) target.openConnection();

            // Authenticate ApkTrack's servers against the bundled certificate.
            if ("https".equals(target.getProtocol()) &&
                "apktrack.kwiatkowski.fr".equals(target.getHost()) &&
                huc instanceof HttpsURLConnection)
            {
                ((HttpsURLConnection) huc).setSSLSocketFactory(
                        SSLHelper.get_ssl_socket_factory(WebService.this));
            }

            String user_agent = System.getProperty("http.agent");
            if (user_agent == null) { // Some devices seem to return null here (see issue #8).
                user_agent = _nexus_5_user_agent;
            }
            huc.setRequestProperty("User-Agent", user_agent);

            huc.setRequestMethod("GET");
            huc.setReadTimeout(10000); // Timeout : 10s
            huc.connect();
            conn = huc.getInputStream();
            String page_contents = _read_all(conn, 2048);

            // Fix update source quirks
            _fix_update_source_response(source, page_contents);

            return new GetResult(page_contents);
        }
        catch (FileNotFoundException e)
        {
            Log.v(MainActivity.TAG, "404 error while getting " + url + "!");
            return new GetResult(GetResult.status_code.ERROR_404);
        }
        catch (UnknownHostException e)
        {
            try {
                Log.v(MainActivity.TAG, "Could not resolve " + new URL(url).getHost() + "!");
            }
            catch (MalformedURLException ignored) {}
            return new GetResult(GetResult.status_code.NETWORK_ERROR);
        }
        catch (ConnectException e)
        {
            Log.v(MainActivity.TAG, "Could not connect to the UpdateSource (" + e.getMessage() + ").");
            return new GetResult(GetResult.status_code.NETWORK_ERROR);
        }
        catch (SocketTimeoutException e)
        {
            Log.v(MainActivity.TAG, "Could not connect to the UpdateSource (" + e.getMessage() + ").");
            return new GetResult(GetResult.status_code.NETWORK_ERROR);
        }
        catch (SSLHandshakeException e)
        {
            Log.w(MainActivity.TAG, "Could not establish a secure connexion.");
            return new GetResult(GetResult.status_code.NETWORK_ERROR);
        }
        catch (Exception e)
        {
            Log.e(MainActivity.TAG, url + " could not be retrieved! (" + e.getMessage() + ")", e);
            return new GetResult(e);
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

    // --------------------------------------------------------------------------------------------

    /**
     * Extracts all the relevant data from a web page.
     * The <code>UpdateSourceEntry</code> object contains a list of regular expressions to apply
     * to the web page contents in order to obtain update information (latest version, APK url,
     * changelog, ...).
     * @param page_contents The contents of the web page to scan.
     * @param use The object containing the regular expressions to apply.
     * @param package_name The name of the app currently being checked.
     * @return An object containing all the version information.
     */
    private static VersionResult apply_regexps(String page_contents,
                                               UpdateSourceEntry use,
                                               String package_name)
    {
        VersionResult vr = new VersionResult();

        if (use.get_version_regexp() != null)
        {
            Matcher m = Pattern.compile(String.format(use.get_version_regexp(),
                                                      package_name))
                    .matcher(page_contents);
            if (m.find()) {
                vr.set_latest_version(m.group(1));
            }
            else
            {
                Log.v(MainActivity.TAG, "The regular expression could not find a version number.");
                Log.d(MainActivity.TAG, page_contents); // Dump the page contents to debug the problem.
            }
        }

        if (use.get_download_url() != null) {
            vr.set_download_url(use.get_download_url());
        }
        else if (use.get_download_regexp() != null)
        {
            Matcher m = Pattern.compile(String.format(use.get_download_regexp(),
                                                      package_name))
                    .matcher(page_contents);
            if (m.find()) {
                vr.set_download_url(m.group(1));
            }
            else
            {
                Log.v(MainActivity.TAG, "The regular expression could not find a download URL.");
                Log.d(MainActivity.TAG, page_contents); // Dump the page contents to debug the problem.
            }
        }

        // TODO: Changelog

        Log.v(MainActivity.TAG, String.format("Obtained version: %s - Download URL: %s",
                vr.get_latest_version(), vr.get_download_url()));
        return vr;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Reads the contents of an InputStream and returns it as a String.
     * @param is The InputStream to read from.
     * @param buffer_size The size of the chunks by which the stream is read.
     * @return The contents of the stream as a String.
     */
    private static String _read_all(final InputStream is, final int buffer_size)
    {
        final char[] buffer = new char[buffer_size];
        final StringBuilder out = new StringBuilder();
        try
        {
            final Reader in = new InputStreamReader(is, "UTF-8");
            try
            {
                for (;;)
                {
                    int rsz = in.read(buffer, 0, buffer.length);
                    if (rsz < 0)
                        break;
                    out.append(buffer, 0, rsz);
                }
            }
            finally {
                in.close();
            }
        }
        catch (IOException ignored) {}
        return out.toString();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Perform a first parsing to normalize Update Source answers.
     * If the response isn't usable as-is, an exception is thown.
     *
     * @param source The source which is currently used.
     * @param page_contents The raw (HTML) data returned by the update source.
     */
    private static void _fix_update_source_response(UpdateSource source, String page_contents)
        throws java.io.IOException
    {
        Matcher m;
        // F-Droid doesn't return a 404 for applications it doesn't have.
        if ("F-Droid".equals(source.get_name()))
        {
            m = _fdroid_not_found.matcher(page_contents);
            if (m.find()) {
                throw new FileNotFoundException();
            }
        }
        // Appbrain fails to return an error when it doesn't have a version number for some apps (see issue #74).
        else if ("AppBrain".equals(source.get_name()))
        {
            m = _appbrain_not_found.matcher(page_contents);
            if (m.find()) {
                throw new FileNotFoundException();
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method is called when a MessageModifiedEvent is posted on the event bus, but the
     * activity is not around to catch it.
     *
     * A notification is displayed to the user since the UI cannot be updated.
     *
     * @param ignored Never used.
     */
    public void onEvent(StickyUpdatedMessage ignored)
    {
        ModelModifiedMessage m = EventBus.getDefault().getStickyEvent(ModelModifiedMessage.class);
        List<InstalledApp> updated_apps = new ArrayList<InstalledApp>();
        List<Pair<ModelModifiedMessage.event_type, String> > events = m.access_events(new MessageAccessor());
        if (events.size() == 0) {
            return;
        }

        // Check the last event posted. It may not require a notification to be trigerred.
        Pair<ModelModifiedMessage.event_type, String> last = events.get(events.size() - 1);
        if (last.first != ModelModifiedMessage.event_type.APP_UPDATED) {
            return;
        }
        InstalledApp app = InstalledApp.find_app(last.second);
        if (app == null || !app.is_update_available() || app.has_notified()) {
            return;
        }

        // Build the list of apps which will be mentioned in the notification.
        for (Pair<ModelModifiedMessage.event_type, String> p : events)
        {
            if (p.first != ModelModifiedMessage.event_type.APP_UPDATED) {
                continue;
            }
            app = InstalledApp.find_app(p.second);
            if (app == null || !app.is_update_available() || app.has_notified()) {
                continue;
            }

            if (!updated_apps.contains(app)) {
                updated_apps.add(app);
            }
        }

        if (updated_apps.size() == 0) {
            return;
        }

        Resources r = getResources();
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        // Show a notification for updated apps
        if (updated_apps.size() == 1)
        {
            app = updated_apps.get(updated_apps.size() - 1);
            b.setContentTitle(r.getString(R.string.app_updated_notification, app.get_display_name()))
                    .setContentText(r.getString(R.string.app_version_available, app.get_latest_version()))
                    .setTicker(r.getString(R.string.app_can_be_updated, app.get_display_name()))
                    .setSmallIcon(R.drawable.ic_menu_refresh)
                    .setAutoCancel(true); //TODO: Think about launching the search/download on user click.
        }
        else
        {
            b.setContentTitle(r.getString(R.string.apps_updated_notification))
                    .setContentText(r.getString(R.string.apps_updated_notification_summary,
                            updated_apps.get(updated_apps.size() - 1).get_display_name(),
                            updated_apps.size() - 1))
                    .setTicker(r.getString(R.string.apps_updated_notification))
                    .setSmallIcon(R.drawable.ic_menu_refresh)
                    .setAutoCancel(true);

            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            for (InstalledApp ia : updated_apps) {
                style.addLine(r.getString(R.string.app_version_available_2,
                        ia.get_display_name(),
                        ia.get_version(),
                        ia.get_latest_version()));
            }
            style.setBigContentTitle(r.getString(R.string.apps_updated_notification));
            b.setStyle(style);
        }

        // Open ApkTrack when the notification is clicked.
        Intent i = new Intent();
        i.setClass(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);
        b.setContentIntent(pi);

        NotificationManager mgr = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        mgr.notify(1, b.build()); // Consolidate notifications.
    }

    /**
     * When the activity is not around to catch the ModelModifiedMessage, this callback is called.
     * In this case, a notification should be displayed.
     *
     * @param event The event thrown by the EventBus.
     */
    public void onEvent(NoSubscriberEvent event)
    {
       if (event.originalEvent instanceof ModelModifiedMessage) {
           onEvent(new StickyUpdatedMessage());
       }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Performs the version check.
     * The app's update source is queried (and detected if none is specified), then the regular
     * expressions of the update source are applied and the app is updated with the new information.
     * @param app The app for which a version check has to be performed.
     * @param request_source The source of the request (may be a background check or a user click).
     *                       Depending on this, a toast may be shown to display an error message.
     */
    private void _perform_version_check(InstalledApp app, String request_source)
    {
        Log.v(MainActivity.TAG, "Launching version check for " + app.get_display_name());

        UpdateSource source = UpdateSource.get_source(app);
        if (source == null)
        {
            Log.v(MainActivity.TAG, "Could not find an update source for " + app.get_display_name());
            app.set_currently_checking(false);
            return;
        }

        GetResult gr = get_page(source, app);
        if (gr.get_status() != GetResult.status_code.SUCCESS)
        {
            // UpdateSource discovery: if no update source was chosen by the user,
            if (gr.get_status() == GetResult.status_code.ERROR_404 && app.get_update_source() == null)
            {
                source = UpdateSource.get_next_source(app, source);
                while (source != null)
                {
                    gr = get_page(source, app);
                    if (gr.get_status() == GetResult.status_code.SUCCESS) {
                        break;
                    }
                    source = UpdateSource.get_next_source(app, source);
                }
            }

            // No suitable update source found, or the specified update source has no information.
            if (source == null || gr.get_status() == GetResult.status_code.ERROR_404) {
                app.set_error_message(getResources().getString(R.string.no_data_found));
            }
            // Network errors don't cause the app to give up on checks.
            else if (gr.get_status() != GetResult.status_code.SUCCESS &&
                    gr.get_status() != GetResult.status_code.NETWORK_ERROR)
            {
                app.set_last_check_error(true);
                if (gr.get_exception() != null) {
                    app.set_error_message(gr.get_exception().getLocalizedMessage());
                }
            }

            // Only display a toast if the user requested a check manually.
            else if (gr.get_status() == GetResult.status_code.NETWORK_ERROR &&
                    AppDisplayFragment.APP_DISPLAY_FRAGMENT_SOURCE.equals(request_source))
            {
                EventBus.getDefault().post(new CreateToastMessage(getResources().getString(R.string.network_error)));
            }
        }

        if (source != null && gr.get_status() == GetResult.status_code.SUCCESS)
        { // Page obtained successfully. Extract the relevant information.
            UpdateSourceEntry entry = source.get_entry(app.get_package_name());

            if (entry == null)
            {
                Log.v(MainActivity.TAG, "No entry or regular expression specified for " + app.get_package_name()
                        + " in " + source.get_name() + "!");
                return;
            }

            // Get the latest version.
            VersionResult vr = apply_regexps(gr.get_page_contents(), entry, app.get_package_name());

            if (vr.get_latest_version() == null) // Regexp did not match anything.
            {
                app.set_error_message(getResources().getString(R.string.regexp_no_match));
            }
            // If we didn't know about this version, save the new information.
            else if (app.get_latest_version() == null || !app.get_latest_version().equals(vr.get_latest_version()))
            {
                app.set_last_check_error(false);
                app.set_latest_version(vr.get_latest_version());
                if (app.is_update_available())
                {
                    app.set_download_url(vr.get_download_url());
                    if (app.get_download_id() != 0) { // A new version is available. If an APK was downloaded,
                        app.clean_downloads(this);    // it is now obsolete.
                    }
                }
                else { // Don't store URLs for current or old APKs.
                    app.set_download_url(null);
                }
            }
            else // We knew about this version.
            {
                // Update the download URL (in case the APK has moved).
                if (app.get_download_url() == null || !app.get_download_url().equals(vr.get_download_url())) {
                    app.set_download_url(vr.get_download_url());
                }
                app.set_last_check_error(false);
            }

            app.set_last_check_date(new Date());
            // Set as default update source if none existed.
            if (app.get_update_source() == null && !app.is_last_ckeck_error()) {
                app.set_update_source(source.get_name());
            }
        }

        // Hide the spinner
        app.set_currently_checking(false);

        // Automatically download the APK if a new one is available
        if (app.is_update_available() &&
            app.get_download_url() != null &&
            ScheduledCheckService.SERVICE_SOURCE.equals(request_source) &&
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.KEY_PREF_DOWNLOAD_APKS, false))
        {
            Log.v(MainActivity.TAG, "Automatically downloading the latest APK.");
            _download_apk(app, request_source); // app.save() + notification are done in this function.
        }
        else
        {
            // Save updates to the app and notify.
            app.save();
            EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());
        }

        // Sleep to avoid flooding the update source.
        try
        {
            if (source != null) {
                Thread.sleep(source.get_request_delay());
            }
        }
        catch (InterruptedException ignored) {}
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Downloads the APK for a given app if a download URL is available.
     * APK downloads take place through the Download Service. Files are stored on a dedicated
     * folder on the external storage of the device.
     * @param app The app whose APK is to be downloaded.
     */
    private void _download_apk(InstalledApp app, String request_source)
    {
        if (app.get_download_url() == null) {
            return;
        }

        Uri uri = Uri.parse(String.format(app.get_download_url(),
                app.get_display_name(),
                app.get_latest_version()));

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        // Indicate whether the download may take place over mobile data.
        // Download over data is okay on user click, but respect the preference for background checks.
        if (ScheduledCheckService.SERVICE_SOURCE.equals(request_source) &&
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsFragment.KEY_PREF_WIFI_ONLY, true))
        {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        else
        {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE |
                    DownloadManager.Request.NETWORK_WIFI);
        }

        // Don't download APKs when roaming.
        request.setAllowedOverRoaming(false)
               .setTitle(getString(getApplicationInfo().labelRes))
               .setDescription(app.get_display_name() + " " + app.get_latest_version() + ".apk")
               .setVisibleInDownloadsUi(false)
               .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, app.get_package_name() +
                       "-" + app.get_latest_version() + ".apk");
        long id = dm.enqueue(request);
        app.set_download_id(id);
        app.save();
        EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Checks whether the intent arguments are valid and whether the version check should proceed.
     * @param app The app which is being checked.
     * @param request_source The source of the intent.
     * @return Whether the version check should proceed.
     */
    private boolean _check_arguments(InstalledApp app, String request_source, String action)
    {
        if (app == null || request_source == null || action == null)
        {
            Log.v(MainActivity.TAG, "WebService was invoked with no targetApp and/or source argument!");
            return false;
        }

        // Unsupported action
        if (!action.equals(ACTION_DOWNLOAD_APK) && !action.equals(ACTION_VERSION_CHECK)) {
            return false;
        }

        if (ScheduledCheckService.SERVICE_SOURCE.equals(request_source) && _check_cancellation()) {
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Checks whether the check should be aborted.
     * This may happen depending on user preferences (i.e. WiFi only)
     * @return True if the check should be cancelled.
     */
    private boolean _check_cancellation()
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        // If the background checks are not allowed, abort.
        if (!pref.getBoolean(SettingsFragment.KEY_PREF_BACKGROUND_CHECKS, false)) {
            return true;
        }

        // If the user requested it, verify that we are using WiFi before each request.
        if (pref.getBoolean(SettingsFragment.KEY_PREF_WIFI_ONLY, true))
        {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info == null)
            {
                Log.v(MainActivity.TAG, "Aborting automatic checks because no network is currently active.");
                return true;
            }
            else if (info.getType() != ConnectivityManager.TYPE_WIFI)
            {
                Log.v(MainActivity.TAG, "Aborting automatic checks over data due to user preferences.");
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------------------------------------
    // Inner class MessageAccessor
    // --------------------------------------------------------------------------------------------

    /**
     * This class is used as an access mechanism to access a method in
     * <code>ModelModifiedMessage</code>. Since it's an inner class, only WebService can
     * create it.
     * @see ModelModifiedMessage#access_events(WebService.MessageAccessor)
     */
    public static class MessageAccessor {
        private MessageAccessor() {}
    }
}

// --------------------------------------------------------------------------------------------
// GetResult
// --------------------------------------------------------------------------------------------

/**
 * This class contains the data obtained when a web page is queried.
 */
class GetResult
{
    public enum status_code { SUCCESS, NETWORK_ERROR, ERROR_404, UNKNOWN_ERROR }
    private status_code _status;
    private String _page_contents;
    private Exception _exception = null;

    // --------------------------------------------------------------------------------------------

    public GetResult(String page_contents)
    {
        _status = status_code.SUCCESS;
        _page_contents = page_contents;
    }

    // --------------------------------------------------------------------------------------------

    public GetResult(status_code status)
    {
        _status = status;
        _page_contents = null;
    }

    // --------------------------------------------------------------------------------------------

    public GetResult(Exception e)
    {
        _status = status_code.UNKNOWN_ERROR;
        _exception = e;
    }

    // --------------------------------------------------------------------------------------------

    public status_code  get_status()        { return _status; }
    public String       get_page_contents() { return _page_contents; }
    public Exception    get_exception()     { return _exception; }
}

// --------------------------------------------------------------------------------------------
// VersionResult
// --------------------------------------------------------------------------------------------

/**
 * This class represents the data returned after regular expressions have been applied to a web page.
 */
class VersionResult
{
    private String _latest_version;
    private String _download_url;
    private String _changelog;
    private GetResult.status_code status = GetResult.status_code.SUCCESS;

    public String get_latest_version() {
        return _latest_version;
    }

    public void set_latest_version(String latest_version) {
        this._latest_version = latest_version.trim();
    }

    public String get_download_url() {
        return _download_url;
    }

    public void set_download_url(String download_url) {
        this._download_url = download_url.trim();
    }

    public String get_changelog() {
        return _changelog;
    }

    public void set_changelog(String changelog) {
        this._changelog = changelog;
    }
}