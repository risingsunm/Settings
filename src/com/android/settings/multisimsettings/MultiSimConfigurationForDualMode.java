/*jiangxiaoke.hoperun 2012.7.12 create :add CDMA/GSM switch option */
package com.android.settings.multisimsettings;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings.System;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.CardSubscriptionManager;
import com.android.internal.telephony.Subscription;
import com.android.internal.telephony.SubscriptionData;
import com.android.internal.telephony.Subscription.SubscriptionStatus;
import com.android.internal.telephony.SubscriptionManager;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;


import java.lang.Thread;

public class MultiSimConfigurationForDualMode extends PreferenceActivity implements OnPreferenceChangeListener {
    private static final String LOG_TAG = "HP_MultiSimConfigurationForDualMode";

    private static final String KEY_SIM_NAME = "sim_name_key";
    private static final String KEY_SIM_ENABLER = "sim_enabler_key";
    private static final String KEY_NETWORK_SETTING = "mobile_network_key";
    private static final String KEY_CALL_SETTING = "call_setting_key";
    private static final String KEY_CDMA_CHECKBOX = "checkbox_cdma_key";
    private static final String KEY_GSM_CHECKBOX = "checkbox_gsm_key";
    private final int MAX_SUBSCRIPTIONS = 2;
	private static final int CARD1 = 0;
	private static final int CARD2 = 1;
    private static final int SUBSCRIPTION_INDEX_INVALID = 99999;
    private static final int EVENT_SIM_STATE_CHANGED =1;
    private static final int EVENT_SET_SUBSCRIPTION_DONE = 2;
    private static final int EVENT_ENABLE_GSM_STEP1_DONE = 3;
    private final int DIALOG_SET_SUBSCRIPTION_IN_PROGRESS = 100;
    private final int DIALOG_DISABLE_CARD2_GSM_IN_PROGRESS = 101;
    private PreferenceScreen mPrefScreen;
    private SubscriptionManager mSubscriptionManager;
    private PreferenceScreen mNetworkSetting;
    private PreferenceScreen mCallSetting;

