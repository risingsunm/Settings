/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-8-7
 *
 */
package com.android.settings.mobilenetwork;

/**
 * @author mzikun
 *
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class MobileNetworkEnabler {
    private static final String TAG = "MobileNetworkEnabler";
    private static final boolean LOGD = true;

    private final Context mContext;
    private Switch mSwitch;
    private final ConnectivityManager cm;
    private final IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(LOGD) Log.d(TAG, "BroadcastReceiver-onReceive");

            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                if(LOGD) Log.d(TAG, "BroadcastReceiver-onReceive-action:android.net.conn.CONNECTIVITY_CHANGE");

                setSwitchChecked(intent);
                }
        }
    };

    public MobileNetworkEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

        cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public void resume() {
        if(LOGD) Log.d(TAG, "MobileNetworkEnabler-resume");

        // CONNECTIVITY state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);
        mSwitch.setChecked(cm.getMobileDataEnabled());
    }

    public void pause() {
        if(LOGD) Log.d(TAG, "MobileNetworkEnabler-resume");

        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);

        boolean isEnabled = cm.getMobileDataEnabled();
        mSwitch.setChecked(isEnabled);
    }

    private OnCheckedChangeListener mDataEnabledListener = new OnCheckedChangeListener() {
        /** {@inheritDoc} */
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            final boolean dataEnabled = isChecked;
            if (dataEnabled) {
                setMobileDataEnabled(true);
            } else {
                setMobileDataEnabled(false);
            }
        }
    };

    private void setMobileDataEnabled(boolean enabled) {
        if (LOGD) Log.d(TAG, "setMobileDataEnabled()");

        cm.setMobileDataEnabled(enabled);
    }

    private void setSwitchChecked(Intent intent) {
        final boolean checked = cm.getMobileDataEnabled();
        if(LOGD) Log.d(TAG, "setSwitchChecked");

        if (checked != mSwitch.isChecked()) {
            if(LOGD) Log.d(TAG, "setSwitchChecked-checked != mSwitch.isChecked()");

            mSwitch.setChecked(checked);
        }
    }

}
