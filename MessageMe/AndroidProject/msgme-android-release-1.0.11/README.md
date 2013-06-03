# MessageMe Android Client

## Setting up the environment

### Download Eclipse

Download the latest version of eclipse from: http://www.eclipse.org/downloads

Although you could use any eclipse flavor, it's recommended for more easy setup to download the Eclipse Clasic

### Altering the memory usage for Eclipse

* Right click the Eclipse launcher Icon
* Go to: Show Package Contents > Contents > MacOs
* Open Eclipse.ini with a text editor
* Change this values:

--launcher.XXMaxPermSize
1024m
-Xms1024m
-Xmx2048m


### Download the Android SDK

Download the latest Android SDK version from: http://developer.android.com/sdk/index.html 

Once you've downloaded the Android SDK it's time to install the ADT plugin for Eclipse: http://developer.android.com/sdk/installing/installing-adt.html

## Setting Up the project in your computer

### Downloading the Project to your computer

Run: $ git clone https://github.com/littleinc/msgme-android.git

### Importing the Project into Eclipse

In Eclipse, go to:

File > Import

And browse for the location of the project.

Although the project have other project folders as Autobahn or Facebook, the core Android project doesn't have any direct dependency with this projects.

## Running the project

This Android application is setup to run only in OS 2.2+.

### Setup the emulator

If you doesn't have any android device at hand, and want to run the project in the Android emulator, follow this steps:

#### Create a new AVD

* CLick in the Android Virtual Device Manager in the top menu of Eclipse 
* Click in "New"
* Add a Name for the AVD
* Select the device (we recommend one of the Nexus family)
* Under SD Card, add a memory size of 100 MiB.
* Click OK 

(Those are the most basic steps to create a new AVD, for more information visit: http://developer.android.com/tools/devices/managing-avds.html)

#### Using your android device

If you have an Android device at hand, you could use it to run the project, to do so, follow this steps:

##### For Android 3.0 and up

* Go to settings
* Scroll down to Development options
* Check USB debugging

##### For older versions

* In the home screen, tap the menu button
* Select settings
* Go to Applications
* Select Development
* Check USB Debugging.

### Running the project

Once the AVD or your Android device has been setup, click in the Run button in the top menu bar of Eclipse, and the project will automatically be loaded into the selected device.

Also, you can Right click in the project folder, go to Run As, and select Android Application.

## Considerations while running the project

For the signup process, if the device has a SIM card, you'll be taken to a screen were you need to register the phone number of the Android device. Each phone number is associated to an account.

### Deleting a registered phone number

This is for testing purposes only!

* Go to: http://logn.foo.msgme.im/admin/account/phone
* Enter the registered phone in E164 format -- i.e. +14155551234
* Click Submit

### Reset the Account Password

* Go to: http://logn.foo.msgme.im/admin/account/password
* Type the registered email and old password
* Click Submit

 