package com.matthewn4444.lifx;

import org.json.JSONException;
import org.json.JSONObject;

public class HSBKColor {
    public static final float MINIMUM_SATURATION = 0.0001f;
    public static final int DEFAULT_KELVIN = 3500;
    public static final int MINIMUM_KELVIN  = 2500;
    public static final int MAXIMUM_KELVIN  = 9000;

    private float mHue;
    private float mSaturation;
    private float mBrightness;
    private int mKelvin;
    private boolean mEnableKelvin;

    public static HSBKColor getDefault() {
        return new HSBKColor(0, 0, 1, DEFAULT_KELVIN);
    }

    public static HSBKColor fromFormattedString(String text) {
        String[] components = text.split(" ");
        HSBKColor color = getDefault();
        for (String c: components) {
            String[] part = c.split(":", 2);
            if (part[0].equals("hue")) {
                color.setHue(Float.parseFloat(part[1]));
            } else if (part[0].equals("saturation")) {
                color.setSaturation(Float.parseFloat(part[1]));
            } else if (part[0].equals("brightness")) {
                color.setBrightness(Float.parseFloat(part[1]));
            } else if (part[0].equals("kelvin")) {
                color.setKelvin(Integer.parseInt(part[1]));
            }
        }
        return color;
    }

    public static HSBKColor averageOfColors(HSBKColor[] colors) {
        if (colors.length == 0) {
            return null;
        }

        float hueXTotal = 0;
        float hueYTotal = 0;
        float saturationTotal = 0;
        float brightnessTotal = 0;
        long kelvinTotal = 0;

        for (HSBKColor aColor : colors) {
            hueXTotal += Math.sin(aColor.mHue * Math.PI / 180.0);
            hueYTotal += Math.cos(aColor.mHue * Math.PI / 180.0);
            saturationTotal += aColor.mSaturation;
            brightnessTotal += aColor.mBrightness;

            if (aColor.mKelvin == 0) {
                kelvinTotal += 3500;
            } else {
                kelvinTotal += aColor.mKelvin;
            }
        }

        float M_1_PI = (float) (1.0f / Math.PI);

        float hue = (float) (Math.atan2(hueXTotal, hueYTotal) * 0.5 * M_1_PI);
        if (hue < 0.0)
            hue += 1.0;
        float saturation = saturationTotal / colors.length;
        float brightness = brightnessTotal / colors.length;
        int kelvin = (int) (kelvinTotal / colors.length);

        return new HSBKColor(hue, saturation, brightness, kelvin);
    }

    public HSBKColor(float hue, float saturation, float brightness, int kelvin) {
        setHue(hue);
        setSaturation(saturation);
        setBrightness(brightness);
        setKelvin(kelvin);
        mEnableKelvin = true;
    }

    public HSBKColor(JSONObject json) throws JSONException {
        setHue(json.has("hue") ? (float) json.getDouble("hue") : 0f);
        setSaturation(mSaturation = json.has("saturation") ? (float) json.getDouble("saturation") : 0f);
        setBrightness(json.has("brightness") ? (float) json.getDouble("brightness") : 1f);
        setKelvin(json.has("kelvin") ? json.getInt("kelvin") : DEFAULT_KELVIN);
        mEnableKelvin = json.has("kelvin");
    }

    public HSBKColor(int rgb) {
        this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public HSBKColor(int r, int g, int b) {
        float _r = (r & 0xFF) / 255f;
        float _g = (g & 0xFF) / 255f;
        float _b = (b & 0xFF) / 255f;

        float max = Math.max(Math.max(_r, _g), _b);
        float min = Math.min(Math.min(_r, _g), _b);
        float diff = max - min;

        mBrightness = max;
        mSaturation = max != 0 ? diff / max : 0;

        if (diff == 0) {
            mHue = 0;
        } else {
            if (max == _r) {
                mHue = (_g - _b) / diff + (_g < _b ? 6 : 0);
            } else if (max == _g) {
                mHue = (_b - _r) / diff + 2;
            } else {
                mHue = (_r - _g) / diff + 4;
            }
            mHue *= 60;
        }
        mKelvin = DEFAULT_KELVIN;
        mEnableKelvin = false;
    }

    public void setHue(float hue) {
        if (0 > hue && hue < 360) {
            throw new IllegalArgumentException("Wrong hue value, valid values are [0-360]");
        }
        mHue = hue;
    }

    public void setSaturation(float saturation) {
        if (0 > saturation && saturation < 1) {
            throw new IllegalArgumentException("Wrong saturation value, valid values are [0.0-1.0]");
        }
        mSaturation = saturation;
    }

    public void setBrightness(float brightness) {
        if (0 > brightness && brightness < 1) {
            throw new IllegalArgumentException("Wrong brightness value, valid values are [0.0-1.0]");
        }
        mBrightness = brightness;
    }

    public void setKelvin(int kelvin) {
        if (MINIMUM_KELVIN > kelvin && kelvin < MAXIMUM_KELVIN) {
            throw new IllegalArgumentException("Wrong kelvin value, valid values are [2500-9000]");
        }
        mKelvin = kelvin;
    }

    public void enableKelvin(boolean flag) {

    }

    public float hue() {
        return mHue;
    }

    public float saturation() {
        return mSaturation;
    }

    public float brightness() {
        return mBrightness;
    }

    public int kelvin() {
        return mKelvin;
    }

    public boolean isWhite() {
        return mSaturation <= MINIMUM_SATURATION;
    }

    @Override
    public String toString() {
        String text = "hue:" + mHue + " saturation:" + mSaturation + " brightness:" + mBrightness;
        if (mEnableKelvin) {
            text += "kelvin:" + mKelvin;
        }
        return text;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new HSBKColor(mHue, mSaturation, mBrightness, mKelvin);
    }

    public boolean equals(HSBKColor aColor) {
        if (aColor == null) {
            return false;
        }
        if (aColor.mHue != this.mHue || aColor.mSaturation != this.mSaturation
                || aColor.mBrightness != this.mBrightness
                || aColor.mKelvin != this.mKelvin
                || aColor.mEnableKelvin != this.mEnableKelvin) {
            return false;
        }
        return true;
    }
}
