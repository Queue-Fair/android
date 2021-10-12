---
## Queue-Fair Android Adapter README & Installation Guide

Queue-Fair can be added to any Android app easily in minutes.  This Queue-Fair Android module is suitable for native Android apps - if your Android app is a Web App running entirely in a browser, then the Client Side JavaScript Adapter is more suited to that.  You will need a Queue-Fair account - please visit https://queue-fair.com/free-trial if you don't already have one.  You should also have received our Technical Guide.


## About the Adapter
The Android Adapter has two major components, the Adapter code that checks and validates your users with the Queue-Fair service, which is similar to our Java Server-Side Adapter, and a Queue-Fair Activity that displays Queue, Hold, PreSale and PostSale display to your users when they cannot be Passed immediately by SafeGuard.

These pages are displayed inside a WebView.  You can create app-specific displays by using the Portal and a named Variant for your app.

All components are encapsulated into QueueFairClient, which is the class you will use to interact with Queue-Fair.

The Adapter manages its own persistent storage to remember that particular users have been Passed by Queue-Fair, in the form of SharedPreferences, and also persistent Cookies when a QueueFairActivity is launched.

Typically, you will replace a call to launch a protected activity or start a protected operation with a call to QueueFairClient, implementing a QueueFairListener object that then launches the protected activity or starts the operation when the app user is Passed by the queue.  

This distribution also includes source code for a demonstration app, QueueFairDemo.  Example code for using QueueFairClient is contained within the QueueFairDemo's MainActivity.java file.

If your vistors navigate away from a displayed Queue Page (by using the back button, by opening another app, or their phone going to sleep, for example), they do not lose their place in the queue - it is saved in the same way as places are saved for your web visitors.

This guide assumes you already have Android Studio.  We recommend you perform the steps below to build the QueueFairDemo app, before importing the QueueFairAdapter module into your existing Android app.

## Building the Demo App ##

**1.** Open Android Studio.  From the File menu, select New Project

**2.** Create a new Phone & Tablet app with an Empty Activity.  Make the package name "com.example.queuefairdemo".  The SDK version can be 16 or above.

**3.** Build and run your empty app with the Play button to make sure it's working.

**4.** In File Explorer or Finder (Not Android Studio), Copy-and-paste QueueFairDemo/app/src from this distribution into the "app" folder that has been created by Studio, overwriting any files therein.

**5.** Android Studio will recognise the changes.  You will have build errors until you add the Adapter module as detailed in the next section.

## Adding the library to an existing app

**6.** In Android Studio with your app open, from the File menu, select New and New Module.

**7.** Select "Android Library" from the left pane.  Name the module QueueFairAdapter, and the package name must be "com.qf.adapter".  Gradle will sync; wait for it to finish.

**8.** You will see QueueFairAdapter appear in the package explorer on the left hand side of Studio.

**9.** In File Explorer / Finder (not Android Studio), Copy-and-paste QueueFairAdapter/src from this distribution into the QueueFairAdapter folder that has been created by Studio, overwriting any files therein.

**10.** Your project will now have three build.gradle files.  Find the one for the QueueFairAdapter Module.  If there is no "dependencies" section at the end of this file, or if there is no "appcompat" implementation line, add the following stanza at the end:

dependencies {
    implementation 'androidx.appcompat:appcompat:1.3.1'
}

**11.** Now press Ctrl+Alt+Shift+S to open the Project Structure dialog.  Select Dependencies, app, and then "+" in the Declared Dependencies section, and "Module Dependency" (not Library).

**12.** Check the QueueFairAdapter box and hit OK.  Gradle will sync, and your app is now buildable.

## Running the Adapter

**13.** In your app's Android Manifest, you must add the following activity definition to the <application> stanza:

	<activity
            android:name="com.qf.adapter.android.QueueFairActivity"
            android:theme="@style/QueueFairNoTitleBar">
        </activity>

The android:theme attribute is optional, and causes the Queue Pages to be shown without a title bar.

