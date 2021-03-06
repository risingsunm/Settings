/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.Locale;

public class LocalePicker extends com.android.internal.app.LocalePicker
        implements com.android.internal.app.LocalePicker.LocaleSelectionListener {
    public LocalePicker() {
        super();
        setLocaleSelectionListener(this);
    }

    @Override
    public void onLocaleSelected(Locale locale) {
        getActivity().onBackPressed();
        LocalePicker.updateLocale(locale);
    }
}
