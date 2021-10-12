package com.qf.adapter;

public interface QueueFairService {

    /**
     * Sets a cookie in the client browser.
     * @param name	the name of the cookie
     * @param value	the value to store
     * @param lifetimeSeconds	cookie lifetime in seconds.  Set to 0 or below to delete a cookie.
     */
    void setCookie(String name, String value, int lifetimeSeconds, String domain);

    /**
     * Causes a Location header to be sent, redirecting the client browser.
     *
     * @param location The URL to which to send the browser.
     */
    void redirect(String location);

    /**
     * Retrieves a cookie from the client browser.
     * @param name the name of the cookie.
     * @return the value as a String, or "" if not found.
     */
    String getCookie(String name);


    /**
     * Adds a header to the HTTP response.
     * @param name the header name
     * @param value the header value
     */
    void addHeader(String name, String value);

}
