package im.tox.antox.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import im.tox.antox.utils.Friend;
import im.tox.antox.R;

public class FriendsListAdapter extends ArrayAdapter<Friend> implements
		Filterable {
	Context context;
	int layoutResourceId;
	List<Friend> data = null;

	private final Object lock = new Object();
	private ArrayList<Friend> originalData;
	private FriendsFilter filter;

	public FriendsListAdapter(Context context, int layoutResourceId,
			Friend[] data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = Arrays.asList(data);
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Friend getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FriendsListHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new FriendsListHolder();
			holder.icon = (TextView) row.findViewById(R.id.icon);
			holder.friendName = (TextView) row.findViewById(R.id.friend_name);
			holder.friendStatus = (TextView) row
					.findViewById(R.id.friend_status);
			row.setTag(holder);
		} else {
			holder = (FriendsListHolder) row.getTag();
		}

		Friend friend = data.get(position);
		holder.friendName.setText(friend.friendName);
		holder.friendStatus.setText(friend.friendStatus);
		return row;
	}

	static class FriendsListHolder {
		TextView icon;
		TextView friendName;
		TextView friendStatus;
	}

	@Override
	public Filter getFilter() {
		if (filter == null) {
			filter = new FriendsFilter();
		}
		return filter;
	}

	class FriendsFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();

			if (originalData == null) {
				synchronized (lock) {
					originalData = new ArrayList<Friend>(data);
				}
			}

			// filter is empty string, result is original data of friends list
			if (constraint == null || constraint.length() == 0) {
				ArrayList<Friend> list;
				synchronized (lock) {
					list = new ArrayList<Friend>(originalData);
				}
				results.values = list;
				results.count = list.size();
			} else {
				String prefixString = constraint.toString().toLowerCase(
						Locale.getDefault());

				ArrayList<Friend> values;
				synchronized (lock) {
					values = new ArrayList<Friend>(originalData);
				}

				final int count = values.size();
				final ArrayList<Friend> newValues = new ArrayList<Friend>();

				for (int i = 0; i < count; i++) {
					final Friend value = values.get(i);
					final String valueText = value.toString().toLowerCase(
							Locale.getDefault());

					if (valueText.startsWith(prefixString)) {
						newValues.add(value);
					} else if (findByWords(valueText, prefixString)) {
						newValues.add(value);
					} else if (valueText.contains(prefixString)) {
						newValues.add(value);
					}
				}

				results.values = newValues;
				results.count = newValues.size();
			}

			return results;
		}

		private boolean findByWords(String haystack, String needle) {
			final String[] words = haystack.split(" ");
			final int wordCount = words.length;

			for (int k = 0; k < wordCount; k++) {
				if (words[k].startsWith(needle)) {
					return true;
				}
			}

			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			data = (List<Friend>) results.values;
			notifyDataSetChanged();
		}
	}

}
