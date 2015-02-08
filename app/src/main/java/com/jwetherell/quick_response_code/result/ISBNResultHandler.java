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

import com.google.zxing.Result;

import im.tox.antox.R;

import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;

/**
 * Handles books encoded by their ISBN values.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ISBNResultHandler extends ResultHandler {

    public ISBNResultHandler(Activity activity, ParsedResult result, Result rawResult) {
        super(activity, result, rawResult);
    }

    @Override
    public CharSequence getDisplayContents() {
        ISBNParsedResult result = (ISBNParsedResult) getResult();
        StringBuilder contents = new StringBuilder(100);
        ParsedResult.maybeAppend(result.getISBN(), contents);
        contents.trimToSize();
        return contents.toString();
    }

    @Override
    public int getDisplayTitle() {
        return R.string.result_isbn;
    }
}
