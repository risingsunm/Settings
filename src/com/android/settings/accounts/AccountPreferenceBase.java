/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2011-2012, Code Aurora Forum. All rights reserved.
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

package com.android.settings.accounts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.android.settings.SettingsPreferenceFragment;
import com.google.android.collect.Maps;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncAdapterType;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

class AccountPreferenceBase extends SettingsPreferenceFragment
        implements OnAccountsUpdateListener {

    protected static final String TAG = "AccountSettings";
    public static final String AUTHORITIES_FILTER_KEY = "authorities";
    public static final String ACCOUNT_TYPES_FILTER_KEY = "account_types";
    public static final String ACCOUNT_TYPE_SIM = "com.android.sim";
    public static final String ACCOUNT_TYPE_PHONE = "com.android.localphone";
    private Map<String, AuthenticatorDescription> mTypeToAuthDescription
            = new HashMap<String, AuthenticatorDescription>();
    protected AuthenticatorDescription[] mAuthDescs;
    private final Handler mHandler = new Handler();
    private Object mStatusChangeListenerHandle;
    private HashMap<String, ArrayList<String>> mAccountTypeToAuthorities = null;

    /**
     * Overload to handle account updates.
     */
    public void onAccountsUpdated(Account[] accounts) {

    }

    /**
     * Overload to handle authenticator description updates
     */
    protected void onAuthDescriptionsUpdated() {

    }

    /**
     * Overload to handle sync state updates.
     */
    protected void onSyncStateUpdated() {

    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                | ContentResolver.SYNC_OBSERVER_TYPE_STATUS
                | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS,
                mSyncStatusObserver);
        onSyncStateUpdated();
    }

    @Override
    public void onPause() {
        super.onPause();
        ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
    }


    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            mHandler.post(new Runnable() {
                public void run() {
                    onSyncStateUpdated();
                }
            });
        }
    };

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        if (mAccountTypeToAuthorities == null) {
            mAccountTypeToAuthorities = Maps.newHashMap();
            SyncAdapterType[] syncAdapters = ContentResolver.getSyncAdapterTypes();
            for (int i = 0, n = syncAdapters.length; i < n; i++) {
                final SyncAdapterType sa = syncAdapters[i];
                ArrayList<String> authorities = mAccountTypeToAuthorities.get(sa.accountType);
                if (authorities == null) {
                    authorities = new ArrayList<String>();
                    mAccountTypeToAuthorities.put(sa.accountType, authorities);
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.d(TAG, "added authority " + sa.authority + " to accountType "
                            + sa.accountType);
                }
                authorities.add(sa.authority);
            }
        }
        return mAccountTypeToAuthorities.get(type);
    }

    /**
     * Gets an icon associated with a particular account type. If none found, return null.
     * @param accountType the type of account
     * @return a drawable for the icon or null if one cannot be found.
     */
    protected Drawable getDrawableForType(final String accountType) {
        Drawable icon = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                Context authContext = getActivity().createPackageContext(desc.packageName, 0);
                icon = authContext.getResources().getDrawable(desc.iconId);
            } catch (PackageManager.NameNotFoundException e) {
                // TODO: place holder icon for missing account icons?
                Log.w(TAG, "No icon name for account type " + accountType);
            } catch (Resources.NotFoundException e) {
                // TODO: place holder icon for missing account icons?
                Log.w(TAG, "No icon resource for account type " + accountType);
            }
        }
        return icon;
    }

    /**
     * Gets the label associated with a particular account type. If none found, return null.
     * @param accountType the type of account
     * @return a CharSequence for the label or null if one cannot be found.
     */
    protected CharSequence getLabelForType(final String accountType) {
        CharSequence label = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            try {
                AuthenticatorDescription desc = mTypeToAuthDescription.get(accountType);
                Context authContext = getActivity().createPackageContext(desc.packageName, 0);
                label = authContext.getResources().getText(desc.labelId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "No label name for account type " + accountType);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "No label icon for account type " + accountType);
            }
        }
        return label;
    }

    /**
     * Gets the preferences.xml file associated with a particular account type.
     * @param accountType the type of account
     * @return a PreferenceScreen inflated from accountPreferenceId.
     */
    protected PreferenceScreen addPreferencesForType(final String accountType) {
        PreferenceScreen prefs = null;
        if (mTypeToAuthDescription.containsKey(accountType)) {
            AuthenticatorDescription desc = null;
            try {
                desc = mTypeToAuthDescription.get(accountType);
                if (desc != null && desc.accountPreferencesId != 0 &&
                    !ACCOUNT_TYPE_PHONE.equals(accountType) &&
                    !ACCOUNT_TYPE_SIM.equals(accountType)) {
                    Context authContext = getActivity().createPackageContext(desc.packageName, 0);
                    prefs = getPreferenceManager().inflateFromResource(authContext,
                            desc.accountPreferencesId, getPreferenceScreen());
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            }
        }
        return prefs;
    }

    /**
     * Updates provider icons. Subclasses should call this in onCreate()
     * and update any UI that depends on AuthenticatorDescriptions in onAuthDescriptionsUpdated().
     */
    protected void updateAuthDescriptions() {
        mAuthDescs = AccountManager.get(getActivity()).getAuthenticatorTypes();
        for (int i = 0; i < mAuthDescs.length; i++) {
            mTypeToAuthDescription.put(mAuthDescs[i].type, mAuthDescs[i]);
        }
        onAuthDescriptionsUpdated();
    }
}
