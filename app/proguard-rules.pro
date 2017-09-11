-dontobfuscate

-dontwarn javax.annotation**
-dontwarn com.google.j2objc.annotations**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.inject.**

-keep class com.commonsware.cwac.wakeful.AlarmReceiver
-keep class fr.kwiatkowski.apktrack.model.InstalledApp
-keep class fr.kwiatkowski.apktrack.model.AppIcon
-keep class android.support.v7.widget.SearchView { *; }
-keep public class * extends android.support.v7.preference.Preference
-keepclassmembers class ** {
    public void onEvent*(**);
}