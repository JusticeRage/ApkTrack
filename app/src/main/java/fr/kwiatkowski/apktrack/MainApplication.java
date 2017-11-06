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

package fr.kwiatkowski.apktrack;

import android.content.Context;
import com.squareup.leakcanary.LeakCanary;
import fr.kwiatkowski.apktrack.service.utils.KeyStoreFactory;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * All this activity does is extend <code>com.orm.SugarApp</code> to set up the persistence layer
 * and set up ACRA crash reporting.
 */
@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://apktrack.kwiatkowski.fr/crashes/acra-apktrack",
        mode = ReportingInteractionMode.DIALOG,
        keyStoreFactoryClass = KeyStoreFactory.class,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = R.drawable.bug_report,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogEmailPrompt = R.string.crash_user_email_label,
        resDialogOkToast = R.string.crash_dialog_ok_toast,
        buildConfigClass = BuildConfig.class)
public class MainApplication extends com.orm.SugarApp
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        ACRA.init(this);
    }
}
