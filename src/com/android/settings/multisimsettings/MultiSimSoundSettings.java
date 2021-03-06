/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of Code Aurora Forum, Inc. nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.

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

import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.Settings.System;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

public class MultiSimSoundSettings extends PreferenceActivity {
    private String LOG_TAG = "MultiSimSoundSettings";
    private static final String KEY_RINGSTONE = "ringtone";
    /* xieyunyang.hoperun 2012.6.19 Define a key */
    private static final String KEY_RINGBACKMUSIC = "ringbackmusic";
    /* xieyunyang.hoperun 2012.6.19 end */
    private int[] ringTones = {RingtoneManager.TYPE_RINGTONE, RingtoneManager.TYPE_RINGTONE_2};

    /* xieyunyang.hoperun 2012.6.19 */
    private final String TAG_HP = "SoundSetting_hp";
    /* xieyunyang.hoperun 2012.6.19 end */
    private DefaultRingtonePreference ringTonePref;
    /* xieyunyang.hoperun 2012.6.19 define a Preference */
    private Preference mRingbackmusicPreference;
    /* xieyunyang.hoperun 2012.6.19 end */
    private int mSubscription;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.multi_sim_sound_settings);
        ringTonePref = (DefaultRingtonePreference) findPreference(KEY_RINGSTONE);
        // /* xieyunyang.hoperun 2012.6.19 According to the key to
        // getmRingbackmusicPreference*/
        mRingbackmusicPreference = findPreference(KEY_RINGBACKMUSIC);

        Log.e(TAG_HP, "the mRingtonePreference is:(" + mRingbackmusicPreference
                + ")");
        Log.e(TAG_HP, "the mRingbackmusicPreference is : [ "
                + mRingbackmusicPreference + " ]");
        /* xieyunyang.hoperun 2012.6.19 end */
        mSubscription = this.getIntent().getIntExtra(SUBSCRIPTION_KEY, 0);
        ringTonePref.setRingtoneType(ringTones[mSubscription]);
    }

    protected void onResume() {
        super.onResume();
    }

}
