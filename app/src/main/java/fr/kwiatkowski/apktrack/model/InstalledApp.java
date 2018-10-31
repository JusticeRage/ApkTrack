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

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orm.SugarRecord;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import fr.kwiatkowski.apktrack.MainActivity;
import fr.kwiatkowski.apktrack.model.comparator.AlphabeticalComparator;
import fr.kwiatkowski.apktrack.model.comparator.PackageInfoComparator;

public class InstalledApp extends SugarRecord
        implements Comparable<InstalledApp> {
    public InstalledApp() {
    } // Default constructor for SugarORM

    // --------------------------------------------------------------------------------------------

    public InstalledApp(String package_name,
                        String display_name,
                        String version,
                        String latest_version,
                        boolean system_app) {
        this._package_name = package_name;
        this._display_name = display_name;
        this._version = version;
        this._latest_version = latest_version;
        this._system_app = system_app;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This function is used to create the list of installed applications from
     * the PackageManager (it asks the OS which packages are present).
     * <p>
     * It should only be called on the app's first launch, to populate the database.
     * In Apktrack 1.x, updates and deletions were detected diffing the data present
     * in the database and the data returned by the system.
     * These events should now be detected through Intents instead.
     *
     * @param pacman The PackageManager obtained from a Context object.
     */
    public static void generate_applist_from_system(PackageManager pacman) {
        if (pacman == null) {
            Log.e(MainActivity.TAG, "[InstalledApp.generate_applist_from_system] pacman is null. " +
                    "Cannot obtain app information.");
            return;
        }

        List<PackageInfo> list = pacman.getInstalledPackages(PackageManager.GET_SIGNATURES);
        for (PackageInfo pi : list) {
            _create_application(pacman, pi);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This function checks whether a given app is present in a list of PackageInfo objects.
     * It is used to verify if a given app is still present on the system.
     *
     * @param package_name The name of the application to look for.
     * @param list         A list of PackageInfo objects.
     * @return Whether the requested package is present in the list.
     */
    private static boolean _is_app_in_package_list(String package_name, List<PackageInfo> list) {
        PackageInfo pi = new PackageInfo();
        pi.packageName = package_name;
        return Collections.binarySearch(list, pi, new PackageInfoComparator()) >= 0;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Update the application list by detecting new apps and updating the version of known ones.
     * This function was added to support polling on Oreo as PACKAGE_ADDED broadcasts cannot be
     * received anymore.
     *
     * @param pacman The PackageManager obtained from a Context object.
     * @return An array containing the number of updated, new and deleted apps detected
     * (respectively).
     */
    public static int[] update_applist(PackageManager pacman) {
        if (pacman == null) {
            Log.e(MainActivity.TAG, "[InstalledApp.update_applist] pacman is null. " +
                    "Cannot obtain app information.");
            return new int[]{0, 0, 0};
        }

        int[] results = {0, 0, 0};

        List<PackageInfo> list = pacman.getInstalledPackages(PackageManager.GET_SIGNATURES);
        // Sort the list by alphabetical order for quicker lookups.
        Collections.sort(list, new PackageInfoComparator());

        Log.v(MainActivity.TAG, "Launching app list refresh...");
        for (PackageInfo pi : list) {
            InstalledApp app = InstalledApp.find_app(pi.packageName);
            // App is not present in the database: add it.
            if (app == null) {
                _create_application(pacman, pi);
                results[1] += 1;
            }
            // The actual version differs from the one in the database. Update it.
            else if (!app.get_version().equals(pi.versionName)) {
                app.set_version(pi.versionName);
                app.save();
                results[0] += 1;
            }
        }

        // Finally, look for deleted apps.
        Iterator<InstalledApp> installed_apps = InstalledApp.findAll(InstalledApp.class);
        while (installed_apps.hasNext()) {
            InstalledApp ia = installed_apps.next();
            if (!_is_app_in_package_list(ia.get_package_name(), list)) {
                // App was deleted.
                ia.delete();
                results[0] += 2;
            }
        }
        return results;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Deletes an app from the model.
     *
     * @param package_name The package name of the app to delete.
     */
    public static void delete_app(String package_name) {
        InstalledApp app = find_app(package_name);
        if (app != null) {
            List<AppIcon> icons = find(AppIcon.class, "_owner = ?", app.get_package_name());
            if (icons.size() != 1) {
                Log.w(MainActivity.TAG, "[InstalledApp.delete_app] Deleting an app with " +
                        icons.size() + " associated icons.");
            }
            for (AppIcon icon : icons) {
                icon.delete();
            }
            app.delete();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Adds a single application to the model based on its package name.
     * The app information is obtained from the system and the app is persisted in the
     * database.
     *
     * @param pacman       The package manager
     * @param package_name The package name of the app to create
     */
    public static void create_app(PackageManager pacman, String package_name) {
        if (pacman == null) {
            Log.e(MainActivity.TAG, "[InstalledApp.create_app] pacman is null. " +
                    "Cannot obtain app information.");
            return;
        }

        try {
            PackageInfo pi = pacman.getPackageInfo(package_name, PackageManager.GET_SIGNATURES);
            _create_application(pacman, pi);
        } catch (PackageManager.NameNotFoundException e) {
            // The requested app does not exist.
            Log.v(MainActivity.TAG, "[InstalledApp.create_app] Trying to create" +
                    package_name + ", but no package on the system exists by that name.");
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This function updates the existing information for a given app by updating its version
     * number. This method is called after a package update has been detected to obtain the
     * latest version installed on the device.
     * <p>
     * If a new version is detected, the APK possibly downloaded for this app is deemed
     * outdated and is deleted.
     * <p>
     * The information is persisted in the database.
     *
     * @param ctx          The context of the application
     * @param package_name The application to update.
     * @return True if the information was updated, and false otherwise (i.e. the app does not
     * exist or the information present in the database is already up to date).
     */
    public static boolean detect_new_version(Context ctx, String package_name) {
        InstalledApp app = find_app(package_name);
        if (app == null) {
            return false;
        }

        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(package_name, 0);
            if (pi == null || pi.versionName == null || pi.versionName.equals(app.get_version())) {
                return false;
            }
            app.set_version(pi.versionName);
            app.set_has_notified(false); // Re-allow notifications when new versions are detected.
            if (!app.is_update_available() && app.get_download_id() != 0) {
                app.clean_downloads(ctx);
            }
            app.save();
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.v(MainActivity.TAG, "[InstalledApp.detect_new_version] " + package_name +
                    " is not installed on the device!");
            return false;
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Helper method which instantiates an <code>InstalledApp</code> based on its package name.
     *
     * @param package_name The package name of the application to obtain.
     * @return An <code>InstalledApp</code> object representing the requested app, or
     * <code>null</code> if there was a problem retreiving the app (no match or too many matches).
     */
    public static InstalledApp find_app(String package_name) {
        if (package_name == null) {
            Log.v(MainActivity.TAG, "[InstalledApp.find_app] Called with a null argument!");
            return null;
        }

        List<InstalledApp> res = find(InstalledApp.class, "_packagename = ?", package_name);
        if (res.size() == 0) {
            return null;
        } else if (res.size() > 1) {
            Log.v(MainActivity.TAG, "[InstalledApp.find_app] Multiple apps match "
                    + package_name + ". This should not happen.");
            for (InstalledApp app : res) {
                Log.v(MainActivity.TAG, "\t" + app.get_display_name());
            }
            return null;
        }
        return res.get(0);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * If an icon is present when detecting new apps, this method extracts it and stores it
     * in the database with a reference to the package which owns it.
     * <p>
     * Separating apps from their icons allows ApkTrack to read all the application info from
     * the database without eating up too much RAM.
     * <p>
     * /!\ VectorDrawable icons are only supported on devices with API >= 21.
     *
     * @param pacman The package manager used to read installed apps.
     * @param info   The ApplicationInfo object related to the current app.
     * @param app    The InstalledApp whose icon we're extracting.
     */
    @TargetApi(21)
    private static void _handle_icon(PackageManager pacman, ApplicationInfo info, InstalledApp app) {
        if (info == null || app == null) {
            return;
        }
        Drawable icon = info.loadIcon(pacman);
        if (icon == null) {
            return;
        }

        if (icon instanceof BitmapDrawable) {
            new AppIcon(app, (BitmapDrawable) icon).save();
        } else {
            final Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bmp);
            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            icon.draw(canvas);
            new AppIcon(app, new BitmapDrawable(Resources.getSystem(), bmp)).save();
        }
    }

    // --------------------------------------------------------------------------------------------

    private static boolean is_system_package(PackageInfo pkgInfo) {
        return !(pkgInfo == null || pkgInfo.applicationInfo == null) &&
                ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method is used to "compare" two InstalledApps. This is done by comparing their
     * _display_name alphabetically.
     *
     * @param other The app that we want to be compared to.
     * @return An integer < 0 if this is less than other, 0 if they are equal, and > 0 if this is greater than other.
     */
    @Override
    public int compareTo(@NonNull InstalledApp other) {
        return comparator.compare(this, other);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This function queries the system to obtain all the information needed to insert a new
     * app in the database.
     * The app will be persisted by this function.
     *
     * @param pacman The package manager
     * @param pi     The PackageInfo object related to the target application
     * @return The resulting InstalledApp object, or <code>null</code> if the app couldn't be
     * created (happens if the app is deleted while this function runs).
     */
    private static InstalledApp _create_application(PackageManager pacman,
                                                    PackageInfo pi) {
        if (pi == null || pacman == null) {
            return null;
        }

        ApplicationInfo info;
        try {
            info = pacman.getApplicationInfo(pi.packageName, PackageManager.GET_META_DATA);
        } catch (final PackageManager.NameNotFoundException e) {
            info = null;
        }

        String applicationName = (String) (info != null ? pacman.getApplicationLabel(info) : null);
        InstalledApp app = new InstalledApp(pi.packageName,
                applicationName,
                pi.versionName,
                null,
                is_system_package(pi));

        // Try to guess the update source
        UpdateSource us = UpdateSource.guess_update_source(pi, info != null ? info.metaData : null);
        if (us != null) {
            app.set_update_source(us.get_name());
        }

        // If the app is disabled, assume the user wants to ignore its updates.
        int status;
        try {
            status = pacman.getApplicationEnabledSetting(pi.packageName);
        }
        // It happened once that the app was deleted in the meantime
        catch (IllegalArgumentException ignored) {
            return null;
        }

        if (status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                status == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            app.set_ignored(true);
        }

        // Handle the icon if there is one present.
        _handle_icon(pacman, info, app);

        app.save();
        return app;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * @return Whether there are system apps which are not ignored.
     */
    public static boolean check_system_apps_tracked() {
        long system_apps = InstalledApp.count(InstalledApp.class,
                "_systemapp = 1 AND _isignored = 0",
                null);
        return system_apps != 0;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Tests whether two applications are "equal". They are deemed identical if their package
     * names are the same.
     * <p>
     * This is not strictly true, but this metric is used only for display purposes.
     *
     * @param other The app to which this should be compared. If other is not an
     *              InstalledApp, the default equals method is called.
     * @return True if this is equal to other, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof InstalledApp) {
            return _package_name.equals(((InstalledApp) other).get_package_name());
        }
        return super.equals(other);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Checks whether the app is up to date.
     * The current version is compared with the latest one in order to detect if a new version is
     * available. A simple string comparison is not sufficient, because their structure may vary
     * (i.e. 1.1 and 1.2.0).
     *
     * @return True if the app is up to date.
     */
    public boolean is_update_available() {
        // First, rule out cases where we can't tell.
        if (_version == null || _latest_version == null) {
            return false;
        }
        if (_last_ckeck_error) {// Could be simplified at the expense of readability
            return false;
        }

        // Special handling for Google Play Services: ignore the device-specific string.
        String version = (_package_name.equals("com.google.android.gms")) ? _version.split(" ")[0] : _version;
        String latest_version = (_package_name.equals("com.google.android.gms")) ? _latest_version.split(" ")[0] : _latest_version;

        // Split the version number into tokens
        String[] tokens_version = version.split("[., -]");
        String[] tokens_latest = latest_version.split("[., -]");

        // Version numbers don't even have the same structure. Revert to lexicographical comparison.
        if (tokens_version.length != tokens_latest.length) {
            return _version.compareTo(_latest_version) < 0;
        }

        // Compare tokens one by one.
        for (int i = 0; i < tokens_version.length; ++i) {
            try {
                int t1 = Integer.parseInt(tokens_version[i]);
                int t2 = Integer.parseInt(tokens_latest[i]);
                if (t1 != t2) { // Different tokens. We've hit a version mismatch.
                    return t1 < t2;
                }
                // Otherwise (identical tokens), go on to the next token.
            } catch (NumberFormatException ignored) {
                // Tokens are not simple numbers. Fall back to lexicographical comparison.
                int result = tokens_version[i].compareTo(tokens_latest[i]);
                if (result != 0) {
                    return result < 0; // True iff tokens_version[i] < tokens_latest[i]
                }
            }
        }
        return false; // All the tokens are identical: the two versions are the same.
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Deletes the APK which may have been downloaded by the app.
     *
     * @param ctx The context of the application.
     */
    public void clean_downloads(Context ctx) {
        if (get_download_id() == 0) {
            return;
        }

        DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.remove(get_download_id());
        set_download_id(0);
        save();
        Log.v(MainActivity.TAG, get_display_name() + "'s APK cleaned.");
    }

    // --------------------------------------------------------------------------------------------

    public String get_display_name() {
        return _display_name;
    }

    public String get_package_name() {
        return _package_name;
    }

    public String get_version() {
        return _version;
    }

    public void set_version(String version) {
        _version = version;
    }

    public String get_latest_version() {
        return _latest_version;
    }

    public void set_latest_version(String latest_version) {
        _latest_version = latest_version;
    }

    public Date get_last_check_date() {
        return _last_check_date;
    }

    public void set_last_check_date(Date date) {
        _last_check_date = date;
    }

    public boolean has_notified() {
        return _has_notified;
    }

    public boolean is_ignored() {
        return _is_ignored;
    }

    public void set_ignored(boolean ignored) {
        _is_ignored = ignored;
    }

    public boolean is_system() {
        return _system_app;
    }

    public boolean is_currently_checking() {
        return _is_currently_checking;
    }

    public void set_currently_checking(boolean checking) {
        _is_currently_checking = checking;
    }

    // --------------------------------------------------------------------------------------------

    public boolean is_last_ckeck_error() {
        return _last_ckeck_error;
    }

    public String get_download_url() {
        return _download_url;
    }

    public void set_download_url(String url) {
        _download_url = url;
    }

    public String get_error_message() {
        return _error_message;
    }

    public void set_error_message(String message) {
        set_last_check_error(true);
        _error_message = message;
    }

    public long get_download_id() {
        return _download_id;
    }

    public void set_download_id(long id) {
        _download_id = id;
    }

    /**
     * This is not the way to obtain an app's update source (it only returns the app's name).
     *
     * @return The name of the stored update source.
     * @see UpdateSource#get_source(InstalledApp)
     */
    public String get_update_source() {
        return _update_source;
    }

    public void set_update_source(String update_source) {
        _update_source = update_source;
    }

    public void set_has_notified(boolean has_notified) {
        _has_notified = has_notified;
    }

    public void set_last_check_error(boolean error) {
        _last_ckeck_error = error;
        if (!error) {
            _error_message = null;
        }
    }

    // --------------------------------------------------------------------------------------------

    private String _package_name;
    private String _display_name;
    private String _version;
    private String _latest_version = null;
    private String _download_url = null;
    private boolean _last_ckeck_error = false;
    private String _error_message;
    private boolean _system_app;
    private Date _last_check_date;
    private String _update_source;
    private boolean _is_ignored = false;
    private boolean _has_notified = false;
    private boolean _is_currently_checking = false;
    private long _download_id = 0;

    private final static AlphabeticalComparator comparator = new AlphabeticalComparator();
}