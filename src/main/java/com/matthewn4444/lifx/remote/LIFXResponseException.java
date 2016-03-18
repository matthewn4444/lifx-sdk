package com.matthewn4444.lifx.remote;

import com.matthewn4444.lifx.LIFXException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LIFXResponseException extends LIFXException {

    public static class ErrorField {
        public final String field;
        public final String[] messages;

        ErrorField(JSONObject data) throws JSONException {
            field = data.getString("field");
            if (data.has("message")) {
                JSONArray array = data.getJSONArray("message");
                messages = new String[array.length()];
                for (int i = 0; i < array.length(); i++) {
                    messages[i] = array.getString(i);
                }
            } else {
                messages = null;
            }
        }
    }

    private final int mResponseCode;
    private final ErrorField[] mFields;

    public LIFXResponseException(String s) {
        this(s, 0, null);
    }

    public LIFXResponseException(String s, int responseCode) {
        this(s, responseCode, null);
    }

    public LIFXResponseException(String s, int responseCode, ErrorField[] fields) {
        super(s);
        mResponseCode = responseCode;
        mFields = fields;
    }

    public int getErrorResponseCode() {
        return mResponseCode;
    }

    public ErrorField[] getErrorFields() {
        return mFields;
    }
}
