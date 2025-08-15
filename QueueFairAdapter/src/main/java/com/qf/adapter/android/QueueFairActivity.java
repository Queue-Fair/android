package com.qf.adapter.android;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.qf.adapter.QueueFairConfig;
import com.qf.adapter.R;

import org.json.JSONException;
import org.json.JSONObject;

public class QueueFairActivity extends AppCompatActivity {

    // To use your own layouts for the QueueFairActivity that is shown, subclass this class
    // and change the below to your custom layout.  It must contain a WebView.
    public int layout = R.layout.activity_queuefair;
    public int webviewID = R.id.queueFairWebView;

    WebView webView = null;
    Handler h;

    boolean complete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (QueueFairConfig.debug) Log.i("QFA", "Creating Queue-Fair Activity");
        setContentView(layout);

    }

    public static void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (QueueFairConfig.debug)
                Log.i("QFA", "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            if (QueueFairConfig.debug)
                Log.i("QFA", "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    private void end(long when) {
        if (when > 500) {
            h.postDelayed(() -> QueueFairActivity.this.finish(), when - 500);
            h.postDelayed(() -> webView.loadUrl("about:blank"), when - 250);
        } else {
            webView.loadUrl("about:blank");
            QueueFairActivity.this.finish();
        }
    }

    private void broadcastError(String message) {
        Intent intent = new Intent();
        intent.setAction(getPackageName() + ".com.qf.adapter.QUEUE_FAIR_BROADCAST");
        intent.putExtra("message", message);
        intent.putExtra("result", "ERROR");
        sendBroadcast(intent);
    }

    private void broadcastAbandon(String cause) {
        Intent intent = new Intent();
        intent.setAction(getPackageName() + ".com.qf.adapter.QUEUE_FAIR_BROADCAST");
        intent.putExtra("cause", cause);
        intent.putExtra("result", "ABANDON");
        sendBroadcast(intent);
    }

    private void broadcastRequest(int request) {
        Intent intent = new Intent();
        intent.setAction(getPackageName() + ".com.qf.adapter.QUEUE_FAIR_BROADCAST");
        intent.putExtra("request", request);
        intent.putExtra("result", "JOIN");
        sendBroadcast(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        try {
            webView = (WebView) findViewById(webviewID);
        } catch (Exception e) {
            broadcastError("Could not find webview - " + e.getMessage());
            return;
        }

        if (webView == null) {
            broadcastError("Could not find webview");
        }

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        
        h = new Handler(getMainLooper());

        complete = false;

        webView.setWebChromeClient(new WebChromeClient() {

            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                if (QueueFairConfig.debug) Log.i("QFWCC", message);

                int i = message.indexOf("{");
                if (i == -1) {
                    return true;
                }
                if (message.indexOf("REDIRECT") == -1 && message.indexOf("JOIN") == -1)
                    return true;

                try {
                    int j = message.lastIndexOf("}");

                    String jsonStr = message.substring(i, j + 1);
                    JSONObject json = null;
                    try {
                        json = new JSONObject(jsonStr);
                    } catch (JSONException e) {
                        if (QueueFairConfig.debug) {
                            Log.i("QFWCC", "Exception processing JSON " + jsonStr, e);
                        }
                        end(0);
                        broadcastError("Queue output could not be parsed.");
                        return true;
                    }
                    if (json == null) {
                        broadcastError("Bad json.");
                        return true;
                    }

                    if (message.indexOf("JOIN") != -1) {
                        int request = json.getInt("request");
                        QueueFairAndroidService.setPreference(QueueFairActivity.this, "mostRecentRequestNumber", "" + request);
                        broadcastRequest(request);
                        return true;
                    }

                    //It's a REDIRECT
                    if(message.indexOf("qfpt") == -1) {
                        return true;   
                    }
                    
                    long when = json.getLong("when");
                    String passType = json.getString("type");
                    String target = json.getString("target");

                    if (target == null) {
                        end(0);
                        broadcastError("Invalid target from queue.");
                        return true;
                    }
                    complete = true;
                    end(when);

                    Intent intent = new Intent();
                    intent.setAction(getPackageName() + ".com.qf.adapter.QUEUE_FAIR_BROADCAST");
                    intent.putExtra("target", target);
                    intent.putExtra("when", when);
                    intent.putExtra("passType", passType);
                    intent.putExtra("result", "SUCCESS");
                    sendBroadcast(intent);
                    return true;

                } catch (Exception e) {
                    if (QueueFairConfig.debug)
                        Log.w("QFWCC", "Exception handing console " + message, e);
                    end(0);
                    broadcastError("Error handling queue: " + e.getMessage());
                    return true;
                }
            }
        });

        String url = extras.getString("location");
        if (url.indexOf("?") != -1) {
            url += "&";
        } else {
            url += "?";
        }
        url += "qfnoredirect=true";
        if (QueueFairConfig.debug) {
            Log.i("QFA", "Opening " + url);
        }
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            h.postDelayed(() -> webView.loadUrl("about:blank"), 250);
        }
        broadcastAbandon("Back");
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            h.postDelayed(() -> webView.loadUrl("about:blank"), 250);
        }
        if (!complete) {
            broadcastAbandon("Pause");
        }
    }
}

