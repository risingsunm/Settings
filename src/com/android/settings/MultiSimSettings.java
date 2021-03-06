/*
 * Copyright (c) 2011-2012, Code Aurora Forum. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.SubscriptionManager;
import com.android.internal.telephony.MSimPhoneFactory;

import com.android.settings.R;

public class MultiSimSettings extends PreferenceActivity implements DialogInterface.OnDismissListener,
        DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener  {
    private static final String TAG = "MultiSimSettings";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_DATA = "data";
    private static final String KEY_SMS = "sms";
    private static final String KEY_CONFIG_SUB = "config_sub";

    /*Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    private static final String KEY_MULTI_SIM_CONFIG_SUB = "multi_sim_config_sub";
    private static final String MULTI_SIM_CONFIG_SUB = "MULTI_SIM_CONFIG_SUB";
    /*End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/

    private static final String CONFIG_SUB = "CONFIG_SUB";

    private static final int DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS = 100;

    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 1;
    protected boolean mIsForeground = false;
    static final int SUBSCRIPTION_ID_0 = 0;
    static final int SUBSCRIPTION_ID_1 = 1;
    static final int SUBSCRIPTION_ID_INVALID = -1;
    static final int PROMPT_OPTION = 2;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;
    /*Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    private PreferenceScreen mMultiSimConfigSub;
    /*End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.multi_sim_settings);

        mVoice = (ListPreference) findPreference(KEY_VOICE);
        mVoice.setOnPreferenceChangeListener(this);
        mData = (ListPreference) findPreference(KEY_DATA);
        mData.setOnPreferenceChangeListener(this);
        mSms = (ListPreference) findPreference(KEY_SMS);
        mSms.setOnPreferenceChangeListener(this);
        mConfigSub = (PreferenceScreen) findPreference(KEY_CONFIG_SUB);
        mConfigSub.getIntent().putExtra(CONFIG_SUB, true);
        if (isAirplaneModeOn()) {
            Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
            mConfigSub.setEnabled(false);
        }

        /*Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
        /*
        mMultiSimConfigSub = (PreferenceScreen) findPreference(KEY_MULTI_SIM_CONFIG_SUB);
        mMultiSimConfigSub.getIntent().putExtra(MULTI_SIM_CONFIG_SUB, true);
        if (isAirplaneModeOn()) {
            Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
            mMultiSimConfigSub.setEnabled(false);
        }
        */
        /*End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        updateState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

    private void updateVoiceSummary() {
        int voiceSub = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);

        try {
            voiceSub = Settings.System.getInt(getContentResolver(),Settings.System.MULTI_SIM_VOICE_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi sim Voice Call Values", snfe);
        }
        boolean promptEnabled  = MSimPhoneFactory.isPromptEnabled();
        Log.d(TAG, "updateVoiceSummary: voiceSub =  " + voiceSub + "promptEnabled = " + promptEnabled);
        if (voiceSub == SUBSCRIPTION_ID_0 && (!promptEnabled)) {
            mVoice.setValue("0");
            mVoice.setSummary(summaries[0]);
        } else if (voiceSub == SUBSCRIPTION_ID_1 && (!promptEnabled)) {
            mVoice.setValue("1");
            mVoice.setSummary(summaries[1]);
        } else if (promptEnabled) {
            Log.d(TAG, "prompt is enabled");
            mVoice.setValue("2");
            mVoice.setSummary(summaries[2]);
        } else {
            mVoice.setValue("0");
            mVoice.setSummary(summaries[0]);
        }
    }

    private void updateDataSummary() {
        int Data_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Data_val = Settings.System.getInt(getContentResolver(),Settings.System.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim Data Subscription Value.", snfe);
        }

        Log.d(TAG, "updateDataSummary: Data_val = " + Data_val);
        if (Data_val == SUBSCRIPTION_ID_0) {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        } else if (Data_val == SUBSCRIPTION_ID_1) {
            mData.setValue("1");
            mData.setSummary(summaries[1]);
        } else {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        }
    }

    private void updateSmsSummary() {
        int Sms_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Sms_val = Settings.System.getInt(getContentResolver(),Settings.System.MULTI_SIM_SMS_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim SMS Call Values.", snfe);
        }

        Log.d(TAG, "updateSmsSummary: Sms_val = " + Sms_val);
        if (Sms_val == SUBSCRIPTION_ID_0) {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        } else if (Sms_val == SUBSCRIPTION_ID_1) {
            mSms.setValue("1");
            mSms.setSummary(summaries[1]);
        } else {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        if (KEY_VOICE.equals(key)) {
            summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);
            int V_value = Integer.parseInt((String) objValue);
            if (V_value == PROMPT_OPTION) {
                MSimPhoneFactory.setPromptEnabled(true);
                Log.d(TAG, "prompt is enabled " + V_value);
            } else {
                Log.d(TAG, "setVoiceSubscription " + V_value);
                MSimPhoneFactory.setPromptEnabled(false);
                MSimPhoneFactory.setVoiceSubscription(V_value);
            }
            mVoice.setSummary(summaries[V_value]);
        }

        if (KEY_DATA.equals(key)) {
            int D_value = Integer.parseInt((String) objValue);
            Log.d(TAG, "setDataSubscription " + D_value);
            if (mIsForeground) {
                showDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
            }
            SubscriptionManager mSubscriptionManager = SubscriptionManager.getInstance();
            Message setDdsMsg = Message.obtain(mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE, D_value,0);
            mSubscriptionManager.setDataSubscription(D_value, setDdsMsg);
        }

        if (KEY_SMS.equals(key)) {
            int S_value = Integer.parseInt((String) objValue);
            Log.d(TAG, "setSMSSubscription " + S_value);
            MSimPhoneFactory.setSMSSubscription(S_value);
            mSms.setSummary(summaries[S_value]);
        }

        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch(msg.what) {
                case EVENT_SET_DATA_SUBSCRIPTION_DONE:
                    Log.d(TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE");
                    if (mIsForeground) {
                        dismissDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
                    }
                    getPreferenceScreen().setEnabled(true);
                    updateDataSummary();

                    ar = (AsyncResult) msg.obj;

                    String status;

                    if (ar.exception != null) {
                        status = getResources().getString(R.string.set_dds_error)
                                    + " " + ar.exception.getMessage();
                        displayAlertDialog(status);
                        break;
                    }

                    boolean result = (Boolean)ar.result;

                    Log.d(TAG, "SET_DATA_SUBSCRIPTION_DONE: result = " + result);

                    if (result == true) {
                        MSimPhoneFactory.setDataSubscription(msg.arg1);
                        status = getResources().getString(R.string.set_dds_success);
                        Toast toast = Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        status = getResources().getString(R.string.set_dds_failed);
                        displayAlertDialog(status);
                    }

                    break;
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(this);

            dialog.setMessage(getResources().getString(R.string.set_data_subscription_progress));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);

            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to disallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    // This is a method implemented for DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss!");
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "onClick!");
    }

    void displayAlertDialog(String msg) {
        Log.d(TAG, "displayErrorDialog!" + msg);
        new AlertDialog.Builder(this).setMessage(msg)
               .setTitle(android.R.string.dialog_alert_title)
               .setIcon(android.R.drawable.ic_dialog_alert)
               .setPositiveButton(android.R.string.yes, this)
               .show()
               .setOnDismissListener(this);
    }
}

