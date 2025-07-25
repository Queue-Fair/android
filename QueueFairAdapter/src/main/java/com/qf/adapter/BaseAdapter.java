package com.qf.adapter;

import com.qf.adapter.android.JsonUrlToMap;
import com.qf.adapter.android.LoggerWrapper;

public class BaseAdapter {

    static UrlToMap urlToMap = new JsonUrlToMap();

    public static LoggerWrapper log = new LoggerWrapper();

    static boolean sendIPAddressToAdapter =  false;

    boolean usesSecrets = false;
}
