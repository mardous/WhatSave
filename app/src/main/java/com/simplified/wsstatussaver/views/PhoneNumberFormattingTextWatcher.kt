/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.simplified.wsstatussaver.views

import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.text.style.TtsSpan
import io.michaelrocks.libphonenumber.android.AsYouTypeFormatter
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

/**
 * Watches a [android.widget.TextView] and if a phone number is entered
 * will format it.
 *
 * Stop formatting when the user
 *
 *  * Inputs non-dialable characters
 *  * Removes the separator in the middle of string.
 *
 * The formatting will be restarted once the text is cleared.
 */
class PhoneNumberFormattingTextWatcher(countryCode: String = Locale.getDefault().country) :
    TextWatcher, KoinComponent {

    private val phoneNumberUtil: PhoneNumberUtil by inject()
    private val formatter: AsYouTypeFormatter = phoneNumberUtil.getAsYouTypeFormatter(countryCode)

    /** Indicates the change was caused by ourselves. */
    private var selfChange = false
    /** Indicates the formatting has been stopped. */
    private var stopFormatting = false

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (selfChange || stopFormatting) {
            return
        }
        // If the user manually deleted any non-dialable characters, stop formatting
        if (count > 0 && hasSeparator(s, start, count)) {
            stopFormatting()
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (selfChange || stopFormatting) {
            return
        }
        // If the user inserted any non-dialable characters, stop formatting
        if (count > 0 && hasSeparator(s, start, count)) {
            stopFormatting()
        }
    }

    @Synchronized
    override fun afterTextChanged(s: Editable) {
        if (stopFormatting) {
            // Restart the formatting when all texts were clear.
            stopFormatting = s.isNotEmpty()
            return
        }
        if (selfChange) {
            // Ignore the change caused by s.replace().
            return
        }
        val formatted = reformat(s, Selection.getSelectionEnd(s))
        if (formatted != null) {
            val rememberedPos = formatter.rememberedPosition
            selfChange = true
            s.replace(0, s.length, formatted, 0, formatted.length)
            // The text could be changed by other TextWatcher after we changed it. If we found the
            // text is not the one we were expecting, just give up calling setSelection().
            if (formatted == s.toString()) {
                Selection.setSelection(s, rememberedPos)
            }
            selfChange = false
        }

        //remove previous TTS spans
        val ttsSpans = s.getSpans(0, s.length, TtsSpan::class.java)
        for (ttsSpan in ttsSpans) {
            s.removeSpan(ttsSpan)
        }

        PhoneNumberUtils.addTtsSpan(s, 0, s.length)
    }

    /**
     * Generate the formatted number by ignoring all non-dialable chars and stick the cursor to the
     * nearest dialable char to the left. For instance, if the number is  (650) 123-45678 and '4' is
     * removed then the cursor should be behind '3' instead of '-'.
     */
    private fun reformat(s: CharSequence, cursor: Int): String? {
        // The index of char to the leftward of the cursor.
        val curIndex = cursor - 1
        var formatted: String? = null
        formatter.clear()
        var lastNonSeparator = 0.toChar()
        var hasCursor = false
        val len = s.length
        for (i in 0 until len) {
            val c = s[i]
            if (PhoneNumberUtils.isNonSeparator(c)) {
                if (lastNonSeparator.code != 0) {
                    formatted = getFormattedNumber(lastNonSeparator, hasCursor)
                    hasCursor = false
                }
                lastNonSeparator = c
            }
            if (i == curIndex) {
                hasCursor = true
            }
        }
        if (lastNonSeparator.code != 0) {
            formatted = getFormattedNumber(lastNonSeparator, hasCursor)
        }
        return formatted
    }

    private fun getFormattedNumber(lastNonSeparator: Char, hasCursor: Boolean): String {
        return if (hasCursor) {
            formatter.inputDigitAndRememberPosition(lastNonSeparator)
        } else {
            formatter.inputDigit(lastNonSeparator)
        }
    }

    private fun stopFormatting() {
        stopFormatting = true
        formatter.clear()
    }

    private fun hasSeparator(s: CharSequence, start: Int, count: Int): Boolean {
        for (i in start until start + count) {
            val c = s[i]
            if (!PhoneNumberUtils.isNonSeparator(c)) {
                return true
            }
        }
        return false
    }
}

