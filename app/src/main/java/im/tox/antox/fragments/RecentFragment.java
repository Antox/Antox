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
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import im.tox.antox.R;
import im.tox.antox.activities.FriendProfileActivity;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.adapters.LeftPaneAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.LeftPaneItem;

/**
 * Created by ollie on 28/02/14.
 */
public class RecentFragment extends Fragment {

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public RecentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_recent, container, false);
        return rootView;
    }

}
