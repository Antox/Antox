package im.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatMessagesAdapter extends ArrayAdapter<ChatMessages> {
    Context context;
    int layoutResourceId;
    ChatMessages data[] = null;

    public ChatMessagesAdapter(Context context, int layoutResourceId,
                               ChatMessages[] data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessages messages = this.getItem(position);
        View row = convertView;
        ChatMessagesHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ChatMessagesHolder();
            holder.message = (TextView) row.findViewById(R.id.message_text);
            holder.layout = (LinearLayout) row.findViewById(R.id.message_text_layout);
            holder.row = (LinearLayout) row.findViewById(R.id.message_row_layout);
            holder.background = (LinearLayout) row.findViewById(R.id.message_text_background);
            holder.time = (TextView) row.findViewById(R.id.message_text_date);
            row.setTag(holder);
        } else {
            holder = (ChatMessagesHolder) row.getTag();
        }

        ChatMessages chatMessages = data[position];
        holder.message.setText(chatMessages.message);
        holder.time.setText(chatMessages.time);

        if (messages.IsMine()) {
            holder.message.setGravity(Gravity.RIGHT);
            holder.time.setGravity(Gravity.RIGHT);
            holder.layout.setGravity(Gravity.RIGHT);
            holder.row.setGravity(Gravity.RIGHT);
        } else {
            holder.message.setGravity(Gravity.LEFT);
            holder.time.setGravity(Gravity.LEFT);
            holder.layout.setGravity(Gravity.LEFT);
            holder.row.setGravity(Gravity.LEFT);
        }
        return row;
    }

    static class ChatMessagesHolder {
        LinearLayout row;
        LinearLayout layout;
        LinearLayout background;
        TextView message;
        TextView time;
    }

}
