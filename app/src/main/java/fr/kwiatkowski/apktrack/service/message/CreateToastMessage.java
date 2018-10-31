/*
 * Copyright (c) 2015
 *
 * app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with app.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.kwiatkowski.apktrack.service.message;

/**
 * This message is sent to tell the <code>AppDisplayFragment</code> that a
 * Toast should be created and displayed.
 * It was created because Toasts should not be created in IntentServices. This
 * mechanism makes sure that the Toast is only displayed if the activity is
 * running
 */
public class CreateToastMessage {
    private String _message;

    public CreateToastMessage(String message) {
        _message = message;
    }

    public String get_message() {
        return _message;
    }
}
