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


 *    author:  zhangyaqiang add MULTI-SIM-SETTING-FOR-STATUSBAR
 *    date: 20120604


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

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC2_OPERATOR_NUMERIC;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import android.content.BroadcastReceiver;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.internal.telephony.SubscriptionManager;
import com.android.internal.telephony.SubscriptionData;
import com.android.internal.telephony.CardSubscriptionManager;
import com.android.internal.telephony.MSimPhoneFactory;
import com.android.internal.telephony.Phone;
import android.graphics.Color;
import android.telephony.MSimTelephonyManager;
import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.os.SystemProperties;
import android.provider.SpnProvider;
import com.android.internal.telephony.TelephonyProperties;
import android.view.View;
import android.view.MotionEvent;
import android.app.AlertDialog;
import com.android.internal.telephony.Subscription.SubscriptionStatus;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import com.android.internal.telephony.TelephonyIntents;

import com.android.settings.R;


public class MultiSimSettingView extends Activity {
    static final boolean DEBUG = false;
    private Context mContext;
    private String country;
    private Configuration configuration;
    private SubscriptionManager mSubscriptionManager;

    private final int CARD1_INDEX = 0;
    private final int CARD2_INDEX = 1;
    private final int MAX_SUBSCRIPTIONS = 2;
    public static final int SUBSCRIPTION_INDEX_INVALID = 99999;

    private ImageView card1;
    private ImageView card2;
    private ImageView card1Fence;
    private ImageView card2Fence;
    private TextView  card1SetPreferred;
    private TextView  card2SetPreferred;
    private ImageView card1CheckBox;
    private ImageView card2CheckBox;
    private TextView  card1Name;
    private TextView  card2Name;
    private TextView    okBtn;
    private TextView    cancelBtn;

    private boolean isChinesgeLanguage = false;
    private boolean isCard1Absent = true;
    private boolean isCard2Absent = true;
    private boolean isCard1Activated = false;
    private boolean isCard2Activated = false;

    private int card1NetworkType = -1;
    private int card2NetworkType = -1;
    private int card1State = -1;
    private int card2State = -1;

    private final int CARD1_PREFERRED = 0;
    private final int CARD2_PREFERRED = 1;
    private int preferredCardIndex = -1;

    private String card1Operator = null;
    private String card2Operator = null;
    private String card1OperatorName;
    private String card2OperatorName;
    private String card1NetworkOperatorName;
    private String card2NetworkOperatorName;

    private String userDefineCard1Name;
    private String userDefineCard2Name;
    private String card1PhoneName;
    private String card2PhoneName;

    private final int NOCARD1_NOCARD2    = 0;
    private final int ONLY_CARD1         = 1;
    private final int ONLY_CARD2         = 2;
    private final int CARD1_AND_CARD2    = 3;
    private int mSlotState = NOCARD1_NOCARD2;

    private static final int DISABLE    = 0;
    private static final int EBABLE    = 1;

    private boolean activatedCheckBox1TempState = false;
    private boolean activatedCheckBox2TempState = false;
    private int preferredTempState = -1;

    private AlertDialog mErrorDialog = null;
    private ProgressDialog mProgressDialog = null;

    private IntentFilter mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    //private Handler mHandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.multi_sim_setting_view);
        setTitle(R.string.select_net);
        Resources res = getResources();
        //Drawable drawable = res.getDrawable(R.drawable.bkcolor);
        //this.getWindow().setBackgroundDrawableResource(R.color.white); //setBackgroundDrawable(drawable);

