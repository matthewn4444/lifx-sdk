package com.matthewn4444.lifx.remote;

import android.util.Log;

import com.matthewn4444.lifx.HSBKColor;

import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class LIFXRemote {
    private static final String TAG = "LIFXRemote";

    /**
     * Listener is to get callbacks for commands and any other errors that may occur
     */
    public interface OnRemoteCommandFinishedListener {
        void onRemoteCommandFinished(int command, LIFXRemoteResponse response);
        void onLIFXError(LIFXResponseException e);
    }

    private final String mAppToken;
    private final LinkedBlockingDeque<LIFXCommand> mCommandQueue;
    private final List<LIFXBulb> mBulbs;

    private final Runnable mThreadLoop = new Runnable() {
        @Override
        public void run() {
            while (!(Thread.currentThread().isInterrupted())) {
                try {
                    LIFXCommand commandObj = mCommandQueue.poll(5, TimeUnit.MINUTES);

                    if (commandObj == null) {
                        // Poll timeout, we will go get the light information
                        requestUpdateAllBulbs();
                        continue;
                    }
                    LIFXRemoteResponse res = commandObj.request(mAppToken);
                    if (mListener != null) {
                        if (res == null) {
                            mListener.onLIFXError(new LIFXResponseException("Error in sending request"));
                        } else if (res.errorMessage != null) {
                            mListener.onLIFXError(new LIFXResponseException(res.errorMessage, res.responseCode, res.errors));
                        } else {
                            updateBulbs(commandObj, res);
                            mListener.onRemoteCommandFinished(commandObj.command, res);
                        }
                    }
                } catch (InterruptedException ignored) {
                    break;
                } catch (JSONException | ParseException | IOException e) {
                    if (mListener != null) {
                        LIFXResponseException ex = new LIFXResponseException(e.getMessage());
                        ex.setStackTrace(e.getStackTrace());
                        mListener.onLIFXError(ex);
                    }
                } catch (LIFXResponseException e) {
                    if (mListener != null) {
                        mListener.onLIFXError(e);
                    }
                }
            }
            mThread = null;
        }
    };

    private Thread mThread;
    private OnRemoteCommandFinishedListener mListener;

    public LIFXRemote(String appToken) {
        mCommandQueue = new LinkedBlockingDeque<>();
        mBulbs = new ArrayList<>();
        mAppToken = appToken;
    }

    /**
     * Get command callbacks and error messages
     * @param listener callback
     */
    public void setListener(OnRemoteCommandFinishedListener listener) {
        mListener = listener;
    }

    /**
     * Start the lightbulb thread and get the all the lightbulbs related to your token
     * Listen for when the callback happens for the first connect to occur
     */
    public void start() {
        if (mThread == null) {
            mThread = new Thread(mThreadLoop);
            mThread.start();
            listAllLights();
        }
    }

    /**
     * Kill the request thread and clean up this file.
     * You can reuse this object by calling start again
     */
    public void destroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
        mCommandQueue.clear();
        mBulbs.clear();
    }

    /**
     * See if the thread in this class is running or not
     * @return is currently running
     */
    public boolean isRunning() {
        return mThread != null;
    }

    /**
     * Request a poll to get all the information on all the lights
     */
    public void listAllLights() {
        listLights(LIFXState.SelectorAll);
    }

    /**
     * Light only the lights that you selecting for. You could also just query the lights
     * in the cache by using getAllBulbs() and searching for the selector
     * @param selector of which lights to turn off
     */
    public void listLights(String selector) {
        mCommandQueue.add(new LIFXCommand(LIFXCommand.ListLights, new LIFXState[]{
                new LIFXState(selector, LIFXState.PowerNoChange, null,
                        LIFXState.BrightnessNoChange, LIFXState.DefaultDuration)
        }));
    }

    /**
     * Turn off all your lights
     */
    public void turnAllOff() {
        turnAllOff(LIFXState.DefaultDuration);
    }

    /**
     * Turn off all the lights with duration
     * @param duration time in ms
     */
    public void turnAllOff(long duration) {
        turnOff(LIFXState.SelectorAll, duration);
    }

    /**
     * Turn off a subsection of your lights
     * @param selector of which lights to turn off
     */
    public void turnOff(String selector) {
        turnOff(selector, LIFXState.DefaultDuration);
    }

    /**
     * Turn off a subsection of your lights with duration
     * @param selector of which lights to turn off
     * @param duration time in ms
     */
    public void turnOff(String selector, long duration) {
        setState(selector, LIFXState.PowerOff, null, LIFXState.BrightnessNoChange, duration);
    }

    /**
     * Turn on all your lights
     */
    public void turnAllOn() {
        turnAllOn(LIFXState.DefaultDuration);
    }

    /**
     * Turn on all the lights with duration
     * @param duration time in ms
     */
    public void turnAllOn(long duration) {
        turnOn(LIFXState.SelectorAll, duration);
    }

    /**
     * Turn onb a subsection of your lights
     * @param selector of which lights to turn on
     */
    public void turnOn(String selector) {
        turnOn(selector, LIFXState.DefaultDuration);
    }

    /**
     * Turn on a subsection of your lights with duration
     * @param selector of which lights to turn on
     * @param duration time in ms
     */
    public void turnOn(String selector, long duration) {
        setState(selector, LIFXState.PowerOn, null, LIFXState.BrightnessNoChange, duration);
    }

    /**
     * Set the brightness of all your lightbulbs
     * @param brightness level between 0.0 to 1.0
     * @param duration time in ms
     */
    public void setAllBrightness(float brightness, long duration) {
        setState(LIFXState.SelectorAll, LIFXState.PowerNoChange, null, brightness, duration);
    }

    /**
     * Set the brightness of a subset of your lightbulbs
     * @param selector of which your lights to set brightness
     * @param brightness level between 0.0 to 1.0
     */
    public void setBrightness(String selector, float brightness) {
        setState(selector, LIFXState.PowerNoChange, null, brightness, 1000);
    }

    /**
     * Set the brightness of a subset of your lightbulbs with duration
     * @param selector of which your lights to set brightness
     * @param brightness level between 0.0 to 1.0
     * @param duration time in ms
     */
    public void setBrightness(String selector, float brightness, long duration) {
        setState(selector, LIFXState.PowerNoChange, null, brightness, duration);
    }

    /**
     * Set the multiple states using multiple selectors at one time
     * @param states collection of state objects
     */
    public void setStates(Collection<LIFXState> states) {
        if (states == null || states.isEmpty()) {
            Log.w(TAG, "States is either null or empty, operation is ignored");
            if (mListener != null) {
                mListener.onLIFXError(new LIFXResponseException("setStates() argument states is either empty or null, operation cannot be conducted"));
            }
        } else if (states.size() > LIFXState.MaxStateSize) {
            Log.w(TAG, "Cannot set more than " + LIFXState.MaxStateSize + " states, operation is ignored");
            if (mListener != null) {
                mListener.onLIFXError(new LIFXResponseException("setStates() argument states is either empty or null, operation cannot be conducted"));
            }
        } else {
            mCommandQueue.add(new LIFXCommand(LIFXCommand.SetStates, states.toArray(new LIFXState[states.size()])));
        }
    }

    /**
     * Set the multiple states using multiple selectors at one time
     * @param states array of state objects
     */
    public void setStates(LIFXState[] states) {
        if (states == null || states.length == 0) {
            Log.w(TAG, "States is either null or empty, operation is ignored");
            if (mListener != null) {
                mListener.onLIFXError(new LIFXResponseException("setStates() argument states is either empty or null, operation cannot be conducted"));
            }
        } else if (states.length > LIFXState.MaxStateSize) {
            Log.w(TAG, "Cannot set more than " + LIFXState.MaxStateSize + " states, operation is ignored");
            if (mListener != null) {
                mListener.onLIFXError(new LIFXResponseException("setStates() argument states is either empty or null, operation cannot be conducted"));
            }
        } else {
            mCommandQueue.add(new LIFXCommand(LIFXCommand.SetStates, states));
        }
    }

    /**
     * Set all lightbulbs with the same state
     * @param powerState on, off or no change
     * @param color HSBKColor color
     * @param brightness level between 0.0 to 1.0
     * @param duration time in ms
     */
    public void setAllState(int powerState, HSBKColor color, float brightness, long duration) {
        setState(LIFXState.SelectorAll, powerState, color, brightness, duration);
    }

    /**
     * Set the selected lightbulbs with the same state
     * @param selector of which your lights to set brightness
     * @param powerState on, off or no change
     * @param color HSBKColor color
     * @param brightness level between 0.0 to 1.0
     * @param duration time in ms
     */
    public void setState(String selector, int powerState, HSBKColor color, float brightness, long duration) {
        mCommandQueue.add(new LIFXCommand(LIFXCommand.SetState, new LIFXState[]{
                new LIFXState(selector, powerState, color, brightness, duration)
        }));
    }

    /**
     * Toggle all lightbulbs from on to off and vice versa
     */
    public void togglePower() {
        togglePower(LIFXState.SelectorAll);
    }

    /**
     * Toggle a selected subset of lightbulbs from on to off and vice versa
     * @param selector of which your lights to set brightness
     */
    public void togglePower(String selector) {
        togglePower(selector, 0);
    }

    /**
     * Toggle all lights from on to off and vice versa with duration
     * @param duration time in ms
     */
    public void togglePower(long duration) {
        togglePower(LIFXState.SelectorAll, duration);
    }

    /**
     * Toggle a selected subset of lightbulbs on to off and vice versa with duration
     * @param selector of which your lights to set brightness
     * @param duration time in ms
     */
    public void togglePower(String selector, long duration) {
        mCommandQueue.add(new LIFXCommand(LIFXCommand.TogglePower, new LIFXState[]{
                new LIFXState(selector, LIFXState.PowerNoChange, null,
                        LIFXState.BrightnessNoChange, duration)
        }));
    }

    /**
     * Get the cache state of all lightbulbs
     * @return cache lightbulbs state
     */
    public List<LIFXBulb> getAllBulbs() {
        return mBulbs;
    }

    private void requestUpdateAllBulbs() throws LIFXResponseException, JSONException, IOException, ParseException {
        LIFXCommand cmd = new LIFXCommand(LIFXCommand.ListLights, new LIFXState[]{
                new LIFXState(LIFXState.SelectorAll, LIFXState.PowerNoChange, null,
                        LIFXState.BrightnessNoChange, LIFXState.DefaultDuration)
        });
        LIFXRemoteResponse response = cmd.request(mAppToken);
        if (response != null && response.errorMessage == null) {
            updateBulbs(cmd, response);
        }
    }

    private void updateBulbs(LIFXCommand commandObj, LIFXRemoteResponse res) throws ParseException, LIFXResponseException, JSONException, IOException {
        if (commandObj.command == LIFXCommand.ListLights && commandObj.states[0].selector.equalsIgnoreCase(LIFXState.SelectorAll)) {
            // Update our list of bulbs mirroring whatever is online
            mBulbs.clear();
            Collections.addAll(mBulbs, res.operations[0].getBulbs());
        } else {
            for (LIFXRemoteResponse.Operation operation: res.operations) {
                updateCachedBulbsWithResponseBulbsAndState(operation.mResults, operation.state);
            }
        }
    }

    private void updateCachedBulbsWithResponseBulbsAndState(LIFXBulb[] resBulbs, LIFXState state)
            throws ParseException, LIFXResponseException, JSONException, IOException {
        for (int i = 0; i < resBulbs.length; i++) {
            LIFXBulb statusBulb = resBulbs[i];
            boolean found = false;
            for (LIFXBulb savedBulb: mBulbs) {
                if (savedBulb.id().equals(statusBulb.id())) {
                    // Update all the information about the bulbs
                    savedBulb.updateState(state, statusBulb.id(), statusBulb.label(), statusBulb.status());

                    // Give the response back with all the data of the bulb
                    resBulbs[i] = savedBulb;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Nope we have different bulbs, list lights again
                requestUpdateAllBulbs();
                return;
            }
        }
    }
}
