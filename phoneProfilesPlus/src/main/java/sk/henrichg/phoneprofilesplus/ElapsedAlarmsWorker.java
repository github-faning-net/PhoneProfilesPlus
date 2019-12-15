package sk.henrichg.phoneprofilesplus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

@SuppressWarnings("WeakerAccess")
public class ElapsedAlarmsWorker extends Worker {

    static final String ELAPSED_ALARMS_GEOFENCE_SCANNER_SWITCH_GPS = "geofence_scanner_swith_gps";
    static final String ELAPSED_ALARMS_LOCK_DEVICE_FINISH_ACTIVITY = "lock_device_finish_activity";
    static final String ELAPSED_ALARMS_LOCK_DEVICE_AFTER_SCREEN_OFF = "lock_device_after_screen_off";
    static final String ELAPSED_ALARMS_START_EVENT_NOTIFICATION = "start_event_notification";
    static final String ELAPSED_ALARMS_RUN_APPLICATION_WITH_DELAY = "run_application_with_delay";
    static final String ELAPSED_ALARMS_PROFILE_DURATION = "profile_duration";
    static final String ELAPSED_ALARMS_EVENT_DELAY_START = "event_delay_start";
    static final String ELAPSED_ALARMS_EVENT_DELAY_END = "event_delay_end";
    static final String ELAPSED_ALARMS_DONATION = "donation";
    static final String ELAPSED_ALARMS_TWILIGHT_SCANNER = "twilight_scanner";

    final Context context;

    public ElapsedAlarmsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        PPApplication.logE("ElapsedAlarmsWorker.doWork", "xxx");

        //Data outputData;

        // Get the input
        String action = getInputData().getString(PhoneProfilesService.EXTRA_ELAPSED_ALARMS_WORK);
        if (action == null) {
            PPApplication.logE("ElapsedAlarmsWorker.doWork", "action ins null");
            return Result.success();
        }

        PPApplication.logE("ElapsedAlarmsWorker.doWork", "action="+action);

        long eventId = getInputData().getLong(PPApplication.EXTRA_EVENT_ID, 0);
        String runApplicationData = getInputData().getString(RunApplicationWithDelayBroadcastReceiver.EXTRA_RUN_APPLICATION_DATA);
        long profileId = getInputData().getLong(PPApplication.EXTRA_PROFILE_ID, 0);
        boolean forRestartEvents = getInputData().getBoolean(ProfileDurationAlarmBroadcastReceiver.EXTRA_FOR_RESTART_EVENTS, false);
        int startupSource = getInputData().getInt(PPApplication.EXTRA_STARTUP_SOURCE, PPApplication.STARTUP_SOURCE_SERVICE_MANUAL);

        //outputData = generateResult(LocationGeofenceEditorActivity.FAILURE_RESULT,
        //                                    getApplicationContext().getString(R.string.event_preferences_location_no_address_found),
        //                                    updateName);

        //return Result.success(outputData);

        Context appContext = context.getApplicationContext();

        switch (action) {
            case ELAPSED_ALARMS_GEOFENCE_SCANNER_SWITCH_GPS:
                GeofencesScannerSwitchGPSBroadcastReceiver.doWork();
                break;
            case ELAPSED_ALARMS_LOCK_DEVICE_FINISH_ACTIVITY:
                LockDeviceActivityFinishBroadcastReceiver.doWork();
                break;
            case ELAPSED_ALARMS_LOCK_DEVICE_AFTER_SCREEN_OFF:
                LockDeviceAfterScreenOffBroadcastReceiver.doWork(false, appContext);
                break;
            case ELAPSED_ALARMS_START_EVENT_NOTIFICATION:
                StartEventNotificationBroadcastReceiver.doWork(false, appContext, eventId);
                break;
            case ELAPSED_ALARMS_RUN_APPLICATION_WITH_DELAY:
                RunApplicationWithDelayBroadcastReceiver.doWork(appContext, runApplicationData);
                break;
            case ELAPSED_ALARMS_PROFILE_DURATION:
                ProfileDurationAlarmBroadcastReceiver.doWork(false, appContext, profileId, forRestartEvents, startupSource);
                break;
            case ELAPSED_ALARMS_EVENT_DELAY_START:
                EventDelayStartBroadcastReceiver.doWork(false, appContext);
                break;
            case ELAPSED_ALARMS_EVENT_DELAY_END:
                EventDelayEndBroadcastReceiver.doWork(false, appContext);
                break;
            case ELAPSED_ALARMS_DONATION:
                DonationBroadcastReceiver.doWork(false, appContext);
                break;
            case ELAPSED_ALARMS_TWILIGHT_SCANNER:
                TwilightScanner.doWork();
                break;
            default:
                break;
        }

        return Result.success();
    }

    /*
    private Data generateResult(int resultCode, String message, boolean updateName) {
        // Create the output of the work
        PPApplication.logE("FetchAddressWorker.generateResult", "resultCode="+resultCode);
        PPApplication.logE("FetchAddressWorker.generateResult", "message="+message);
        PPApplication.logE("FetchAddressWorker.generateResult", "updateName="+updateName);

        return new Data.Builder()
                .putInt(LocationGeofenceEditorActivity.RESULT_CODE, resultCode)
                .putString(LocationGeofenceEditorActivity.RESULT_DATA_KEY, message)
                .putBoolean(LocationGeofenceEditorActivity.UPDATE_NAME_EXTRA, updateName)
                .build();
    }
    */

}
