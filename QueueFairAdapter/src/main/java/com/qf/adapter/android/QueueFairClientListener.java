package com.qf.adapter.android;

public interface QueueFairClientListener {
    void onNoInternet();

    void onNoSettings();

    void onPass(String passType);

    void onError(String s);

    void onShow();

    void onAbandon(String cause);
}
