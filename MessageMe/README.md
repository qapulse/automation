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


TEST EXECUTION
==============
Start MessageMe app from eclipse choosing appropriate target (emulator or usb conneted device). 
Calabash tests are run from command line and shows color coded test results in console. 
To format test result in html use `--format html --out filename.html` option

### Smoke Test

`calabash-android run /path/<MessageMe.apk> features/smokeTest.feature`

or

`calabash-android run /path/<MessageMe.apk> features/smokeTest.feature --format html --out smokeTest.html`


### Perf Test

`calabash-android run /path/<MessageMe.apk> features/sendMessagesPerformanceTest.feature`

