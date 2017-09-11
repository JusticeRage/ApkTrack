/*
 * Copyright (c) 2016
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
import org.acra.security.BaseKeyStoreFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class used to generate a KeyStore that ACRA can use to veryfy apktrack.kwiatkowski.fr's SSL certificate.
 */
public class KeyStoreFactory extends BaseKeyStoreFactory
{
    @Override
    protected InputStream getInputStream(@NonNull Context context) {
        try {
            return context.getAssets().open("apktrack.store");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected char[] getPassword() {
        return "ApkTrackSSL".toCharArray();
    }

    @Override
    protected BaseKeyStoreFactory.Type getStreamType() {
        return Type.KEYSTORE;
    }
}
