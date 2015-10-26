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

package fr.kwiatkowski.apktrack.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.kwiatkowski.apktrack.R;
import fr.kwiatkowski.apktrack.model.AppIcon;
import fr.kwiatkowski.apktrack.model.InstalledApp;
import fr.kwiatkowski.apktrack.service.EventBusHelper;
import fr.kwiatkowski.apktrack.service.message.ModelModifiedMessage;
import fr.kwiatkowski.apktrack.service.WebScraperService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AppViewHolder extends    RecyclerView.ViewHolder
                           implements View.OnClickListener,
                                      View.OnLongClickListener
{
    // --------------------------------------------------------------------------------------------

    /**
     * Keep a copy of the package name so the app can be identified in case
     * a user gesture is detected.
     */
    private String _package_name;

    // --------------------------------------------------------------------------------------------

    AppViewHolder(View v)
    {
        super(v);
        _app_name = (TextView)v.findViewById(R.id.app_name);
        _app_version = (TextView)v.findViewById(R.id.app_version);
        _app_icon = (ImageView)v.findViewById(R.id.app_icon);
        _check_date = (TextView)v.findViewById(R.id.last_check);
        _action_icon = (ImageView)v.findViewById(R.id.action_icon);
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);

        // Adjust the ripple position on Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                public boolean onTouch(View v, MotionEvent event)
                {
                    v.findViewById(R.id.list_item)
                     .getBackground()
                     .setHotspot(event.getX(), event.getY());
                    return false;
                }
            });
        }

        // Keep a copy of the default text color, because apparently there is no API for this.
        if (_default_color == null) {
            _default_color = _app_name.getTextColors();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Binds an application to the view holder. All the app data will be converted visually to
     * be displayed inside the view.
     * @param app The app to bind.
     * @param ctx The context of the application.
     */
    public void bind_app(InstalledApp app, Context ctx)
    {
        _set_name(app);
        _set_version(app, ctx);
        _set_date(app, ctx);
        _set_icon(app, ctx);
        _set_action_icon(app, ctx);
        _package_name = app.get_package_name();
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void onClick(View v)
    {
        InstalledApp app = InstalledApp.find_app(_package_name);
        if (app == null) {
            return;
        }

        // Display spinner
        app.set_currently_checking(true);
        app.save();
        EventBusHelper.post_sticky(ModelModifiedMessage.event_type.APP_UPDATED, app.get_package_name());

        // Launch an update check
        Intent i = new Intent(v.getContext(), WebScraperService.class);
        i.putExtra(WebScraperService.TARGET_APP_PARAMETER, _package_name);
        i.putExtra(WebScraperService.SOURCE_PARAMETER, AppDisplayFragment.APP_DISPLAY_FRAGMENT_SOURCE);
        v.getContext().startService(i);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public boolean onLongClick(View v)
    {
        UpdateSourceChooser.show_dialog(InstalledApp.find_app(_package_name), v.getContext());
        return true; // Don't trigger onClick as well
    }

    // --------------------------------------------------------------------------------------------

    public String get_package_name() { return _package_name; }

    // --------------------------------------------------------------------------------------------

    /**
     * This function sets the name of the application to display.
     * If a display name exists, use it - otherwise use the package name.
     *
     * @param app The app to display
     */
    private void _set_name(InstalledApp app)
    {
        String appname = app.get_display_name();
        if (appname != null) {
            _app_name.setText(appname);
        }
        else {
            _app_name.setText(app.get_package_name());
        }
    }

    // --------------------------------------------------------------------------------------------

    private void _set_version(InstalledApp app, Context ctx)
    {
        if (app.is_last_ckeck_error())
        {
            String text = app.get_version();
            if (app.get_error_message() != null) {
                text += " (" + app.get_error_message() + ")";
            }
            else if (text != null && app.get_latest_version() != null) {
                text += " (" + app.get_latest_version() + ")";
            }
            _app_version.setText(text);
            _app_version.setTextColor(Color.GRAY);
            return;
        }

        if (app.get_latest_version() == null)
        {
            _app_version.setText(app.get_version());
            _app_version.setTextColor(_default_color);
            return;
        }

        if (!app.is_update_available())
        {
            // App is more recent than the latest version found
            if (app.get_version() != null && !app.get_version().equals(app.get_latest_version())) {
                _app_version.setText(String.format("%s (> %s)", app.get_version(), app.get_latest_version()));
            }
            else {
                _app_version.setText(app.get_version());
            }
            _app_version.setTextColor(Color.GREEN);
        }
        else // App is outdated
        {
            _app_version.setText(String.format("%s (%s %s)",
                    app.get_version(),
                    ctx.getResources().getString(R.string.current),
                    app.get_latest_version()));
            _app_version.setTextColor(Color.RED);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Sets the last check date of the application and its update source on the third line.
     * @param app The app whose information is to be displayed.
     * @param ctx The context of the application
     */
    private void _set_date(InstalledApp app, Context ctx)
    {
        String update_source = app.get_update_source();
        if (app.get_last_check_date() == null)
        {
            _check_date.setText(String.format(update_source == null ? "%s %s." : "[" + update_source + "] %s %s.",
                    ctx.getResources().getString(R.string.last_check),
                    ctx.getResources().getString(R.string.never)));
            _check_date.setTextColor(Color.GRAY);
        }
        else
        {
            DateFormat sdf = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            _check_date.setText(String.format(update_source == null ? "%s %s." : "[" + update_source + "] %s %s.",
                    ctx.getResources().getString(R.string.last_check),
                    sdf.format(app.get_last_check_date())));
            _check_date.setTextColor(_default_color);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Updates the ImageView with the app's icon. The retrieval of the icon is performed in an
     * AsyncTask to prevent from freezing the UI while the image data is retrieved from the
     * database.
     * @param app The app whose icon we want to retrieve.
     * @param ctx The context of the application.
     */
    private void _set_icon(final InstalledApp app, Context ctx) {
        new IconSetter(ctx, app, _app_icon).execute();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Updates the ImageView at the right of the app information. It may be invisible, contain a
     * spinner to indicate that a check is in progress, or a download icon.
     * @param app The app to display.
     * @param ctx The context of the application.
     */
    private void _set_action_icon(final InstalledApp app, final Context ctx)
    {
        if (app.is_currently_checking())
        {
            _action_icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_popup_sync));
            _action_icon.setVisibility(View.VISIBLE);
            ((Animatable) _action_icon.getDrawable()).start();
            if (_action_icon.hasOnClickListeners()) {
                _action_icon.setOnClickListener(null);
            }
        }
        else if (app.is_update_available())
        {
            if (app.get_download_url() != null) {
                _action_icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_download));
            }
            else {
                _action_icon.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_btn_search));
            }

            // User clicks open the download or search for an APK.
            _action_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    Uri uri;
                    if (app.get_download_url() != null)
                    {
                        uri = Uri.parse(String.format(app.get_download_url(),
                                                      app.get_display_name(),
                                                      app.get_latest_version()));
                    }
                    else {
                        uri = Uri.parse(
                                String.format(
                                    PreferenceManager.getDefaultSharedPreferences(ctx).
                                        getString(SettingsFragment.KEY_PREF_SEARCH_ENGINE,
                                                ctx.getString(R.string.search_engine_default)),
                                    app.get_display_name(),
                                    app.get_latest_version(),
                                    app.get_package_name()));
                    }
                    ctx.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            });

            _action_icon.setVisibility(View.VISIBLE);
        }
        else
        {
            if (_action_icon.hasOnClickListeners()) {
                _action_icon.setOnClickListener(null);
            }
            _action_icon.setVisibility(View.INVISIBLE);
        }
    }

    // --------------------------------------------------------------------------------------------

    private TextView _app_name;
    private TextView _app_version;
    private TextView _check_date;
    private ImageView _app_icon;
    private ImageView _action_icon;
    private ColorStateList _default_color = null;

    // --------------------------------------------------------------------------------------------
    // Nested class: IconSetter
    // --------------------------------------------------------------------------------------------

    /**
     * This class is used to set an app's icon, but the work is performed in a separate thread.
     * Icons are retrieved from the database and doing this inside the UI thread causes lags
     * when the user scrolls fast.
     */
    public static class IconSetter extends AsyncTask<Void, Integer, Drawable>
    {
        public IconSetter(Context ctx, InstalledApp app, ImageView view)
        {
            _ctx = ctx;
            _app = app;
            _view = view;
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            return AppIcon.get_icon(_app, _ctx);
        }

        @Override
        protected void onPostExecute(Drawable icon) {
            _view.setImageDrawable(icon);
        }

        private Context _ctx;
        private InstalledApp _app;
        private ImageView _view;
    }
}