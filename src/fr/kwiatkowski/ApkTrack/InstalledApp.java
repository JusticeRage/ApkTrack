package fr.kwiatkowski.ApkTrack;

import android.graphics.drawable.Drawable;

public class InstalledApp implements Comparable<InstalledApp>
{
    private String package_name;
    private String display_name;
    private String version;
    private String latest_version = null;
    private Drawable icon;

    private String last_check_date;

    public InstalledApp(String package_name, String version, String display_name, Drawable icon)
    {
        this.package_name = package_name;
        this.version = version;
        this.display_name = display_name;
        this.icon = icon;
    }

    public String getPackageName() {
        return package_name;
    }

    public void setPackageName(String package_name) {
        this.package_name = package_name;
    }

    public String getDisplayName() {
        return display_name;
    }

    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setLatestVersion(String latest_version) {
        this.latest_version = latest_version;
    }

    public String getLatestVersion() {
        return latest_version;
    }

    public String getLastCheckDate() {
        return last_check_date;
    }

    public void setLastCheckDate(String last_check_date) {
        this.last_check_date = last_check_date;
    }

    @Override
    public int compareTo(InstalledApp installedApp) {
        return display_name.compareTo(installedApp.display_name);
    }
}
