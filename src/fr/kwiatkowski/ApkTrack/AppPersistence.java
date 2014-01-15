package fr.kwiatkowski.ApkTrack;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AppPersistence extends SQLiteOpenHelper
{
    public AppPersistence(Context context) {
        super(context, "apktrack.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String create_table = "CREATE TABLE apps (" +
                "name TEXT PRIMARY KEY," +
                "version TEXT," +
                "latest TEXT," +
                "last_check TEXT)";
        db.execSQL(create_table);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldver, int newver) {
        db.execSQL("DROP TABLE IF EXISTS apps");
        onCreate(db);
    }

    public void persistApp(InstalledApp app)
    {
        // TODO: Check if already exists


        ContentValues cv = new ContentValues();
        cv.put("name", app.getDisplayName());
        cv.put("version", app.getVersion());
        cv.put("latest", app.getLatestVersion());
        cv.put("lact_check", app.getLastCheckDate());

        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.insert("app", null, cv);
            db.close();
        }
        else {
            Log.v("ApkTrack", "Could not open database to save " + app.getDisplayName() + "!");
        }
    }
}
