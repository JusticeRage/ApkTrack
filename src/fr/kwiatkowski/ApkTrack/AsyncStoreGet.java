package fr.kwiatkowski.ApkTrack;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncStoreGet extends AsyncTask<String, Void, String>
{
    private InstalledApp app;
    private Context ctx;
    private BaseAdapter la;

    private static Pattern find_version_pattern;

    static {
        find_version_pattern = Pattern.compile("itemprop=\"softwareVersion\">([^<]+?)</div>");
    }

    public AsyncStoreGet(InstalledApp app, Context ctx, AppAdapter la)
    {
        super();
        this.app = app;
        this.ctx = ctx;
        this.la = la;
    }

    @Override
    protected String doInBackground(String... package_names)
    {
        String package_name = package_names[0];
        try
        {
            InputStream conn = new URL("https://play.google.com/store/apps/details?id=" + package_name).openStream();
            return Misc.readAll(conn, 2048);
        }
        catch (FileNotFoundException e)
        {
            return "Error: Not a Play Store application.";
        }
        catch (Exception e)
        {
            String err = "https://play.google.com/store/apps/details?id=" + package_name + " could not be retrieved! (" +
                    e.getMessage() + ")";
            Log.e("ApkTrack", err);
            e.printStackTrace();

            return "Error:Could not get open Play Store page! (" + e.getMessage() + ")";
        }
    }

    @Override
    protected void onPostExecute(String s)
    {
        if (!s.startsWith("Error:"))
        {
            Log.v("ApkTrack", s);
            Matcher m = find_version_pattern.matcher(s);
            if (m.find())
            {
                String version = m.group(1).trim();
                app.setLatestVersion(version);
                app.setLastCheckDate(DateFormat.getDateTimeInstance().format(new Date(0)));
                if (la != null) {
                    la.notifyDataSetChanged();
                }
            }
            else
            {
                Toast toast = Toast.makeText(ctx, "Version not found.", Toast.LENGTH_LONG);
                toast.show();
            }
        }
        else {
            s = s.substring(6);
            Toast toast = Toast.makeText(ctx, s, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
