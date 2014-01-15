/*
 * Copyright (c) 2014
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

package fr.kwiatkowski.ApkTrack;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AppPersistence extends SQLiteOpenHelper
{
    private Resources rsrc;

    public AppPersistence(Context context, Resources rsrc)
    {
        super(context, "apktrack.db", null, 2);
        this.rsrc = rsrc;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String create_table = "CREATE TABLE apps (" +
                "package_name TEXT PRIMARY KEY," +
                "name TEXT," +
                "version TEXT," +
                "latest_version TEXT," +
                "last_check TEXT," +
                "last_check_error INTEGER," +
                "icon BLOB)";
        db.execSQL(create_table);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldver, int newver) {
        db.execSQL("DROP TABLE IF EXISTS apps");
        onCreate(db);
    }

    /**
     * Stores the application data into ApkTrack's SQLite database, so it can be
     * reloaded the next time ApkTrack is launched.
     * @param app The application whose data we want to save.
     */
    public synchronized void persistApp(InstalledApp app)
    {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            SQLiteStatement prepared_statement = db.compileStatement("INSERT OR REPLACE INTO apps " +
                    "(package_name, name, version, latest_version, last_check, last_check_error, icon) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)");
            nullable_bind(
                    new String[]
                            {
                                    app.getPackageName(),
                                    app.getDisplayName(),
                                    app.getVersion(),
                                    app.getLatestVersion(),
                                    app.getLastCheckDate(),
                            },
                    prepared_statement);
            prepared_statement.bindLong(6, app.isLastCheckError() ? 1 : 0);

            // Cache the application icon as well: loading it from the package manager is extremely slow.
            Bitmap bmp = ((BitmapDrawable) app.getIcon()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            prepared_statement.bindBlob(7, baos.toByteArray());
                    
            prepared_statement.execute();
        }
        else {
            Log.v("ApkTrack", "Could not open database to save " + app.getDisplayName() + "!");
        }
    }

    /**
     * Returns all the applications stored in the database.
     * @return A list containing an InstalledApp object for each savec application.
     */
    public synchronized List<InstalledApp> getStoredApps()
    {
        ArrayList<InstalledApp> res = new ArrayList<InstalledApp>();
        SQLiteDatabase db = getWritableDatabase();
        if (db == null) {
            return res;
        }
        Cursor c = db.rawQuery( "SELECT * FROM apps;", null);
        if (c.moveToFirst())
        {
            do {
                InstalledApp app = new InstalledApp(c.getString(0),
                                                    c.getString(2),
                                                    c.getString(1),
                                                    null);
                app.setLatestVersion(c.getString(3));
                app.setLastCheckDate(c.getString(4));
                app.setLastCheckError(c.getLong(5) == 1);

                // Reload icon
                byte[] raw = c.getBlob(6);
                Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                BitmapDrawable icon = new BitmapDrawable(rsrc, bmp);
                app.setIcon(icon);

                res.add(app);
            } while (c.moveToNext());
        }
        return res;
    }

    /**
     * Helper function used to bind values to prepared statements. Its advantage over
     * SQLiteStatement.bindAllArgsAsStrings is that null strings can be passed as well.
     * @param args The strings to bind into the statement
     * @param p The prepared statement to bind.
     */
    private void nullable_bind(String[] args, SQLiteStatement p)
    {
        for (int i = 0 ; i < args.length ; ++i)
        {
            if (args[i] != null) {
                p.bindString(i + 1, args[i]);
            }
            else {
                p.bindNull(i + 1);
            }
        }
    }
}
