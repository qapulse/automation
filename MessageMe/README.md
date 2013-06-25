Installation
============
### Prerequisites (Mac)
- Ruby 1.8.7 (or higher)
- Brew
- Android SDK 
- Set `ANDROID_HOME` environment variable pointing to Android SDK home
- Ant 1.8 (or higher)
- Set `ANT_HOME` environment variable pointing to Ant folder
- Add $ANT_HOME/bin to PATH environment variable
- Set `JAVA_HOME` pointing to Java SDK folder

### Installation

- Install `calabash-android` by running`gem install calabash-android`
- You might have to run `sudo gem install calabash-android` if you do not have the right permissions.


Test Execution
==============
Start MessageMe app from eclipse choosing appropriate target (emulator or usb conneted device). 
Calabash tests are run from command line and shows color coded test results in console. 
To format test result in html use `--format html --out filename.html` option

### Smoke Test

Preconditions:
It must be run in a device with no sim, to let the application to create an account by email.

`calabash-android run /path/<MessageMe.apk> features/smokeTest.feature`

or

`calabash-android run /path/<MessageMe.apk> features/smokeTest.feature --format html --out smokeTest.html`


### Perf Test

`calabash-android run /path/<MessageMe.apk> features/sendMessagesPerformanceTest.feature`

Tested Environments
===================

Version | Status
3.2.6 | Some tests fail due to slow device, investigating test fixes
4.0.1 | Tested OK
4.0.4 | Tested OK
4.1.2 | Tested OK
4.2.0 | Tested OK
3.2.6 
