package com.matthewn4444.lifx.remote;

import com.matthewn4444.lifx.HSBKColor;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class LIFXBulb {

    public enum Status { OK, TIMED_OUT, OFFLINE, UNKNOWN };

    public static class Product {
        public final String name;
        public final String identifier;
        public final String company;
        public final Map<String, Boolean> capabilities;

        Product(JSONObject data) throws JSONException {
            name = data.getString("name");
            identifier = data.getString("identifier");
            company = data.getString("company");
            JSONObject obj = data.getJSONObject("capabilities");
            capabilities = new HashMap<>();
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                capabilities.put(key, obj.getBoolean(key));
            }
        }
    }

    // Light response
    private String mId;
    private String mLabel;
    private Status mStatus;

    // State response
    public final String uuid;
    public final boolean connected;
    public final String[] group;
    public final String[] location;
    public final Product product;
    public final Calendar lastSeen;
    public final double secLastSeen;

    private float mBrightness;
    private int mPowerState;
    private HSBKColor mColor;

    LIFXBulb(JSONObject data) throws JSONException, ParseException {
        // Basic response
        mId = data.getString("id");
        mLabel = data.getString("label");
        if (data.has("status")) {
            String s = data.getString("status");
            if (s.equals("ok")) {
                mStatus = Status.OK;
            } else if (s.equals("timed_out")) {
                mStatus = Status.TIMED_OUT;
            } else if (s.equals("offline")) {
                mStatus = Status.OFFLINE;
            } else {
                throw new IllegalStateException("Unknown state: " + s);
            }
        } else {
            mStatus = Status.UNKNOWN;
        }

        // State response when calling list of lights
        if (data.has("uuid")) {
            uuid = data.getString("uuid");
            connected = data.getBoolean("connected");
            mBrightness = (float) data.getDouble("brightness");
            mPowerState = data.getString("power").equals("on") ? LIFXState.PowerOn : LIFXState.PowerOff;
            JSONObject groupJson = data.getJSONObject("group");
            JSONObject locationJson = data.getJSONObject("location");
            group = new String[] { groupJson.getString("id"), groupJson.getString("name") };
            location = new String[] { locationJson.getString("id"), locationJson.getString("name") };
            product = new Product(data.getJSONObject("product"));
            mColor = new HSBKColor(data.getJSONObject("color"));
            mColor.setBrightness((float) mBrightness);
            lastSeen = parseTime(data.getString("last_seen"));
            secLastSeen = -1;
        } else {
            mPowerState = LIFXState.PowerOff;
            uuid = null;
            connected = false;
            mBrightness = -1;
            mColor = null;
            group = null;
            location = null;
            product = null;
            lastSeen = null;
            secLastSeen = -1;
        }
    }

    public boolean isOn() {
        return mPowerState == LIFXState.PowerOn;
    }

    public String id() {
        return mId;
    }

    public float brightness() {
        return mBrightness;
    }

    public String label() {
        return mLabel;
    }

    public Status status() {
        return mStatus;
    }

    public HSBKColor color() {
        return mColor;
    }

    void updateState(LIFXState state, String id, String label, Status status) {
        int power = state.powerState;
        HSBKColor color = state.color;
        float brightness = state.brightness;
        if (power != LIFXState.PowerNoChange) {
            mPowerState = power;
        }
        if (color != null) {
            mColor = color;
        }
        if (brightness != LIFXState.BrightnessNoChange) {
            mBrightness = brightness;
        }
        mId = id;
        mLabel = label;
        if (status != Status.UNKNOWN) {
            mStatus = status;
        }
    }

    private Calendar parseTime(String text) throws ParseException {
        text = text.substring(0, text.length() - 6);        // TEMP until move to android?
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        c.setTime(sdf.parse(text));
        return c;
    }
}
