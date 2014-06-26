package im.tox.antox.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.BitmapManager;
import im.tox.antox.utils.ChatMessages;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.PrettyTimestamp;

public class ChatMessagesAdapter extends ArrayAdapter<ChatMessages> {
    Context context;
    int layoutResourceId;
    public ArrayList<ChatMessages> data = null;
    private int density;
    private int paddingscale = 8;
    private ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public ChatMessagesAdapter(Context context, int layoutResourceId,
                               ArrayList<ChatMessages> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        density = (int) context.getResources().getDisplayMetrics().density;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    private void ownMessage(ChatMessagesHolder holder) {
        holder.time.setGravity(Gravity.RIGHT);
        holder.layout.setGravity(Gravity.RIGHT);
        holder.message.setTextColor(context.getResources().getColor(R.color.white_absolute));
        holder.row.setGravity(Gravity.RIGHT);
        holder.background.setBackground(context.getResources().getDrawable(R.drawable.chatright));
        holder.background.setPadding(1*density*paddingscale, 1*density*paddingscale, 6*density + 1*density*paddingscale, 1*density*paddingscale);
    }

    private void friendMessage(ChatMessagesHolder holder) {
        holder.message.setTextColor(context.getResources().getColor(R.color.black));
        holder.background.setBackground(context.getResources().getDrawable(R.drawable.chatleft));
        holder.background.setPadding(6 * density + 1 * density * paddingscale, 1 * density * paddingscale, 1 * density * paddingscale, 1 * density * paddingscale);
        holder.time.setGravity(Gravity.LEFT);
        holder.layout.setGravity(Gravity.LEFT);
        holder.row.setGravity(Gravity.LEFT);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        int type = data.get(position).getType();

        ChatMessages messages = this.getItem(position);
        View row = convertView;
        ChatMessagesHolder holder = new ChatMessagesHolder();

        ChatMessages chatMessages = data.get(position);

        LayoutInflater inflater = LayoutInflater.from(context);
        row = inflater.inflate(layoutResourceId, parent, false);

        holder.message = (TextView) row.findViewById(R.id.message_text);
        holder.layout = (LinearLayout) row.findViewById(R.id.message_text_layout);
        holder.row = (LinearLayout) row.findViewById(R.id.message_row_layout);
        holder.background = (LinearLayout) row.findViewById(R.id.message_text_background);
        holder.time = (TextView) row.findViewById(R.id.message_text_date);
        holder.sent = (ImageView) row.findViewById(R.id.chat_row_sent);
        holder.received = (ImageView) row.findViewById(R.id.chat_row_received);
        holder.title = (TextView) row.findViewById(R.id.message_title);
        holder.progress = (ProgressBar) row.findViewById(R.id.file_transfer_progress);
        holder.imageMessage = (ImageView) row.findViewById(R.id.message_sent_photo);
        holder.imageMessageFrame = (FrameLayout) row.findViewById(R.id.message_sent_photo_frame);
        holder.progressText = (TextView) row.findViewById(R.id.file_transfer_progress_text);
        holder.padding = (View) row.findViewById(R.id.file_transfer_padding);

        switch(type) {
            case Constants.MESSAGE_TYPE_OWN:
                ownMessage(holder);
                if (messages.sent) {
                    if (messages.received) {
                        holder.sent.setVisibility(View.GONE);
                        holder.received.setVisibility(View.VISIBLE);
                    } else {
                        holder.sent.setVisibility(View.VISIBLE);
                        holder.received.setVisibility(View.GONE);
                    }
                } else {
                    holder.sent.setVisibility(View.GONE);
                    holder.received.setVisibility(View.GONE);
                }
                break;

            case Constants.MESSAGE_TYPE_FRIEND:
                friendMessage(holder);
                holder.sent.setVisibility(View.GONE);
                holder.received.setVisibility(View.GONE);
                break;

            case Constants.MESSAGE_TYPE_FILE_TRANSFER:
            case Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND:
                if (type == Constants.MESSAGE_TYPE_FILE_TRANSFER) {
                    ownMessage(holder);
                    String[] split = chatMessages.message.split("/");
                    holder.message.setText(split[split.length - 1]);
                } else {
                    friendMessage(holder);
                    holder.message.setText(chatMessages.message);
                }

                holder.title.setVisibility(View.VISIBLE);
                holder.title.setText(R.string.chat_file_transfer);
                holder.received.setVisibility(View.GONE);
                holder.sent.setVisibility(View.GONE);

                if (messages.received) {
                    holder.progress.setVisibility(View.GONE);
                    holder.progressText.setText("Finished");
                    holder.progressText.setVisibility(View.VISIBLE);
                } else {
                    if (messages.sent) {
                        if (messages.message_id != -1) {
                            holder.progress.setVisibility(View.VISIBLE);
                            holder.progress.setMax(messages.size);
                            holder.progress.setProgress(toxSingleton.getProgress(messages.id));
                            holder.progressText.setVisibility(View.GONE);
                        } else { //Filesending failed, it's sent, we no longer have a filenumber, but it hasn't been received
                            holder.progress.setVisibility(View.GONE);
                            holder.progressText.setText("Failed");
                            holder.progressText.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (messages.message_id != -1) {
                            holder.progress.setVisibility(View.GONE);
                            if (messages.isMine()) {
                                holder.progressText.setText("Sent filesending request");
                            } else {
                                holder.progressText.setText("Received filesending request");
                            }
                            holder.progressText.setVisibility(View.VISIBLE);
                        } else { //Filesending request not accepted, it's sent, we no longer have a filenumber, but it hasn't been accepted
                            holder.progress.setVisibility(View.GONE);
                            holder.progressText.setText("Failed");
                            holder.progressText.setVisibility(View.VISIBLE);
                        }
                    }
                }

                if (messages.received || messages.isMine()) {
                    File f = null;
                    if (messages.message.contains("/")) {
                        f = new File(messages.message);
                    } else {
                        f = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY);
                        f = new File(f.getAbsolutePath() + "/" + messages.message);
                    }

                    if (f.exists()) {
                            final File file = f;
                            final String[] okFileExtensions =  new String[] {"jpg", "png", "gif","jpeg"};

                            for (String extension : okFileExtensions) {

                                if (file.getName().toLowerCase().endsWith(extension)) {

                                    if (messages.received) {
                                        if (BitmapManager.checkValidImage(file)) {
                                            BitmapManager.loadBitmap(file, file.getPath().hashCode(), holder.imageMessage);
                                        }

                                        holder.imageMessage.setVisibility(View.VISIBLE);
                                        holder.imageMessageFrame.setVisibility(View.VISIBLE);

                                        holder.padding.setVisibility(View.GONE);
                                        holder.progressText.setVisibility(View.GONE);
                                        holder.title.setVisibility(View.GONE);
                                        holder.message.setVisibility(View.GONE);

                                        holder.imageMessage.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                Intent i = new Intent();
                                                i.setAction(android.content.Intent.ACTION_VIEW);
                                                i.setDataAndType(Uri.fromFile(file), "image/*");
                                                getContext().startActivity(i);
                                            }
                                        });

                                    } else {
                                        holder.padding.setVisibility(View.VISIBLE);
                                    }

                                }

                                break; // break for loop
                            }
                    }
                }

