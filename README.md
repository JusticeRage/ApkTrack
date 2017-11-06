# ApkTrack

#### Check for updates on PlayStore and other sources

ApkTrack checks, if updates for installed apps are available.

It was created for users, who do not want use Google PlayStore, but still would like to be informed when new versions of their installed applications are available.  ApkTrack performs simple website scraping to obtain the latest version information of APKs present on a device.  It can query F-Droid, PlayStore, Xposed, plus many other sources of APKs via the ApkTrack Proxy.

This application is distributed under the terms of the [GPL v3 License](https://www.gnu.org/licenses/gpl.html).

<a href="https://f-droid.org/packages/fr.kwiatkowski.ApkTrack/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>

-------------------------------

## Usage

![ApkTrack screenshot](http://img11.hostingpics.net/pics/161407screenshot.png)

* Click on an application in the list of installed apps to perform a manual update check for this APK.
* Buttons in ApkTrack's window header (i.e. at the top of its main page):
   * The leftmost button searches for a specific app in the application list.
   * The middle button performs an update check for *all* applications.
   * The rightmost button offers additional options, such as sorting.  ApkTrack's settings are also accessible via this menu.
* Apps with their version information displayed in red are outdated.  Click the magnifying glass icon to search for the latest APK with your favorite search engine, which is configurable in ApkTrack's settings.
* Swiping an application entry in the list of installed apps to the left or right removes it from the list and hence excludes it from being queried for updates.  Such an exclusion can be reverted in ApkTrack's settings.
* Apps can also be excluded by category (e.g. system apps, Xposed apps, unknown apps) in the settings.
* ApkTrack can also be configured to use a network proxy in its settings.
* ApkTrack's settings also offer to delete the APKs it downloaded.

That's it.

## Things to keep in mind

* Applications are *not* updated automatically.  ApkTrack may provide a direct link to the latest APK available, but in general users are expected to download APKs by themselves.
* ApkTrack uses regular expressions to scrape webpages, so it may cease to work without notice if a source website is modified.
* Although there is a background service checking for updates every day, it may get killed by the OS.  Remember to check for updates manually from time to time, i.e. by hitting the middle button in ApkTrack's window header.

-------------------------------

### Download
Stable releases of ApkTrack are [available on F-Droid](https://f-droid.org/packages/fr.kwiatkowski.ApkTrack/).   
A precompiled and packaged version of ApkTrack can be also [downloaded here](http://apktrack.kwiatkowski.fr/apk/ApkTrack.apk).   
If you want to help testing ApkTrack, feel free to use the current [beta version](http://apktrack.kwiatkowski.fr/apk/ApkTrack_beta.apk).  More features are implemented, but bugs may occur.  Be sure to report them!

### Translate
You can help translating ApkTrack on [OneSky](https://apktrack.oneskyapp.com/).  Get in touch with me (using the contact information below), if you need a new language to be set up.

### Build ApkTrack
ApkTrack's build system has been switched to Gradle in order to make it easier for contributors to compile this project.  Use the following commands to generate the APK:

```
git clone https://github.com/JusticeRage/ApkTrack.git
cd ApkTrack
./gradlew build
ls -l app/build/outputs/apk/
```

![Travis](https://travis-ci.org/JusticeRage/ApkTrack.svg?branch=beta)

### Donations
ApkTrack is completely free, and I don't expect any kind of compensation for using this application.  I do like Bitcoins though, so if you want to send some my way, here is an address you can use for that: ```19wFVDUWhrjRe3rPCsokhcf1w9Stj3Sr6K```  
If you don't have bitcoins, but still want to show your appreciation, please consider donating to either [La Quadrature du Net](https://support.laquadrature.net/) or the [EFF](https://supporters.eff.org/donate/).   
Be sure to drop me a line if you choose to donate in any way, so I can thank you personally and add you to the list of donators below.

#### Generous donators:
- Zongo Saiba

### Contact information
[![](https://manalyzer.org/static/mail.png)](mailto:justicerage *at* manalyzer.org)   
[![](https://manalyzer.org/static/twitter.png)](https://twitter.com/JusticeRage)   
[![](https://manalyzer.org/static/gpg.png)](https://pgp.mit.edu/pks/lookup?op=vindex&search=0x40E9F0A8F5EA8754)
