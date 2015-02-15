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
                "system_app INTEGER," +
                "icon BLOB)";
        db.execSQL(create_table);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldver, int newver) {
        db.execSQL("DROP TABLE IF EXISTS apps");
        onCreate(db);
    }

    public synchronized void insertApp(InstalledApp app)
    {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null)
        {
            String request;
            ArrayList<Object> bind_args = new ArrayList<Object>();
            bind_args.add(app.getPackageName());
            bind_args.add(app.getDisplayName());
            bind_args.add(app.getVersion());
            bind_args.add(app.getLatestVersion());
            bind_args.add(app.getLastCheckDate());
            bind_args.add(app.isLastCheckFatalError());
            bind_args.add(app.isSystemApp());

            if (app.getIcon() != null && app.getIcon() instanceof BitmapDrawable)
            {
                request = "INSERT OR REPLACE INTO apps " +
                    "(package_name, name, version, latest_version, last_check, last_check_error, system_app, icon) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                Bitmap bmp = ((BitmapDrawable) app.getIcon()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                bind_args.add(baos.toByteArray());
            }
            else {
                request = "INSERT OR REPLACE INTO apps " +
                        "(package_name, name, version, latest_version, last_check, last_check_error, system_app)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?)";
            }
            SQLiteStatement prepared_statement = db.compileStatement(request);
            nullable_bind(bind_args, prepared_statement);
            prepared_statement.execute();
        }
    }

    public synchronized void updateApp(InstalledApp app)
    {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null)
        {
            String request;
            ArrayList<Object> bind_args = new ArrayList<Object>();
            bind_args.add(app.getDisplayName());
            bind_args.add(app.getVersion());
            bind_args.add(app.getLatestVersion());
            bind_args.add(app.getLastCheckDate());
            bind_args.add(app.isLastCheckFatalError());
            bind_args.add(app.isSystemApp());

            if (app.getIcon() != null && app.getIcon() instanceof BitmapDrawable)
            {
                request = "UPDATE apps SET " +
                          "name = ?, version = ?, latest_version = ?, last_check = ?, last_check_error = ?, system_app = ?, icon = ? " +
                          "WHERE package_name = ?";
                Bitmap bmp = ((BitmapDrawable) app.getIcon()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                bind_args.add(baos.toByteArray());
            }
            else {
                request = "UPDATE apps SET " +
                          "name = ?, version = ?, latest_version = ?, last_check = ?, last_check_error = ?, system_app = ? " +
                          "WHERE package_name = ?";
            }

            bind_args.add(app.getPackageName());
            SQLiteStatement prepared_statement = db.compileStatement(request);
            nullable_bind(bind_args, prepared_statement);

            prepared_statement.execute();
        }
        else {
            Log.v("ApkTrack", "Could not open database to save " + app.getDisplayName() + "!");
        }
    }

    /**
     * Deletes an application from the database.
     * @param app The application which should be removed.
     */
    public synchronized void removeFromDatabase(InstalledApp app)
    {
        SQLiteDatabase db = getWritableDatabase();
        if (db != null)
        {
            SQLiteStatement prepared = db.compileStatement("DELETE FROM apps WHERE package_name = ?");
            prepared.bindString(1, app.getPackageName());
            prepared.execute();
        }
    }

    /**
     * Unserializes an InstalledApp stored inside the database.
     * @param c A cursor pointing to the app to unserialize
     * @return The unserialized app
     */
    public InstalledApp unserialize(Cursor c)
    {
        InstalledApp app = new InstalledApp(c.getString(0),
                c.getString(2),
                c.getString(1),
                c.getInt(6) == 1,
                null);
        app.setLatestVersion(c.getString(3));
        app.setLastCheckDate(c.getString(4));
        app.setLastCheckFatalError(c.getLong(5) == 1);

        // Reload icon
        byte[] raw = c.getBlob(7);
        if (raw != null && rsrc != null)
        {
            Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            BitmapDrawable icon = new BitmapDrawable(rsrc, bmp);
            app.setIcon(icon);
        }

        return app;
    }

    /**
     * Returns an application stored in the database.
     * @param package_name The name of the application to return.
     * @return An InstalledApp object representing the stored application.
     */
    public synchronized InstalledApp getStoredApp(String package_name)
    {
        SQLiteDatabase db = getReadableDatabase();
        if (db == null) {
            return null;
        }
        Cursor c = db.rawQuery( "SELECT * FROM apps WHERE package_name = ?;", new String[]{ package_name });
        if (!c.moveToFirst()) { // False if the cursor is empty
            return null;
        }
        else {
            return unserialize(c);
        }
    }

    /**
     * Returns all the applications stored in the database.
     * @return A list containing an InstalledApp object for each savec application.
     */
    public synchronized List<InstalledApp> getStoredApps()
    {
        ArrayList<InstalledApp> res = new ArrayList<InstalledApp>();
        SQLiteDatabase db = getReadableDatabase();
        if (db == null) {
            return res;
        }
        Cursor c = db.rawQuery( "SELECT * FROM apps;", null);
        if (c.moveToFirst())
        {
            do {
                InstalledApp app = unserialize(c);
                res.add(app);
            } while (c.moveToNext());
        }
        return res;
    }

    /**
     * Helper function used to bind values to prepared statements. Its advantage over
     * SQLiteStatement.bindAllArgsAsStrings is that null strings can be passed as well.
     * @param args The objects to bind into the statement
     * @param p The prepared statement to bind.
     */
    private void nullable_bind(ArrayList<Object> args, SQLiteStatement p)
    {
        for (int i = 0 ; i < args.size() ; ++i)
        {
            if (args.get(i) != null)
            {
                if (args.get(i) instanceof String)
                    p.bindString(i + 1, (String) args.get(i));
                else if (args.get(i) instanceof Boolean)
                    p.bindLong(i + 1, (Boolean) args.get(i) ? 1 : 0);
                else if (args.get(i) instanceof byte[])
                    p.bindBlob(i + 1, (byte[]) args.get(i));
                else
                    throw new UnsupportedOperationException("Please implement default binding for " + args.get(i).getClass());
            }
            else {
                p.bindNull(i + 1);
            }
        }
    }
}
