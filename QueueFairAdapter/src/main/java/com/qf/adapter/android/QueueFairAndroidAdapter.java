package com.qf.adapter.android;

import com.qf.adapter.QueueFairAdapter;
import com.qf.adapter.QueueFairService;
import com.qf.adapter.QueueFairSettings;

public class QueueFairAndroidAdapter extends QueueFairAdapter {

    String variant;

    public QueueFairAndroidAdapter(QueueFairService service) {
        super(service);
    }

    @Override
    public String getVariant(QueueFairSettings.Queue queue) {
        return variant;
    }

}
