package com.example.queuefairdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.qf.adapter.QueueFairConfig;
import com.qf.adapter.android.QueueFairClient;
import com.qf.adapter.android.QueueFairClientListener;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void resetAdapter(View view) {
        QueueFairClient.resetAdapter(this);
        Toast.makeText(this,"Adapter storage cleared.",Toast.LENGTH_LONG).show();
    }

    public void resetQueueFair(View view) {
        QueueFairClient.resetQueueFair(this);
        Toast.makeText(this,"QueueFair storage cleared.",Toast.LENGTH_LONG).show();
    }

    private void startProtectedActivity() {
        Intent intent = new Intent(MainActivity.this, ProtectedActivity.class);
        startActivity(intent);
    }


    public void continueClicked(View view) {

        //For development/debug, Uncomment the line below to enable adapter debug logging.
        //Please do not release your app with this line uncommented!
        //QueueFairConfig.debug=true;

        //For development/debug, Uncomment the line below to download a fresh copy of your portal settings on every run.
        //Please do not release your app with this line uncommented!
        //QueueFairConfig.settingsCacheLifetimeMinutes = 0;

        //You MUST replace the fields indicated with the system name of your account, and the system name of the queue you wish to use.
        //This version gets the PassedLifetime direct from the servers so you don't need to set it here.
        QueueFairClient client = new QueueFairClient(this, null, "REPLACE_WITH_ACCOUNT_SYSTEM_NAME","REPLACE_WITH_QUEUE_SYSTEM_NAME", null, new QueueFairClientListener() {

            @Override
            //Called by the adapter if there is no internet connection.
            //You will usually want to start the protected activity/operation in this case.
            public void onNoInternet() {
                Toast.makeText(MainActivity.this,"No internet connection.  Try again later.",Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter when someone is passed by the queue, either immediately (with passType "SafeGuard")
            //or from a queue (with passType "Queued").
            //People are considered passed for the PassedLifetime from your queue settings in the portal.  If someone
            //goes back and through again, they get passType "Repass".
            public void onPass(String passType) {
                // If you have already told your Push Notification system to send a notification in onAbandon(),
                // tell it to cancel the request here.
                Toast.makeText(MainActivity.this,"Pass: "+passType, Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter if something goes wrong.
            //You will usually want to start the protected activity/operation in this case.
            //If you give the wrong account system name or queue system name, this method will be
            //called with the message Network error.
            public void onError(String s) {
                Toast.makeText(MainActivity.this,"Error: "+s, Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter when it is about to show a Queue, PreSale, PostSale or Hold page.
            public void onShow() {
                // If you have already requested your Push Notification system to send a notification when this user
                // reaches the front of the queue in your implementation of onAbandon(), cancel the request here.
                Toast.makeText(MainActivity.this,"Showing Queue-Fair", Toast.LENGTH_LONG).show();
            }


            @Override
            // Called by the adapter when a user is assigned a queue position.  You can use this with
            // your notification system to send a Push Notification to a user who has closed your app
            // that they have reached the front of the queue.  See https://firebase.google.com/docs/cloud-messaging
            // and https://firebase.google.com/docs/cloud-messaging/android/first-message for a tutorial on Push Notifications.
            public void onJoin(int request) {
                // You may wish to store the request number (queue position) within your own code.  It will also be persistently
                // stored by the Adapter automatically.  You can get the most recently assigned request number (queue position)
                // with QueueFairAndroidService.getPreference(MainActivity.this, "mostRecentRequestNumber");
                //
                // You should wait until onAbandon() is called to tell your Push Notification system to send a
                // notification when the visitor reaches the front of the queue.  For now just remember the request number.
                Toast.makeText(MainActivity.this, "Joined with request "+request,Toast.LENGTH_LONG).show();
            }

            @Override
            // Called by the adapter when a user has left the queue, for example by pressing the back button,
            // or opening another app.  Their place is saved, and when they come back, they will be
            // recognised.  If they leave the app open with the queue displayed on the screen, this method is not
            // called.
            public void onAbandon(String cause) {
                // If you wish to send the user a notification when this user has reached the front of the queue,
                // tell your Push Notification system here, using the request number stored from onJoin()
                Toast.makeText(MainActivity.this, "Abandon: "+cause, Toast.LENGTH_LONG).show();
            }

        });

        //run the adapter
        client.go();

        //Without Queue-Fair, the button press would do the following immediately.
        //startProtectedActivity(intent);
    }
}
