package com.qf.adapter.android;

public interface QueueFairClientListener {
    void onNoInternet();

    // OBSOLETE - will call onError with a Network Error instead
    // if your account system name or queue system name are incorrect.
    // void onNoSettings()
    
    void onPass(String passType);

    void onError(String s);

    void onShow();

    void onAbandon(String cause);

    void onJoin(int request);
}
