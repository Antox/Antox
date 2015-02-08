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
import com.google.zxing.client.result.URIParsedResult;

import android.app.Activity;

import java.util.Locale;

/**
 * Offers appropriate actions for URLS.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class URIResultHandler extends ResultHandler {

    // URIs beginning with entries in this array will not be saved to history or
    // copied to the clipboard for security.
    private static final String[] SECURE_PROTOCOLS = { "otpauth:" };

    public URIResultHandler(Activity activity, ParsedResult result) {
        super(activity, result);
    }

    @Override
    public CharSequence getDisplayContents() {
        URIParsedResult uriResult = (URIParsedResult) getResult();
        String uri = uriResult.getURI().toLowerCase(Locale.ENGLISH);
        StringBuilder contents = new StringBuilder(100);
        ParsedResult.maybeAppend(uri, contents);
        contents.trimToSize();
        return contents.toString();
    }

    @Override
    public int getDisplayTitle() {
        return R.string.result_uri;
    }

    @Override
    public boolean areContentsSecure() {
        URIParsedResult uriResult = (URIParsedResult) getResult();
        String uri = uriResult.getURI().toLowerCase(Locale.ENGLISH);
        for (String secure : SECURE_PROTOCOLS) {
            if (uri.startsWith(secure)) {
                return true;
            }
        }
        return false;
    }
}
