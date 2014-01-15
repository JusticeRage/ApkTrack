package fr.kwiatkowski.ApkTrack;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppAdapter extends BaseAdapter
{
    private static Pattern test_version_pattern;
    static {
        test_version_pattern = Pattern.compile("[0-9.]+");
    }

    private List<InstalledApp> data;
    private Context ctx;
    private ColorStateList default_color = null;

    public AppAdapter(Context ctx, List<InstalledApp> objects)
    {
        super();
        this.data = objects;
        this.ctx = ctx;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public InstalledApp getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.list_item, parent, false);
        }
        InstalledApp app = data.get(position);

        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView version = (TextView) convertView.findViewById(R.id.version);

        if (default_color == null) {
            default_color = name.getTextColors();
        }

        // Set application name
        String appname = app.getDisplayName();
        if (appname != null) {
            name.setText(appname);
        }
        else {
            name.setText(app.getPackageName());
        }

        // Set version. Check whether the application is up to date.
        String latest_version = app.getLatestVersion();
        if (latest_version != null)
        {
            if (app.getVersion().equals(latest_version))
            {
                version.setText(app.getVersion());
                Log.d("ApkTrack", "Setting greentext for " + app.getDisplayName() + " - " + version.getText());
                version.setTextColor(Color.GREEN);
            }
            else
            {
                if (!test_version(latest_version))
                {
                    version.setText(app.getVersion() + " (" + latest_version + ")");
                    version.setTextColor(Color.GRAY);
                }
                else
                {
                    version.setText(app.getVersion() + " (Current: " + latest_version + ")");
                    version.setTextColor(Color.RED);
                }
            }
        }
        else {
            version.setText(app.getVersion());
            version.setTextColor(default_color);
        }

        if (app.getIcon() != null)
        {
            ImageView i = (ImageView) convertView.findViewById(R.id.img);
            i.setImageDrawable(app.getIcon());
        }

        return convertView;
    }

    /**
     * Checks whether the latest version for a program is a valid version number or an error message.
     * Actual version numbers are supposed to be a combination of numbers and dots.
     *
     * Note that this is only a syntaxic check, and by no means a semantic one.
     *
     * @param v The version to check.
     */
    private boolean test_version(String v)
    {
        Matcher m = test_version_pattern.matcher(v);
        return m.matches();
    }


}
