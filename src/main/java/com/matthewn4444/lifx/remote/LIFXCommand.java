package com.matthewn4444.lifx.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.ParseException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class LIFXCommand {
    private static final String ApiUrl = "https://api.lifx.com/v1/lights/";

    public static final int ListLights = 1;
    public static final int SetState = 2;
    public static final int SetStates = 3;
    public static final int TogglePower = 4;

    public final int command;
    public final LIFXState[] states;

    LIFXCommand(int command, LIFXState[] states) {
        this.command = command;
        this.states = states;
    }

    public String url() {
        switch (command) {
            case ListLights:
                return ApiUrl + states[0].selector;
            case SetState:
                return ApiUrl + states[0].selector + "/state";
            case SetStates:
                return ApiUrl + "states";
            case TogglePower:
                return ApiUrl + states[0].selector + "/toggle";
        }
        throw new IllegalStateException("Invalid command getting url: " + command);
    }

    public String buildJsonData() throws JSONException {
        switch (command) {
            case ListLights:
                return "{}";
            case SetState:
                if (states.length == 0) {
                    throw new IllegalStateException("Setting state did not provide any states!");
                }
                return states[0].formatJson(false).toString();
            case SetStates:
                JSONArray array = new JSONArray();
                for (LIFXState s : states) {
                    array.put(s.formatJson(true));
                }
                JSONObject json = new JSONObject();
                json.put("states", array);
                return json.toString();
            case TogglePower:
                if (states.length == 0) {
                    throw new IllegalStateException("Setting state did not provide any states!");
                }
                return "{ \"duration\": \"" + ((double) states[0].duration / 1000f) + "\"}";
        }
        return null;
    }

    private String getAction() {
        switch (command) {
            case ListLights:
                return "get";
            case SetStates:
                return "put";
            case SetState:
                return "put";
            case TogglePower:
                return "post";
        }
        throw new IllegalStateException("Invalid command getting action: " + command);
    }

    public LIFXRemoteResponse request(String token) throws JSONException, LIFXResponseException, IOException, ParseException {
        BufferedReader reader = null;
        try {
            HttpsURLConnection connection;
            URL url = new URL(url());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod(getAction().toUpperCase(Locale.getDefault()));
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("Accept", "*/*");
            if (!getAction().equalsIgnoreCase("get")) {
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(
                        connection.getOutputStream());
                out.write(buildJsonData());
                out.close();
            }
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            // Parse response
            int code = connection.getResponseCode();
            if (200 <= code && code < 500) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String data = sb.toString().trim();
                if (data.charAt(0) == '{' || (command != ListLights && command != SetStates)) {
                    return new LIFXRemoteResponse(new JSONObject(data), code, states[0]);
                } else {
                    return new LIFXRemoteResponse(new JSONArray(data), code, states[0]);
                }
            }
            throw new LIFXResponseException("Server error in '" + getAction() + "' for command " + command, code);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
