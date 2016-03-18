# LIFX-SDK (0.1.0) [Java]
This is a library project is used to communicate to the [LIFX](http://www.lifx.com/)
lightbulbs. For now this library only supports remote usage which means you need
your token. Note that all commands will require internet and will go to their
server in order to communicate to your lightbulbs. The source will work for
Android or normal Java.

Currently the remote access is not fully implemented but the basics are there.

You can read the documentation for the remote access here:
[http://api.developer.lifx.com/docs/introduction](http://api.developer.lifx.com/docs/introduction)

## This over the old one

Currently this was quickly written to replace the old sdk that LIFX made for
Android, because it was been depreciated I wanted a better library so I wrote this.
Eventually this will have access to lightbulbs over the local router but for now
I have not added it to the library.

## Sample Code

    // Create the remote class and start it
    LIFXRemote remote = new LIFXRemote("my token goes here");
    remote.setListener(this);           // For command callbacks
    remote.start();                     // Start the event loop, get the original state of all lights

    remote.turnAllOff();                // Turn off all the lights

    // Do more commands later


    <later...>



    @Override
    public void onRemoteCommandFinished(int command, LIFXRemoteResponse response) {
        if (response.warnings != null) {
            for (LIFXRemoteResponse.Warning warning: response.warnings) {
                Log.w(TAG, warning.message);
            }
        }

        // Get data from all the bulbs
        List<LIFXBulb> bulbs = mRemote.getAllBulb();

        if (command != LIFXCommand.ListLights) {
            // Get the data of what we just changed
            LIFXRemoteResponse.Operation[] operations = response.operations;
            for (LIFXRemoteResponse.Operation operation: operations) {
                LIFXBulb[] affectedBulbs = operation.getBulbs();
                LIFXState state = operation.state;

                // Do stuff with it
            }
        }
    }

    @Override
    public void onLIFXError(LIFXResponseException e) {
        log("Error", e.getMessage());
        e.printStackTrace();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        remote.destroy();
    }

## Integration with an Android Studio project

1. You can clone the project in the root of your project here:
    ``https://github.com/matthewn4444/lifx-sdk.git``

    Or add it as a submodule
    ``git submodule add https://github.com/matthewn4444/lifx-sdk.git``


2. In your project's **settings.gradle**:
    ``include ':lifx-sdk'``

3. In your app's **build.gradle** add this line to dependencies:
    ``compile project(':lifx-sdk')``

4. You are now ready to use this library

## Future Work/TODO

- Finish the remote api
- Add local lan support
- Make the sdk easier and intuitive to use
