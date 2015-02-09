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

import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handles address book entries.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class AddressBookResultHandler extends ResultHandler {

    private static final DateFormat[] DATE_FORMATS = { new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH),
            new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH), new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH), };

    private final boolean[] fields;

    public AddressBookResultHandler(Activity activity, ParsedResult result) {
        super(activity, result);
        AddressBookParsedResult addressResult = (AddressBookParsedResult) result;
        String[] addresses = addressResult.getAddresses();
        boolean hasAddress = addresses != null && addresses.length > 0 && addresses[0].length() > 0;
        String[] phoneNumbers = addressResult.getPhoneNumbers();
        boolean hasPhoneNumber = phoneNumbers != null && phoneNumbers.length > 0;
        String[] emails = addressResult.getEmails();
        boolean hasEmailAddress = emails != null && emails.length > 0;

        fields = new boolean[4];
        fields[0] = true; // Add contact is always available
        fields[1] = hasAddress;
        fields[2] = hasPhoneNumber;
        fields[3] = hasEmailAddress;
    }

    // Overriden so we can hyphenate phone numbers, format birthdays, and bold
    // the name.
    @Override
    public CharSequence getDisplayContents() {
        AddressBookParsedResult result = (AddressBookParsedResult) getResult();
        StringBuilder contents = new StringBuilder(100);
        ParsedResult.maybeAppend(result.getNames(), contents);
        int namesLength = contents.length();

        String pronunciation = result.getPronunciation();
        if (pronunciation != null && pronunciation.length() > 0) {
            contents.append("\n(");
            contents.append(pronunciation);
            contents.append(')');
        }

        ParsedResult.maybeAppend(result.getTitle(), contents);
        ParsedResult.maybeAppend(result.getOrg(), contents);
        ParsedResult.maybeAppend(result.getAddresses(), contents);
        String[] numbers = result.getPhoneNumbers();
        if (numbers != null) {
            for (String number : numbers) {
                ParsedResult.maybeAppend(PhoneNumberUtils.formatNumber(number), contents);
            }
        }
        ParsedResult.maybeAppend(result.getEmails(), contents);
        ParsedResult.maybeAppend(result.getURL(), contents);

        String birthday = result.getBirthday();
        if (birthday != null && birthday.length() > 0) {
            Date date = parseDate(birthday);
            if (date != null) {
                ParsedResult.maybeAppend(DateFormat.getDateInstance().format(date.getTime()), contents);
            }
        }
        ParsedResult.maybeAppend(result.getNote(), contents);

        if (namesLength > 0) {
            // Bold the full name to make it stand out a bit.
            Spannable styled = new SpannableString(contents.toString());
            styled.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, namesLength, 0);
            return styled;
        }
        return contents.toString();
    }

    private static Date parseDate(String s) {
        for (DateFormat currentFomat : DATE_FORMATS) {
            synchronized (currentFomat) {
                currentFomat.setLenient(false);
                Date result = currentFomat.parse(s, new ParsePosition(0));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public int getDisplayTitle() {
        return R.string.result_address_book;
    }
}