                break;

            case Constants.MESSAGE_TYPE_ACTION:

                holder.time.setGravity(Gravity.CENTER);
                holder.layout.setGravity(Gravity.CENTER);
                holder.sent.setVisibility(View.GONE);
                holder.received.setVisibility(View.GONE);
                holder.message.setTextColor(context.getResources().getColor(R.color.gray_darker));
                holder.row.setGravity(Gravity.CENTER);
                holder.background.setBackgroundColor(context.getResources().getColor(R.color.white_absolute));
                holder.message.setTextSize(12);

                break;
        }

        if(type != Constants.MESSAGE_TYPE_FILE_TRANSFER && type != Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND) {
            holder.title.setVisibility(View.GONE);
            holder.message.setText(chatMessages.message);
            holder.progress.setVisibility(View.GONE);
        }

        holder.time.setText(PrettyTimestamp.prettyChatTimestamp(chatMessages.time));

        return row;
    }

    static class ChatMessagesHolder {
        LinearLayout row;
        LinearLayout layout;
        LinearLayout background;
        TextView message;
        TextView time;
        ImageView sent;
        ImageView received;
        ImageView imageMessage;
        FrameLayout imageMessageFrame;
        TextView title;
        ProgressBar progress;
        TextView progressText;
        View padding;
    }

}