**14.** Example code with instructions on how to use the QueueFairClient in your own code is contained in QueueFairDemo's MainActivity.java, in the continueClicked() method at the end.  Typically you will construct it with a QueueFairClientListener instance and then call queueFairClient.go();

**15.** In the line that constucts the QueueFairClient, change the accountSystemName to the System Name for your account from the Queue-Fair Portal's Account -> Your Account page.  Also change the queueSystemName to the System Name for the queue you want to use in this app or for your protected activity/operation, visible in the Queue-Fair Portal on the Queue Settings page.  If you create a custom Variant for display in your app, also pass in the variant name here.

**16.** Build and run your app.

That's it you're done!

### To test the Server-Side Adapter

Use a queue that is not in use on other pages/apps, or create a new queue for testing.

#### Testing SafeGuard
Make sure your code uses the correct queue System Name, and that the Queue is set to SafeGuard.

Open the app and hit Continue.  A Toast will appear to indicate that you have been passed by SafeGuard, without seeing a Queue Page, and the protected activity will launch.

Use the back button on your phone/emulator.  Tap Continue again.  The Toast will indicate that you have been Repassed, because the Passed Lifetime for your queue has not expired, and the protected activity will launch again.


#### Testing Queue
Go back to the Portal and put the queue in Demo mode on the Queue Settings page.  Hit Make Live.  

In the App tap Reset Adapter to delete your Passed status, stored by the app.

Tap Continue.
 - Verify that you are now sent to queue.
 - When you come back to the page from the queue, verify that a Toast appears containing the word "Passed", and that the protected activity launches.
 - Use the back button and hit Continue again.  Verify that you are shown as "Repassed", without seeing a Queue Page a second time.

If you wish to fully clear your Passed status, then if you have been shown a Queue Page, you must tap both Reset Adapter and Reset Queue-Fair.


### Advanced Topics

Activation Rules and Variant Rules from the portal are not germane to this use case and are ignored.  Instead, specify the queue you wish to use and (optionally) the variant you want to display in the construction call to QueueFairClient.

Any Target settings for your queue are also not germane to this use case and are also ignored - rather you set the target within your app in the QueueFairClientListener implementation that you supply to QueueFairClient.  Any Queue Pages shown within your app will not go on to request a target page from any site.

QueueFairClient objects are not reusable - you should create a new one every time your app is about to start the protected activity/operation.

The Queue-Fair Adapter will by default launch an activity with a whole-screen webview in which to run any Queue Pages. 

To customise the display for your app, the easiest way is to can create a variant of your queue for use within your app in the Queue-Fair Portal, and tell your app to use it by passing its name as the variant parameter to the QueueFairClient constructor.  This means that your app users can participate in the same queue as your website visitors.

For finer display control, you may wish to subclass QueueFairActivity, which will allow you to use your own custom layouts, including Android UI components.  For example, you may wish to use Android UI components for the text of the queue page, with just the progress bar within a WebView.  To do this, you can set QueueFairClient.defaultActivityClass to your new subclass.

If you need to use a variety of different QueueFairActivity subclasses for different queues within your app, then set a QueueFairClient instance's activityClass field to YourClass.class after the QueueFairClient has been constructed but before QueueFairClient.go() is called.

Logging to Logcat is disabled by default, but you can enable it with QueueFairConfig.debug = true - but please make sure it is disabled for release versions of your app.

Your Account and Queue settings are downloaded by the Adapter in normal operation.  No queue or account secrets are downloaded or used, for security reasons (as they would be accessible to a very technically skilled user).  Secrets are not necessary for this use case.

The downloaded settings are cached for up to 5 minutes by default.  You can set QueueFairConfig.settingsCacheLifetimeMinutes to 0 to download a fresh copy of the settings every time, which may be useful while you are coding - but please set this back to at least 5 for release versions of your app.

Unlike our Server-Side Adapters, The Android adapter always works in SAFE_MODE - SIMPLE_MODE is not suitable for this use case.


## AND FINALLY

Remember we are here to help you! The integration process shouldn't take you more than an hour - so if you are scratching your head, ask us.  Many answers are contained in the Technical Guide too.  We're always happy to help!
