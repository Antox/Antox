/*
 * Copyright (C) 2008 ZXing authors
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

package com.jwetherell.quick_response_code.result;

import im.tox.antox.R;

import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;
import android.telephony.PhoneNumberUtils;

/**
 * Offers relevant actions for telephone numbers.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class TelResultHandler extends ResultHandler {

    public TelResultHandler(Activity activity, ParsedResult result) {
        super(activity, result);
    }

    // Overriden so we can take advantage of Android's phone number hyphenation
    // routines.
    @Override
    public CharSequence getDisplayContents() {
        String contents = getResult().getDisplayResult();
        contents = contents.replace("\r", "");
        return PhoneNumberUtils.formatNumber(contents);
    }

    @Override
    public int getDisplayTitle() {
        return R.string.result_tel;
    }
}
