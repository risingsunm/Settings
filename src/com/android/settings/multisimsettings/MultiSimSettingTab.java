/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *    Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *    Neither the name of Code Aurora Forum, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.settings.multisimsettings;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.settings.R;
/*jiangxiaoke.hoperun 2012.7.12?add?CDMA/GSM switch option*/
import android.content.Context;
import com.android.internal.telephony.CardSubscriptionManager;
import android.telephony.MSimTelephonyManager;
import com.android.internal.telephony.SubscriptionData;
import android.os.SystemProperties;
/*jiangxiaoke.hoperun 2012.7.12?end*/
public class MultiSimSettingTab extends TabActivity {

    private static final String LOG_TAG = "MultiSimSettingWidget";

    private static final boolean DBG = true;
    /*--Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    private static final boolean DEBUG = true;
    /*--End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/

    private int[] tabIcons = {
            R.drawable.ic_tab_sim1, R.drawable.ic_tab_sim2
    };

    private String[] tabSpecTags = {
            "sub1", "sub2"
    };

    private LocalMultiSimSettingsManager mLocalManager;

    private Intent mIntent;
    /*--Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    private TelephonyManager mTelephonyManager;
    //private PhoneStateListener mListener;
    /*--End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/

    /*jiangxiaoke.hoperun 2012.7.12?add?CDMA/GSM switch option*/
    private static final int CARD1 = 0;
    private static final int CARD2 = 1;
    /*jiangxiaoke.hoperun 2012.7.12?end*/

    @Override
    public void onPause() {
        super.onPause();
    }

    /*
     * Activity class methods
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG)
            log("Creating activity");
        mLocalManager = LocalMultiSimSettingsManager.getInstance(this);

        mIntent = getIntent();

        setContentView(R.layout.multi_sim_setting_tab);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab

        /*jiangxiaoke.hoperun 2012.7.12?add?CDMA/GSM switch option*/
        SubscriptionData cardSubscrInfo = CardSubscriptionManager.getInstance().getCardSubscriptions(CARD1);
        MSimTelephonyManager stm = (MSimTelephonyManager)this.getSystemService(Context.MSIM_TELEPHONY_SERVICE);
        boolean isDualModeCard = false;
        if(cardSubscrInfo != null) {
            //isDualModeCard = (cardSubscrInfo.getLength() == 2);
            isDualModeCard = (cardSubscrInfo.getLength() == 2 && SystemProperties.getBoolean("persist.dual.mode.enable",true));
            Log.d(LOG_TAG, "cardSubscrInfo.getLengthH:" + cardSubscrInfo.getLength() + " CardType(CARD1):" + stm.getCardType(CARD1));
        } else {
            Log.d(LOG_TAG,"We get nothing about card1, you need check the card slot");
        }
        /*jiangxiaoke.hoperun 2012.7.12?end*/
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            String packageName = mIntent.getStringExtra(MultiSimSettingsConstants.TARGET_PACKAGE);
            String className = mIntent.getStringExtra(MultiSimSettingsConstants.TARGET_CLASS);

