package im.tox.antox.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.utils.GroupItem;

/**
 * Created by dragos on 10.04.2014.
 */
public class GroupsListAdapter extends ArrayAdapter<GroupItem> {
    private Context context;

    public GroupsListAdapter(Context context, ArrayList<GroupItem> groups) {
        super(context, 0, groups);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        GroupsHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.manage_groups_item, null);
            holder = new GroupsHolder();
            holder.nameLabel = (TextView) row.findViewById(R.id.text_view_group_name_label);
            row.setTag(holder);
        }
        else {
            holder = (GroupsHolder) row.getTag();
        }

        GroupItem groupItem = this.getItem(position);
        holder.nameLabel.setText(groupItem.getGroupName() + " (" + groupItem.getNumberOfMembers() + ")");

        return row;
    }

    static class GroupsHolder {
        TextView nameLabel;
        TextView numberLabel;
        TextView numberOfMembers;
    }
}
