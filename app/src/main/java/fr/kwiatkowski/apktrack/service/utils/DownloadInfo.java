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

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;

/**
 * This class is used to retrieve the information regarding a downloaded file, based on
 * the download ID returned by the the Download Service during the .
 */
public class DownloadInfo
{
    public DownloadInfo(long download_id, Context ctx)
    {
        long id = download_id;
        DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
        if (!c.moveToFirst())
        {
            c.close();
            return;
        }

        _last_modified = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));
        _local_uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
        _status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
        _reason = c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON));

        c.close();
        _valid = true;
    }

    // --------------------------------------------------------------------------------------------

    public String   get_last_modified() { return _last_modified; }
    public String   get_local_uri()     { return _local_uri; }
    public int      get_status()        { return _status; }
    public String   get_reason()        { return _reason; }
    public boolean  is_valid()          { return _valid; }

    // --------------------------------------------------------------------------------------------

    private String  _last_modified;
    private String  _local_uri;
    private int     _status;
    private String  _reason;
    private boolean _valid = false;
}
