# ApkTrack

ApkTrack is a simple Android application which periodically checks if your installed apps can be updated.

It was created for users who don't use the Google Play Store, but still need to know when new APKs are available for their apps. ApkTrack performs simple website scraping to grab the latest versions of packages present on the device.

This application is distributed under the terms of the [GPL v3 License](https://www.gnu.org/licenses/gpl.html).

-------------------------------

## Usage

![ApkTrack screenshot](http://img11.hostingpics.net/pics/352620screenshot.png)

* Click on an application to perform a manual version check.
* The buttons at the top are used to respectively search the application list and perform a version check for all applications.
* The last button displays additional options, such as sorting. The settings are in there as well.
* Apps marked in red are outdated. Click the magnifying glass icon to search for the latest APK with your favorite search engine. 

That's it!

## Things to keep in mind

* Applications are *not* updated automatically. On some cases, ApkTrack may provide a direct link to the latest APK, but in the general case, users are expected to find the APK themselves. 
* Updates, installations and uninstallations are detected automatically by the application in most cases. When it fails, you can press the ![](http://img4.hostingpics.net/pics/230860icmenufind.png) button to refresh the installed apps.
* ApkTrack uses regular expressions to scrape webpages, so it may cease to work without notice if the target websites are modified.
* Although there is a background service checking for updates every day, it may get killed by the OS. Remember to check for updates manually in the application from time to time.

-------------------------------

### Download
A precompiled version of the application can be found here: [ApkTrack 1.1](http://kwiatkowski.fr/apktrack/ApkTrack.apk).  
If you want to help me test ApkTrack, feel free to use the [beta version](http://kwiatkowski.fr/apktrack/ApkTrack_beta.apk). More features are implemented, but bugs may occur! Be sure to report them!

### Translate
You can help translate ApkTrack in your language on [OneSky](https://apktrack.oneskyapp.com/)!

### Build ApkTrack
ApkTrack's build system has been switched to Gradle in order to make it easier for contributors to compile the project. Use the following commands to generate the APK:

```
git clone https://github.com/JusticeRage/ApkTrack.git
cd ApkTrack
gradle build
ls -l ./build/outputs/apk/
```

You may need to [install gradle](http://gradle.org/gradle-download/) first, and set up the `ANDROID_HOME` environment variable.

### Donations
ApkTrack is completely free, and I don't expect any kind of compensation for using this application. I do like Bitcoins though, so if you want to send some my way, here's an address you can use: ```19wFVDUWhrjRe3rPCsokhcf1w9Stj3Sr6K```  
Feel free to drop me a line if you donate to the project, so I can thank you personally!

### Contact
[![](http://img11.hostingpics.net/pics/871895mailbutton.png)](mailto:justicerage *at* manalyzer.org)
[![](http://img11.hostingpics.net/pics/637656twitterbutton.png)](https://twitter.com/JusticeRage)