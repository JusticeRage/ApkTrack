# ApkTrack

ApkTrack is a simple Android application which periodically checks if your installed apps can be updated.

It was created for users who don't use the Google Play Store, but still need to know when new APKs are available for their apps. ApkTrack performs simple website scraping to grab the latest versions of packages present on the device.

This application is distributed under the terms of the [GPL v3 License](https://www.gnu.org/licenses/gpl.html).

-------------------------------

## Usage

![ApkTrack screenshot](http://img11.hostingpics.net/pics/161407screenshot.png)

* Click on an application to perform a manual version check.
* The buttons at the top are used to respectively search the application list and perform a version check for all applications.
* The last button displays additional options, such as sorting. The settings are in there as well.
* Apps marked in red are outdated. Click the magnifying glass icon to search for the latest APK with your favorite search engine. 

That's it!

## Things to keep in mind

* Applications are *not* updated automatically. ApkTrack may provide a direct link to the latest APK when available, but in the general case, users are expected to find the APK themselves. 
* ApkTrack uses regular expressions to scrape webpages, so it may cease to work without notice if the target websites are modified.
* Although there is a background service checking for updates every day, it may get killed by the OS. Remember to check for updates manually in the application from time to time.

-------------------------------

### Download
A precompiled version of the application can be found here: [ApkTrack 2.1.1](http://apktrack.kwiatkowski.fr/apk/ApkTrack.apk).
If you want to help me test ApkTrack, feel free to use the [beta version](http://apktrack.kwiatkowski.fr/apk/ApkTrack_beta.apk). More features are implemented, but bugs may occur! Be sure to report them!

### Translate
You can help translate ApkTrack on [OneSky](https://apktrack.oneskyapp.com/)! Get in touch with me if you need a new language set up. 

### Build ApkTrack
ApkTrack's build system has been switched to Gradle in order to make it easier for contributors to compile the project. Use the following commands to generate the APK:

```
git clone https://github.com/JusticeRage/ApkTrack.git
cd ApkTrack
./gradlew build
ls -l app/build/outputs/apk/
```

![Travis](https://travis-ci.org/JusticeRage/ApkTrack.svg?branch=beta)

### Donations
ApkTrack is completely free, and I don't expect any kind of compensation for using this application. I do like Bitcoins though, so if you want to send some my way, here's an address you can use: ```19wFVDUWhrjRe3rPCsokhcf1w9Stj3Sr6K```  
If you don't have bitcoins but still want to show your appreciation, please consider donating to either [La Quadrature du Net](https://support.laquadrature.net/) or the [EFF](https://supporters.eff.org/donate/)!   
Be sure to drop me a line if you choose to donate in any way, so I can thank you personally and add you to the list below!

#### Generous donators
- Zongo Saiba

### Contact
[![](https://manalyzer.org/static/mail.png)](mailto:justicerage *at* manalyzer.org)
[![](https://manalyzer.org/static/twitter.png)](https://twitter.com/JusticeRage)
[![](https://manalyzer.org/static/gpg.png)](https://pgp.mit.edu/pks/lookup?op=vindex&search=0x40E9F0A8F5EA8754)