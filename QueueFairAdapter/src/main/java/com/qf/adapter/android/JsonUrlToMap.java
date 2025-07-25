package com.qf.adapter.android;

import com.qf.adapter.QueueFairAdapter;
import com.qf.adapter.UrlToMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


public class JsonUrlToMap extends UrlToMap {

    public Map<String, Object> urlToMap(String url) {

        Reader r = loadURL(url);
        if (r == null)
            return null;

        String input;

        try {
            StringBuilder sb = new StringBuilder();
            int intValueOfChar;
            while ((intValueOfChar = r.read()) != -1) {
                sb.append((char) intValueOfChar);
            }
            input = sb.toString();
        } catch (Exception e) {
            QueueFairAdapter.log.log(Level.WARNING, "Exception reading string", e);
            return null;
        }

        if(input.contains("{")) {
            input=input.substring(input.indexOf("{"),input.lastIndexOf("}")+1);
        }

        JSONObject json = null;

        try {
            json = new JSONObject(input);
        } catch (Exception e) {
            QueueFairAdapter.log.log(Level.WARNING, "Exception converting " + input + " to json", e);
        }

        if (json == null)
            return null;

        try {
            return toMap(json);
        } catch (Exception e) {
            QueueFairAdapter.log.log(Level.WARNING, "Exception converting "+json+" to map",e);
        }
        return null;
    }


    public static Map<String, Object> toMap(JSONObject jsonobj) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keys = jsonobj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
//            Log.i("QF",key+":"+value+" "+value.getClass());
            if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray)value;
                List list = toList(arr);
                map.put(key, list);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
                map.put(key, value);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

}
