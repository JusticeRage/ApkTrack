package fr.kwiatkowski.apktrack;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * All this activity does is extend <code>com.orm.SugarApp</code> to set up the persistence layer
 * and set up ACRA crash reporting.
 */
@ReportsCrashes(httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://apktrack.kwiatkowski.fr:5984/acra-apktrack/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "acra-apktrack",
        formUriBasicAuthPassword = "$ecureP@ssw0rd-random-stuff-acid-senate",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogEmailPrompt = R.string.crash_user_email_label,
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class MainApplication extends com.orm.SugarApp
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        ACRA.init(this);
    }
}
