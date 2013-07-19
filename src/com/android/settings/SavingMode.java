/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-5-16 下午7:12:44
 *
 */

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFrameLayout;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author mzikun
 */
public class SavingMode extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "SavingMode";
    static boolean LOG = true;

    private ListPreference listBatterySet;
    private ListPreference listBrightness;
    private ListPreference listScreenTimeout;
    private CheckBoxPreference chkSavingMode;
    private CheckBoxPreference chkWlanSet;
    private CheckBoxPreference chkBluetoothSet;
    private CheckBoxPreference chkGpsSet;
    private CheckBoxPreference chkSyncSet;
    private CheckBoxPreference chkBrightnessSet;

    private static final String BATTERYLEVEL = "battery_set";
    private static final String BRIGHTNESSLEVEL = "brightness_detail";
    private static final String SCREENTIMEOUT = "screen_timeout";
    private static final String SAVINGMODESWITCH = "savingmode_switch";
    private static final String WLANSWITCH = "wlan_set";
    private static final String BTSWITCH = "bluetooth_set";
    private static final String GPSSWITCH = "gps_set";
    private static final String SYNCSWITCH = "sync_set";
    private static final String BRIGHTNEEESWITCH = "brightness_set";

    private static int mBatteryLevel;
    private static int mBrightnessLevel;
    private static int mScreenTimeout;
    private static boolean mSavingModeSwitch;
    private static boolean mWlanSwitch;
    private static boolean mBTSwitch;
    private static boolean mGpsSwitch;
    private static boolean mSyncSwitch;
    private static boolean mBrightnessSwitch;

    private static SharedPreferences pre;
    private static NotificationManager mNM;

    private static ConnectivityManager mConnectivityManager = null;
    private static WifiManager mWifiManager = null;
    private static BluetoothAdapter mBluetoothAdapter = null;
    private static boolean actvie_flag = false;
    private static int mBatteryCurrLevel;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.savingmode_preference);

        bindSavingViews();

        pre = PreferenceManager.getDefaultSharedPreferences(getActivity());

        getSavingConfig(getActivity());
        getService(getActivity());

        // set summary text
        setBatterySummary(mBatteryLevel);
        setBrightnessSummary(mBrightnessLevel);
        setScreenTimeoutSummary(mScreenTimeout);

        // set listener
        listBatterySet.setOnPreferenceChangeListener(this);
        listBrightness.setOnPreferenceChangeListener(this);
        chkSavingMode.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        getSavingConfig(getActivity());

        if (preference.equals(chkSavingMode)) {
            logd("mzikun=preference.equals(chkSavingMode");
            if (chkSavingMode.isChecked()) {

                logd("mzikun=mBatteryCurrLevel:" + mBatteryCurrLevel + ". mBatteryLevel:"
                        + mBatteryLevel);
                if (mBatteryCurrLevel < mBatteryLevel) {
//                    showDialog(getActivity());
                    ActiveSavingMode(getActivity());
                    ActiveNotification(getActivity());
                }

                logd("mzikun=ActiveSavingMode()");

                ActiveNotification(getActivity());
            }
            else {
                actvie_flag = false;
                DeactiveNotification();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /*
     * (non-Javadoc)
     * @see
     * android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange
     * (android.preference.Preference, java.lang.Object)
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(listBatterySet)) {
            setBatterySummary(Integer.parseInt(newValue.toString()));
        } else if (preference.equals(listBrightness)) {
            setBrightnessSummary(Integer.parseInt(newValue.toString()));
        } else if(preference.equals(listScreenTimeout)){
            setScreenTimeoutSummary(Integer.parseInt(newValue.toString()));
        }

        return true;
    }

    /**
     * @author mzikun
     * @description 2012-5-8 上午11:49:49
     * @param BatteryLevel
     * @return void
     */
    private void setBatterySummary(int BatteryLevel) {
        if ((BatteryLevel < 0) || (BatteryLevel > 100)) {
            return;
        }

        // BatterySummary += "%%电量";
        String BatterySummary = BatteryLevel + getResources().getString(R.string.battery_percent);

        Log.d(TAG, BatterySummary);

        listBatterySet.setSummary(BatterySummary);
    }

    /**
     * @author mzikun
     * @description 2012-5-8 上午11:49:40
     * @param BrightnessLevel
     * @return void
     */
    private void setBrightnessSummary(int BrightnessLevel) {
        if ((BrightnessLevel < 0) || (BrightnessLevel > 100)) {
            //return;
            BrightnessLevel = 15;
        }

        // BrightnessSummary += "%%电量";
        String BrightnessSummary = BrightnessLevel
                + getResources().getString(R.string.brightness_percent);

        Log.d(TAG, BrightnessSummary);

        listBrightness.setSummary(BrightnessSummary);
    }

    private void setScreenTimeoutSummary(int currentTimeout) {
        ListPreference preference = listScreenTimeout;
        String summary;
        if (currentTimeout == -1) {
            summary = preference.getContext().getString(R.string.screensaver_timeout_zero_summary);
        } else if (currentTimeout < -1) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                int timeout = Integer.parseInt(values[i].toString());
                if ((currentTimeout >= timeout)&&(timeout > 0)) {
                    best = i;
                }
            }
            summary = preference.getContext().getString(R.string.screen_timeout_summary,
                    entries[best]);
        }
        preference.setSummary(summary);
    }

    /**
     * @author mzikun
     * @description 2012-5-8 上午11:49:35
     * @return void
     */
    private void bindSavingViews() {
        listBatterySet = (ListPreference) findPreference("battery_set");
        listBrightness = (ListPreference) findPreference("brightness_detail");
        listScreenTimeout = (ListPreference) findPreference("screen_timeout");

        chkSavingMode = (CheckBoxPreference) findPreference("savingmode_switch");
        chkWlanSet = (CheckBoxPreference) findPreference("savingmode_switch");
        chkBluetoothSet = (CheckBoxPreference) findPreference("savingmode_switch");
        chkGpsSet = (CheckBoxPreference) findPreference("savingmode_switch");
        chkSyncSet = (CheckBoxPreference) findPreference("savingmode_switch");
        chkBrightnessSet = (CheckBoxPreference) findPreference("savingmode_switch");

    }

    /**
     * @author mzikun
     * @description 2012-5-8 上午11:49:30
     * @return void
     */
    private static void getSavingConfig(Context context) {

        if (pre == null) {
            pre = PreferenceManager.getDefaultSharedPreferences(context);
        }

        // 两个参数,一个是key，就是在PreferenceActivity的xml中设置的,另一个是取不到值时的默认值
        mBatteryLevel = Integer.parseInt(pre.getString(BATTERYLEVEL, "30"));
        mBrightnessLevel = Integer.parseInt(pre
                .getString(BRIGHTNESSLEVEL, "15"));
        if ((mBrightnessLevel < 0) || (mBrightnessLevel > 100)) {
            //return;
            mBrightnessLevel = 15;
        }
        mScreenTimeout = Integer.parseInt(pre.getString(SCREENTIMEOUT, "30"));

        mSavingModeSwitch = pre.getBoolean(SAVINGMODESWITCH, false);
        mWlanSwitch = pre.getBoolean(WLANSWITCH, true);
        mBTSwitch = pre.getBoolean(BTSWITCH, true);
        mGpsSwitch = pre.getBoolean(GPSSWITCH, true);
        mSyncSwitch = pre.getBoolean(SYNCSWITCH, true);
        mBrightnessSwitch = pre.getBoolean(BRIGHTNEEESWITCH, true);

        int test = Settings.System.getInt(context.getContentResolver(), BATTERYLEVEL, 30);
        logd("mzikun:Settings.System.getInt" + test);

    }

    // using when get reference
    private static <T> void nullJudge(T ref, String pos) {

        if (ref == null) {
            loge(pos + "=null");
            // ProfileMain.this.finish();
        }
    }

    private static void loge(Object e) {

        if (!LOG)
            return;

        Thread mThread = Thread.currentThread();
        StackTraceElement[] mStackTrace = mThread.getStackTrace();
        String mMethodName = mStackTrace[3].getMethodName();
        e = "[" + mMethodName + "] " + e;
        Log.e(TAG, e + "");
    }

    private static void logd(Object s) {

        if (!LOG)
            return;

        Thread mThread = Thread.currentThread();
        StackTraceElement[] mStackTrace = mThread.getStackTrace();
        String mMethodName = mStackTrace[3].getMethodName();

        s = "[" + mMethodName + "] " + s;
        Log.d(TAG, s + "");
    }

    private static void getService(Context context) {
        mConnectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        nullJudge(mConnectivityManager, "mConnectivityManager");

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        nullJudge(mWifiManager, "mWifiManager");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        nullJudge(mBluetoothAdapter, "mBluetoothAdapter");

        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nullJudge(mNM, "mNM");
    }

    private static void wifiSetting(boolean wifi) {
        if (mWifiManager == null) {
            loge("No wifiManager.");
            return;
        }

        int wifiApState = mWifiManager.getWifiState();
        logd("wifiApState:" + wifiApState);
        logd("wifi:" + wifi);

        mWifiManager.setWifiEnabled(wifi);

        wifiApState = mWifiManager.getWifiState();
        logd("wifiApState:" + wifiApState);

    }

    private static void bluetoothSetting(boolean bluetooth) {
        if (mBluetoothAdapter != null) {
            if (bluetooth)
                mBluetoothAdapter.enable();
            else
                mBluetoothAdapter.disable();
        }
    }

    private static void gpsLocationSetting(Context context, boolean gpsLocation) {
        try {
            Settings.Secure.setLocationProviderEnabled(context.getContentResolver(),
                    LocationManager.GPS_PROVIDER, gpsLocation);
        } catch (Exception e) {
        }
    }

    private void networkLocationSetting(boolean networkLocation) {
        try {
            Settings.Secure.setLocationProviderEnabled(getActivity()
                    .getContentResolver(), LocationManager.NETWORK_PROVIDER,
                    networkLocation);
        } catch (Exception e) {

        }
    }

    private static void brightnessSetting(Context context, int brightness) {
        // brightness 0-100

        int value;
        value = brightness;
        if (brightness <= 0) {
            value = 1;
        }
        else if (brightness > 100) {
            value = 100;
        }
        value = (brightness * 255) / 100;

        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        if (!android.provider.Settings.System.putInt(context.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, value))
        {
            Log.e(TAG, "SCREEN_BRIGHTNESS writing error");
        }

        Uri localUri = Settings.System.getUriFor("screen_brightness");
        context.getContentResolver().notifyChange(localUri, null);

//        WindowManager.LayoutParams mParam = ((Activity) context).getWindow().getAttributes();
//        mParam.screenBrightness = brightness / 100.0f; // 0.0 - 1.0
//        ((Activity) context).getWindow().setAttributes(mParam);

        try {
            IPowerManager power = IPowerManager.Stub
                    .asInterface(ServiceManager.getService("power"));
            if (power != null) {
                power.setBacklightBrightness(brightness);
            }
        } catch (RemoteException doe) {

        }

    }

    private static void ActiveNotification(Context context) {

        CharSequence from = context.getResources().getString(R.string.usesavingmode);
        CharSequence message = context.getResources().getString(R.string.notifysavingmode);

        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, SavingMode.class), 0);

        // The ticker text, this uses a formatted string so our message could be
        // localized
