package im.tox.antox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

    private String prettifyTimestamp(String t) {
        String output = "";
        String month = "";
        switch (Integer.parseInt(t.substring(5, 7))) {
            case 1:
                month = "Jan";
                break;
            case 2:
                month = "Feb";
                break;
            case 3:
                month = "Mar";
                break;
            case 4:
                month = "Apr";
                break;
            case 5:
                month = "May";
                break;
            case 6:
                month = "Jun";
                break;
            case 7:
                month = "Jul";
                break;
            case 8:
                month = "Aug";
                break;
            case 9:
                month = "Sep";
                break;
            case 10:
                month = "Oct";
                break;
            case 11:
                month = "Nov";
                break;
            case 12:
                month = "Dec";
                break;
        }
        output = month + " " + Integer.parseInt(t.substring(8,10)) + " " + t.substring(11,16);
        return output;
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
            holder.alignment = (LinearLayout) row.findViewById(R.id.message_alignment_box);
            holder.layout = (LinearLayout) row.findViewById(R.id.message_text_layout);
            holder.row = (LinearLayout) row.findViewById(R.id.message_row_layout);
            holder.background = (LinearLayout) row.findViewById(R.id.message_text_background);
            holder.time = (TextView) row.findViewById(R.id.message_text_date);
            holder.check = (ImageView) row.findViewById(R.id.chat_row_check);
            holder.halfcheck = (ImageView) row.findViewById(R.id.chat_row_halfcheck);
            row.setTag(holder);
        } else {
            holder = (ChatMessagesHolder) row.getTag();
        }

        ChatMessages chatMessages = data[position];
        holder.message.setText(chatMessages.message);
        holder.time.setText(prettifyTimestamp(chatMessages.time));

        if (messages.IsMine()) {
            holder.alignment.setGravity(Gravity.RIGHT);
            holder.time.setGravity(Gravity.RIGHT);
            holder.layout.setGravity(Gravity.RIGHT);
            holder.row.setGravity(Gravity.RIGHT);
            if (messages.sent) {
                holder.check.setVisibility(View.VISIBLE);
                if (messages.received) {
                    holder.halfcheck.setVisibility(View.VISIBLE);
                } else {
                    holder.halfcheck.setVisibility(View.GONE);
                }
            } else {
                holder.check.setVisibility(View.GONE);
                holder.halfcheck.setVisibility(View.GONE);
            }
        } else {
            holder.alignment.setGravity(Gravity.LEFT);
            holder.time.setGravity(Gravity.LEFT);
            holder.layout.setGravity(Gravity.LEFT);
            holder.row.setGravity(Gravity.LEFT);
            holder.check.setVisibility(View.GONE);
            holder.halfcheck.setVisibility(View.GONE);
        }
        return row;
    }

    static class ChatMessagesHolder {
        LinearLayout row;
        LinearLayout layout;
        LinearLayout background;
        LinearLayout alignment;
        TextView message;
        TextView time;
        ImageView check;
        ImageView halfcheck;
    }

}
