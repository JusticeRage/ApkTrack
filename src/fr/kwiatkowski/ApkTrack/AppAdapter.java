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

        if (app.getIcon() != null)
        {
            ImageView i = (ImageView) convertView.findViewById(R.id.img);
            i.setImageDrawable(app.getIcon());
        }

        return convertView;
    }

}
