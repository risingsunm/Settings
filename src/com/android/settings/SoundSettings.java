/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012, Code Aurora Forum. All rights reserved.
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Images.Media;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import com.android.internal.telephony.Phone;
import com.android.settings.multisimsettings.MultiSimSettingsConstants;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_EMERGENCY_TONE_VALUE = 0;

    private static final String KEY_SILENT_MODE = "silent_mode";
    private static final String KEY_VIBRATE = "vibrate_on_ring";
    private static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_MUSICFX = "musicfx";
    private static final String KEY_DTMF_TONE = "dtmf_tone";
    private static final String KEY_SOUND_EFFECTS = "sound_effects";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_EMERGENCY_TONE = "emergency_tone";
    private static final String KEY_SOUND_SETTINGS = "sound_settings";
    private static final String KEY_LOCK_SOUNDS = "lock_sounds";
    private static final String KEY_RINGTONE = "ringtone";
    private static final String KEY_MULTISIM_RINGTONE = "multisim_ringtone";
    private static final String KEY_NOTIFICATION_SOUND = "notification_sound";
    private static final String KEY_CATEGORY_CALLS = "category_calls";

    //mzikun add for settings menu
    private static final String KEY_REVERSE_CALL_MUTE = "reverse_call_mute";
    private static final String KEY_REVERSE_MP3_MUTE = "reverse_mp3_mute";
    private static final String KEY_REVERSE_MP4_MUTE = "reverse_mp4_mute";
