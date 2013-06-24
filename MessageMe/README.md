Installation
============
### Prerequisites (Mac)
- Ruby 1.8.7 (or higher)
- Brew
- Android SDK 
- Set `ANDROID_HOME` environment variable pointing to Android SDK home
- Ant 1.8 (or higher)

### Installation

- Install `calabash-android` by running`gem install calabash-android`
- You might have to run `sudo gem install calabash-android` if you do not have the right permissions.

### Smoke Test

`calabash-android run /path/<MessageMe.apk> features/smoketest.feature`

### Perf Test

`calabash-android run /path/<MessageMe.apk> features/sendMessagesPerformanceTest.feature`

