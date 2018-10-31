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

/**
 * Represents an entry in the update source.
 * It describes regular expressions which correspond to a particular package
 * or set of packages.
 */
public class UpdateSourceEntry {
    private String _applicable_packages;
    private String _version_regexp;
    private String _download_url;
    private String _download_regexp;
    private String _changelog_regexp;

    public UpdateSourceEntry(String applicable_packages) {
        _applicable_packages = applicable_packages;
    }

    public String get_applicable_packages() {
        return _applicable_packages;
    }

    public String get_version_regexp() {
        return _version_regexp;
    }

    public void set_version_regexp(String version_regexp) {
        this._version_regexp = version_regexp;
    }

    public String get_download_url() {
        return _download_url;
    }

    public void set_download_url(String download_url) {
        this._download_url = download_url;
    }

    public String get_download_regexp() {
        return _download_regexp;
    }

    public void set_download_regexp(String download_regexp) {
        this._download_regexp = download_regexp;
    }

    public String get_changelog_regexp() {
        return _changelog_regexp;
    }

    public void set_changelog_regexp(String changelog_regexp) {
        this._changelog_regexp = changelog_regexp;
    }
}