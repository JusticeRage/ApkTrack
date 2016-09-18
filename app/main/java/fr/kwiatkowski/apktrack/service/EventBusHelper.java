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

import de.greenrobot.event.EventBus;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;
import fr.kwiatkowski.apktrack.service.message.StickyUpdatedMessage;

/**
 * This class contains boilerplate code which facilitates the handling of events in the application.
 */
public class EventBusHelper
{
    /**
     * This method is used to post a sticky event on the bus.
     * It adds the notion of processed events. If the latest event posted has been
     * processed by the reciever, it is replaced by a new one. Otherwise, the existing
     * event is updated to include the new information.
     *
     * This method only handles ModelModifiedMessage events.
     *
     * @param type The type of the event to post
     * @param package_name The package concerned by the change.
     */
    public static void post_sticky(ModelModifiedMessage.event_type type, String package_name)
    {
        ModelModifiedMessage existing = EventBus.getDefault().getStickyEvent(ModelModifiedMessage.class);
        if (existing != null)
        {
            try
            {
                existing.add_event(type, package_name);
                // Only happens when the Activity didn't already process the message.
                EventBus.getDefault().post(new StickyUpdatedMessage());
                return;
            }
            // The event was already processed. Post a new one instead.
            catch (ModelModifiedMessage.EventAlreadyProcessedException e)
            {
                EventBus.getDefault().postSticky(new ModelModifiedMessage(type, package_name));
                return;
            }
        }
        EventBus.getDefault().postSticky(new ModelModifiedMessage(type, package_name));
        // If the Activity is not around to catch the message, EventBus will create a NoSubscriberEvent
        // which will also be caught by the service which displays notifications. This is why there is
        // no need to check if the message was received and create a StickyUpdatedMessage.
    }
}