        mContext = getBaseContext();
        mSubscriptionManager = SubscriptionManager.getInstance();
        initialize();
        initMultiSimView();
        setQuickSwitchListener();
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        //mHandler = new MyHandler(this);
    }
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregisterReceiver(mReceiver);
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
            mErrorDialog = null;
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    private boolean hasCard(int subscription) {
        CardSubscriptionManager cardSubMgr = CardSubscriptionManager.getInstance();
        if (cardSubMgr != null && cardSubMgr.getCardSubscriptions(subscription) != null) {
            return true;
        }
        return false;
    }

    private void initialize() {
        configuration = getResources().getConfiguration();
        country = configuration.locale.getCountry();
        //get language
        if (configuration.locale.getCountry().equals("CN")) {
            isChinesgeLanguage = true;
        } else {
            isChinesgeLanguage = false;
        }
        //get card1 is absent
        if (isCardAbsent(CARD1_INDEX)){
            isCard1Absent = true;
        } else {
            isCard1Absent = false;
        }
        //get card2 is absent
        if (isCardAbsent(CARD2_INDEX)){
            isCard2Absent = true;
        } else {
            isCard2Absent = false;
        }

        //get card1 is activated
        if (isSubActivated(CARD1_INDEX)){
            isCard1Activated = true;
            activatedCheckBox1TempState = true;
        } else {
            isCard1Activated = false;
            activatedCheckBox1TempState = false;
        }
        //get card2 is activated
        if (isSubActivated(CARD2_INDEX)){
            isCard2Activated = true;
            activatedCheckBox2TempState = true;
        } else {
            isCard2Activated = false;
            activatedCheckBox2TempState = false;
        }

        //get preferred
        preferredCardIndex = getPreferredSubscription();
        preferredTempState = preferredCardIndex;

        //get card1 and card2 network type
        getCardType();
        //get card1 and card2 state
        getCardState();
        //get card1 and card2 operator
        getCardOperator();
        //get card1 and card2 operator name
        getCardOperatorName();
        //get card1 and card2 network operator name
        getCardNetworkOperatorName();
        //get user define card1 name
        userDefineCard1Name = getMultiSimName(CARD1_INDEX);
        //get user define card2 name
        userDefineCard2Name = getMultiSimName(CARD2_INDEX);
        //get user define card1 name
        card1PhoneName = getMultiPhoneName(CARD1_INDEX);
        //get user define card2 name
        card2PhoneName = getMultiPhoneName(CARD2_INDEX);

        if (isCard1Absent && isCard2Absent) {
            mSlotState = NOCARD1_NOCARD2;
        } else if (!isCard1Absent && isCard2Absent) {
            mSlotState = ONLY_CARD1;
        } else if (isCard1Absent && !isCard2Absent) {
            mSlotState = ONLY_CARD2;
        } else if (!isCard1Absent && !isCard2Absent) {
            mSlotState = CARD1_AND_CARD2;
        }
    }
    private void initMultiSimView() {
        int icon = 0;
        String sim1Name = null;
        String sim2Name = null;

        card1 = (ImageView)findViewById(R.id.card1);
        card2 = (ImageView)findViewById(R.id.card2);
        card1Fence = (ImageView)findViewById(R.id.card1_fence);
        card2Fence = (ImageView)findViewById(R.id.card2_fence);

        card1SetPreferred = (TextView)findViewById(R.id.card1_preferred);
        card2SetPreferred = (TextView)findViewById(R.id.card2_preferred);
        card1CheckBox = (ImageView)findViewById(R.id.card1_checkbox);
        card2CheckBox = (ImageView)findViewById(R.id.card2_checkbox);
        card1Name = (TextView)findViewById(R.id.card1_name);
        card2Name = (TextView)findViewById(R.id.card2_name);

        okBtn = (TextView)findViewById(R.id.ok);
        cancelBtn = (TextView)findViewById(R.id.cancel);

        if ((card1OperatorName!=null && !card1OperatorName.isEmpty()) &&
            (card1NetworkOperatorName!=null && !card1NetworkOperatorName.isEmpty()
                && !card1NetworkOperatorName.equals("null"))) {
            sim1Name = isChinesgeLanguage ? card1OperatorName : card1NetworkOperatorName;
        } else if (card1OperatorName!=null && !card1OperatorName.isEmpty()) {
            sim1Name = card1OperatorName;
        } else if (card1NetworkOperatorName!=null && !card1NetworkOperatorName.isEmpty()
                && !card1NetworkOperatorName.equals("null")) {
            sim1Name = card1NetworkOperatorName;
        } else {
            sim1Name = getString(R.string.no_service);
        }

        if ((card2OperatorName!=null && !card2OperatorName.isEmpty()) &&
            (card2NetworkOperatorName!=null && !card2NetworkOperatorName.isEmpty()
                && !card2NetworkOperatorName.equals("null"))) {
            sim2Name = isChinesgeLanguage ? card2OperatorName : card2NetworkOperatorName;
        } else if (card2OperatorName!=null && !card2OperatorName.isEmpty()) {
            sim2Name = card2OperatorName;
        } else if (card2NetworkOperatorName!=null && !card2NetworkOperatorName.isEmpty()
                && !card2NetworkOperatorName.equals("null")) {
            sim2Name = card2NetworkOperatorName;
        } else {
            sim2Name = getString(R.string.no_service);
        }

        switch (mSlotState) {
            case NOCARD1_NOCARD2:
                //Card1
                card1.setImageResource(R.drawable.card_bg);
                card1Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card1 : R.drawable.en_card1);
                card1SetPreferred.setClickable(false);
                card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                //card1SetPreferred.setTextColor(Color.GRAY);
                card1CheckBox.setClickable(false);
                card1CheckBox.setBackgroundResource(R.drawable.btn_check_disable);
                card1Name.setText(sim1Name);
                card1Name.setClickable(false);

                //Card2
                card2.setImageResource(R.drawable.card_bg);
                card2Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card2 : R.drawable.en_card2);
                card2SetPreferred.setClickable(false);
                card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                //card2SetPreferred.setTextColor(Color.GRAY);
                card2CheckBox.setClickable(false);
                card2CheckBox.setBackgroundResource(R.drawable.btn_check_disable);
                card2Name.setText(sim2Name);
                card2Name.setClickable(false);
                break;
            case ONLY_CARD1:
                //Card1
                icon = getCardTypeIconId(CARD1_INDEX);
                card1.setImageResource(icon);
                card1Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card1 : R.drawable.en_card1);
                card1SetPreferred.setClickable(false);
                if (preferredCardIndex==CARD1_PREFERRED && isCard1Activated) {
                    card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_preferred);
                } else {
                    card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                }
                card1SetPreferred.setBackgroundResource(preferredCardIndex==CARD1_PREFERRED ?
                    R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal);
                //card1SetPreferred.setTextColor(isCard1Activated ? Color.WHITE : Color.GRAY);
                card1CheckBox.setClickable(true);
                card1CheckBox.setBackgroundResource(isCard1Activated ?
                    R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6);
                card1Name.setText(sim1Name);
                card1Name.setClickable(true);

                //Card2
                card2.setImageResource(R.drawable.card_bg);
                card2Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card2 : R.drawable.en_card2);
                card2SetPreferred.setClickable(false);
                card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                //card2SetPreferred.setTextColor(Color.GRAY);
                card2CheckBox.setClickable(false);
                card2CheckBox.setBackgroundResource(R.drawable.btn_check_disable);
                card2Name.setText(sim2Name);
                card2Name.setClickable(false);
                break;
            case ONLY_CARD2:
                //Card1
                card1.setImageResource(R.drawable.card_bg);
                card1Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card1 : R.drawable.en_card1);
                card1SetPreferred.setClickable(false);
                card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                //card1SetPreferred.setTextColor(Color.GRAY);
                card1CheckBox.setClickable(false);
                card1CheckBox.setBackgroundResource(R.drawable.btn_check_disable);
                card1Name.setText(sim1Name);
                card1Name.setClickable(false);

                //Card2
                icon = getCardTypeIconId(CARD2_INDEX);
                card2.setImageResource(icon);
                card2Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card2 : R.drawable.en_card2);
                card2SetPreferred.setClickable(false);
                if (preferredCardIndex==CARD2_PREFERRED && isCard2Activated) {
                    card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_preferred);
                } else {
                    card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                }
                card2SetPreferred.setBackgroundResource(preferredCardIndex==CARD2_PREFERRED ?
                    R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal);
                //card2SetPreferred.setTextColor(isCard2Activated ? Color.WHITE : Color.GRAY);
                card2CheckBox.setClickable(true);
                card2CheckBox.setBackgroundResource(isCard2Activated ?
                    R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6);
                card2Name.setText(sim2Name);
                card2Name.setClickable(true);
                break;
            case CARD1_AND_CARD2:
                //Card1
                icon = getCardTypeIconId(CARD1_INDEX);
                card1.setImageResource(icon);
                card1Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card1 : R.drawable.en_card1);
                card1SetPreferred.setClickable(true);
                if (preferredCardIndex==CARD1_PREFERRED && isCard1Activated) {
                    card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_preferred);
                } else {
                    card1SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                }
                card1SetPreferred.setBackgroundResource(preferredCardIndex==CARD1_PREFERRED ?
                    R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal);
                //card1SetPreferred.setTextColor(isCard1Activated ? Color.WHITE : Color.GRAY);
                card1CheckBox.setClickable(true);
                card1CheckBox.setBackgroundResource(isCard1Activated ?
                    R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6);
                card1Name.setText(sim1Name);
                card1Name.setClickable(true);

                //Card2
                icon = getCardTypeIconId(CARD2_INDEX);
                card2.setImageResource(icon);
                card2Fence.setImageResource(isChinesgeLanguage ?
                    R.drawable.zh_cn_card2 : R.drawable.en_card2);
                card2SetPreferred.setClickable(true);
                if (preferredCardIndex==CARD2_PREFERRED && isCard2Activated) {
                    card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_preferred);
                } else {
                    card2SetPreferred.setBackgroundResource(R.drawable.btn_switch_normal);
                }
                card2SetPreferred.setBackgroundResource(preferredCardIndex==CARD2_PREFERRED ?
                    R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal);
                //card2SetPreferred.setTextColor(isCard2Activated ? Color.WHITE : Color.GRAY);
                card2CheckBox.setClickable(true);
                card2CheckBox.setBackgroundResource(isCard2Activated ?
                    R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6);
                card2Name.setText(sim2Name);
                card2Name.setClickable(true);
                break;
        }
    }

    /**
       * Returns a constant indicating the state of the
       * device SIM card in a slot.
       *
       * @param slotId
       *
       * @see #SIM_STATE_UNKNOWN
       * @see #SIM_STATE_ABSENT
       * @see #SIM_STATE_PIN_REQUIRED
       * @see #SIM_STATE_PUK_REQUIRED
       * @see #SIM_STATE_NETWORK_LOCKED
       * @see #SIM_STATE_READY
       * @see #SIM_STATE_CARD_IO_ERROR
       */
    private void getCardState() {
        card1State = MSimTelephonyManager.getDefault().getSimState(CARD1_INDEX);
        card2State = MSimTelephonyManager.getDefault().getSimState(CARD2_INDEX);
    }

    /**
       * Returns a constant indicating the radio technology (network type)
       * currently in use on the device for data transmission for a subscription
       * @return the network type
       *
       * @param subscription for which network type is returned
       *
       * @see #NETWORK_TYPE_UNKNOWN
       * @see #NETWORK_TYPE_GPRS
       * @see #NETWORK_TYPE_EDGE
       * @see #NETWORK_TYPE_UMTS
       * @see #NETWORK_TYPE_HSDPA
       * @see #NETWORK_TYPE_HSUPA
       * @see #NETWORK_TYPE_HSPA
       * @see #NETWORK_TYPE_CDMA
       * @see #NETWORK_TYPE_EVDO_0
       * @see #NETWORK_TYPE_EVDO_A
       * @see #NETWORK_TYPE_EVDO_B
       * @see #NETWORK_TYPE_1xRTT
       * @see #NETWORK_TYPE_EHRPD
       * @see #NETWORK_TYPE_LTE
       */
    private void getCardType() {
        MSimTelephonyManager mSimTelephonyManager = MSimTelephonyManager.getDefault();
        card1NetworkType = mSimTelephonyManager.getNetworkType(CARD1_INDEX);
        card2NetworkType = mSimTelephonyManager.getNetworkType(CARD2_INDEX);
    }

    /**
       * Returns a card is absent
       * device SIM card in a slot.
       *
       * @param cardIndex is card index,  0 is card1, 1 is card2
       *
       */
    private boolean isCardAbsent(int cardIndex) {
        //MSimTelephonyManager telManager = MSimTelephonyManager.getDefault();
        //return telManager.getSimState(cardIndex) == TelephonyManager.SIM_STATE_ABSENT;

        CardSubscriptionManager cardSubMgr = CardSubscriptionManager.getInstance();
        if (cardSubMgr != null && cardSubMgr.getCardSubscriptions(cardIndex) != null) {
            return false;
        }
        return true;
    }

    /**
       * Returns a card is activated
       * device SIM card in a slot.
       *
       * @param cardIndex is card index,  0 is card1, 1 is card2
       *
       */
    private boolean isSubActivated(int cardIndex) {
        return mSubscriptionManager.isSubActive(cardIndex);
    }
    /**
       * get card1 and card2 Operator
       *
       * @return 46003 or 46002 or ...
       */
    private void getCardOperator() {
        card1Operator = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperator(CARD1_INDEX);
        card2Operator = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperator(CARD2_INDEX);
    }
    /**
       * get card1 and card2 OperatorName
       *
       * @return 46003 or 46002 or ...
       */
    private void getCardOperatorName() {
        card1OperatorName = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorName(CARD1_INDEX);
        card2OperatorName = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorName(CARD2_INDEX);
    }

    private void getCardNetworkOperatorName() {
        card1NetworkOperatorName = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName(CARD1_INDEX);
        card2NetworkOperatorName = ((MSimTelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName(CARD2_INDEX);
    }

    private String getMultiSimName(int subscription) {
        return Settings.System.getString(this.getContentResolver(),
                Settings.System.MULTI_SIM_NAME[subscription]);
    }

    /**
       * Returns a constant indicating the radio technology (Phone Name)
       * currently in use on the device for data transmission for a subscription
       * @return the network type
       *
       * @param subscription for which network type is returned
       *
       * @see #GSM
       * @see #CDMA
       * @see #LTE
       * @see #null
       */
    private String getMultiPhoneName(int subscription) {
        return MSimPhoneFactory.getPhone(subscription).getPhoneName();
    }
    private int getCardTypeIconId(int subscription) {
        int iconId = 0;
        String cardOperator = subscription==CARD1_INDEX ? card1Operator: card2Operator;

        if (cardOperator.equals("46003")) {
            //China Telecom
            iconId = R.drawable.china_telecom;
        } else if (cardOperator.equals("46001")) {
            //China Unicom
            iconId = R.drawable.china_lt;
        } else if (cardOperator.equals("46000") || cardOperator.equals("46002")) {
            // China Mobile
            iconId = R.drawable.china_mobile;
        } else {
            if (getMultiPhoneName(subscription).equals("CDMA") || getMultiPhoneName(CARD1_INDEX).equals("cdma")) {
                //DEFAULT CDMA
                iconId = R.drawable.default_card_cdma;
            } else if (getMultiPhoneName(subscription).equals("GSM") || getMultiPhoneName(CARD2_INDEX).equals("GSM")){
                //DEFAULT GSM
                iconId = R.drawable.default_card_gsm;
            } else {
                //DEFAULT GSM
                iconId = R.drawable.default_card_gsm;
            }
        }
        return iconId;
    }
    private int getPreferredSubscription() {
        return MSimPhoneFactory.getVoiceSubscription();
    }

    private void setQuickSwitchListener() {
        if (card1CheckBox != null && !isCard1Absent) {
            card1CheckBox.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card1CheckBox.setOnClickListener(mDualSimSettingViewOnClickListener);
        }
        if (card2CheckBox != null && !isCard2Absent) {
            card2CheckBox.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card2CheckBox.setOnClickListener(mDualSimSettingViewOnClickListener);
        }

        if (card1Name != null && !isCard1Absent) {
            card1Name.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card1Name.setOnClickListener(mDualSimSettingViewOnClickListener);
        }
        if (card2Name != null && !isCard2Absent) {
            card2Name.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card2Name.setOnClickListener(mDualSimSettingViewOnClickListener);
        }

        if (okBtn != null) {
            okBtn.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            okBtn.setOnClickListener(mDualSimSettingViewOnClickListener);
        }
        if (cancelBtn != null) {
            cancelBtn.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            cancelBtn.setOnClickListener(mDualSimSettingViewOnClickListener);
        }

        if (card1SetPreferred != null && mSlotState == CARD1_AND_CARD2 && isCard1Activated && isCard2Activated) {
            card1SetPreferred.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card1SetPreferred.setOnClickListener(mDualSimSettingViewOnClickListener);
        }
        if (card2SetPreferred != null && mSlotState == CARD1_AND_CARD2 && isCard1Activated && isCard2Activated) {
            card2SetPreferred.setOnTouchListener(mDualSimSettingViewOnTouchListener);
            card2SetPreferred.setOnClickListener(mDualSimSettingViewOnClickListener);
        }

    }

    View.OnTouchListener mDualSimSettingViewOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int id = v.getId();

            switch (id) {
                case R.id.card1_checkbox:
                case R.id.card1_name:
                    break;

                case R.id.card2_checkbox:
                case R.id.card2_name:
                    break;

                case R.id.ok:
                case R.id.cancel:
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        v.setBackgroundResource(R.drawable.button_press);
                    }
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                        v.setBackgroundResource(R.drawable.button_normal);
                    }
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_CANCEL) {
                        v.setBackgroundResource(R.drawable.button_normal);
                    }
                    break;

                default:
                    break;
            }
            return false;
        }
    };

    View.OnClickListener mDualSimSettingViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            int icon = 0;

            switch (id) {
                case R.id.card1_checkbox:
                    activatedCheckBox1TempState = !activatedCheckBox1TempState;
                    icon = activatedCheckBox1TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                    v.setBackgroundResource(icon);
                    break;
                case R.id.card1_name:
                    activatedCheckBox1TempState = !activatedCheckBox1TempState;
                    icon = activatedCheckBox1TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                    card1CheckBox.setBackgroundResource(icon);
                    break;

                case R.id.card2_checkbox:
                    activatedCheckBox2TempState = !activatedCheckBox2TempState;
                    icon = activatedCheckBox2TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                    v.setBackgroundResource(icon);
                    break;
                case R.id.card2_name:
                    activatedCheckBox2TempState = !activatedCheckBox2TempState;
                    icon = activatedCheckBox2TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                    card2CheckBox.setBackgroundResource(icon);
                    break;

                case R.id.card1_preferred:
                    preferredTempState = CARD1_INDEX;
                    icon = preferredTempState==CARD1_PREFERRED ? R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal;
                    card1SetPreferred.setBackgroundResource(icon);
                    icon = preferredTempState==CARD2_PREFERRED ? R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal;
                    card2SetPreferred.setBackgroundResource(icon);
                    break;

                case R.id.card2_preferred:
                    preferredTempState = CARD2_INDEX;
                    icon = preferredTempState==CARD1_PREFERRED ? R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal;
                    card1SetPreferred.setBackgroundResource(icon);
                    icon = preferredTempState==CARD2_PREFERRED ? R.drawable.btn_switch_preferred : R.drawable.btn_switch_normal;
                    card2SetPreferred.setBackgroundResource(icon);
                    break;

                case R.id.ok:
                    if (activatedCheckBox1TempState != isCard1Activated) {
                        if (Settings.System.getInt(mContext.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) != 0) {
                            // do nothing but warning
                            displayAlertDialog(mContext.getString(R.string.sim_enabler_airplane_on));
                            return;
                        }

                        for (int i=0; i<TelephonyManager.getDefault().getPhoneCount(); i++) {
                            if (MSimTelephonyManager.getDefault().getCallState(i) != TelephonyManager.CALL_STATE_IDLE) {
                                // do nothing but warning
                                displayAlertDialog(mContext.getString(R.string.sim_enabler_in_call));
                                activatedCheckBox1TempState = isCard1Activated;
                                icon = activatedCheckBox1TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                                card1CheckBox.setBackgroundResource(icon);
                                return;
                            }
                        }
                        if (!activatedCheckBox1TempState){
                            if (mSubscriptionManager.getActiveSubscriptionsCount() > 0){
                                sendCommand(activatedCheckBox1TempState, 0);
                            }else{
                                displayAlertDialog(mContext.getString(R.string.sim_enabler_both_inactive));
                                return;
                            }
                        }else{
                            sendCommand(activatedCheckBox1TempState, 0);
                        }
                    }
                    if (activatedCheckBox2TempState != isCard2Activated) {
                        if (Settings.System.getInt(mContext.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) != 0) {
                            // do nothing but warning
                            displayAlertDialog(mContext.getString(R.string.sim_enabler_airplane_on));
                            return;
                        }

                        for (int i=0; i<TelephonyManager.getDefault().getPhoneCount(); i++) {
                            if (MSimTelephonyManager.getDefault().getCallState(i) != TelephonyManager.CALL_STATE_IDLE) {
                                // do nothing but warning
                                displayAlertDialog(mContext.getString(R.string.sim_enabler_in_call));
                                activatedCheckBox2TempState = isCard2Activated;
                                icon = activatedCheckBox2TempState ? R.drawable.btn_check_on_6 : R.drawable.btn_check_off_6;
                                card2CheckBox.setBackgroundResource(icon);
                                return;
                            }
                        }
                        if (!activatedCheckBox2TempState){
                            if (mSubscriptionManager.getActiveSubscriptionsCount() > 0){
                                sendCommand(activatedCheckBox2TempState, 1);
                            }else{
                                displayAlertDialog(mContext.getString(R.string.sim_enabler_both_inactive));
                                return;
                            }
                        }else{
                            sendCommand(activatedCheckBox2TempState, 1);
                        }
                    }
                    if (preferredTempState != preferredCardIndex) {
                        if (((preferredTempState==CARD1_PREFERRED) && activatedCheckBox1TempState) ||
                                ((preferredTempState==CARD2_PREFERRED) && activatedCheckBox2TempState)) {
                            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_PREFERRED), 500);
                        }
                    }
                    finish();
                    break;
                case R.id.cancel:
                    finish();
                    break;

                default:
                    break;
            }
        }
    };
    private void displayAlertDialog(String msg) {
        mErrorDialog = new AlertDialog.Builder(MultiSimSettingView.this)
             .setTitle(android.R.string.dialog_alert_title)
             .setMessage(msg)
             .setCancelable(false)
             .setNeutralButton(R.string.close_dialog, null)
             .show();
    }
    private void sendCommand(boolean enabled, int mSubscriptionId){
        SubscriptionData subData = new SubscriptionData(MAX_SUBSCRIPTIONS);
        for(int i=0;i<MAX_SUBSCRIPTIONS;i++) {
            subData.subscription[i].copyFrom(mSubscriptionManager.getCurrentSubscription(i));
        }
        if (enabled){
            subData.subscription[mSubscriptionId].slotId = mSubscriptionId;
            subData.subscription[mSubscriptionId].subId = mSubscriptionId;
            mSubscriptionManager.setDefaultAppIndex(subData.subscription[mSubscriptionId]);
            subData.subscription[mSubscriptionId].subStatus = SubscriptionStatus.SUB_ACTIVATE;
            //mSubscriptionManager.registerForSubscriptionActivated(
            //    mSubscriptionId, mHandler, EVENT_SIM_ACTIVATE_DONE, null);
        }else{
            subData.subscription[mSubscriptionId].slotId = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].subId = mSubscriptionId;
            subData.subscription[mSubscriptionId].subStatus = SubscriptionStatus.SUB_DEACTIVATE;
            //mSubscriptionManager.registerForSubscriptionDeactivated(
            //    mSubscriptionId, mHandler, EVENT_SIM_DEACTIVATE_DONE, null);
        }
        //displayProgressDialog(enabled, mSubscriptionId);
        mSubscriptionManager.setSubscription(subData);
    }
    private void displayProgressDialog(boolean enabled, int mSubscriptionId){
        String title = Settings.System.getString(mContext.getContentResolver(),Settings.System.MULTI_SIM_NAME[mSubscriptionId]);
        String msg = mContext.getString(enabled?R.string.sim_enabler_enabling:R.string.sim_enabler_disabling);
        mProgressDialog = new ProgressDialog(MultiSimSettingView.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action) ||
                Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                update();
            }
        }
    };
    private void update() {
        mSubscriptionManager = SubscriptionManager.getInstance();
        initialize();
        initMultiSimView();
        setQuickSwitchListener();
    }

    private static final int MSG_SET_PREFERRED = 100;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case MSG_SET_PREFERRED:
                    if (MSimPhoneFactory.isPromptEnabled()) {
                        MSimPhoneFactory.setVoiceSubscription(preferredTempState);
                        MSimPhoneFactory.setDataSubscription(0);//preferredTempState
                        MSimPhoneFactory.setSMSSubscription(preferredTempState);

                        SubscriptionManager subManager = SubscriptionManager.getInstance();
                        subManager.setDataSubscription(0, null);//preferredTempState
                    } else {
                    }
                    break;
            }
            return;
        }
    };

}
