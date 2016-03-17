package com.ihealth.devices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ihealth.R;
import com.ihealth.communication.control.Am3sControl;
import com.ihealth.communication.control.AmProfile;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.utils.Connector;
import com.pryv.api.StreamsCallback;
import com.pryv.api.StreamsSupervisor;
import com.pryv.api.model.Stream;

import org.joda.time.field.PreciseDateTimeField;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class AM3S extends Activity {
    private Am3sControl am3sControl;
    private String mac;
    private int clientId;
    private TextView tv_return;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_am3_s);

        Connector.initiateConnection();

        clientId = iHealthDevicesManager.getInstance().registerClientCallback(iHealthDevicesCallback);

        iHealthDevicesManager.getInstance().addCallbackFilterForDeviceType(clientId,
                iHealthDevicesManager.TYPE_AM3S);

        Intent intent = getIntent();
        this.mac = intent.getStringExtra("mac");

        am3sControl = iHealthDevicesManager.getInstance().getAm3sControl(this.mac);

        tv_return = (TextView) findViewById(R.id.tv_return);
    }

    @Override
    protected void onDestroy() {
        reset();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        reset();
        super.onBackPressed();
    }

    private void reset() {
        if (am3sControl != null)
            am3sControl.disconnect();
        iHealthDevicesManager.getInstance().unRegisterClientCallback(clientId);
    }


    private iHealthDevicesCallback iHealthDevicesCallback = new iHealthDevicesCallback() {

        @Override
        public void onScanDevice(String mac, String deviceType) {
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status) {
            if (status == 2) {
                Intent intent = new Intent();
                setResult(Activity.RESULT_CANCELED, intent);
                reset();
                finish();
            }
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {

            switch (action) {
                case AmProfile.ACTION_QUERY_STATE_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String battery = info.getString(AmProfile.QUERY_BATTERY_AM);
                        saveAction("batteryLevel","ratio/percent",battery);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case AmProfile.ACTION_USERID_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String id = info.getString(AmProfile.USERID_AM);
                        saveAction("userID", "count/generic", id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_GET_ALARMNUM_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String alarm_num = info.getString(AmProfile.GET_ALARMNUM_AM);
                        saveAction("alarmNum", "count/generic", alarm_num);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_SYNC_STAGE_DATA_AM:
                    try {
                        //TODO: time, activity with duration
                        JSONObject info = new JSONObject(message);
                        saveAction("syncStage", "note/txt", info.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_SYNC_SLEEP_DATA_AM:
                    try {
                        //TODO: time, activity with duration
                        JSONObject info = new JSONObject(message);
                        JSONArray sleep_info = info.getJSONArray(AmProfile.SYNC_SLEEP_DATA_AM);
                        JSONArray activities = sleep_info.getJSONObject(0).getJSONArray(AmProfile.SYNC_SLEEP_EACH_DATA_AM);

                        tv_return.setText("Sync " + activities.length() + " sleeps...");
                        String streamID = "AM3S_syncSleeps";
                        Stream s = Connector.saveStream(streamID, streamID);

                        for(int i=0; i<activities.length(); i++) {
                            JSONObject activity = activities.getJSONObject(i);
                            String time = activity.getString(AmProfile.SYNC_SLEEP_DATA_TIME_AM);
                            String level = activity.getString(AmProfile.SYNC_SLEEP_DATA_LEVEL_AM);
                            Connector.saveEvent(s.getId(), "note/txt", time);
                            Connector.saveEvent(s.getId(), "count/generic", level);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_SYNC_ACTIVITY_DATA_AM:
                    try {
                        //TODO: time, activity with duration
                        JSONObject info = new JSONObject(message);
                        JSONArray activity_info = info.getJSONArray(AmProfile.SYNC_ACTIVITY_DATA_AM);
                        JSONArray activities = activity_info.getJSONObject(0).getJSONArray(AmProfile.SYNC_ACTIVITY_EACH_DATA_AM);

                        tv_return.setText("Sync "+activities.length()+" activities...");
                        String streamID = "AM3S_syncActivities";
                        Stream s = Connector.saveStream(streamID, streamID);

                        for(int i=0; i<activities.length(); i++) {
                            JSONObject activity = activities.getJSONObject(i);
                            String time = activity.getString(AmProfile.SYNC_ACTIVITY_DATA_TIME_AM);
                            String stepLength = activity.getString(AmProfile.SYNC_ACTIVITY_DATA_STEP_LENGTH_AM);
                            String steps = activity.getString(AmProfile.SYNC_ACTIVITY_DATA_STEP_AM);
                            String calories = activity.getString(AmProfile.SYNC_ACTIVITY_DATA_CALORIE_AM);

                            Connector.saveEvent(s.getId(), "note/txt", time);
                            Connector.saveEvent(s.getId(), "time/min", stepLength);
                            Connector.saveEvent(s.getId(), "count/steps", steps);
                            Connector.saveEvent(s.getId(), "energy/cal", calories);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_SYNC_REAL_DATA_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String real_info = info.getString(AmProfile.SYNC_REAL_STEP_AM);
                        saveAction("syncRealSteps", "count/steps", real_info);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_GET_USERINFO_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String user_info = info.getString(AmProfile.GET_USER_AGE_AM);
                        saveAction("userAge", "count/generic", user_info);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_GET_ALARMINFO_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String alarm_id = info.getString(AmProfile.GET_ALARM_ID_AM);
                        saveAction("alarmInfo", "count/generic", alarm_id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case AmProfile.ACTION_SET_USERID_SUCCESS_AM:
                    String obj = "Set ID success";
                    saveAction("userID", "count/generic", obj);
                    break;
                case AmProfile.ACTION_GET_RANDOM_AM:
                    try {
                        JSONObject info = new JSONObject(message);
                        String random = info.getString(AmProfile.GET_RANDOM_AM);
                        saveAction("randomNum", "count/generic", random);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void saveAction(String action, String type, String content) {
        tv_return.setText(content);
        String streamID = "AM3S_"+action;
        Stream s = Connector.saveStream(streamID,streamID);
        Connector.saveEvent(s.getId(), type, content);
    }

    public void getBattery(View v) {
        am3sControl.queryAMState();
    }

    public void getUserId(View v) {
        am3sControl.getUserId();
    }

    public void getAlarmNum(View v) {
        am3sControl.getAlarmClockNum();
    }

    public void syncStage(View v) {
        am3sControl.syncStageReprotData();
    }

    public void syncSleep(View v) {
        am3sControl.syncSleepData();
    }

    public void syncActivity(View v) {
        am3sControl.syncActivityData();
    }

    public void syncReal(View v) {
        am3sControl.syncRealData();
    }

    public void getUserInfo(View v) {
        am3sControl.getUserInfo();
    }

    public void getAlarmInfo(View v) {
        am3sControl.checkAlarmClock(1);
    }

    public void setUserId(View v) {
        am3sControl.setUserId(1);
    }

    public void sendRandom(View v) {
        am3sControl.sendRandom();
    }

}