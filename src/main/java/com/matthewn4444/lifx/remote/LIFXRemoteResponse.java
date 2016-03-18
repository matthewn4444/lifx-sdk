package com.matthewn4444.lifx.remote;

import com.matthewn4444.lifx.remote.LIFXResponseException.ErrorField;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LIFXRemoteResponse {

    public static class Warning {
        public final String message;
        public final Map<String, String> unknownParameters;

        Warning(JSONObject data) throws JSONException {
            message = data.getString("warning");
            if (data.has("unknown_params")) {
                unknownParameters = new HashMap<>();
                JSONObject json = data.getJSONObject("unknown_params");
                Iterator<String> it = json.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    unknownParameters.put(key, json.getString(key));
                }
            } else {
                unknownParameters = null;
            }
        }
    }

    public static class Operation {
        public final LIFXState state;
        LIFXBulb[] mResults;

        Operation(LIFXState state, LIFXBulb[] bulbs) {
            this.state = state;
            mResults = bulbs;
        }

        Operation(JSONObject data) throws JSONException, ParseException {
            JSONObject operationJson = data.getJSONObject("operation");
            state = LIFXState.fromJson(operationJson);
            if (data.has("results")) {
                JSONArray arr = new JSONArray();
                mResults = new LIFXBulb[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    mResults[i] = new LIFXBulb(arr.getJSONObject(i));
                }
            } else {
                mResults = null;
            }
        }

        public LIFXBulb[] getBulbs() {
            return mResults;
        }
    }

    public final int responseCode;

    // Any Errors
    final String errorMessage;
    final ErrorField[] errors;

    // Any Warnings
    public final Warning[] warnings;

    // Operation for states
    public final Operation[] operations;

    public LIFXRemoteResponse(JSONObject json, int responseCode, LIFXState state) throws JSONException, ParseException {
        this.responseCode = responseCode;
        if (json.has("error")) {
            // Error state
            warnings = null;
            operations = null;
            errorMessage = json.getString("error");
            if (json.has("errors")) {
                JSONArray errorsJson = json.getJSONArray("errors");
                errors = new ErrorField[errorsJson.length()];
                for (int i = 0; i < errorsJson.length(); i++) {
                    errors[i] = new ErrorField(errorsJson.getJSONObject(i));
                }
            } else {
                errors = null;
            }
        } else {
            errorMessage = null;
            errors = null;

            if (json.has("warnings")) {
                // Has a warning
                JSONArray warningsJson = json.getJSONArray("warnings");
                warnings = new Warning[warningsJson.length()];
                for (int i = 0; i < warningsJson.length(); i++) {
                    warnings[i] = new Warning(warningsJson.getJSONObject(i));
                }
            } else {
                warnings = null;
            }

            JSONArray results = json.getJSONArray("results");
            LIFXBulb[] bulbs = new LIFXBulb[results.length()];
            for (int i = 0; i < results.length(); i++) {
                bulbs[i] = new LIFXBulb(results.getJSONObject(i));
            }
            operations = new Operation[] {
                new Operation(state, bulbs)
            };

        }
    }

    public LIFXRemoteResponse(JSONArray json, int responseCode, LIFXState state) throws JSONException, ParseException {
        this.responseCode = responseCode;

        // List array
        errorMessage = null;
        errors = null;
        warnings = null;

        if (json.toString().contains("operation")) {
            operations = new Operation[json.length()];
            for (int i = 0; i < json.length(); i++) {
                operations[i] = new Operation(json.getJSONObject(i));
            }
        } else {
            LIFXBulb[] bulbs = new LIFXBulb[json.length()];
            for (int i = 0; i < json.length(); i++) {
                bulbs[i] = new LIFXBulb(json.getJSONObject(i));
            }
            operations = new Operation[] {
                    new Operation(state, bulbs)
            };
        }
    }
}
