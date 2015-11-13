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

package fr.kwiatkowski.apktrack.service.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import fr.kwiatkowski.apktrack.MainActivity;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;

public class SSLHelper
{
    private static KeyStore _keystore = null;
    private static SSLContext _ssl_context = null;

    /**
     * Obtains the keystore containing the app's trusted certificates from the assets.
     * @param context The context of the application.
     * @return A loaded keystore containing the SSL certificates with which the app
     * interacts, or <code>null</code> if there was a problem reading them from the
     * assets.
     */
    public static KeyStore get_keystore(@NonNull Context context)
    {
        if (_keystore != null) {
            return _keystore;
        }

        try
        {
            InputStream kis = context.getAssets().open("apktrack.store");
            _keystore = KeyStore.getInstance("BKS");
            _keystore.load(kis, "ApkTrackSSL".toCharArray());
            return _keystore;
        }
        catch (IOException e) {
            Log.e(MainActivity.TAG, "[SSLHelper.get_keystore] Could not open apktrack.store!", e);
        }
        catch (KeyStoreException e) {
            Log.e(MainActivity.TAG, "[SSLHelper.get_keystore] Could not create a KeyStore!", e);
        }
        catch (GeneralSecurityException e) {
            Log.e(MainActivity.TAG, "[SSLHelper.get_keystore] Error while processing apktrack.store contents!", e);
        }
        _keystore = null;
        return null;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Creates an SSLSocketFactory to be used with <code>HttpsUrlConnection</code>. The object is
     * preloaded with ApkTrack's bundled SSL certificates, which allows the app to perform strict
     * server authentication and prevent man in the middle attacks.
     * @param context The context of the application.
     * @return An SSLSocketFactory to use for SSL connections to ApkTracks known servers, or
     * <code>null</code> if it could not be created.
     */
    public static SSLSocketFactory get_ssl_socket_factory(Context context)
    {
        if (_ssl_context != null) {
            return _ssl_context.getSocketFactory();
        }

        KeyStore keystore = get_keystore(context);
        if (keystore == null) {
            return null;
        }

        try
        {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keystore);
            _ssl_context = SSLContext.getInstance("TLS");
            _ssl_context.init(null, tmf.getTrustManagers(), null);
            return _ssl_context.getSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            Log.e(MainActivity.TAG, "[SSLHelper.get_ssl_socket_factory] Could not create " +
                    "the SSLContext.", e);
        }
        _ssl_context = null;
        return null;
    }
}
