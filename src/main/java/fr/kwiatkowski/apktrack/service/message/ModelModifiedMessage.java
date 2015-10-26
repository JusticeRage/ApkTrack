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

package fr.kwiatkowski.apktrack.service.message;

import android.util.Pair;
import fr.kwiatkowski.apktrack.service.WebScraperService;

import java.util.ArrayList;
import java.util.List;

/**
 * This message indicates that an app was modified.
 * The contents is a list of package names and types descibing the nature of the modification.
 * Ex: ((APP_UPDATED, fr.kwiatkowski.ApkTrack), (APP_REMOVED, com.google.stuff), ...)
 */
public class ModelModifiedMessage
{
    public enum event_type { APP_ADDED, APP_REMOVED, APP_UPDATED }

    private List<Pair<event_type, String>> _events = new ArrayList<Pair<event_type, String>>();
    private boolean _processed = false;

    // --------------------------------------------------------------------------------------------

    public ModelModifiedMessage(event_type type, String package_name)
    {
        Pair<event_type, String> p = new Pair<event_type, String>(type, package_name);
        _events.add(p);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Reading the list of events marks the event as processed.
     * This mechanism prevents sticky events from being updated while another thread is reading
     * from them.
     * @return The list of events detected by the sender.
     */
    public synchronized List<Pair<event_type, String>> get_events()
    throws EventAlreadyProcessedException
    {
        if (_processed) { // Don't allow events to be processed more than once.
            throw new EventAlreadyProcessedException();
        }
        _processed = true;
        return _events;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * This method allows classes from the service package to access the current events without
     * marking them as processed.
     * It allows the WebScraperService to display a notification to the user if the Activity is
     * not around to display the change.
     *
     * Sadly, this trick is required because the two classes are not located in the same package,
     * and Java has no concept of "friend" or sub-package.
     *
     * @param ignored A discarded item that is only here to make sure that only the
     *                WebScraperService is able to call this method.
     * @return The list of events detected by the sender.
     */
    public synchronized
    List<Pair<event_type, String>> access_events(WebScraperService.MessageAccessor ignored) {
        return _events;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Adds information to the message: if the event hasn't already been processed, new
     * modifications to the model may have occurred and can be appended to an existing sticky
     * message.
     * @param type The type of the event.
     * @param package_name The name of the package which was modified.
     * @throws EventAlreadyProcessedException Trying to update a message which was already
     * processed will cause this exception. In this case, a new ModelModifiedMessage should
     * be posted with this new event.
     */
    public synchronized void add_event(event_type type, String package_name)
            throws EventAlreadyProcessedException
    {
        if (_processed) {
            throw new EventAlreadyProcessedException();
        }
        _events.add(new Pair<event_type, String>(type, package_name));
    }

    // --------------------------------------------------------------------------------------------

    /**
     * A simple exception which is thrown when trying to update a ModelModifiedMessage which
     * has already been processed.
     */
    public class EventAlreadyProcessedException extends Exception {}
}