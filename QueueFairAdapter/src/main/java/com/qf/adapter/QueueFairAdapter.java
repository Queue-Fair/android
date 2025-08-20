package com.qf.adapter;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class QueueFairAdapter extends BaseAdapter {

    static final String synch = "synch";

    public class Queue {
        public String name;
        public String displayName;
        public String cookieDomain;
        public String adapterMode = "safe";
        public String adapterServer;
        public String queueServer;
        public String secret;
        public String variant;
        public String dynamicTarget = "disabled";
        public int passedLifetimeMinutes = -1;    //Will be filled in later.
    }

    public static final String COOKIE_NAME_BASE = "QueueFair-Pass-";

    private QueueFairService service;

    private boolean addedCacheControlHeader = false;

    // State
    public Map<String, Object> adapterResult = null;
    public Queue adapterQueue = null;
    private HashSet<String> passedQueues = null;
    private String uid = null;
    private boolean continuePage = true;
    public long now = -1;

    // Input
    public String requestedURL = null;
    public String userAgent = null;
    public String remoteIPAddress = null;
    public String extra = null;

    private QueueFairAdapter reset() {
        adapterResult = null;
        adapterQueue = null;
        passedQueues = null;
        uid = null;
        continuePage = true;
        now = -1;
        requestedURL = null;
        remoteIPAddress = null;
        userAgent = null;
        extra = null;
        return this;
    }

    /**
     * Convenience constructor. Make sure you call setService() and setConfig()
     * before running an adapter created this way. getAdapter() is the preferred
     * method of obtaining an instance.
     */
    public QueueFairAdapter() {

    }

    private QueueFairAdapter init(QueueFairService service) {
        if (QueueFairConfig.debug) {
            log.setLevel(Level.INFO);
        } else {
            log.setLevel(Level.WARNING);
        }

        this.service = service;

        return this;
    }

    /**
     * Another convenience constructor. getAdapter() is the preferred method of
     * obtaining an instance.
     *
     * @param service a QueueFairService instance.
     */
    public QueueFairAdapter(QueueFairService service) {
        init(service);
    }

    public static String getStr(Map<String, Object> m, String key) {
        Object obj = m.get(key);
        if (obj == null)
            return null;
        if (obj instanceof String) {
            return (String) obj;
        }
        return "" + obj;
    }

    public static int getInt(Map<String, Object> m, String key) {
        Object obj = m.get(key);
        if (obj == null)
            return 0;
        if (obj instanceof Integer) {
            return (int) obj;
        } else if (obj instanceof Double) {
            return (int) (double) obj;
        }
        return Integer.parseInt(obj.toString());
    }

    public static boolean getBool(Map<String, Object> m, String key) {
        Object obj = m.get(key);
        if (obj == null)
            return false;
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return Boolean.parseBoolean(obj.toString());
    }


    private void markPassed(String name) {
        if (passedQueues == null) {
            passedQueues = new HashSet<String>();
        }

        passedQueues.add(name);
    }

    private void setCookie(String queueName, String value, int lifetimeSeconds, String cookieDomain) {
        if (QueueFairConfig.debug)
            log.info("QF Setting cookie for " + queueName + " to " + value + " domain " + cookieDomain);

        String tStr = COOKIE_NAME_BASE + queueName;

        service.setCookie(tStr, value, lifetimeSeconds, cookieDomain);

        if (lifetimeSeconds > 0) {
            markPassed(queueName);
            if (QueueFairConfig.stripPassedString) {
                String loc = requestedURL;
                int i = loc.lastIndexOf("qfqid=");
                if (i != -1) {
                    if (QueueFairConfig.debug)
                        log.info("QF Stripping passedString from URL");
                    loc = loc.substring(0, i - 1);
                    redirect(loc, 0);
                }

            }
        }
    }

    public void setUIDFromCookie() {
        String cookieBase = "QueueFair-Store-" + QueueFairConfig.account;

        String uidCookie = service.getCookie(cookieBase);
        if (uidCookie == null || "".equals(uidCookie))
            return;

        int i = uidCookie.indexOf(":");
        if (i == -1) {
            i = uidCookie.indexOf("=");
        }

        if (i == -1) {
            return;
        }

        uid = uidCookie.substring(i + 1);
    }


    private String processIdentifier(String param) {
        if (param == null)
            return null;
        int i = param.indexOf("[");
        if (i == -1) {
            return param;
        }
        if (i < 20)
            return param;
        return param.substring(0, i);
    }

    public void consultAdapter(Queue queue) {

        if (QueueFairConfig.debug)
            log.info("QF Consulting Adapter Server for " + queue.name);

        adapterQueue = queue;
        int adapterMode = QueueFairConfig.adapterMode;

        if (queue.adapterMode != null && !"".equals(queue.adapterMode)) {
            if ("safe".equals(queue.adapterMode)) {
                adapterMode = QueueFairConfig.MODE_SAFE;
            } else if ("simple".equals(queue.adapterMode)) {
                adapterMode = QueueFairConfig.MODE_SIMPLE;
            }
        }

        if (QueueFairConfig.debug)
            log.info("QF Adapter mode is " + adapterMode);
        if (QueueFairConfig.MODE_SAFE == adapterMode) {
            String url = QueueFairConfig.protocol + "://" + queue.adapterServer + "/adapter/" + queue.name;

            char sep = '?';
            if (sendIPAddressToAdapter) {
                url += "?ipaddress=" + urlencode(remoteIPAddress);
                sep = '&';
            }
            if (uid != null) {
                url += sep + "uid=" + uid;
                sep = '&';
            }

            url += sep + "identifier=" + urlencode(processIdentifier(userAgent));
            if (QueueFairConfig.debug)
                log.info("QF Consulting adapter at " + url);

            adapterResult = urlToMap.urlToMap(url);

            if (adapterResult == null) {
                log.warning("QF No Adapter JSON!");
                return;
            }

            if (QueueFairConfig.debug)
                log.info("QF Downloaded JSON Settings " + adapterResult);

            gotAdapter();

        } else {
            String url = QueueFairConfig.protocol + "://" + queue.queueServer + "/" + queue.name + "?target="
                    + urlencode(requestedURL);

            url = appendVariantToRedirectLocation(queue, url);
            url = appendExtraToRedirectLocation(queue, url);
            if (QueueFairConfig.debug)
                log.info("QF Redirecting to queue server " + url);

            redirect(url, 0);
        }
    }

    public String getVariant(Queue queue) {
        if (queue == null)
            return null;
        return queue.variant;
    }

    public String appendVariantToRedirectLocation(Queue queue, String url) {
        if (QueueFairConfig.debug)
            log.info("appendVariant looking for variant");
        String variant = getVariant(queue);
        if (variant == null) {
            if (QueueFairConfig.debug)
                log.info("appendVariant no variant found.");
            return url;
        }
        if (QueueFairConfig.debug)
            log.info("appendVariant found " + variant);
        if (!url.contains("?")) {
            url += "?";
        } else {
            url += "&";
        }
        url += "qfv=" + urlencode(variant);
        return url;
    }

    public String appendExtraToRedirectLocation(Queue queue, String url) {
        if (extra == null) {
            return url;
        }
        if (QueueFairConfig.debug)
            log.info("appendExtra found extra " + extra);
        if (!url.contains("?")) {
            url += "?";
        } else {
            url += "&";
        }
        url += "qfx=" + urlencode(extra);
        return url;
    }

    private void gotAdapter() {
        if (QueueFairConfig.debug)
            log.info("QF Got adapter " + adapterResult);
        if (adapterResult == null) {
            log.warning("QF ERROR: onAdapter() called without result");
            return;
        }

        if (getStr(adapterResult, "uid") != null) {
            if (uid != null && !uid.equals(getStr(adapterResult, "uid"))) {
                log.warning("QF UID Cookie Mismatch - Contact Queue-Fair Support! expected " + uid + " but received "
                        + getStr(adapterResult, "uid"));
            } else {
                if(uid==null) {
                    uid = getStr(adapterResult, "uid");
                    checkAndAddCacheControl();
                    service.setCookie("QueueFair-Store-" + QueueFairConfig.account, "u:" + uid,
                            getInt(adapterResult, "cookieSeconds"), adapterQueue.cookieDomain);
                }
            }
        }

        String action = getStr(adapterResult, "action");

        if (action == null) {
            if (QueueFairConfig.debug)
                log.info("QF ERROR: onAdapter() called without result action");
            return;
        }

        if ("SendToQueue".equals(action)) {
            if (QueueFairConfig.debug)
                log.info("GotAdapter Sending to queue server.");
            String dt = adapterQueue.dynamicTarget;
            String queryParams = "";
            String target = requestedURL;
            if (!"disabled".equals(dt)) {
                if ("path".equals(dt)) {
                    int i = target.indexOf("?");
                    if (i != -1) {
                        target = target.substring(0, i);
                    }
                }
                queryParams += "target=";
                queryParams += urlencode(target);
            }

            if (uid != null) {
                if (!"".equals(queryParams)) {
                    queryParams += "&";
                }
                queryParams += "qfuid=" + uid;
            }
            String redirectLoc = getStr(adapterResult, "location");
            if (!"".equals(queryParams)) {
                redirectLoc = redirectLoc + "?" + queryParams;
            }

            redirectLoc = appendVariantToRedirectLocation(adapterQueue, redirectLoc);
            redirectLoc = appendExtraToRedirectLocation(adapterQueue, redirectLoc);

            if (QueueFairConfig.debug)
                log.info("GotAdapter Redirecting to " + redirectLoc);
            redirect(redirectLoc, 0);
            return;
        }

        // SafeGuard etc
        int pl = getInt(adapterResult,"pl");
        conditionalSetPassedLifetime(pl);
        
        checkAndAddCacheControl();
        setCookie(getStr(adapterResult, "queue"), urldecode(getStr(adapterResult, "validation")),
                adapterQueue.passedLifetimeMinutes * 60, adapterQueue.cookieDomain);

        if (!continuePage) {
            return;
        }
        if (QueueFairConfig.debug)
            log.info("QF Marking " + getStr(adapterResult, "queue") + " as passed by adapter.");
        markPassed(getStr(adapterResult, "queue"));

    }

    public int conditionalSetPassedLifetime(int pl) {
        if (adapterQueue.passedLifetimeMinutes != -1) {
            if (QueueFairConfig.debug) {
                log.info("PassedLifetime set in code as " + adapterQueue.passedLifetimeMinutes + " - using.");
            }
            return adapterQueue.passedLifetimeMinutes;
        }

        if (pl > 0) {
            if (QueueFairConfig.debug) log.info("Using received PassedLifetime " + pl + " minutes.");
            adapterQueue.passedLifetimeMinutes = pl;
            return pl;
        }

        if (QueueFairConfig.debug) log.info("Response does not contain PassedLifetime and not set in code - defaulting to 20 mins");
        adapterQueue.passedLifetimeMinutes = 20;
        return 20;
    }

    public void redirect(String location, int sleep) {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep * 1000L);
            } catch (Exception e) {
                log.log(Level.WARNING, "Exception sleeping", e);
            }
        }

        if (QueueFairConfig.debug)
            log.info("QF redirecting to " + location);

        checkAndAddCacheControl();
        service.redirect(location);

        continuePage = false;
    }


    private void checkAndAddCacheControl() {
        if(addedCacheControlHeader) {
            return;
        }
        addedCacheControlHeader = true;
        service.addHeader("Cache-Control", "no-store,max-age=0");

    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public final static String createHash(final String secret, final String message) {
        try {
            final Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC.init(secret_key);

            final byte[] digest = sha256HMAC.doFinal(message.getBytes("UTF-8"));
            return bytesToHex(digest);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean validateCookie(Queue queue, String cookie) {
        return validateCookie(queue.secret, queue.passedLifetimeMinutes, cookie);
    }

    /**
     * Convenience method to check whether a cookie is a valid PassedCookie.
     *
     * @param queueSecret the queue's Secret from the Portal.
     * @param cookie      the value of the cookie
     * @return true if the cookie is valid for the given secret, false otherwise.
     */
    public boolean validateCookie(String queueSecret, int passedLifetimeMinutes, String cookie) {
        if (QueueFairConfig.debug)
            log.info("QF Validating cookie " + cookie);

        try {
            if(cookie==null)
                return false;

            int hpos = cookie.lastIndexOf("qfh=");
            if(hpos==-1)
                return false;

            String check = cookie.substring(0, hpos);
            String hash = cookie.substring(hpos + "qfh=".length());

            if (!QueueFairConfig.account.equals(getValueQuick(cookie, "qfa="))) {
                if (QueueFairConfig.debug)
                    log.info("QF Cookie Account Does Not Match");
                return false;
            }

            if (usesSecrets) {

                String checkHash = createHash(queueSecret, processIdentifier(userAgent) + check);
                if (checkHash != null && !checkHash.equals(hash)) {
                    if (QueueFairConfig.debug)
                        log.info("QF Cookie Hash Mismatch given " + hash + " should be " + checkHash);
                    return false;
                }
            }

            String tsStr = getValueQuick(cookie, "qfts=");

            if (tsStr == null) {
                if (QueueFairConfig.debug)
                    log.info("QF Cookie contains no timestamp.");
                return false;
            }

            if (!isNumeric(tsStr)) {
                if (QueueFairConfig.debug)
                    log.info("QF Cookie bad timestamp " + tsStr);
                return false;
            }

            if(usesSecrets) {
                //Only check this for server side adapters - client may not have clock properly set.
                long ts;

                try {
                    ts = Long.parseLong(tsStr);
                } catch (Exception e) {
                    log.info("QF bad timestamp " + tsStr);
                    return false;
                }

                // Don't use 'now' as may be calling this method without rest of adapter
                // process.
                if (ts * 1000L < System.currentTimeMillis() - (passedLifetimeMinutes * 60000L)) {
                    if (QueueFairConfig.debug)
                        log.info("ValidateCookie Too Old" + ts + " " + System.currentTimeMillis() / 1000);
                    return false;
                }
            }

            if (QueueFairConfig.debug)
                log.info("QF Cookie Validated");
            return true;

        } catch (Exception e) {
            log.log(Level.WARNING, "Could not validate cookie " + cookie, e);
            return false;
        }

    }

    // Make sure to have the "=" sign in the name.
    private String getValueQuick(String query, String name) {
        int i = query.lastIndexOf(name);
        if (i == -1)
            return null;

        i = i + name.length();

        int j = query.indexOf('&', i);
        if (j == -1) {
            return query.substring(i);
        }
        return query.substring(i, j);
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.equals("")) {
            return false;
        }
        return java.util.regex.Pattern.matches("\\d+", s);
    }

    private String urlencode(String input) {
        if (input == null) {
            return null;
        }
        try {

            return URLEncoder.encode(input, "UTF-8");
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not encode " + input, e);
        }
        return null;
    }

    private String urldecode(String input) {
        if (input == null)
            return null;

        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public QueueFairService getService() {
        return service;
    }

    public void setService(QueueFairService service) {
        this.service = service;
    }

}

