package im.tox.antox.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.IconColor;
import im.tox.antox.utils.LeftPaneItem;
import im.tox.antox.utils.PrettyTimestamp;

/**
 * Created by ollie on 04/03/14.
 */
public class LeftPaneAdapter extends BaseAdapter implements Filterable {

    private ArrayList<LeftPaneItem> mDataOriginal = new ArrayList<LeftPaneItem>();
    private ArrayList<LeftPaneItem> mData = new ArrayList<LeftPaneItem>();
    private LayoutInflater mInflater;
    private Context context;

    Filter mFilter;

    public LeftPaneAdapter(Context context) {
        mInflater = ((Activity) context).getLayoutInflater();
        this.context = context;
    }

    public void addItem(final LeftPaneItem item) {
        mData.add(item);
        mDataOriginal.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        int type = getItem(position).viewType;
        return type;
    }

    @Override
    public int getViewTypeCount() {
        return Constants.TYPE_MAX_COUNT;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public LeftPaneItem getItem(int position) {
        return mData.get(position);
    }

    public String getKey(int position) {
        return getItem(position).key;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        int type = getItemViewType(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (type) {
                case Constants.TYPE_FRIEND_REQUEST:
                    convertView = mInflater.inflate(R.layout.friendrequest_list_item, null);
                    holder.firstText = (TextView)convertView.findViewById(R.id.request_key);
                    holder.secondText = (TextView)convertView.findViewById(R.id.request_message);
                    break;
                case Constants.TYPE_CONTACT:
                    convertView = mInflater.inflate(R.layout.contact_list_item, null);
                    holder.firstText = (TextView)convertView.findViewById(R.id.friend_name);
                    holder.secondText = (TextView)convertView.findViewById(R.id.friend_status);
                    holder.icon = (TextView)convertView.findViewById(R.id.icon);
                    holder.countText = (TextView)convertView.findViewById(R.id.unread_messages_count);
                    holder.timeText = (TextView)convertView.findViewById(R.id.last_message_timestamp);
                    break;
                case Constants.TYPE_HEADER:
                    convertView = mInflater.inflate(R.layout.header_list_item, null);
                    holder.firstText = (TextView)convertView.findViewById(R.id.left_pane_header);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        LeftPaneItem item = getItem(position);

        holder.firstText.setText(item.first);

        if (type != Constants.TYPE_HEADER) {
            if(!item.second.equals(""))
                holder.secondText.setText(item.second);
            else
                holder.firstText.setGravity(Gravity.CENTER_VERTICAL);
        }
        if (type == Constants.TYPE_CONTACT) {
            if (item.count > 0) {
                holder.countText.setVisibility(View.VISIBLE);
                holder.countText.setText(Integer.toString(item.count));
            } else {
                holder.countText.setVisibility(View.GONE);
            }
            holder.timeText.setText(PrettyTimestamp.prettyTimestamp(item.timestamp, false));
            holder.icon.setBackgroundColor(IconColor.iconColorAsColor(item.isOnline,item.status));
        }

        if(holder.timeText != null) {
            holder.timeText.setTextColor(context.getResources().getColor(R.color.gray_darker));
        }

        return convertView;
    }


    private static class ViewHolder {
        public TextView firstText;
        public TextView secondText;
        public TextView icon;
        public TextView countText;
        public TextView timeText;
    }

    @Override
    public Filter getFilter() {
        if(mFilter == null) {
            mFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();

                    if (mDataOriginal != null) {

                        if (constraint.equals("") || constraint == null) {

                            filterResults.values = mDataOriginal;
                            filterResults.count = mDataOriginal.size();

                        } else {
                            mData = mDataOriginal;
                            ArrayList<LeftPaneItem> tempList1 = new ArrayList<LeftPaneItem>();
                            ArrayList<LeftPaneItem> tempList2 = new ArrayList<LeftPaneItem>();
                            int length = mData.size();
                            int i = 0;
                            while (i < length) {
                                LeftPaneItem item = mData.get(i);
                                if (item.first.toUpperCase().startsWith(constraint.toString().toUpperCase()))
                                    tempList1.add(item);
                                else if (item.first.toLowerCase().contains(constraint.toString().toLowerCase()))
                                    tempList2.add(item);
                                i++;
                            }
                            tempList1.addAll(tempList2);
                            filterResults.values = tempList1;
                            filterResults.count = tempList1.size();
                        }

                    }

                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence contraint, FilterResults results) {
                    mData = (ArrayList<LeftPaneItem>) results.values;
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }

        return mFilter;
    }
}
