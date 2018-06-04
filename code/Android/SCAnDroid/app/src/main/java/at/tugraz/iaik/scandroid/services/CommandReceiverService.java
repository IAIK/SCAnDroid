// The following code is based on the ProcHarvester implementation
// See https://github.com/IAIK/ProcHarvester/blob/master/code/ProcHarvesterApp/app/src/main/java/com/procharvester/Activities/CommandReceiveActivity.java

package at.tugraz.iaik.scandroid.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import at.tugraz.iaik.scandroid.Config;

public class CommandReceiverService extends IntentService {
    private static final String TAG = CommandReceiverService.class.getName();

    public CommandReceiverService() {
        super("CommandReceiverService");
    }

    /**
     * Do not change these constants without changing the python adb scripts!
     **/
    private static final String COMMAND_KEY = "CMD";
    private static final String ARG_KEY = "ARG";
    private static final String CMD_START_LOGGING = "START_LOGGING";
    private static final String CMD_STOP_LOGGING = "STOP_LOGGING";
    private static final String CMD_TRIGGER_EVENT = "TRIGGER_EVENT";

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle bundle = intent.getExtras();
        String command = null;
        String arg = null;
        if (bundle != null) {
            command = bundle.getString(COMMAND_KEY);
            arg = bundle.getString(ARG_KEY);
        }

        Log.v(TAG, "HANDLING COMMAND " + command);

        if (command == null) {
            Log.e(TAG, "No command received");
        } else {
            handleCommand(command, arg);
        }
    }

    private void triggerNewTargetName(String newTargetName) {
        Log.v(TAG, "Set new target name!");
        APIHarvestingService recordService = APIHarvestingService.getInstance();
        if (recordService != null) {
            Log.v(TAG, "New target name set!");
            recordService.setNewTargetName(newTargetName);
        } else {
            throw new RuntimeException(Config.LOGCAT_RUNTIME_ERROR + "Init not done yet! Did something go wrong?");
        }
    }

    private void handleCommand(String command, String args) {
        Log.i(TAG, "HANDLING COMMAND " + command);
        switch (command) {
            case CMD_START_LOGGING:
                startLogging();
                break;
            case CMD_STOP_LOGGING:
                stopLogging();
                break;
            case CMD_TRIGGER_EVENT:
                triggerNewTargetName(args);
                break;
            default:
                Log.e(TAG, "COMMAND NOT FOUND");
        }
    }

    private void stopLogging() {
        Log.i(TAG, "Stopping logging");
        APIHarvestingService instance = APIHarvestingService.getInstance();
        if (instance != null) {
            instance.stopLogging();
        } else {
            Log.e(TAG, "NOT YET STARTED");
        }
    }

    private void startLogging() {
        APIHarvestingService instance = APIHarvestingService.getInstance();
        if (instance == null) {
            APIHarvestingService.startAPIHarvestingService(getBaseContext());
        }
    }
}
