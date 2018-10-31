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
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.kwiatkowski.apktrack.MainActivity;

/**
 * A simple class representing an update source.
 * An update source is a website that can be scraped to get update information, or even APKs.
 */
public class UpdateSource implements Serializable {
    private static ArrayList<UpdateSource> _SOURCES = null;

    private String _name;

    // --------------------------------------------------------------------------------------------
    private String _url;
    private List<String> _autoselect_conditions;
    private ArrayList<UpdateSourceEntry> _entries;

    // --------------------------------------------------------------------------------------------
    private int _request_delay = 200;

    /**
     * The constructor is private, because outside classes are not supposed to create
     * UpdateSources. Those should only be read from sources.json, and created during
     * the initialization process.
     *
     * @param name    The name of the update source
     * @param url     The page on which the update information should be fetched.
     * @param entries A list of objects which link package names to regular expressions
     */
    private UpdateSource(String name,
                         String url,
                         ArrayList<UpdateSourceEntry> entries) {
        this._name = name;
        this._url = url;
        this._entries = entries;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Initializes the update sources by reading the ones available in sources.json.
     * They are kept in memory in order to avoid looking them up in the assets every time.
     *
     * @param ctx The context of the application.
     */
    public static void initialize_update_sources(Context ctx) {
        if (_SOURCES != null) {
            return;
        }

        _SOURCES = new ArrayList<>();
        Log.v(MainActivity.TAG, "Reading update sources...");
        try {
            InputStream is = ctx.getAssets().open("sources.json");

            StringBuilder buffer = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String s;
            while ((s = br.readLine()) != null) {
                buffer.append(s);
            }

            JSONArray sources = new JSONArray(buffer.toString());
            for (int i = 0; i < sources.length(); ++i) {
                String name = sources.getJSONObject(i).getString("name");
                Log.v(MainActivity.TAG, "Reading " + name);
                String url = sources.getJSONObject(i).getString("url");

                // Get the list of regular expressions and URLs for each supported package name.
                JSONObject packages = sources.getJSONObject(i).optJSONObject("packages");
                if (packages == null || packages.length() == 0) {
                    throw new JSONException("packages missing or empty for " + name);
                }
                ArrayList<UpdateSourceEntry> entries = new ArrayList<>();

                Iterator<String> it = packages.keys();
                while (it.hasNext()) {
                    String applicable_packages = it.next();
                    JSONObject entry = packages.getJSONObject(applicable_packages);
                    UpdateSourceEntry use = new UpdateSourceEntry(applicable_packages);
                    use.set_version_regexp(entry.getString("version"));
                    try {
                        use.set_download_regexp(entry.getString("download_regexp"));
                    } catch (JSONException ignored) {
                    }
                    try {
                        use.set_download_url(entry.getString("download"));
                    } catch (JSONException ignored) {
                    }
                    try {
                        use.set_changelog_regexp(entry.getString("changelog"));
                    } catch (JSONException ignored) {
                    }
                    entries.add(use);
                }

                UpdateSource us = new UpdateSource(name, url, entries);

                // Get autoselection conditions if available:
                JSONArray conditions = sources.getJSONObject(i).optJSONArray("autoselect_if");
                if (conditions != null) {
                    List<String> autoselect_conditions = new ArrayList<>();
                    for (int j = 0; j < conditions.length(); ++j) {
                        autoselect_conditions.add(conditions.getString(j));
                    }
                    if (autoselect_conditions.size() > 0) {
                        us.set_autoselect_conditions(autoselect_conditions);
                    }
                }

                // Get optional request delay (in milliseconds)
                int delay = sources.getJSONObject(i).optInt("request_delay", 0);
                if (delay > 0) {
                    us.set_request_delay(delay);
                }

                _SOURCES.add(us);
            }
        } catch (IOException e) {
            Log.e(MainActivity.TAG, "Could not open sources.json!", e);
        } catch (JSONException e) {
            Log.e(MainActivity.TAG, "sources.json seems to be malformed!", e);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Reads the update sources from the JSON asset file.
     *
     * @return A list of available update sources.
     */
    public static ArrayList<UpdateSource> get_update_sources() {
        return _SOURCES;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the update source set for an app, or the first applicable one.
     *
     * @param app The target application.
     * @return An update source that can be used for this application, or null if no source could be found.
     */
    public static UpdateSource get_source(InstalledApp app) {
        if (get_update_sources() == null) {
            return null;
        }
        // Return the stored update source first, if available.
        if (app.get_update_source() != null) {
            UpdateSource s = get_source(app.get_update_source());
            if (s != null) {
                return s;
            } else // Source has been removed from sources.json.
            {
                app.set_update_source(null);
                app.save();
            }
        }
        // Find the first applicable source.
        for (UpdateSource s : get_update_sources()) {
            if (s.is_applicable(app)) {
                return s;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the update source matching a specific name.
     * Generally used with a source name stored in the apps table.
     *
     * @param name The name of the source to retrieve.
     * @return The requested source, or null if it doesn't exist.
     */
    public static UpdateSource get_source(String name) {
        if (get_update_sources() == null) {
            return null;
        }
        for (UpdateSource s : get_update_sources()) {
            if (name.equals(s.get_name())) {
                return s;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method tries to guess the best update source for a given app based on
     * its signature.
     *
     * @param pi       The PackageInfo object returned by the PackageManager
     * @param metadata The metadata contained in the app's manifest
     * @return An adequate update source for the app, or null if auto-disvocery must take place.
     */
    public static UpdateSource guess_update_source(PackageInfo pi, Bundle metadata) {
        android.content.pm.Signature[] signs = pi.signatures;
        ArrayList<String> details = new ArrayList<>();
        for (Signature sign : signs) {
            X509Certificate cert;
            try {
                cert = (X509Certificate) CertificateFactory.getInstance("X509")
                        .generateCertificate(new ByteArrayInputStream(sign.toByteArray()));
            } catch (CertificateException e) {
                Log.v(MainActivity.TAG, "Error while reading " + pi.packageName + "'s certificate.");
                return null;
            }
            details.addAll(Arrays.asList(cert.getSubjectDN().getName().split(",")));
        }

        // Also add the metadata contained in the manifest file.
        if (metadata != null) {
            for (String key : metadata.keySet()) {
                details.add("metadata=" + key);
            }
        }

        for (UpdateSource us : get_update_sources()) {
            if (us.test_autoselection(pi.packageName, details)) {
                return us;
            }
        }

        return null;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the next applicable UpdateSource for an app, after a given one.
     * The UpdateSources are returned in the order given in the JSON asset file.
     *
     * @param app    The application whose version we want to check.
     * @param source The latest source used.
     * @return The next applicable UpdateSource, or null if no source could be found.
     */
    public static UpdateSource get_next_source(InstalledApp app, UpdateSource source) {
        if (get_update_sources() == null || app == null) {
            return null;
        }
        int index = get_update_sources().indexOf(source);
        if (index == -1) {
            return null;
        }
        for (int i = ++index; i < _SOURCES.size(); ++i) {
            if (_SOURCES.get(i).is_applicable(app)) {
                return _SOURCES.get(i);
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the name of all the update sources available for a given package.
     *
     * @param app The app we want to check against.
     * @return A list of names for UpdateSources that can be used to check the application's version.
     */
    public static String[] get_sources(InstalledApp app) {
        if (app == null) {
            return new String[]{};
        }

        ArrayList<String> res = new ArrayList<>();
        if (get_update_sources() == null) {
            return new String[]{};
        }
        for (UpdateSource s : get_update_sources()) {
            if (s.is_applicable(app)) {
                res.add(s.get_name());
            }
        }
        String[] retval = new String[res.size()];
        res.toArray(retval);
        return retval;
    }

    // --------------------------------------------------------------------------------------------

    public String get_name() {
        return _name;
    }

    // --------------------------------------------------------------------------------------------

    public String get_url() {
        return _url;
    }

    // --------------------------------------------------------------------------------------------

    public int get_request_delay() {
        return _request_delay;
    }

    // --------------------------------------------------------------------------------------------

    public void set_request_delay(int delay) {
        _request_delay = delay;
    }

    // --------------------------------------------------------------------------------------------

    public void set_autoselect_conditions(List<String> conditions) {
        _autoselect_conditions = conditions;
    }

    /**
     * Checks whether the UpdateSource is applicable for a given application.
     * The package name of the given app is checked against the list of packages for which an
     * update source can provide version information.
     *
     * @param app The application to check.
     * @return Whether the UpdateSource is valid for a given application.
     */
    public boolean is_applicable(@NonNull InstalledApp app) {
        return is_applicable(app.get_package_name());
    }

    /**
     * Checks whether the UpdateSource is applicable for a given application.
     * The package name of the given app is checked against the different regular expressions
     * available for the update source.
     *
     * @param package_name The package name of the application to check.
     * @return Whether the UpdateSource is valid for a given application.
     */
    public boolean is_applicable(String package_name) {
        // Verify that the update source supports given application by matching the package name.
        for (UpdateSourceEntry pe : _entries) {
            Matcher m = Pattern.compile(pe.get_applicable_packages()).matcher(package_name);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the entry related to a given package name.
     *
     * @param package_name The package name of the application to check.
     * @return An object containing all the necessary information to extract a version and/or a
     * download url and changelog from a web page.
     */
    public UpdateSourceEntry get_entry(String package_name) {
        for (UpdateSourceEntry use : _entries) {
            Matcher m = Pattern.compile(use.get_applicable_packages()).matcher(package_name);
            if (m.find()) {
                return use;
            }
        }
        return null;
    }

    /**
     * Checks whether this update source should be used as a default for the given app.
     * <p>
     * This is a very primitive DSL related to source.json's "autoselect_if" information.
     * A list of conditions is given in the file, and if any of them match, the update
     * source is selected. Conditions may be related to the APK's signature (tests on
     * its CN, O, etc.), metadata present in the manifest or the hardcoded keyword "applicable",
     * which means that the update source should be used as default for any applicable packages.
     *
     * @param package_name The package name of the application to test.
     * @param details      The details of the APK signature and the metadata of the app. It is an array
     *                     of strings containing information such as ["CN=Name", "metadata=xposedmodule",
     *                     ...].
     * @return True if this update source should be used by default for the given app.
     */
    public boolean test_autoselection(String package_name, List<String> details) {
        if (_autoselect_conditions == null || !is_applicable(package_name)) {
            return false; // No available autoselection conditions: cannot be default.
        }

        if (_autoselect_conditions.contains("applicable")) {
            return true;
        }

        for (String detail : details) {
            if (_autoselect_conditions.contains(detail)) {
                return true;
            }
        }
        return false;
    }
}