//        String tickerText = context.getResources().getString(R.string.imcoming_message_ticker_text,
//                message);

        // construct the Notification object.
        Notification notif = new Notification(R.drawable.saving_mode, message,
                System.currentTimeMillis());

        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo(context, from, message, contentIntent);

        mNM.notify(R.string.imcoming_message_ticker_text, notif);
    }

    private void DeactiveNotification() {
        mNM.cancel(R.string.imcoming_message_ticker_text);
    }

    /**
     * @author mzikun
     * @description 2012-5-15 上午9:27:27
     * @return void
     */
    private static void ActiveSavingMode(Context context) {

        if (actvie_flag) {
            logd("actvie_flag:" + actvie_flag);
            return;
        }

        mBatteryLevel = Integer.parseInt(pre.getString(BATTERYLEVEL, "30"));
        mBrightnessLevel = Integer.parseInt(pre
                .getString(BRIGHTNESSLEVEL, "30"));
        if ((mBrightnessLevel < 0) || (mBrightnessLevel > 100)) {
            //return;
            mBrightnessLevel = 15;
        }
        mScreenTimeout = Integer.parseInt(pre.getString(SCREENTIMEOUT, "30"));

        mSavingModeSwitch = pre.getBoolean(SAVINGMODESWITCH, false);
        mWlanSwitch = !(pre.getBoolean(WLANSWITCH, true));
        mBTSwitch = !(pre.getBoolean(BTSWITCH, true));
        mGpsSwitch = !(pre.getBoolean(GPSSWITCH, true));
        mSyncSwitch = !(pre.getBoolean(SYNCSWITCH, true));
        mBrightnessSwitch = pre.getBoolean(BRIGHTNEEESWITCH, true);

        logd("ActiveSavingMode:Entry");

        if (mSavingModeSwitch) {
            logd("ActiveSavingMode:if (mSavingModeSwitch)");

            // mBatteryLevel
            wifiSetting(mWlanSwitch);
            bluetoothSetting(mBTSwitch);
            gpsLocationSetting(context, mGpsSwitch);

            brightnessSetting(context, mBrightnessLevel);
            Settings.System.putInt(context.getContentResolver(), SCREEN_OFF_TIMEOUT,
                    mScreenTimeout);
            ContentResolver.setMasterSyncAutomatically(mSyncSwitch);

            actvie_flag = true;

            logd("mSavingModeSwitch:" + mSavingModeSwitch);

        }
    }

    public static void showDialog(Context context) {
        final Context tempContext = context;
        String tickerText = context.getResources().getString(R.string.dial_saving_message,
                mBatteryLevel);

        Dialog dl = new AlertDialog.Builder(context)
                .setTitle(R.string.saving_mode)
                .setMessage(tickerText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        logd("showDialog======================ActiveSavingMode");
                        ActiveSavingMode(tempContext);
                        ActiveNotification(tempContext);
                    }
                })
                // .setNeutralButton(R.string.hello, new
                // DialogInterface.OnClickListener() {
                // public void onClick(DialogInterface dialog, int whichButton)
                // {
                //
                // /* User clicked Something so do some stuff */
                // }
                // })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* User clicked Cancel so do some stuff */
                    }
                })
                .create();
        dl.show();
    }

    public static class BootBatteryReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent bootServiceIntent = new Intent(arg0, CheckBatteryService.class);
                arg0.startService(bootServiceIntent);
                Log.d(TAG, "--------Boot start service-------------");
            }
            logd("mzikun==============BatteryReceiver");
        }
    }

    // CheckBatteryService
    public static class CheckBatteryService extends Service {
        private final int CLOSE_ALERTDIALOG = 0;
        private int timecount = 0;
        private DelayCloseController delayCloseController = new DelayCloseController();

        /*
         * (non-Javadoc)
         * @see android.app.Service#onBind(android.content.Intent)
         */
        @Override
        public IBinder onBind(Intent arg0) {
            logd("CheckBatteryService:onBind");
            return null;
        }

        @Override
        public void onCreate() {
            Log.d(TAG, "---------onCreate--------");
            registerReceiver(batteryChangedReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            // delayCloseController.timer.schedule(delayCloseController, 1500,
            // 1000);
        }

        private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    int level = intent.getIntExtra("level", 0);
                    int scale = intent.getIntExtra("scale", 100);
                    Log.d(TAG, "----------level---" + level);
                    Log.d(TAG, "----------scale---" + scale);
                    Log.d(TAG, "----------Total---" + level * 100 / scale + "%");
                    mBatteryCurrLevel = level * 100 / scale;

                    pre = PreferenceManager.getDefaultSharedPreferences(context);
                    getSavingConfig(context);
                    getService(context);

                    logd("BroadcastReceiver:onReceive:mBatteryLevel:" + mBatteryLevel
                            + " and mBatteryCurrLevel:" + mBatteryCurrLevel);

                    if ((mBatteryLevel > mBatteryCurrLevel) && (mSavingModeSwitch)){
                        logd("BroadcastReceiver:onReceive:if(mBatteryLevel > mBatteryCurrLevel)");
                        //showDialog(getApplication().getApplicationContext());
                        ActiveSavingMode(context);
                        ActiveNotification(context);
                    }
                }
            }
        };

        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CLOSE_ALERTDIALOG: {
                        if (timecount % 2 == 1) {
                            if (mBatteryCurrLevel <= 15) {
                                onPowerLed();
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                                offPowerLed();
                                Log.d(TAG, "------PowerLed------");
                            }
                        } else {
                            onWorkLed();
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            offWorkLed();
                            Log.d(TAG, "------WorkLed------");
                        }
                    }
                        timecount++;
                        break;
                    default:
                        break;
                }
            }
        };

        private class DelayCloseController extends TimerTask {
            private Timer timer = new Timer();

            @Override
            public void run() {
                Message messageFinish = new Message();
                messageFinish.what = CLOSE_ALERTDIALOG;
                mHandler.sendMessage(messageFinish);
            }
        }

        public void onPowerLed() {
            Log.d(TAG, "------------onPowerLed-------------");
        }

        public void offPowerLed() {
            Log.d(TAG, "------------offPowerLed-------------");
        }

        public void onWorkLed() {
            Log.d(TAG, "------------onWorkLed-------------");
        }

        public void offWorkLed() {
            Log.d(TAG, "------------offWorkLed-------------");
        }
    }

}