    private int mSubscription;
    private MultiSimNamePreference mNamePreference;
    private MultiSimEnablerForDualMode mEnablerPreference;
    private CardSubscriptionManager mCardSubscriptionManager;
    private SubscriptionData[] mCardSubscrInfo;
    private SubscriptionData mCurrentSelSub;
    private SubscriptionData mUserSelSub;
    private CheckBoxPreference mCDMACheckbox;
    private CheckBoxPreference mGSMCheckbox;
    private int mPhoneState;
    private IntentFilter mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    private MSimTelephonyManager mstm;
    private String mIccId;
    private AlertDialog mErrorDialog = null;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("onReceive " + action);
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action) ||
                Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                setScreenState();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.multi_sim_configuration_dual_mode);

        mPrefScreen = getPreferenceScreen();

        Intent intent = getIntent();
        mSubscription = intent.getIntExtra(SUBSCRIPTION_KEY, CARD1);
        mSubscriptionManager = SubscriptionManager.getInstance();

        mNamePreference = (MultiSimNamePreference)findPreference(KEY_SIM_NAME);
        mNamePreference.setSubscription(mSubscription);

        mEnablerPreference = (MultiSimEnablerForDualMode)findPreference(KEY_SIM_ENABLER);
        mEnablerPreference.setSubscription(this, mSubscription);
        mNetworkSetting = (PreferenceScreen)findPreference(KEY_NETWORK_SETTING);
        mNetworkSetting.getIntent().putExtra(MultiSimSettingsConstants.TARGET_PACKAGE,
                               MultiSimSettingsConstants.NETWORK_PACKAGE)
                                    .putExtra(MultiSimSettingsConstants.TARGET_CLASS,
                               MultiSimSettingsConstants.NETWORK_CLASS)
                                    .putExtra(SUBSCRIPTION_KEY, mSubscription);

        mCallSetting = (PreferenceScreen)findPreference(KEY_CALL_SETTING);
        mCallSetting.getIntent().putExtra(MultiSimSettingsConstants.TARGET_PACKAGE,
                               MultiSimSettingsConstants.CALL_PACKAGE)
                                    .putExtra(MultiSimSettingsConstants.TARGET_CLASS,
                               MultiSimSettingsConstants.CALL_CLASS)
                                    .putExtra(SUBSCRIPTION_KEY, mSubscription);

        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mCardSubscriptionManager = CardSubscriptionManager.getInstance();
        mCDMACheckbox = (CheckBoxPreference)findPreference(KEY_CDMA_CHECKBOX);
        mGSMCheckbox = (CheckBoxPreference)findPreference(KEY_GSM_CHECKBOX);
        mCDMACheckbox.setOnPreferenceChangeListener(this);
        mGSMCheckbox.setOnPreferenceChangeListener(this);
        mUserSelSub = new SubscriptionData(MAX_SUBSCRIPTIONS);
        mCardSubscrInfo = new SubscriptionData[MAX_SUBSCRIPTIONS];
        mCurrentSelSub = new SubscriptionData(MAX_SUBSCRIPTIONS);
        for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
            mCardSubscrInfo[i] = mCardSubscriptionManager.getCardSubscriptions(i);
            if(mCardSubscrInfo[i] != null){
                Log.d(LOG_TAG, mCardSubscrInfo[i].toString());
                if (i==CARD1){
                    mIccId = mCardSubscrInfo[CARD1].subscription[0].iccId;
                }
            }

            Subscription sub = mSubscriptionManager.getCurrentSubscription(i);
            mCurrentSelSub.subscription[i].copyFrom(sub);
        }
        mUserSelSub.copyFrom(mCurrentSelSub);
        Log.d(LOG_TAG, "Current sub start....");
        Log.d(LOG_TAG, mCurrentSelSub.toString());
        Log.d(LOG_TAG, "Current sub end....");

        if (mSubscriptionManager.isSetSubscriptionInProgress()) {
            Log.d(LOG_TAG, "onCreate: SetSubscription is in progress when started this activity");
            showDialog(DIALOG_SET_SUBSCRIPTION_IN_PROGRESS);
            mSubscriptionManager.registerForSetSubscriptionCompleted(mHandler, EVENT_SET_SUBSCRIPTION_DONE, null);
        }
        mstm = (MSimTelephonyManager)this.getSystemService(Context.MSIM_TELEPHONY_SERVICE);
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        mNamePreference.resume();
        mEnablerPreference.resume();
        setScreenState();
    }

    protected void onPause() {
        super.onPause();

        unregisterReceiver(mReceiver);
        mNamePreference.pause();
        mEnablerPreference.pause();
        if (mErrorDialog != null) {
            logd("pause: dismiss error dialog");
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
        mSubscriptionManager.unRegisterForSetSubscriptionCompleted(mHandler);
    }

	public boolean onPreferenceChange(Preference preference, Object newValue){
        String key = preference.getKey();
        if (key.equals(KEY_CDMA_CHECKBOX)) {
            Log.d(LOG_TAG, "KEY_CDMA_CHECKBOX");
            if(!mCDMACheckbox.isChecked()){
               Log.d(LOG_TAG, "set to CDMA");
               mGSMCheckbox.setChecked(false);
               switchToCDMA();
            }
        } else if (key.equals(KEY_GSM_CHECKBOX)) {
            Log.d(LOG_TAG, "KEY_GSM_CHECKBOX");
            if(!mGSMCheckbox.isChecked()){
               Log.d(LOG_TAG, "set to GSM");
               mCDMACheckbox.setChecked(false);
               switchToGSM();
            }
        }

        return (Boolean)newValue;
    }


    private void switchToCDMA() {
        mSubscriptionManager.registerForSetSubscriptionCompleted(mHandler, EVENT_SET_SUBSCRIPTION_DONE, null);

       for (int i = 0; i <MAX_SUBSCRIPTIONS; i++) {
           mUserSelSub.subscription[i].copyFrom(mSubscriptionManager.getCurrentSubscription(i));
       }


        mUserSelSub.subscription[0].clear();
        mUserSelSub.subscription[0].subId = 0;
        mUserSelSub.subscription[0].iccId = mIccId;
        mUserSelSub.subscription[0].slotId = 0;
        mUserSelSub.subscription[0].m3gppIndex = -1;
        mUserSelSub.subscription[0].m3gpp2Index = 1;
        //mSubscriptionManager.setDefaultAppIndex(mUserSelSub.subscription[0]);
        mUserSelSub.subscription[0].subStatus = Subscription.SubscriptionStatus.SUB_ACTIVATE;
        mUserSelSub.subscription[0].appType = "RUIM";

        if(mUserSelSub.subscription[1].slotId == 0){
            mUserSelSub.subscription[1].clear();
            mUserSelSub.subscription[1].subId = 1;
            //mUserSelSub.subscription[1].iccId = mIccId;
            mUserSelSub.subscription[1].slotId = SUBSCRIPTION_INDEX_INVALID;
            mUserSelSub.subscription[1].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
            mUserSelSub.subscription[1].m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
            mUserSelSub.subscription[1].subStatus = Subscription.SubscriptionStatus.SUB_DEACTIVATE;
            mUserSelSub.subscription[1].appType = "SIM";
        }
        setSubscription();
    }

    private void switchToGSMStep2(){
       mSubscriptionManager.registerForSetSubscriptionCompleted(mHandler, EVENT_SET_SUBSCRIPTION_DONE, null);

       for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
           mUserSelSub.subscription[i].copyFrom(mSubscriptionManager.getCurrentSubscription(i));
       }

       for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
           mUserSelSub.subscription[i].clear();
           mUserSelSub.subscription[i].subId = i;
           mUserSelSub.subscription[i].iccId = mIccId;
       }
       mUserSelSub.subscription[0].slotId = SUBSCRIPTION_INDEX_INVALID;
       mUserSelSub.subscription[0].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
       mUserSelSub.subscription[0].m3gpp2Index =SUBSCRIPTION_INDEX_INVALID;
       mUserSelSub.subscription[0].subStatus = Subscription.SubscriptionStatus.SUB_DEACTIVATE;
       mUserSelSub.subscription[0].appType = "RUIM";


       mUserSelSub.subscription[1].slotId = 0;
       mUserSelSub.subscription[1].m3gppIndex = 0;
       mUserSelSub.subscription[1].m3gpp2Index = -1;
       mUserSelSub.subscription[1].subStatus = Subscription.SubscriptionStatus.SUB_ACTIVATE;
       mUserSelSub.subscription[1].appType = "SIM";
       setSubscription();

    }
    private void switchToGSM()
    {
        switchToGSMStep2();
    }

    private void setSubscription() {
       Log.d(LOG_TAG, "setSubscription ********\n" + mUserSelSub.toString());
       boolean ret = mSubscriptionManager.setSubscription(mUserSelSub);
       Log.d(LOG_TAG, "set Subscription ret= " + ret);
       if (ret) {
            showDialog(DIALOG_SET_SUBSCRIPTION_IN_PROGRESS);
       } else {
            //TODO: Already some set sub in progress. Display a Toast?
       }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch(msg.what) {
                case EVENT_SET_SUBSCRIPTION_DONE:
                    Log.d(LOG_TAG, "EVENT_SET_SUBSCRIPTION_DONE");
                    mEnablerPreference.handleSetSubDone();
                    mSubscriptionManager.unRegisterForSetSubscriptionCompleted(mHandler);
                    try{
                        dismissDialog(DIALOG_SET_SUBSCRIPTION_IN_PROGRESS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    getPreferenceScreen().setEnabled(true);
                    ar = (AsyncResult) msg.obj;
                    String result[] = (String[]) ar.result;
                    if (result != null) {
                        displayAlertDialog(resultToMsg(result[CARD1]));
                        setScreenState();
                    } else {
                        finish();
                    }

                    break;
                case EVENT_ENABLE_GSM_STEP1_DONE:
                    try{
                        dismissDialog(DIALOG_DISABLE_CARD2_GSM_IN_PROGRESS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mSubscriptionManager.unRegisterForSetSubscriptionCompleted(mHandler);
                    switchToGSMStep2();
                    break;
                case EVENT_SIM_STATE_CHANGED:
                    Log.d(LOG_TAG, "EVENT_SIM_STATE_CHANGED");
                    break;
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setIndeterminate(true);

        if (id == DIALOG_SET_SUBSCRIPTION_IN_PROGRESS) {
            dialog.setMessage(getResources().getString(R.string.switch_networkType_progress));
            return dialog;
        } else if(id == DIALOG_DISABLE_CARD2_GSM_IN_PROGRESS){
            dialog.setMessage(getResources().getString(R.string.disable_card2_gsm_progress));
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_SET_SUBSCRIPTION_IN_PROGRESS) {
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to disallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

   void displayAlertDialog(String msg) {
       mErrorDialog =  new AlertDialog.Builder(this)
             .setTitle(android.R.string.dialog_alert_title)
             .setMessage(msg)
             .setCancelable(false)
             .setNeutralButton(R.string.close_dialog, null)
             .show();
    }

    private String resultToMsg(String result){
        if(result.equals(SubscriptionManager.SUB_ACTIVATE_SUCCESS)
                 || result.equals(SubscriptionManager.SUB_DEACTIVATE_SUCCESS)
                 || result.equals(SubscriptionManager.SUB_NOT_CHANGED)){
            return this.getString(R.string.switch_networkType_success);
        }
        if (result.equals(SubscriptionManager.SUB_ACTIVATE_FAILED)
                 || result.equals(SubscriptionManager.SUB_DEACTIVATE_FAILED)
                 || result.equals(SubscriptionManager.SUB_DEACTIVATE_NOT_SUPPORTED)
                 || result.equals(SubscriptionManager.SUB_ACTIVATE_NOT_SUPPORTED)){
            return this.getString(R.string.switch_networkType_failed);
        }

        return this.getString(R.string.switch_networkType_success);
    }

    private boolean isSubActivated(int subScription) {
        return mSubscriptionManager.isSubActive(subScription);
    }

    private boolean isSubActivated() {
        return mSubscriptionManager.isSubActive(mSubscription);
    }

    private boolean isAirplaneModeOn() {
        return (System.getInt(getContentResolver(), System.AIRPLANE_MODE_ON, 0) != 0);
    }

    private boolean hasCard() {
        CardSubscriptionManager cardSubMgr = CardSubscriptionManager.getInstance();
        if (cardSubMgr != null && cardSubMgr.getCardSubscriptions(mSubscription) != null) {
            return true;
        }
        return false;
    }
    private boolean isCardAbsent() {
        MSimTelephonyManager telManager = MSimTelephonyManager.getDefault();
        return telManager.getSimState(mSubscription) == TelephonyManager.SIM_STATE_ABSENT;
    }

    private void setScreenState() {
        if (isAirplaneModeOn()) {
            mNetworkSetting.setEnabled(false);
            mCallSetting.setEnabled(false);
            mEnablerPreference.setEnabled(false);
            mCDMACheckbox.setChecked(false);
            mGSMCheckbox.setChecked(false);
        } else {
            mNetworkSetting.setEnabled(isSubActivated());
            mCallSetting.setEnabled(isSubActivated());
            mEnablerPreference.setEnabled(hasCard());
            mCDMACheckbox.setEnabled(mEnablerPreference.isCard1Active());
            mGSMCheckbox.setEnabled(mEnablerPreference.isCard1Active());
            for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
                Subscription sub = mSubscriptionManager.getCurrentSubscription(i);
                Log.d(LOG_TAG, "kangta" + sub.toString());
                if(sub.subId == 0 ){
                    mCDMACheckbox.setChecked(((sub.subStatus == Subscription.SubscriptionStatus.SUB_ACTIVATED && sub.slotId == 0) ? true : false));
                } else if(sub.subId == 1){
                    mGSMCheckbox.setChecked(((sub.subStatus == Subscription.SubscriptionStatus.SUB_ACTIVATED&&sub.slotId == 0) ? true : false));
                }
            }
        }
    }

    private void logd(String msg) {
        Log.d(LOG_TAG, "[" + LOG_TAG + "(" + mSubscription + ")] " + msg);
    }
}
