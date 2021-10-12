package com.qf.adapter.android;

import android.util.Log;

import java.util.logging.Level;

public class LoggerWrapper {

    public static String tag = "QF";

    public void info(String msg) {
        Log.i(tag, msg);
    }

    public void log(Level level, String msg, Throwable t) {
        if(level.equals(Level.WARNING)) {
            Log.w(tag,msg,t);
        }
        Log.i(tag,msg,t);
    }

    public void warning(String msg) {
        Log.w(tag,msg);
    }

    public void setLevel(Level level) {
        //Ignored
    }
 }
