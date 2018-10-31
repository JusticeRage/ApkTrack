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

package fr.kwiatkowski.apktrack.model.comparator;

import android.content.pm.PackageInfo;

import java.util.Comparator;

public class PackageInfoComparator implements Comparator<PackageInfo> {
    public int compare(PackageInfo p1, PackageInfo p2) {
        return AlphabeticalComparator.collator.compare(p1.packageName, p2.packageName);
    }
}
