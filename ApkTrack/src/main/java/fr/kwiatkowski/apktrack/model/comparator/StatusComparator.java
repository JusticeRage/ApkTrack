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
import java.util.Comparator;

/**
 * Sorts applications based on their status (updated / not updated / error) and
 * then their alphabetical order.
 */
public class StatusComparator implements Comparator<InstalledApp>
{
    static AlphabeticalComparator alphabetical_comparator = new AlphabeticalComparator();

    @Override
    public int compare(InstalledApp a1, InstalledApp a2)
    {
        // a1 can be updated but not a2: a1 is "smaller" (goes on top in ascending order).
        if (a1.is_update_available() && !a2.is_update_available()) {
            return -1;
        }
        if (!a1.is_update_available() && a2.is_update_available()) {
            return 1;
        }

        // Both apps can be updated: sort alphabetically.
        if (a1.is_update_available() && a2.is_update_available()) {
            return alphabetical_comparator.compare(a1, a2);
        }

        // Last case left: no update available for a1 and a2. Sort based on error status.
        // Apps with errors go to the bottom (= are deemed "bigger").
        if (!a1.is_last_ckeck_error() && a2.is_last_ckeck_error()) {
            return -1;
        }
        if (a1.is_last_ckeck_error() && !a2.is_last_ckeck_error()) {
            return 1;
        }
        // Both have the same status: sort alphabetically.
        return alphabetical_comparator.compare(a1, a2);
    }
}
