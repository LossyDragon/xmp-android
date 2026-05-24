# Notice

A modern Android rewrite based on this project is being developed at [Dragon Mod Player](https://github.com/LossyDragon/DragonModPlayer).

# Xmp Mod Player (unofficial)

Xmp Mod Player is an Android app that uses [libxmp](https://github.com/libxmp/libxmp) and plays over 90 mainstream and obscure module formats, including Protracker (MOD), Scream Tracker 3 (S3M), Fast Tracker II (XM) and Impulse Tracker (IT), and more!

### Builds
---
[![Signed Release APK](https://github.com/LossyDragon/xmp-android/actions/workflows/app-build-release.yml/badge.svg?branch=2023)](https://github.com/LossyDragon/xmp-android/actions/workflows/app-build-release.yml)
<br>
Release builds can be download by clicking on the badge above, click on the title of the top most green checkmark, and download the `XmpAndroid` zip file in the Artifacts area.
<br><br>
Note: If upgrading with a recent CI build and it fails to install, it's probably because the build version number wasn't changed.
<br><br>

### This is a unofficial fan made revival
---
*This fork is a playground to a degree. Things may change or break depending on the idea being explored.*
</br>

Since android has changed so much over the past 5+ years. The original app has fallen behind on modern development standards, making it crash prone, dated in looks, restrictions with newer APIs,  and uses an old version of libxmp which has gotten much love since the latest version was posted on the Play Store.

The original/official app is made by [Claudio Matsuoka](https://github.com/cmatsuoka/xmp-android).
<br><br>
The [Play Store](https://play.google.com/store/apps/details?id=org.helllabs.android.xmp) version is **delisted** for some reason.
<br><br>
[F-Droid](https://f-droid.org/) has build 4.12.0 (latest official version) built with libxmp 4.6.0 [Xmp Mod Player](https://f-droid.org/en/packages/org.helllabs.android.xmp/)
<br><br>

### Features and Changes
---
- Support for the latest android sdk versions
- Storage Access Framework - Mods and Playlists will now be under one directory instead of two separate folders in your root storage
- A complete UI overhaul using Jetpack Compose
- New json based playlist - Old playlist migration planned
- Oboe - A high performance audio library using either OpenSL or AAudio
- (Planned) media3 - A media support library for a rich media experience

Full list of dependencies can be found [here](https://github.com/LossyDragon/xmp-android/blob/2023/gradle/libs.versions.toml)
</br></br>

### Building the app
---
1. Make sure to have an up to date version of Android Studio (Iguana | 2023.2.1) or newer
2. Git clone this repo. `git clone --recursive https://github.com/LossyDragon/xmp-android.git`
3. If you plan to use the downloading feature, make sure to have a valid API key from [The Mod Archive](https://modarchive.org/). Add your key to `modArchiveApiKey="<api-key>"`  which will need to be in your gradle.properties (Usually in your global C:\\Users\\username\\.gradle), otherwise make it an empty string value `modArchiveApiKey=""`
4. Build and Launch

Note: Debug builds using Jetpack Compose will have slim-to-severe performance issues.
