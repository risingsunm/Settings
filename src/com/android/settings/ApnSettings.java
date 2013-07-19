/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2010-2012, Code Aurora Forum. All rights reserved.

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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObservable;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.telephony.MSimTelephonyManager;
import android.database.ContentObserver;
import android.content.ContentResolver;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import java.util.ArrayList;

public class ApnSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";
    public Context mContext;
    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static final String ChinaUnionPLMN = "46001";
    /*ren_kang.hoperun 2012.6.6 add for show China Telecom Apn*/
    private static final String ChinaTelePLMN = "46003";
    /*ren_kang.hoperun 2012.6.6 end*/
    private static boolean mRestoreDefaultApnMode;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;

    private String mSelectedKey;
    private int mSubscription = 0;

    private IntentFilter mMobileStateFilter;
    /*ren_kang.hoperun 2012.7.12 The status bar switch APN can be updated in the interface*/
    private ContentObserver observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            fillList();
        }
    };
    /*ren_kang.hoperun 2012.7.12 end*/
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                Phone.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        Log.i("TAG", "onReceive-!mRestoreDefaultApnMode="+!mRestoreDefaultApnMode);
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
            }
        }
    };
    

    private static Phone.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(Phone.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(Phone.DataState.class, str);
        } else {
            return Phone.DataState.DISCONNECTED;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);
        mSubscription = getIntent().getIntExtra(SelectSubscription.SUBSCRIPTION_KEY,
                MSimTelephonyManager.getDefault().getDefaultSubscription());
        Log.d(TAG, "onCreate received sub :" + mSubscription);
        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        /*ren_kang.hoperun 2012.7.12 The status bar switch APN can be updated in the interface*/
        getContentResolver().registerContentObserver(PREFERAPN_URI, true, observer);
        /*ren_kang.hoperun 2012.7.12 end*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mSubscription = intent.getIntExtra(SelectSubscription.SUBSCRIPTION_KEY,
                        MSimTelephonyManager.getDefault().getDefaultSubscription());
        Log.d(TAG,"onNewIntent mSubscription="+mSubscription);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mMobileStateReceiver, mMobileStateFilter);
        /*ren_kang.hoperun 2012.7.12 The status bar switch APN can be updated in the interface*/
        getContentResolver().registerContentObserver(PREFERAPN_URI, true, observer);
        /*ren_kang.hoperun 2012.7.12 end*/
        if (!mRestoreDefaultApnMode) {
            Log.i("TAG", "onResume-!mRestoreDefaultApnMode="+!mRestoreDefaultApnMode);
            fillList();
        } else {
            showDialog(DIALOG_RESTORE_DEFAULTAPN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*ren_kang.hoperun 2012.7.12 The status bar switch APN can be updated in the interface*/
        getContentResolver().unregisterContentObserver(observer);
        /*ren_kang.hoperun 2012.7.12 end*/
        unregisterReceiver(mMobileStateReceiver);
    }

    private void fillList() {
        String where = "numeric=\""
                + MSimTelephonyManager.getTelephonyProperty
                    (TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, mSubscription, "46003")
                + "\"";
        Log.i("TAG", "where="+where);
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "numeric", "name", "apn", "type"}, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        Log.i("TAG", "filllist1");
        PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
        Log.i("TAG", "after apn_list");
        apnList.removeAll();
        ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

        mSelectedKey = getSelectedApnKey();
        Log.i("TAG", "mSelectedKey="+mSelectedKey);
        cursor.moveToFirst();
        Log.i("TAG", "cursor movetofirst");
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String apn = cursor.getString(cursor.getColumnIndex("apn"));
            String key = cursor.getString(cursor.getColumnIndex("_id"));
            String type = cursor.getString(cursor.getColumnIndex("type"));
            /*ren_kang.hoperun 2012.8.4 add for you can add gprs apn or ro apn but it is not selectable*/
            String numeric = cursor.getString(cursor.getColumnIndex("numeric"));
            /*ren_kang.hoperun 2012.8.4 end*/
            Log.i(TAG, "name="+name);
            Log.i(TAG, "apn="+apn);
            Log.i(TAG, "key="+key);
            Log.i(TAG, "type="+type);
            Log.i(TAG, "rk_numeric="+numeric);
            ApnPreference pref = new ApnPreference(this);
            if (name.contains("China Telecom")){
                //for china union
                /*ren_kang.hoperun 2012.6.6 change to show China Telecom APN*/
                Log.i("TAG", "if name contains Telecom");
//                if (ChinaTelePLMN.equals(MSimTelephonyManager.getTelephonyProperty(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, mSubscription, "46003"))){
//                    if (type.equals("default")){
                        if (apn.contains("wap")){
                            name = getString(R.string.china_telecom_wap_apn_name);
                        }else{
                            name = getString(R.string.china_telecom_net_apn_name);
                        }
//                    }
//                    if (type.equals("mms")) name = getString(R.string.china_union_mms_apn_name);
//                    if (type.equals("supl")) name = getString(R.string.china_union_supl_apn_name);
                /*ren_kang.hoperun END*/
                    //pref.setIsDefault(true);
                    pref.setIsDefault(false);
//                }
            }

            pref.setKey(key);
            pref.setTitle(name);
            pref.setSummary(apn);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);
            /*ren_kang.hoperun 2012.8.4 add for you can add gprs apn or ro apn but it is not selectable*/
            boolean selectable;
            boolean gsmApnEnable = SystemProperties.getBoolean("ro.ril.gsm.apn.enable", true);
            if (!gsmApnEnable){
                selectable = ((type == null) || (!type.equals("mms") && !numeric.equals("46000") &&
                        !numeric.equals("46001") && !numeric.equals("46002") && !numeric.equals("46007")));
            }
            else{
                selectable = ((type == null) || (!type.equals("mms")));
            }
            /*ren_kang.hoperun 2012.8.4 end*/
            pref.setSelectable(selectable);
            if (selectable) {
                if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                    pref.setChecked();
                }
                apnList.addPreference(pref);
            } else {
                mmsApnList.add(pref);
            }
            cursor.moveToNext();
        }
        cursor.close();

        for (Preference preference : mmsApnList) {
            apnList.addPreference(preference);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        intent.putExtra(SelectSubscription.SUBSCRIPTION_KEY, mSubscription);
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }
        /*ren_kang.hoperun 2012.7.19 add to make effect when you change APN*/
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(false);
        cm.setMobileDataEnabled(true);
        /*ren_kang.hoperun 2012.7.19 end*/
        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(PREFERAPN_URI, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null) {
            HandlerThread restoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            restoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    restoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    dismissDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        ApnSettings.this,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(DEFAULTAPN_URI, null, null);                  
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            getPreferenceScreen().setEnabled(false);
        }
    }
    
}
