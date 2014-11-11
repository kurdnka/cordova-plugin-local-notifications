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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;

/**
 * Class that helps to store the options that can be specified per alarm.
 */
public class Options {

    private JSONObject options = new JSONObject();
    private String packageName = null;

    Options (Context context) {
        packageName = context.getPackageName();
    }

    public Options parse (JSONObject options)
    {
        this.options = options;
        return this;
    }

    /**
     * Returns options as JSON object
     */
    public JSONObject getJSONObject() {
        return options;
    }

    /**
     * Returns messages as List<JSONObject>
     */
    public List<JSONObject> getMessages() {
        JSONArray array = options.optJSONArray("messages");
        List<JSONObject> messages = new ArrayList<JSONObject>();
        if (array == null)
            return messages;

        for (int i = 0; i < messages.size(); i++)
        {
            messages.add(array.optJSONObject(i));
        }
        return messages;
    }

    /**
     * Returns the notification's message
     */
    public String getSummary () {
        return options.optString("summary", "");
    }

    /**
     * Returns the notification's title
     */
    public String getTitle () {
        return options.optString("title", "");
    }

    /**
     * Returns the icon's ID
     */
    public Bitmap getIcon () {
        String icon = options.optString("icon", "icon");
        Bitmap bmp = null;

        if (icon.startsWith("http")) {
            bmp = getIconFromURL(icon);
        } else if (icon.startsWith("file://")) {
            bmp = getIconFromURI(icon);
        }

        if (bmp == null) {
            bmp = getIconFromRes(icon);
        }

        return bmp;
    }

    /**
     * Returns the small icon's ID
     */
    public int getSmallIcon () {
        int resId;
        String iconName = options.optString("smallIcon", "");

        resId = getIconValue(packageName, iconName);

        if (resId == 0) {
            resId = getIconValue("android", iconName);
        }

        if (resId == 0) {
            resId = getIconValue(packageName, "icon");
        }

        return options.optInt("smallIcon", resId);
    }

    /**
     * Returns notification badge number
     */
    public int getBadge () {
        return options.optInt("badge", 0);
    }

    /**
     * Returns PluginResults' callback ID
     */
    public String getTag () {
        return options.optString("tag", "messages");
    }

    /**
     * Returns additional data as string
     */
    public String getJSON () {
        return options.optString("json", "");
    }

    /**
     * Returns numerical icon Value
     */
    private int getIconValue (String className, String iconName) {
        int icon = 0;

        try {
            Class<?> klass  = Class.forName(className + ".R$drawable");

            icon = (Integer) klass.getDeclaredField(iconName).get(Integer.class);
        } catch (Exception ignored) {}

        return icon;
    }

    /**
     * Converts an resource to Bitmap.
     *
     * @param icon
     *      The resource name
     * @return
     *      The corresponding bitmap
     */
    private Bitmap getIconFromRes (String icon) {
        Resources res = MessengerNotification.context.getResources();
        int iconId;

        iconId = getIconValue(packageName, icon);

        if (iconId == 0) {
            iconId = getIconValue("android", icon);
        }

        if (iconId == 0) {
            iconId = android.R.drawable.ic_menu_info_details;
        }

        return BitmapFactory.decodeResource(res, iconId);
    }

    /**
     * Converts an Image URL to Bitmap.
     *
     * @param src
     *      The external image URL
     * @return
     *      The corresponding bitmap
     */
    private Bitmap getIconFromURL (String src) {
        Bitmap bmp = null;
        ThreadPolicy origMode = StrictMode.getThreadPolicy();

        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            ThreadPolicy policy =
                    new ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);

            connection.setDoInput(true);
            connection.connect();

            InputStream input = connection.getInputStream();

            bmp = BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrictMode.setThreadPolicy(origMode);

        return bmp;
    }

    /**
     * Converts an Image URI to Bitmap.
     *
     * @param src
     *      The internal image URI
     * @return
     *      The corresponding bitmap
     */
    private Bitmap getIconFromURI (String src) {
        AssetManager assets = MessengerNotification.context.getAssets();
        Bitmap bmp = null;

        try {
            String path = src.replace("file:/", "www");
            InputStream input = assets.open(path);

            bmp = BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bmp;
    }
}
