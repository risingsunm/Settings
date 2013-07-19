/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-8-8
 *
 */
package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.widget.LockPatternUtils;
import com.qrd.plugin.common_interface.ISystemInfoDetectHandler;
import com.qrd.plugin.feature_query.DefaultQuery;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * @author mzikun
 * Gesture lock pattern settings.
 */

public class LocationAndSecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener {

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST = 124;

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SIM_LOCK_SETTINGS = "sim_lock_settings";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_RESET_CREDENTIALS = "reset_credentials";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;

    private CheckBoxPreference mShowPassword;

    private Preference mResetCredentials;

    private CheckBoxPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;
    private CheckBoxPreference mPowerButtonInstantlyLocks;

    private static final String SYSTEM_DETECT_HANDLER = "com.qrd.systeminfodetect.SystemInfoDetectHandler";

    // Location Settings
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_GPS = "location_gps";
    private static final String KEY_ASSISTED_GPS = "assisted_gps";
    private static final String KEY_USE_LOCATION = "location_use_for_services";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;
    private CheckBoxPreference mUseLocation;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        // Location Settings
        addPreferencesFromResource(R.xml.location_settings);

        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Location Settings
        mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
        mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) root.findPreference(KEY_ASSISTED_GPS);
        if (GoogleLocationSettingHelper.isAvailable(getActivity())) {
            // GSF present, Add setting for 'Use My Location'
            CheckBoxPreference useLocation = new CheckBoxPreference(getActivity());
            useLocation.setKey(KEY_USE_LOCATION);
            useLocation.setTitle(R.string.use_location_title);
            useLocation.setSummary(R.string.use_location_summary);
            useLocation.setChecked(
                    GoogleLocationSettingHelper.getUseLocationForServices(getActivity())
                    == GoogleLocationSettingHelper.USE_LOCATION_FOR_SERVICES_ON);
            useLocation.setPersistent(false);
            useLocation.setOnPreferenceChangeListener(this);
            getPreferenceScreen().addPreference(useLocation);
            mUseLocation = useLocation;
        }

        // Change the summary for wifi-only devices
        if (Utils.isWifiOnly(getActivity())) {
            mNetwork.setSummaryOn(R.string.location_neighborhood_level_wifi);
        }

        //Security Settings
        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure()) {
            if (mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        addPreferencesFromResource(resid);

        boolean isSystemEmmcDetect = false;
        try {
            Class c = Class.forName(SYSTEM_DETECT_HANDLER);
            ISystemInfoDetectHandler handler = (ISystemInfoDetectHandler) c.newInstance();
            isSystemEmmcDetect = handler.isFileSystemAutoDetect();
            Log.i("SecuritySettings", "isSystemEmmcDetect = " + isSystemEmmcDetect);
        } catch (Exception e) {
            Log.i("SecuritySettings", "exception " + e);
            isSystemEmmcDetect = false;
        }
        if ((DefaultQuery.SYSTEM_INFO_DETECT_ENABLED == 1) ||
                ((DefaultQuery.SYSTEM_INFO_DETECT_ENABLED == 2) && isSystemEmmcDetect)) {
            // Add options for device encryption
            DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

            switch (dpm.getStorageEncryptionStatus()) {
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    // The device is currently encrypted.
                    addPreferencesFromResource(R.xml.security_settings_encrypted);
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                    // This device supports encryption but isn't encrypted.
                    addPreferencesFromResource(R.xml.security_settings_unencrypted);
                    break;
            }
        }

        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (CheckBoxPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);

        // don't display visible pattern if biometric and backup is not
        // pattern
        if (resid == R.xml.security_settings_biometric_weak &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }

        // tactile feedback. Should be common to all unlock preference
        // screens.
        mTactileFeedback = (CheckBoxPreference) root
                .findPreference(KEY_TACTILE_FEEDBACK_ENABLED);
        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mTactileFeedback != null) {
                securityCategory.removePreference(mTactileFeedback);
            }
        }

        // Append the rest of the settings
        addPreferencesFromResource(R.xml.security_settings_misc);

        MSimTelephonyManager tm = MSimTelephonyManager.getDefault();
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        boolean disableLock = false;
        for (int i = 0; i < numPhones; i++) {
            // Disable SIM lock if sim card is missing or unknown
            // notice:cdma can also set sim lock
            if ((tm.getSimState(i) == TelephonyManager.SIM_STATE_ABSENT) ||
                    (tm.getSimState(i) == TelephonyManager.SIM_STATE_UNKNOWN)) {
                disableLock = true;
            } else {
                disableLock = false;
                break;
            }
        }
        if (disableLock) {
            root.findPreference(KEY_SIM_LOCK).setEnabled(false);
        }

        // Show password
        mShowPassword = (CheckBoxPreference) root.findPreference(KEY_SHOW_PASSWORD);

        // SIM/RUIM lock
        Preference iccLock = (Preference) root.findPreference(KEY_SIM_LOCK_SETTINGS);

        Intent intent = new Intent();
        if (tm.isMultiSimEnabled()) {
            intent.setClassName("com.android.settings",
                    "com.android.settings.multisimsettings.MultiSimSettingTab");
            intent.putExtra(SelectSubscription.PACKAGE, "com.android.settings");
            intent.putExtra(SelectSubscription.TARGET_CLASS,
                    "com.android.settings.IccLockSettings");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.IccLockSettings");
        }
        iccLock.setIntent(intent);

        // Credential storage
        mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);

        mToggleAppInstallation = (CheckBoxPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        return root;
    }


    private boolean isNonMarketAppsAllowed() {
        return Settings.Secure.getInt(getContentResolver(),
                                      Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        // Change the system setting
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps && which == DialogInterface.BUTTON_POSITIVE) {
            setNonMarketAppsAllowed(true);
            mToggleAppInstallation.setChecked(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mTactileFeedback != null) {
            mTactileFeedback.setChecked(lockPatternUtils.isTactileFeedbackEnabled());
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }

        mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);

        KeyStore.State state = KeyStore.getInstance().state();
        mResetCredentials.setEnabled(state != KeyStore.State.UNINITIALIZED);


        //Location Settings
        updateLocationToggles();

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
            mContentQueryMap.addObserver(mSettingsObserver);
        }

    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateLocationToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mNetwork.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(gpsEnabled);
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Secure.getInt(res,
                    Settings.Secure.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST, null, null)) {
                startBiometricWeakImprove(); // no password set, so no need to confirm
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            lockPatternUtils.setPowerButtonInstantlyLocks(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mToggleAppInstallation) {
            if (mToggleAppInstallation.isChecked()) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
            } else {
                setNonMarketAppsAllowed(false);
            }
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        }
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        }
        //Location Settings
        else if (preference == mUseLocation) {
            boolean newValue = (value == null ? false : (Boolean) value);
            GoogleLocationSettingHelper.setUseLocationForServices(getActivity(), newValue);
            // We don't want to change the value immediately here, since the user may click
            // disagree in the dialog that pops up. When the activity we just launched exits, this
            // activity will be restated and the new value re-read, so the checkbox will get its
            // new value then.
            return false;
        }

        return true;
    }

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }
}
