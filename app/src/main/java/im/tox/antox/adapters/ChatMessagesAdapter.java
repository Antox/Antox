package im.tox.antox.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.BitMapHelper;
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
    // Todo: Use different layout resources for views depending on type case
    // see: https://stackoverflow.com/questions/3514548/creating-viewholders-for-listviews-with-different-item-layouts
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

        switch(type) {

            case Constants.MESSAGE_TYPE_OWN:
                ownMessage(holder);
                if (messages.sent) {
                    holder.sent.setVisibility(View.VISIBLE);
                    if (messages.received) {
                        holder.sent.setVisibility(View.GONE);
                        holder.received.setVisibility(View.VISIBLE);
                    } else {
                        holder.received.setVisibility(View.GONE);
                    }
                }
                break;

            case Constants.MESSAGE_TYPE_FRIEND:
                friendMessage(holder);

                holder.sent.setVisibility(View.GONE);
                holder.received.setVisibility(View.GONE);
                break;

            case Constants.MESSAGE_TYPE_FILE_TRANSFER:
                break;
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
                        holder.progress.setVisibility(View.GONE);
                        if (messages.isMine()) {
                            holder.progressText.setText("Sent filesending request");
                        } else {
                            holder.progressText.setText("Received filesending request");
                        }
                        holder.progressText.setVisibility(View.VISIBLE);
                    }
                }
                boolean isImage = true;
                if (messages.received || messages.isMine()) {
                    File f = null;
                    f = new File(holder.message.getText().toString());
                    if (f.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getPath())) {
                        f = new File(holder.message.getText().toString());
                    } else {
                        f = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS), Constants.DOWNLOAD_DIRECTORY);
                        f = new File(f.getAbsolutePath() + "/" + holder.message.getText().toString());
                    }
                /*should check file mime/type here and then decide what to do*/
                    Bitmap bmp = null;
                    if (f.exists()) {
                        try {
                            final File path = f;
                            final BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            options.inSampleSize = calculateInSampleSize(options, 200, 200);
                            bmp = BitmapFactory.decodeFile(path.getPath(), options);
                            holder.imageMessage.setImageBitmap(bmp);
                            holder.imageMessage.setVisibility(View.VISIBLE);
                            holder.imageMessageFrame.setVisibility(View.VISIBLE);
                            holder.imageMessage.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    Intent i = new Intent();
                                    i.setAction(android.content.Intent.ACTION_VIEW);
                                    i.setDataAndType(Uri.fromFile(path), "image/*");
                                    getContext().startActivity(i);
                                }
                            });
                            bmp = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (messages.received && isImage) {
                    holder.progressText.setVisibility(View.GONE);
                    holder.title.setVisibility(View.GONE);
                    holder.message.setVisibility(View.GONE);
                }
                break;

            case Constants.MESSAGE_TYPE_ACTION:

                holder.time.setGravity(Gravity.LEFT);
                holder.layout.setGravity(Gravity.CENTER);
                holder.message.setTextColor(context.getResources().getColor(R.color.grey_light));
                holder.row.setGravity(Gravity.CENTER);

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
    private static int calculateInSampleSize(
                BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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
        //imageMessage.se
    }

}
