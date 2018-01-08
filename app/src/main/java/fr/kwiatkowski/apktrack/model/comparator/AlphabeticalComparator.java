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

import fr.kwiatkowski.apktrack.model.InstalledApp;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Sorts application in alphabetical order
 */
public class AlphabeticalComparator implements Comparator<InstalledApp>
{
    static Collator collator;

    static
    {
        collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.SECONDARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
    }

    public int compare(InstalledApp a1, InstalledApp a2)
    {
        return collator.compare(a1.get_display_name(), a2.get_display_name());
    }
}
