package im.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FriendRequestsAdapter extends ArrayAdapter<FriendRequests> implements
		Filterable {

	Context context;
	int layoutResourceId;
	List<FriendRequests> data = null;

	private final Object lock = new Object();
	private ArrayList<FriendRequests> originalData;

	public FriendRequestsAdapter(Context context, int layoutResourceId,
                                 FriendRequests[] data) {
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
	public FriendRequests getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		FriendRequestsHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new FriendRequestsHolder();
			holder.requestKey = (TextView) row.findViewById(R.id.request_key);
			holder.requestMessage = (TextView) row.findViewById(R.id.request_message);
			row.setTag(holder);
		} else {
			holder = (FriendRequestsHolder) row.getTag();
		}

		FriendRequests friendRequests = data.get(position);
		holder.requestKey.setText(friendRequests.requestKey);
		holder.requestMessage.setText(friendRequests.requestMessage);

		return row;
	}

	static class FriendRequestsHolder {
		TextView requestKey;
		TextView requestMessage;
	}
}
