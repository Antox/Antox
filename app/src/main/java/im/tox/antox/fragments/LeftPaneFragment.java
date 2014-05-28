package im.tox.antox.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.Map;

import im.tox.antox.activities.FriendProfileActivity;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.LeftPaneItem;
import im.tox.antox.R;
import im.tox.antox.tox.ToxService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.adapters.LeftPaneAdapter;

/**
 * Created by ollie on 28/02/14.
 */
public class LeftPaneFragment extends Fragment {

    public class LeftPagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = { "Recent", "Contacts", "Settings"};

        public LeftPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {

            case 0: return new ContactsFragment();
            case 1: return new ContactsFragment();
            case 2: return new ContactsFragment();
            default: return new ContactsFragment();
            }
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }
    private MainActivity main_act;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */

        main_act = (MainActivity) getActivity();

        View rootView = inflater.inflate(R.layout.fragment_leftpane, container, false);

        // Initialize the ViewPager and set an adapter
        ViewPager pager = (ViewPager) rootView.findViewById(R.id.pager);
        pager.setAdapter(new LeftPagerAdapter(getFragmentManager()));

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setViewPager(pager);

        return rootView;
    }
}
