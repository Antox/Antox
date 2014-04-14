package im.tox.antox.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import im.tox.antox.R;
import im.tox.antox.adapters.GroupsListAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.GroupItem;

/**
 * Created by dragos on 10.04.2014.
 */
public class ManageGroupsActivity extends ActionBarActivity {
    private ArrayList<GroupItem> groups;
    private ListView listView;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);

        context = this;

        //list containing the groups
        listView = (ListView) findViewById(R.id.list_view_groups);
        listView.setEmptyView(findViewById(android.R.id.empty));

        groups = new ArrayList<GroupItem>();

        AntoxDB db = new AntoxDB(this);

        //There will always be the Friends group
        groups.add(new GroupItem( getResources().getString(R.string.manage_groups_friends), db.getNumberOfFriendsInAGroup("Friends")));

        //Add the rest of the groups
        SharedPreferences sharedPreferences = getSharedPreferences("groups", Context.MODE_PRIVATE);
        if (!sharedPreferences.getAll().isEmpty()) {
            Map<String,?> keys = sharedPreferences.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){
                String groupName = entry.getValue().toString();
                groups.add(new GroupItem(groupName, db.getNumberOfFriendsInAGroup(groupName)));
            }
        }

        db.close();
        listView.setAdapter(new GroupsListAdapter(this, groups));
        setListViewLongClick();
    }

    private void setListViewLongClick() {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
                //no modify the "Friends" group
                if (index == 0) {
                    Toast.makeText(context, getResources().getString(R.string.manage_groups_no_delete_friends),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                final GroupItem groupItem = (GroupItem) adapterView.getAdapter().getItem(index);

                final AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                CharSequence[] items = new CharSequence[]{
                        getResources().getString(R.string.manage_groups_rename_group),
                        getResources().getString(R.string.manage_groups_delete_group)
                };
                dialog.setTitle(getResources().getString(R.string.manage_groups_action_on) + " "
                        + groupItem.getGroupName());
                dialog.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch(i) {
                            //edit
                            case 0:
                                showGroupModifyDialog(groupItem.getGroupName());
                                dialog.create().cancel();
                                break;
                            //delete
                            case 1:
                                deleteGroup(groupItem.getGroupName(), "Friends");
                                for (GroupItem g : groups) {
                                    if (g.getGroupName().equals("Friends")) {
                                        AntoxDB db = new AntoxDB(context);
                                        g.setNumberOfMembers(db.getNumberOfFriendsInAGroup("Friends"));
                                        ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
                                    }
                                }
                                Toast.makeText(context, getResources().getString(R.string.manage_groups_toast_deleted),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }

                    }
                });
                dialog.create().show();
                return true;
            }
        });
    }

    //add button click
    public void onClick(View view) {
        showGroupAddDialog();
    }

    private void showGroupAddDialog() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getResources().getString(R.string.manage_groups_dialog_add_group));

        final EditText groupName = new EditText(this);
        groupName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        dialog.setView(groupName);
        dialog.setPositiveButton(getResources().getString(R.string.manage_groups_dialog_add_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = groupName.getText().toString();
                if (name.equals("")) {
                    Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_no_name),
                            Toast.LENGTH_SHORT).show();
                } else {
                    SharedPreferences sharedPreferences = getSharedPreferences("groups", Context.MODE_PRIVATE);
                    if (!sharedPreferences.getAll().isEmpty()) {
                        Map<String, ?> keys = sharedPreferences.getAll();
                        boolean okToSave = true;

                        //Check if the group name is already used
                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                            String groupName = entry.getValue().toString();
                            if (groupName.equals(name)) {
                                Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_already_exits),
                                        Toast.LENGTH_SHORT).show();
                                okToSave = false;
                                break;
                            }
                        }

                        //check if the name is Friends
                        if (getResources().getString(R.string.manage_groups_friends).equals(name)) {
                            okToSave = false;
                            Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_already_exits),
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (okToSave) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(name, name);
                            editor.commit();
                            groups.add(new GroupItem(name));
                            ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
                            Toast.makeText(context, getResources().getString(R.string.manage_groups_toast_added),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(name, name);
                        editor.commit();
                        groups.add(new GroupItem(name));
                        ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
                        Toast.makeText(context, getResources().getString(R.string.manage_groups_toast_added),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        dialog.setNegativeButton(getResources().getString(R.string.manage_groups_dialog_cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.create().cancel();
            }
        });
        dialog.show();
    }

    //dialog for renaming
    private void showGroupModifyDialog(String text) {
        final String originalName = text;
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getResources().getString(R.string.manage_groups_dialog_rename_group));

        final EditText groupName = new EditText(this);
        groupName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        groupName.setText(originalName);
        groupName.setSelection(groupName.getText().length());
        dialog.setView(groupName);
        dialog.setPositiveButton(getResources().getString(R.string.manage_groups_dialog_rename_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = groupName.getText().toString();
                if (name.equals("")) {
                    Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_no_name),
                            Toast.LENGTH_SHORT).show();
                } else {
                    SharedPreferences sharedPreferences = getSharedPreferences("groups", Context.MODE_PRIVATE);
                    if (!sharedPreferences.getAll().isEmpty()) {
                        Map<String, ?> keys = sharedPreferences.getAll();
                        boolean okToSave = true;

                        //Check if the group name is already used
                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                            String groupName = entry.getValue().toString();
                            if (groupName.equals(name) && !groupName.equals(originalName)) {
                                Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_already_exits),
                                        Toast.LENGTH_SHORT).show();
                                okToSave = false;
                                break;
                            }
                        }

                        //check if the name is Friends
                        if (getResources().getString(R.string.manage_groups_friends).equals(name)) {
                            okToSave = false;
                            Toast.makeText(dialog.getContext(), getResources().getString(R.string.manage_groups_already_exits),
                                    Toast.LENGTH_SHORT).show();
                        }

                        if (okToSave) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            deleteGroup(originalName, name);
                            editor.putString(name, name);
                            editor.commit();
                            AntoxDB db = new AntoxDB(context);
                            groups.add(new GroupItem(name, db.getNumberOfFriendsInAGroup(name)));
                            db.close();
                            ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
                            Toast.makeText(context, getResources().getString(R.string.manage_groups_toast_renamed),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    else {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(name, name);
                        editor.commit();
                        groups.add(new GroupItem(name));
                        ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
                        Toast.makeText(context, getResources().getString(R.string.manage_groups_toast_renamed),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        dialog.setNegativeButton(getResources().getString(R.string.manage_groups_dialog_cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.create().cancel();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.manage_groups, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            //scanQR button to call the barcode reader app
            case R.id.add_new_group:
                showGroupAddDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void deleteGroup(String oldGroup, String newGroup) {
        //database process
        AntoxDB db = new AntoxDB(this);
        db.moveAllUsersFromAGroupToOtherGroup(oldGroup, newGroup);
        db.close();

        //delete from the shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("groups", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(oldGroup);
        editor.commit();

        //delete from groups array
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).getGroupName().equals(oldGroup)) {
                groups.remove(i);
                break;
            }
        }

        //notify adapter that data has changed
        ((GroupsListAdapter)listView.getAdapter()).notifyDataSetChanged();
    }
 }
