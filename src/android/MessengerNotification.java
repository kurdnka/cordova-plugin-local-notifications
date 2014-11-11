/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package com.ginasystem.plugins.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MessengerNotification extends CordovaPlugin {

    protected final static String PLUGIN_NAME = "MessengerNotification";
    public static final String OPTIONS = "MESSENGER_NOTIFICATION_OPTIONS";

    private   static CordovaWebView webView = null;
    private   static Boolean deviceready = false;
    protected static Context context = null;
    private   static ArrayList<String> eventQueue = new ArrayList<String>();

    @Override
    public void initialize (CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        MessengerNotification.webView = super.webView;
        MessengerNotification.context = super.cordova.getActivity().getApplicationContext();
    }

    @Override
    public boolean execute (String action, final JSONArray args, final CallbackContext command) throws JSONException {
        if (action.equalsIgnoreCase("add")) {
            cordova.getThreadPool().execute( new Runnable() {
                public void run() {
                    JSONObject arguments = args.optJSONObject(0);
                    Options options      = new Options(context).parse(arguments);

                    add(options);
                    command.success();
                }
            });
        }

        if (action.equalsIgnoreCase("cancel")) {
            cordova.getThreadPool().execute( new Runnable() {
                public void run() {
                    String tag = args.optString(0);

                    cancel(tag);
                    command.success();
                }
            });
        }

        if (action.equalsIgnoreCase("cancelAll")) {
            cordova.getThreadPool().execute( new Runnable() {
                public void run() {
                    cancelAll();
                    command.success();
                }
            });
        }

        if (action.equalsIgnoreCase("init")) {
            cordova.getThreadPool().execute( new Runnable() {
                public void run() {
                    deviceready();
                }
            });
        }

        return true;
    }

    /**
     * Calls all pending callbacks after the deviceready event has been fired.
     */
    private static void deviceready () {
        deviceready = true;

        for (String js : eventQueue) {
            webView.sendJavascript(js);
        }

        eventQueue.clear();
    }

    /**
     * Set an alarm.
     *
     * @param options
     *            The options that can be specified per alarm.
     */
    public void add (Options options) {

        Notification.Builder notification = buildNotification(options);

        showNotification(notification, options);
    }

    /**
     * Converts tag (string) to integer ID using internal hashCode method
     * @param tag notification tag
     * @return notification ID
     */
    private static int tagToId(String tag)
    {
        return tag.hashCode();
    }

    /**
     * Creates the notification.
     */
    @SuppressLint("NewApi")
    private Notification.Builder buildNotification (Options options) {

        Notification.Builder notification = new Notification.Builder(context)
                .setDefaults(0) // Do not inherit any defaults
                .setContentTitle(options.getTitle())
                .setContentText(options.getTicker())
                .setNumber(options.getBadge())
                .setTicker(options.getTicker())
                .setSmallIcon(options.getSmallIcon())
                .setLargeIcon(options.getIcon())
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(Notification.PRIORITY_MAX);
        
        if (options.getMessages().size() > 0)
        {
            Notification.InboxStyle style = new Notification.InboxStyle()
                    .setBigContentTitle(options.getTitle())
                    .setSummaryText(options.getSummary());

            for (JSONObject message : options.getMessages())
            {
                String label = message.optString("label");
                String text = message.optString("text");

                Spannable sb = new SpannableString(label + "  " + text);
                sb.setSpan(
                        new StyleSpan(android.graphics.Typeface.BOLD),
                        0,
                        label.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                style.addLine(sb);
            }
            notification.setStyle(style);
        }

        setClickEvent(notification, options);

        return notification;
    }

    /**
     * Adds an onclick handler to the notification
     */
    private Notification.Builder setClickEvent (Notification.Builder notification, Options options) {
        Intent intent = new Intent(context, ReceiverActivity.class)
                .putExtra(OPTIONS, options.getJSONObject().toString())
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        int requestCode = new Random().nextInt();

        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return notification.setContentIntent(contentIntent);
    }

    /**
     * Shows the notification
     */
    @SuppressWarnings("deprecation")
    private void showNotification (Notification.Builder notification, Options options) {
        int id = tagToId(options.getTag());


        getNotificationManager().notify(id, notification.build());
    }

    /**
     * Cancel a specific notification that was previously registered.
     *
     * @param tag
     *            The original tag used when add() was called
     */
    public static void cancel (String tag) {
        NotificationManager nc = getNotificationManager();

        try {
            nc.cancel(tagToId(tag));
        } catch (Exception ignored) {}
    }

    /**
     * Cancel all notifications that were created by this plugin.
     */
    public static void cancelAll() {
        NotificationManager nc     = getNotificationManager();
        nc.cancelAll();
    }


    /**
     * Fires the given event.
     *
     * @param  tag   The tag of the notification
     */
    public static void fireClickEvent(String tag, String json) {
        String js     = "setTimeout(function () { MessengerNotification.fire(\"click\", \"" + tag + /*"\", \"" + json  + */"\"); }, 1)";

        // webview may available, but callbacks needs to be executed
        // after deviceready
        if (!deviceready) {
            eventQueue.add(js);
        } else {
            webView.sendJavascript(js);
        }
    }

    /**
     * The notification manager for the application.
     */
    protected static NotificationManager getNotificationManager () {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
