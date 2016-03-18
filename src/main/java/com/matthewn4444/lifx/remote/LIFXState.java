package com.matthewn4444.lifx.remote;

import com.matthewn4444.lifx.HSBKColor;

import org.json.JSONException;
import org.json.JSONObject;

public class LIFXState {
    public static final int MaxStateSize = 50;

    public static final int PowerNoChange = 0;
    public static final int PowerOn = 1;
    public static final int PowerOff = 2;
    public static final long DefaultDuration = 1000;

    public static final float BrightnessNoChange = -1;
    public static final String SelectorAll = "all";

    public final String selector;
    public final int powerState;
    public final HSBKColor color;
    public final float brightness;
    public final long duration;

    public LIFXState(String selector, int powerState, HSBKColor color, float brightness, long duration) {
        this.selector = selector;
        this.powerState = powerState;
        this.color = color;
        this.brightness = brightness;
        this.duration = duration;
    }

    static LIFXState fromJson(JSONObject json) throws JSONException {
        String selector = json.has("selector") ? json.getString("selector") : null;
        int power = PowerNoChange;
        HSBKColor color = null;
        float brightness = BrightnessNoChange;
        long duration = DefaultDuration;

        if (json.has("power")) {
            power = json.getString("power").equals("on") ? LIFXState.PowerOn : LIFXState.PowerOff;
        }
        if (json.has("color")) {
            color = HSBKColor.fromFormattedString(json.getString("selector"));
        }
        if (json.has("brightness")) {
            brightness = (float) json.getDouble("brightness");
        }
        if (json.has("duration")) {
            duration = (int)(json.getDouble("duration") * 1000);
        }
        return new LIFXState(selector, power, color, brightness, duration);
    }

    JSONObject formatJson(boolean showSelector) throws JSONException {
        JSONObject data = new JSONObject();
        if (powerState == PowerOn) {
            data.put("power", "on");
        } else if (powerState == PowerOff) {
            data.put("power", "off");
        }
        data.put("duration", (double) duration / 1000f);
        if (color != null) {
            data.put("color", color.toString());
        }
        if (brightness != BrightnessNoChange) {
            data.put("brightness", brightness);
        }
        if (showSelector) {
            data.put("selector", selector);
        }
        return data;
    }
}