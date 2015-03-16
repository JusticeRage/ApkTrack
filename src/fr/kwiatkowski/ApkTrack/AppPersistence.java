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
    private Resources rsrc = null;
    private static AppPersistence singleton = null;

    public static AppPersistence getInstance(Context context)
    {
        if (singleton == null) {
            singleton = new AppPersistence(context);
        }
        return singleton;
    }

    private AppPersistence(Context context)
    {
        super(context, "apktrack.db", null, 2);
        try {
            rsrc = context.getResources();
        }
        catch (Exception ignored) {}
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // Since v3 of the schema
        createSourcesTable(db);

        createAppsTable(db);
    }

    private void createSourcesTable(SQLiteDatabase db)
    {
        Log.v(MainActivity.TAG, "Creating sources database...");
        String create_table = "CREATE TABLE sources (" +
                "name TEXT PRIMARY KEY," +
                "version_check_url TEXT," +
                "version_check_regexp TEXT," +
                "download_url TEXT," +
                "applicable_packages TEXT NOT NULL  DEFAULT \".*\")";
        db.execSQL(create_table);
    }

    private void createAppsTable(SQLiteDatabase db)
    {
        Log.v(MainActivity.TAG, "Creating apps database...");
        String create_table = "CREATE TABLE apps (" +
                "package_name TEXT PRIMARY KEY," +
                "name TEXT," +
                "version TEXT," +
                "latest_version TEXT," +
                "last_check TEXT," +
                "last_check_error INTEGER," +
                "system_app INTEGER," +
                "icon BLOB," +
                "source_name TEXT," +
                "FOREIGN KEY(source_name) REFERENCES sources(name))";
        db.execSQL(create_table);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldver, int newver)
    {
        Log.v(MainActivity.TAG, "Upgrading database from version " + oldver + " to " + newver + " required.");
        if (oldver < 3)
        {
            createSourcesTable(db);

            String backup_apps = "BEGIN TRANSACTION; " +
                    "CREATE TEMPORARY TABLE apps_backup(package_name TEXT, name TEXT, " +
                    "version TEXT, latest_version TEXT, last_check TEXT, last_check_error INTEGER," +
                    "system_app INTEGER, icon BLOB); " +
                    "INSERT INTO apps_backup SELECT * from apps; " +
                    "DROP TABLE apps;";
            db.execSQL(backup_apps);

            // Recreate the apps database
            createAppsTable(db);

            // Put the data back and delete the temporary table
            String copy_apps = "INSERT INTO apps SELECT * FROM apps_backup; " +
                    "DROP TABLE apps_backup;" +
                    "COMMIT;";
            db.execSQL(copy_apps);
        }
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
            Log.v(MainActivity.TAG, "Could not open database to save " + app.getDisplayName() + "!");
        }
    }

    /**
     * Restores the icon inside an InstalledApp object. The icon is fetched in the database.
     *
     * @param   app     The application whose icon should be restored.
     */
    public synchronized void restoreIcon(InstalledApp app)
    {
        SQLiteDatabase db = getReadableDatabase();
        if (db == null) {
            return;
        }
        Cursor c = db.rawQuery( "SELECT icon FROM apps WHERE package_name = ?;", new String[]{ app.getPackageName() });
        if (c.moveToFirst()) { // False if the cursor is empty
            app.setIcon(makeDrawable(c.getBlob(0)));
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
        app.setIcon(makeDrawable(c.getBlob(7)));
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
     * Stores an update source in the database. Existing sources are updated.
     * @param name The name of the source.
     * @param version_check_url The URL used to check versions.
     * @param version_check_regexp The regular expressions used to detect the latest version on the page.
     * @param download_url An optional link to the updated APK.
     * @param applicable_packages Packages which can
     */
    public synchronized void persistSource(String name,
                                           String version_check_url,
                                           String version_check_regexp,
                                           String download_url,
                                           String applicable_packages)
    {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<Object> bind_args = new ArrayList<Object>();
        bind_args.add(name);
        bind_args.add(version_check_url);
        bind_args.add(version_check_regexp);
        bind_args.add(download_url);
        bind_args.add(applicable_packages);
        String request = "INSERT OR REPLACE INTO sources " +
                "(name, version_check_url, version_check_regexp, download_url, applicable_packages)" +
                " VALUES (?, ?, ?, ?, ?)";
        SQLiteStatement prepared_statement = db.compileStatement(request);
        nullable_bind(bind_args, prepared_statement);
        prepared_statement.execute();
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

    /**
     * Helper function which creates a BitmapDrawable from a byte array.
     * @param raw The raw Bitmap data
     * @return A BitmapDrawable object based on the input data.
     */
    private BitmapDrawable makeDrawable(byte[] raw)
    {
        if (raw != null && rsrc != null)
        {
            Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            return new BitmapDrawable(rsrc, bmp);
        }
        return null;
    }
}
