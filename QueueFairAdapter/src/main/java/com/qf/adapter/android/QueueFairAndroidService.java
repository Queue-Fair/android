package com.qf.adapter.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.qf.adapter.QueueFairConfig;
import com.qf.adapter.QueueFairService;

public class QueueFairAndroidService implements QueueFairService {

    Context context;
    String redirectLocation=null;

    private String getPreference(String key) {

        if(context == null) {
            if(QueueFairConfig.debug) Log.i("QFS","Trying to get preference with null context!");
            return "DEFAULT_VALUE";
        }
        SharedPreferences preferences = context.getSharedPreferences(
                "QueueFair", Context.MODE_PRIVATE);
        return preferences.getString(key, "DEFAULT_VALUE");
    }

    private void setPreference(String key, String value) {
        if(QueueFairConfig.debug) Log.i("QFS","Seting pref " + key + " to " + value);
        SharedPreferences preferences = context.getSharedPreferences(
                "QueueFair", Context.MODE_PRIVATE);
        if(value==null)
            value="DEFAULT_VALUE";
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value); // value to store
        editor.commit();
    }

    public void clear() {
        SharedPreferences settings = context.getSharedPreferences("QueueFair", Context.MODE_PRIVATE);
        settings.edit().clear().commit();
    }

    public QueueFairAndroidService(Context context) {
        this.context = context;
    }

    @Override
    public void setCookie(String name, String value, int lifetimeSeconds, String domain) {
        //Called by adapter to set any cookie
        if(QueueFairConfig.debug) Log.i("QFS","Setting cookie "+name+" to "+value+" life "+lifetimeSeconds);
        if(lifetimeSeconds <= 0) {
            setPreference(name, "DEFAULT_VALUE");
            setPreference(name+":expires","DEFAULT_VALUE");
            return;
        }
        setPreference(name,value);
        setPreference(name+":expires",""+(System.currentTimeMillis()+(lifetimeSeconds*1000l)));
    }

    @Override
    public void redirect(String location) {
        //Called by adapter if sent to queue.
        if(QueueFairConfig.debug) Log.i("QFS","Redirect to "+location);
        redirectLocation=location;
    }

    @Override
    public String getCookie(String name) {
        String ret=getPreference(name);
        if(QueueFairConfig.debug) Log.i("QFS","Preference for "+name+" is "+ret);
        if("DEFAULT_VALUE".equals(ret)) {
            return "";
        }
        String expires=getPreference(name+":expires");
        if("DEFAULT_VALUE".equals(expires)) {
            setPreference(name,"DEFAULT_VALUE");
            return "";
        }
        Long expireEpoch = Long.parseLong(expires);
        if(expireEpoch < System.currentTimeMillis()) {
            if(QueueFairConfig.debug) Log.i("QFS","Preference for "+name+" has expired");
            setPreference(name,"DEFAULT_VALUE");
            setPreference(name+":expires","DEFAULT_VALUE");
            return "";
        }
        return ret;
    }

    @Override
    public void addHeader(String name, String value) {
        if(QueueFairConfig.debug) Log.i("QFS","Add header "+name+": "+value);
    }
}
