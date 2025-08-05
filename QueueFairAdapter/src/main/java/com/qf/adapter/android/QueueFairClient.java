package com.qf.adapter.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


import com.qf.adapter.QueueFairAdapter;
import com.qf.adapter.QueueFairConfig;

public class QueueFairClient {

    public static String domainSuffix = "queue-fair.net";

    Activity parent;
    String queueServerDomain;
    String accountSystemName;
    String queueSystemName;
    String variant;
    int passedLifetimeMinutes = 20;
    QueueFairClientListener listener;
    QueueFairAndroidService service;
    QueueFairAndroidAdapter adapter;

    public static Class defaultActivityClass = QueueFairActivity.class;

    public Class activityClass = defaultActivityClass;

    Handler h;
    boolean d;
    int checks=0;

    static final String TAG = "QFC";


    class ClientReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (QueueFairConfig.debug) {
                Log.i("QFCR", "Received " + intent + " from " + context);
            }
            Bundle x = intent.getExtras();
            if (x == null) {
                h.post(() -> listener.onError("No result from queue."));
                return;
            }
            String result = x.getString("result");
            if (result == null) {
                h.post(() -> listener.onError("Invalid result from queue."));
                return;
            }
            switch(result) {
                case "JOIN":
                    h.post(() -> listener.onJoin(x.getInt("request")));
                    //Return without unregistering.
                    return;
                case "SUCCESS":
                    onPassFromQueue(x.getString("target"), x.getString("passType"), x.getLong("when"));
                    break;
                case "ERROR":
                    h.post(() -> listener.onError(x.getString("message")));
                    break;
                case "ABANDON":
                    h.post(() -> listener.onAbandon(x.getString("cause")));
                    if("Pause".equals(x.getString("cause"))) {
                        //Do not unregister receiver on app pause.
                        return;
                    }
                    break;
                default:
                    h.post(() -> listener.onError("Invalid result "+result));
                    break;
            }
            parent.unregisterReceiver(this);
        }
    }

    /* Retained for backward compatibility - defaults to 20 minutes Passed Lifetime */
    public QueueFairClient(Activity parent, String queueServerDomain, String accountSystemName, String queueSystemName, String variant, QueueFairClientListener listener) {
        this(parent,queueServerDomain,accountSystemName,queueSystemName,variant,20,listener);
    }

    public QueueFairClient(Activity parent, String queueServerDomain, String accountSystemName, String queueSystemName, String variant, int passedLifetimeMinutes, QueueFairClientListener listener) {
        this.parent = parent;
        this.accountSystemName = accountSystemName;
        this.queueSystemName = queueSystemName;
        if (queueServerDomain == null) {
            this.queueServerDomain = accountSystemName + "."+domainSuffix;
        } else {
            this.queueServerDomain = queueServerDomain;
        }
        this.variant = variant;
        this.listener = listener;
        this.passedLifetimeMinutes = passedLifetimeMinutes;
        this.d = QueueFairConfig.debug;
        h = new Handler(parent.getMainLooper());
        QueueFairConfig.account = accountSystemName;
    }

    public void go() {
       h.post(new InternetChecker());
    }

    public class InternetChecker implements Runnable {

        @Override
        public void run() {
            if(haveNetworkConnection()) {
                Thread t = new Thread(QueueFairClient.this::gotNetworkConnection);
                t.start();
                return;
            }
            checks++;
            if(checks >= Math.max(QueueFairConfig.readTimeoutSeconds,5)) {
                h.post(() -> listener.onNoInternet());
                return;
            }

            h.postDelayed(this,1000);
        }
    }

    private void gotNetworkConnection() {
        if (d) Log.i(TAG, "Got network connection.");
        service = new QueueFairAndroidService(parent);
        adapter = new QueueFairAndroidAdapter(service);
        adapter.now = System.currentTimeMillis();
        adapter.variant = variant;

        adapter.userAgent = "QueueFair Android Adapter";
        adapter.requestedURL = "javascript:void(0)";

        adapter.setUIDFromCookie();

        QueueFairAdapter.Queue queue = adapter.new Queue();
        queue.name = queueSystemName;
        queue.adapterServer = (queueServerDomain != null ? queueServerDomain : accountSystemName + "." +  domainSuffix);
        queue.queueServer = queue.adapterServer;
        queue.displayName = queue.name;
        queue.variant = variant;
        queue.passedLifetimeMinutes = this.passedLifetimeMinutes;

        adapter.adapterQueue = queue;

        String cookie = service.getCookie("QueueFair-Pass-" + queue.name);
        if (!"".equals(cookie)) {
            if (adapter.validateCookie(queue, cookie)) {
                h.post(() -> listener.onPass("Repass"));
                return;
            }
        }

        adapter.consultAdapter(queue);

        if(adapter.adapterResult == null) {
            h.post(() -> listener.onError("Network error"));
            return;
        }

        cookie = service.getCookie("QueueFair-Pass-" + queue.name);
        if (!"".equals(cookie)) {
            if (adapter.validateCookie(queue, cookie)) {
                h.post(() -> listener.onPass("SafeGuard"));
                return;
            }
        }
        String redirectLocation = service.redirectLocation;
        if (redirectLocation == null) {
            h.post(() -> listener.onError("Invalid result."));
            return;
        }

        h.post(() -> listener.onShow());
        h.post(() -> launchPage(redirectLocation));

    }

    private void launchPage(String location) {
        IntentFilter intentFilter = new IntentFilter();
        String action=parent.getPackageName() + ".com.qf.adapter.QUEUE_FAIR_BROADCAST";
        if(QueueFairConfig.debug) {
            Log.i(TAG,"Registering listener for "+action);
        }
        intentFilter.addAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            parent.registerReceiver(new ClientReceiver(), intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            parent.registerReceiver(new ClientReceiver(), intentFilter);
        }

        if (QueueFairConfig.debug) Log.i(TAG, "Launching Queue-Fair Activity");
        Intent intent = new Intent(parent, activityClass);
        intent.putExtra("location", location);

        parent.startActivity(intent);
    }

    public static void resetAdapter(Activity parent) {
        QueueFairAndroidService service = new QueueFairAndroidService(parent);
        service.clear();
    }

    public static void resetQueueFair(Activity parent) {
        QueueFairActivity.clearCookies(parent);
    }

    public void onPassFromQueue(String target, String passType, long when) {
        h.post(() -> {
            if (QueueFairConfig.debug) Log.i(TAG, "Got pass to target " + target);
            if (adapter == null) {
                h.post(() -> listener.onError("No adapter."));
                return;
            }
            if (service == null) {
                h.post(() -> listener.onError("No service."));
                return;
            }
            int i = target.lastIndexOf("qfqid");
            if (i == -1) {
                h.post(() -> listener.onError("Not validated."));
                return;
            }
            String validation = target.substring(i);

            service.setCookie(QueueFairAdapter.COOKIE_NAME_BASE + queueSystemName, validation, adapter.adapterQueue.passedLifetimeMinutes * 60, null);

            h.postDelayed(() -> listener.onPass(passType), when);
        });
    }

    private boolean haveNetworkConnection() {
        try {
            boolean haveConnectedWifi = false;
            boolean haveConnectedMobile = false;

            ConnectivityManager cm = (ConnectivityManager) parent.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
            NetworkInfo ni = cm.getActiveNetworkInfo();
            boolean hasActive = false;
            if (ni != null && ni.isConnected()) {
                hasActive = true;
            }
            return haveConnectedWifi || haveConnectedMobile || hasActive;
        } catch (Exception e) {
            if (d)
                Log.i(TAG, "Exception checking network " + e.getMessage());
        }
        return true;
    }
}