            // come in from shortcut packagename and classname is null
            if (packageName == null)
                packageName = MultiSimSettingsConstants.CONFIG_PACKAGE;
            /*jiangxiaoke.hoperun 2012.7.12?add?CDMA/GSM switch option*/
            if(i == 0 && isDualModeCard){
                if(className == null){
                    className = MultiSimSettingsConstants.CONFIG_CLASS_FOR_DUAL_MODE;
                } else if (className.equals(MultiSimSettingsConstants.CONFIG_CLASS)){
                    className = MultiSimSettingsConstants.CONFIG_CLASS_FOR_DUAL_MODE;
                }
            } else if (className == null) {
                className = MultiSimSettingsConstants.CONFIG_CLASS;
            }
            /*jiangxiaoke.hoperun 2012.7.12 end*/
            // Create an Intent to launch an Activity for the tab (to be reused)
            intent = new Intent().setClassName(packageName, className)
                    .setAction(mIntent.getAction()).putExtra(SUBSCRIPTION_KEY, i);
            // Initialize a TabSpec for each tab and add it to the TabHost
            spec = tabHost.newTabSpec(tabSpecTags[i])
                    .setIndicator(getMultiSimName(i), res.getDrawable(tabIcons[i]))
                    .setContent(intent);
            Log.v("SETTINGS", " ");
            Log.v("SETTINGS", "getMultiSimName("+i+")  name is " + getMultiSimName(i));//getMultiSimName(0) is SLOT1    getMultiSimName(1) is SLOT2
            // Add new spec to Tab
            tabHost.addTab(spec);
        }
        tabHost.setCurrentTab(mIntent.getIntExtra(SUBSCRIPTION_KEY, 0));

        /*--Begin: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
        //mTelephonyManager = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        MSimTelephonyManager mSimTelephonyManager = MSimTelephonyManager.getDefault();
        int NetworkType1 = mSimTelephonyManager.getNetworkType(0);
        int NetworkType2 = mSimTelephonyManager.getNetworkType(1);

        String alpha1 = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorName(0);
        String alpha2 = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorName(1);

        if (DEBUG) Log.v("SETTINGS", " ");
        if (DEBUG) Log.v("SETTINGS", "alpha(" + 0 + ") :  " + alpha1);
        if (DEBUG) Log.v("SETTINGS", "alpha(" + 1 + ") :  " + alpha2);
        if (DEBUG) Log.v("SETTINGS", "NetworkType1 (" + 0 + ") :  " + NetworkType1);
        if (DEBUG) Log.v("SETTINGS", "NetworkType2 (" + 1 + ") :  " + NetworkType2);

        if (DEBUG) Log.v("SETTINGS", "getVoiceMailNumber: " + mTelephonyManager.getVoiceMailNumber());//    *86
        if (DEBUG) Log.v("SETTINGS", "getSimOperatorName: " + mTelephonyManager.getSimOperatorName());//    ZHONGWEN  China telcom
        if (DEBUG) Log.v("SETTINGS", "getNetworkCountryIso: " + mTelephonyManager.getNetworkCountryIso());//cn,cn
        if (DEBUG) Log.v("SETTINGS", "getCellLocation: " + mTelephonyManager.getCellLocation());//[8977,324591,1640916,13844,2]
        if (DEBUG) Log.v("SETTINGS", "getSimSerialNumber: " + mTelephonyManager.getSimSerialNumber());
        if (DEBUG) Log.v("SETTINGS", "getSimOperator: " + mTelephonyManager.getSimOperator());
        if (DEBUG) Log.v("SETTINGS", "getNetworkOperatorName: " + mTelephonyManager.getNetworkOperatorName());//China Telecom,China Mobile
        if (DEBUG) Log.v("SETTINGS", "getSubscriberId: " + mTelephonyManager.getSubscriberId());//null
        if (DEBUG) Log.v("SETTINGS", "getLine1Number: " + mTelephonyManager.getLine1Number());//null
        if (DEBUG) Log.v("SETTINGS", "getNetworkOperator: " + mTelephonyManager.getNetworkOperator());// 46003,46000
        if (DEBUG) Log.v("SETTINGS", "getSimCountryIso: " + mTelephonyManager.getSimCountryIso());//cn,
        if (DEBUG) Log.v("SETTINGS", "getVoiceMailAlphaTag: " + mTelephonyManager.getVoiceMailAlphaTag());//ZHONGWEN  YUYINXINXIANG
        if (DEBUG) Log.v("SETTINGS", "getNeighboringCellInfo: " + mTelephonyManager.getNeighboringCellInfo());//null
        if (DEBUG) Log.v("SETTINGS", "isNetworkRoaming: " + mTelephonyManager.isNetworkRoaming());//false
        if (DEBUG) Log.v("SETTINGS", "getDeviceId: " + mTelephonyManager.getDeviceId());//8020f946
        if (DEBUG) Log.v("SETTINGS", "getDeviceSoftwareVersion: " + mTelephonyManager.getDeviceSoftwareVersion());//0
        if (DEBUG) Log.v("SETTINGS", " ");
        /*--End: zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR, 20120604--*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocalManager.registerForSimNameChange(mHandler,
                MultiSimSettingsConstants.EVENT_MULTI_SIM_NAME_CHANGED, null);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private String getMultiSimName(int subscription) {
        return Settings.System.getString(this.getContentResolver(),
                Settings.System.MULTI_SIM_NAME[subscription]);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            switch (msg.what) {
                case MultiSimSettingsConstants.EVENT_MULTI_SIM_NAME_CHANGED:
                    handleSimNameChanged(ar);
                    break;
            }
        }
    };

    private void handleSimNameChanged(AsyncResult ar) {
        int subscription = ((Integer) ar.result).intValue();
        if (DBG)
            Log.d(LOG_TAG, "sim name changed on sub" + subscription);
        TextView simName = (TextView) getTabHost().getTabWidget().getChildAt(subscription)
                .findViewById(com.android.internal.R.id.title);
        simName.setText(mLocalManager.getMultiSimName(subscription));
    }
}
