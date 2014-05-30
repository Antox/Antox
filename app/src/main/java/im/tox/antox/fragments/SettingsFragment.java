package im.tox.antox.fragments;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import im.tox.antox.R;
import im.tox.antox.tox.ToxSingleton;
import im.tox.jtoxcore.ToxException;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsFragment extends com.github.machinarius.preferencefragment.PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceCategory header = new PreferenceCategory(getActivity());

        /* Add Profile header and preferences */
        addPreferencesFromResource(R.xml.pref_profile);

        /* Add Notification header and preferences */
        header = new PreferenceCategory(getActivity());
        header.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(header);
        addPreferencesFromResource(R.xml.pref_notification);

        /* Add Other header and preferences */
        header = new PreferenceCategory(getActivity());
        header.setTitle(R.string.pref_header_other);
        getPreferenceScreen().addPreference(header);
        addPreferencesFromResource(R.xml.pref_other);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference("nickname"));
        bindPreferenceSummaryToValue(findPreference("status"));
        bindPreferenceSummaryToValue(findPreference("status_message"));
        bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        bindPreferenceSummaryToValue(findPreference("language"));
        bindPreferenceSummaryToValue(findPreference("tox_id"));
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /* Callback will handle updating the new settings on the tox network */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if(key.equals("nickname")) {

            ToxSingleton toxSingleton = ToxSingleton.getInstance();
            try {
                toxSingleton.jTox.setName(sharedPreferences.getString(key, ""));
            } catch (ToxException e) {
                e.printStackTrace();
            }

        }

        if(key.equals("status")) {

            ToxUserStatus newStatus = ToxUserStatus.TOX_USERSTATUS_NONE;
            String newStatusString = sharedPreferences.getString(key, "");
            if(newStatusString.equals("2"))
                newStatus = ToxUserStatus.TOX_USERSTATUS_AWAY;
            if(newStatusString.equals("3"))
                newStatus = ToxUserStatus.TOX_USERSTATUS_BUSY;

            ToxSingleton toxSingleton = ToxSingleton.getInstance();
            try {
                toxSingleton.jTox.setUserStatus(newStatus);
            } catch (ToxException e) {
                e.printStackTrace();
            }

        }

        if(key.equals("status_message")) {

            ToxSingleton toxSingleton = ToxSingleton.getInstance();
            try {
                toxSingleton.jTox.setStatusMessage(sharedPreferences.getString(key, ""));
            } catch (ToxException e) {
                e.printStackTrace();
            }

        }
    }
}
