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
import com.google.zxing.client.result.CalendarParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Handles calendar entries encoded in QR Codes.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CalendarResultHandler extends ResultHandler {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);

    public CalendarResultHandler(Activity activity, ParsedResult result) {
        super(activity, result);
    }

    @Override
    public CharSequence getDisplayContents() {
        CalendarParsedResult calResult = (CalendarParsedResult) getResult();
        StringBuilder result = new StringBuilder(100);
        ParsedResult.maybeAppend(calResult.getSummary(), result);
        Date start = calResult.getStart();
        String startString = start.toGMTString();
        appendTime(startString, result, false, false);

        Date end = calResult.getEnd();
        String endString = end.toGMTString();
        if (endString != null) {
            boolean sameStartEnd = startString.equals(endString);
            appendTime(endString, result, true, sameStartEnd);
        }

        ParsedResult.maybeAppend(calResult.getLocation(), result);
        ParsedResult.maybeAppend(calResult.getAttendees(), result);
        ParsedResult.maybeAppend(calResult.getDescription(), result);
        return result.toString();
    }

    private static void appendTime(String when, StringBuilder result, boolean end, boolean sameStartEnd) {
        if (when.length() == 8) {
            // Show only year/month/day
            Date date;
            synchronized (DATE_FORMAT) {
                date = DATE_FORMAT.parse(when, new ParsePosition(0));
            }
            // if it's all-day and this is the end date, it's exclusive, so show
            // the user
            // that it ends on the day before to make more intuitive sense.
            // But don't do it if the event already (incorrectly?) specifies the
            // same start/end
            if (end && !sameStartEnd) {
                date = new Date(date.getTime() - 24 * 60 * 60 * 1000);
            }
            ParsedResult.maybeAppend(DateFormat.getDateInstance().format(date.getTime()), result);
        } else {
            // The when string can be local time, or UTC if it ends with a Z
            Date date;
            synchronized (DATE_TIME_FORMAT) {
                date = DATE_TIME_FORMAT.parse(when.substring(0, 15), new ParsePosition(0));
            }
            long milliseconds = date.getTime();
            if (when.length() == 16 && when.charAt(15) == 'Z') {
                Calendar calendar = new GregorianCalendar();
                int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
                milliseconds += offset;
            }
            ParsedResult.maybeAppend(DateFormat.getDateTimeInstance().format(milliseconds), result);
        }
    }

    @Override
    public int getDisplayTitle() {
        return R.string.result_calendar;
    }
}