//    protected Phone mPhone;

    private static final String SILENT_MODE_OFF = "off";
    private static final String SILENT_MODE_VIBRATE = "vibrate";
    private static final String SILENT_MODE_MUTE = "mute";

    private static final String[] NEED_VOICE_CAPABILITY = {
            KEY_RINGTONE, KEY_DTMF_TONE, KEY_CATEGORY_CALLS,
            KEY_EMERGENCY_TONE
    };

    private static final int MSG_UPDATE_RINGTONE_SUMMARY = 1;
    private static final int MSG_UPDATE_NOTIFICATION_SUMMARY = 2;

    private CheckBoxPreference mVibrateOnRing;
    private ListPreference mSilentMode;
    private CheckBoxPreference mDtmfTone;
    private CheckBoxPreference mSoundEffects;
    private CheckBoxPreference mHapticFeedback;
    private Preference mMusicFx;
    private CheckBoxPreference mLockSounds;
    private Preference mRingtonePreference;
    private Preference mMultiSimRingtonePreference;
    private Preference mNotificationPreference;

    //mzikun add for settings menu
    private CheckBoxPreference mReverseCallMute;
    private CheckBoxPreference mReverseMP3Mute;
    private CheckBoxPreference mReverseMP4Mute;

    private Runnable mRingtoneLookupRunnable;

    private AudioManager mAudioManager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                updateState(false);
            }
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_RINGTONE_SUMMARY:
                mRingtonePreference.setSummary((CharSequence) msg.obj);
                break;
            case MSG_UPDATE_NOTIFICATION_SUMMARY:
                mNotificationPreference.setSummary((CharSequence) msg.obj);
                break;
            }
        }
    };

    private PreferenceGroup mSoundSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
        int activePhoneType = TelephonyManager.getDefault().getCurrentPhoneType();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.sound_settings);

        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType) {
            // device is not CDMA, do not display CDMA emergency_tone
            getPreferenceScreen().removePreference(findPreference(KEY_EMERGENCY_TONE));
        }

        //mzikun add for settings menu
        //mPhone = PhoneApp.getInstance().getPhone();

        mSilentMode = (ListPreference) findPreference(KEY_SILENT_MODE);
        if (!getResources().getBoolean(R.bool.has_silent_mode)) {
            getPreferenceScreen().removePreference(mSilentMode);
            findPreference(KEY_RING_VOLUME).setDependency(null);
        } else {
            mSilentMode.setOnPreferenceChangeListener(this);
        }

        mVibrateOnRing = (CheckBoxPreference) findPreference(KEY_VIBRATE);
        mVibrateOnRing.setOnPreferenceChangeListener(this);

        mDtmfTone = (CheckBoxPreference) findPreference(KEY_DTMF_TONE);
        mDtmfTone.setPersistent(false);
        mDtmfTone.setChecked(Settings.System.getInt(resolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        mSoundEffects = (CheckBoxPreference) findPreference(KEY_SOUND_EFFECTS);
        mSoundEffects.setPersistent(false);
        mSoundEffects.setChecked(Settings.System.getInt(resolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0);
        mHapticFeedback = (CheckBoxPreference) findPreference(KEY_HAPTIC_FEEDBACK);
        mHapticFeedback.setPersistent(false);
        mHapticFeedback.setChecked(Settings.System.getInt(resolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0);
        mLockSounds = (CheckBoxPreference) findPreference(KEY_LOCK_SOUNDS);
        mLockSounds.setPersistent(false);
        mLockSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0);

        mRingtonePreference = findPreference(KEY_RINGTONE);
        mMultiSimRingtonePreference = findPreference(KEY_MULTISIM_RINGTONE);
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            //if it support multi sim, remove ringtone setting, show multi sim ringtone setting
            getPreferenceScreen().removePreference(mRingtonePreference);
            mRingtonePreference = null;
        } else {
            //if it is not multi sim, remove multi sim ringtone setting, and show ringtone setting
            getPreferenceScreen().removePreference(mMultiSimRingtonePreference);
            mMultiSimRingtonePreference = null;
        }
        mNotificationPreference = findPreference(KEY_NOTIFICATION_SOUND);

        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            getPreferenceScreen().removePreference(mVibrateOnRing);
            getPreferenceScreen().removePreference(mHapticFeedback);
        }

        if (TelephonyManager.PHONE_TYPE_CDMA == activePhoneType) {
            ListPreference emergencyTonePreference =
                (ListPreference) findPreference(KEY_EMERGENCY_TONE);
            emergencyTonePreference.setValue(String.valueOf(Settings.System.getInt(
                resolver, Settings.System.EMERGENCY_TONE, FALLBACK_EMERGENCY_TONE_VALUE)));
            emergencyTonePreference.setOnPreferenceChangeListener(this);
        }

        mSoundSettings = (PreferenceGroup) findPreference(KEY_SOUND_SETTINGS);

        mMusicFx = mSoundSettings.findPreference(KEY_MUSICFX);
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        PackageManager p = getPackageManager();
        List<ResolveInfo> ris = p.queryIntentActivities(i, PackageManager.GET_DISABLED_COMPONENTS);
        if (ris.size() <= 2) {
            // no need to show the item if there is no choice for the user to make
            // note: the built in musicfx panel has two activities (one being a
            // compatibility shim that launches either the other activity, or a
            // third party one), hence the check for <=2. If the implementation
            // of the compatbility layer changes, this check may need to be updated.
            mSoundSettings.removePreference(mMusicFx);
        }

        if (!Utils.isVoiceCapable(getActivity())) {
            for (String prefKey : NEED_VOICE_CAPABILITY) {
                Preference pref = findPreference(prefKey);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        /*Begin: mzikun add for settings menu*/
        mReverseCallMute = (CheckBoxPreference) findPreference(KEY_REVERSE_CALL_MUTE);
        if (mReverseCallMute != null) {
            mReverseCallMute.setPersistent(false);
            mReverseCallMute.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(mReverseCallMute);
            mReverseCallMute = null;
        }

        mReverseMP3Mute = (CheckBoxPreference) findPreference(KEY_REVERSE_MP3_MUTE);
        if (mReverseMP3Mute != null) {
            mReverseMP3Mute.setPersistent(false);
            mReverseMP3Mute.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(mReverseMP3Mute);
            mReverseMP3Mute = null;
        }

        mReverseMP4Mute = (CheckBoxPreference) findPreference(KEY_REVERSE_MP4_MUTE);
        if (mReverseMP4Mute != null) {
            mReverseMP4Mute.setPersistent(false);
            mReverseMP4Mute.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(mReverseMP4Mute);
            mReverseMP4Mute = null;
        }
        /*End: mzikun add for settings menu*/

        mRingtoneLookupRunnable = new Runnable() {
            public void run() {
                if (mRingtonePreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_RINGTONE, mRingtonePreference,
                            MSG_UPDATE_RINGTONE_SUMMARY);
                }
                if (mNotificationPreference != null) {
                    updateRingtoneName(RingtoneManager.TYPE_NOTIFICATION, mNotificationPreference,
                            MSG_UPDATE_NOTIFICATION_SUMMARY);
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState(true);
        lookupRingtoneNames();

        IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
        getActivity().registerReceiver(mReceiver, filter);

        /*Begin: mzikun add for settings menu*/
        if (mReverseCallMute != null) {
            boolean checked =Settings.System.getInt(getContentResolver(), "reversal_mute",0) != 0;
            mReverseCallMute.setChecked(checked);
            mReverseCallMute.setSummary(checked ? R.string.reversal_call_off_summary : R.string.reversal_call_on_summary);
       }

        if (mReverseMP3Mute != null) {
            boolean checked =Settings.System.getInt(getContentResolver(), "reversal_mp3_mute",0) != 0;
            mReverseMP3Mute.setChecked(checked);
       }

        if (mReverseMP4Mute != null) {
            boolean checked =Settings.System.getInt(getContentResolver(), "reversal_mp4_mute",0) != 0;
            mReverseMP4Mute.setChecked(checked);
       }
        /*End: mzikun add for settings menu*/
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    /**
     * Put the audio system into the correct vibrate setting
     */
    private void setPhoneVibrateSettingValue(boolean vibeOnRing) {
        // If vibrate-on-ring is checked, use VIBRATE_SETTING_ON
        // Otherwise vibrate is off when ringer is silent
        int vibrateMode = vibeOnRing ? AudioManager.VIBRATE_SETTING_ON
                : AudioManager.VIBRATE_SETTING_ONLY_SILENT;
        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, vibrateMode);
        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, vibrateMode);
    }

    private void setPhoneSilentSettingValue(String value) {
        int ringerMode = AudioManager.RINGER_MODE_NORMAL;
        if (value.equals(SILENT_MODE_MUTE)) {
            ringerMode = AudioManager.RINGER_MODE_SILENT;
        } else if (value.equals(SILENT_MODE_VIBRATE)) {
            ringerMode = AudioManager.RINGER_MODE_VIBRATE;
        }
        mAudioManager.setRingerMode(ringerMode);
    }

    private String getPhoneSilentModeSettingValue() {
        switch (mAudioManager.getRingerMode()) {
        case AudioManager.RINGER_MODE_NORMAL:
            return SILENT_MODE_OFF;
        case AudioManager.RINGER_MODE_VIBRATE:
            return SILENT_MODE_VIBRATE;
        case AudioManager.RINGER_MODE_SILENT:
            return SILENT_MODE_MUTE;
        }
        // Shouldn't happen
        return SILENT_MODE_OFF;
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        if (getActivity() == null) return;

        final int vibrateMode = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);

        mVibrateOnRing.setChecked(vibrateMode == AudioManager.VIBRATE_SETTING_ON);
        mSilentMode.setValue(getPhoneSilentModeSettingValue());

        mSilentMode.setSummary(mSilentMode.getEntry());
    }

    private void updateRingtoneName(int type, Preference preference, int msg) {
        if (preference == null) return;
        Context context = getActivity();
        if (context == null) return;
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        CharSequence summary = context.getString(com.android.internal.R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            try {
                Cursor cursor = context.getContentResolver().query(ringtoneUri,
                        new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                    cursor.close();
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(msg, summary));
    }

    private void lookupRingtoneNames() {
        new Thread(mRingtoneLookupRunnable).start();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDtmfTone) {
            Settings.System.putInt(getContentResolver(), Settings.System.DTMF_TONE_WHEN_DIALING,
                    mDtmfTone.isChecked() ? 1 : 0);

        } else if (preference == mSoundEffects) {
            if (mSoundEffects.isChecked()) {
                mAudioManager.loadSoundEffects();
            } else {
                mAudioManager.unloadSoundEffects();
            }
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED,
                    mSoundEffects.isChecked() ? 1 : 0);

        } else if (preference == mHapticFeedback) {
            Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    mHapticFeedback.isChecked() ? 1 : 0);

        } else if (preference == mLockSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED,
                    mLockSounds.isChecked() ? 1 : 0);

        } else if (preference == mMusicFx) {
            // let the framework fire off the intent
            return false;
        } else if (mMultiSimRingtonePreference != null && preference == mMultiSimRingtonePreference) {
            Intent intent = mMultiSimRingtonePreference.getIntent();
            intent.putExtra(MultiSimSettingsConstants.TARGET_PACKAGE, MultiSimSettingsConstants.SOUND_PACKAGE);
            intent.putExtra(MultiSimSettingsConstants.TARGET_CLASS, MultiSimSettingsConstants.SOUND_CLASS);
            startActivity(intent);
        }
        /*Begin: mzikun add for settings menu*/
        else if (preference == mReverseCallMute) {
            boolean checked = mReverseCallMute.isChecked();
            Settings.System.putInt(getContentResolver(),
                    "reversal_mute",
                    checked ? 1 : 0);
            mReverseCallMute.setSummary(checked ? R.string.reversal_call_off_summary : R.string.reversal_call_on_summary);
            return true;
        }
        else if (preference == mReverseMP3Mute) {
            boolean checked = mReverseMP3Mute.isChecked();
            Settings.System.putInt(getContentResolver(),
                    "reversal_mp3_mute",
                    checked ? 1 : 0);
            return true;
        }
        else if (preference == mReverseMP4Mute) {
            boolean checked = mReverseMP4Mute.isChecked();
            Settings.System.putInt(getContentResolver(),
                    "reversal_mp4_mute",
                    checked ? 1 : 0);
            return true;
        }
        /*End: mzikun add for settings menu*/
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_EMERGENCY_TONE.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.EMERGENCY_TONE, value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist emergency tone setting", e);
            }
        } else if (preference == mVibrateOnRing) {
            setPhoneVibrateSettingValue((Boolean) objValue);
        } else if (preference == mSilentMode) {
            setPhoneSilentSettingValue(objValue.toString());
        }
        /*Begin: mzikun add for settings menu*/
        else if (preference == mReverseCallMute) {
            handleReversalMute(preference, objValue);
        }
        else if (preference == mReverseMP3Mute) {
//            handleReversalMute(preference, objValue);
        }
        else if (preference == mReverseMP4Mute) {
//            handleReversalMute(preference, objValue);
        }
        /*End: mzikun add for settings menu*/
        return true;
    }

    /*Begin: mzikun add for settings menu*/
    private void handleReversalMute(Preference preference,Object objValue) {
        boolean isMute = Boolean.parseBoolean(objValue.toString());
        //log("handleReversalMute() display is mute " + (isMute? "true" : "false"));
        android.provider.Settings.System.putInt(getContentResolver(),
                "reversal_mute", isMute ? 1 : 0);
        mReverseCallMute.setSummary(isMute ? R.string.reversal_call_off_summary : R.string.reversal_call_on_summary);
    }
    /*End: mzikun add for settings menu*/
}
