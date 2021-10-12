package com.example.queuefairdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.qf.adapter.QueueFairConfig;
import com.qf.adapter.android.QueueFairClient;
import com.qf.adapter.android.QueueFairClientListener;

public class MainActivity extends AppCompatActivity {

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
        Toast.makeText(this,"Queue-Fair storage cleared.",Toast.LENGTH_LONG).show();
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

        QueueFairClient client = new QueueFairClient(this, null, "YOUR_ACCOUNT_SYSTEM_NAME","YOUR_QUEUE_SYSTEM_NAME",null, new QueueFairClientListener() {

            @Override
            //Called by the adapter if there is no internet connection.
            //You will usually want to start the protected activity/operation in this case.
            public void onNoInternet() {
                Toast.makeText(MainActivity.this,"No internet connection.  Try again later.",Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter if there your account and queue settings could not be found.
            //Check that you have put in the correct account system name and queue system name in the client constructor call above.
            public void onNoSettings() {
                Toast.makeText(MainActivity.this,"Settings not found.  Try again later.",Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter when someone is passed by the queue, either immediately (with passType "SafeGuard")
            //or from a queue (with passType "Queued").
            //People are considered passed for the PassedLifetime from your queue settings in the portal.  If someone
            //goes back and through again, they get passType "Repass".
            public void onPass(String passType) {
                Toast.makeText(MainActivity.this,"Pass: "+passType, Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter if something goes wrong.
            //You will usually want to start the protected activity/operation in this case.
            public void onError(String s) {
                Toast.makeText(MainActivity.this,"Error: "+s, Toast.LENGTH_LONG).show();
                startProtectedActivity();
            }

            @Override
            //Called by the adapter when it is about to show a Queue, PreSale, PostSale or Hold page.
            public void onShow() {
                Toast.makeText(MainActivity.this,"Showing Queue-Fair", Toast.LENGTH_LONG).show();
            }

            //Called by the adapter when a user has left the queue, for example by pressing the back button,
            //or opening another app.  Their place is saved, and when they come back, they will be
            //recognised.
            public void onAbandon(String cause) {
                Toast.makeText(MainActivity.this, "Abandon: "+cause, Toast.LENGTH_LONG).show();
            }

        });

        //run the adapter
        client.go();

        //Without Queue-Fair, the button press would do the following immediately.
        //startProtectedActivity(intent);
    }
}