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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AppAdapter extends BaseAdapter
{
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

        View app_info = convertView.findViewById(R.id.app_info);
        TextView name = (TextView) app_info.findViewById(R.id.name);
        TextView version = (TextView) app_info.findViewById(R.id.version);
        TextView date = (TextView) app_info.findViewById(R.id.date);
        ImageView loader = (ImageView) convertView.findViewById(R.id.loader);

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

        // Display the loader if we're currently checking for updates for that application
        if (app.isCurrentlyChecking()) {
            loader.setVisibility(View.VISIBLE);
        }
        else {
            loader.setVisibility(View.INVISIBLE);
        }

        // Set version. Check whether the application is up to date.
        String latest_version = app.getLatestVersion();
        if (app.isLastCheckError())
        {
            version.setText(app.getVersion() + " (" + latest_version + ")");
            version.setTextColor(Color.GRAY);
        }
        else if (latest_version != null)
        {
            if (app.getVersion().equals(latest_version))
            {
                version.setText(app.getVersion());
                version.setTextColor(Color.GREEN);
            }
            else
            {
                version.setText(app.getVersion() + " (Current: " + latest_version + ")");
                version.setTextColor(Color.RED);
            }
        }
        else {
            version.setText(app.getVersion());
            version.setTextColor(default_color);
        }

        // Set last check date
        String last_check_date = app.getLastCheckDate();
        if (last_check_date == null)
        {
            date.setText("Last check: never.");
            date.setTextColor(Color.GRAY);
        }
        else
        {
            SimpleDateFormat sdf = new SimpleDateFormat();
            date.setText("Last check: " + sdf.format(new Date(Long.parseLong(last_check_date) * 1000)));
            date.setTextColor(default_color);
        }

        if (app.getIcon() != null)
        {
            ImageView i = (ImageView) convertView.findViewById(R.id.img);
            i.setImageDrawable(app.getIcon());
        }

        return convertView;
    }

}
