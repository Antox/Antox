package im.tox.antox.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import im.tox.antox.R;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.adapters.ChatMessagesAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.ChatMessages;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Message;
import im.tox.jtoxcore.ToxException;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by ollie on 28/02/14.
 */
public class ChatFragment extends Fragment {
    private static String TAG = "im.tox.antox.fragments.ChatFragment";
    public static String ARG_CONTACT_NUMBER = "contact_number";
    private ListView chatListView;
    private int counter = 0;

    private ChatMessagesAdapter adapter;
    private EditText messageBox;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();
    Subscription messagesSub;
    private ArrayList<ChatMessages> chatMessages;
    private String activeKey;


    public ChatFragment() {
    }

    public ChatFragment(String key) {
        this.activeKey = key;
    }

    @Override
    public void onResume(){
        super.onResume();
        messagesSub = toxSingleton.updatedMessagesSubject.map(new Func1<Boolean, ArrayList<Message>>() {
            @Override
            public ArrayList<Message> call(Boolean input) {
                Log.d("ChatFragment","updatedMessageSubject map");
                AntoxDB antoxDB = new AntoxDB(getActivity());
                ArrayList<Message> messageList = antoxDB.getMessageList(activeKey);
                antoxDB.close();
                return messageList;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<ArrayList<Message>>() {
            @Override
            public void call(ArrayList<Message> messages) {
                Log.d("ChatFragment", "updatedMessageSubject subscription");
                updateChat(messages);
            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        messagesSub.unsubscribe();
    }

    public void sendMessage() {
        Log.d("ChatFragment","sendMessage");
        if (messageBox.getText() != null && messageBox.getText().toString().length() == 0) {
            return;
        }
        final String msg;
        if (messageBox.getText() != null ) {
            msg = messageBox.getText().toString();
        } else {
            msg = "";
        }
        final String key = activeKey;
        messageBox.setText("");
        Observable<Boolean> send = Observable.create(
                new Observable.OnSubscribe<Boolean>() {
                     @Override
                         public void call(Subscriber<? super Boolean> subscriber) {
                            try {
                                /* Send message */
                                AntoxFriend friend = null;
                                Random generator = new Random();
                                int id = generator.nextInt();
                                try {
                                    friend = toxSingleton.getAntoxFriend(key);
                                } catch (Exception e) {
                                    Log.d(TAG, e.toString());
                                }
                                if (friend != null) {
                                    boolean sendingSucceeded = true;
                                    try {
                                        toxSingleton.jTox.sendMessage(friend, msg, id);
                                    } catch (ToxException e) {
                                        Log.d(TAG, e.toString());
                                        e.printStackTrace();
                                        sendingSucceeded = false;
                                    }
                                    AntoxDB db = new AntoxDB(getActivity());
                                    /* Add message to chatlog */
                                    db.addMessage(id, key, msg, true, false, false, sendingSucceeded);
                                    db.close();
                                    /* update UI */
                                    toxSingleton.updateMessages(getActivity());
                                }
                                subscriber.onCompleted();
                            } catch (Exception e) {
                                subscriber.onError(e);
                            }
                         }
                     });
        send.subscribeOn(Schedulers.io()).subscribe();
    }

    public void updateChat(ArrayList<Message> messages) {
        if (messages.size() >= 0) {
            adapter.data.clear();
            for (int i = 0; i < messages.size(); i++) {
                adapter.data.add(new ChatMessages(messages.get(i).message_id, messages.get(i).message, messages.get(i).timestamp.toString(), messages.get(i).is_outgoing, messages.get(i).has_been_received, messages.get(i).successfully_sent));
            }
            Log.d("ChatFragment", "Updating chat");
            adapter.notifyDataSetChanged();
            chatListView.setSelection(adapter.getCount() - 1);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("ChatFragment ImageResult resultCode", Integer.toString(resultCode));
        Log.d("ChatFragment ImageResult requestCode", Integer.toString(requestCode));
        Log.d("ChatFragment ImageResult data", data.toString());
        if (requestCode == Constants.IMAGE_RESULT  && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = null;
            String[] filePathColumn = {MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME};
            String filePath = null;
            String fileName = null;
            CursorLoader loader = new CursorLoader(getActivity(), uri, filePathColumn, null, null, null);
            Cursor cursor = loader.loadInBackground();
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
                    filePath = cursor.getString(columnIndex);
                    int fileNameIndex = cursor.getColumnIndexOrThrow(filePathColumn[1]);
                    fileName = cursor.getString(fileNameIndex);
                }
            }
            try {
                path = filePath;
            } catch (Exception e) {
                Log.d("onActivityResult", e.toString());
            }
            if (path != null) {
                toxSingleton.sendFileSendRequest(path, activeKey, getActivity());
            }
        }
    }




    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        chatMessages = new ArrayList<ChatMessages>();
        adapter = new ChatMessagesAdapter(getActivity(), R.layout.chat_message_row, chatMessages);
        chatListView = (ListView) rootView.findViewById(R.id.chatMessages);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setStackFromBottom(true);
        chatListView.setAdapter(adapter);

        messageBox = (EditText) rootView.findViewById(R.id.yourMessage);
        messageBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                chatListView.setSelection(adapter.getCount() - 1);
            }
        });

        View b = (View) rootView.findViewById(R.id.sendMessageButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        View attachmentButton = (View) rootView.findViewById(R.id.attachmentButton);
        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final CharSequence items[];
                items = new CharSequence[] {
                        "Attach image"
                };
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(intent, Constants.IMAGE_RESULT);
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
        return rootView;
    }
}
