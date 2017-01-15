-dontobfuscate

-dontwarn javax.annotation**
-dontwarn com.google.j2objc.annotations**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep class com.commonsware.cwac.wakeful.AlarmReceiver
-keep class fr.kwiatkowski.apktrack.model.InstalledApp
-keep class fr.kwiatkowski.apktrack.model.AppIcon
-keepclassmembers class ** {
    public void onEvent*(**);
}