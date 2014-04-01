package im.tox.antox.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.ChatMessages;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Message;
import im.tox.antox.R;
import im.tox.antox.tox.ToxService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.activities.*;
import im.tox.antox.adapters.ChatMessagesAdapter;

/**
 * Created by ollie on 28/02/14.
 */
public class ChatFragment extends Fragment {
    private static String TAG = "im.tox.antox.fragments.ChatFragment";
    public static String ARG_CONTACT_NUMBER = "contact_number";
    private ListView chatListView;
    private int counter = 0;

    private ChatMessages chat_messages[] = new ChatMessages[counter];
    private ChatMessagesAdapter adapter;
    private EditText messageBox;
    private MainActivity main_act;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();


    public ChatFragment() {

    }



    public void sendMessage() {
        if(messageBox.getText().toString().length()==0){
            return;
        }
        EditText message = (EditText) getView().findViewById(R.id.yourMessage);
        Intent intent = new Intent(main_act, ToxService.class);
        intent.setAction(Constants.SEND_MESSAGE);
        intent.putExtra("message", message.getText().toString());
        intent.putExtra("key", toxSingleton.activeFriendKey);
        message.setText("");
        getActivity().startService(intent);
    }


    public void setContact(int position, String contact) {
        if (contact != null) {

        }
    }

    public void updateChat(ArrayList<Message> messages) {
        Log.d(TAG, "updating chat");
        Log.d(TAG, "chat message size = " + messages.size());
        if(messages.size() >= 0 ) {
            ArrayList<ChatMessages> data = new ArrayList<ChatMessages>(messages.size());
            for (int i = 0; i < messages.size(); i++) {
                data.add(new ChatMessages(messages.get(i).message_id, messages.get(i).message, messages.get(i).timestamp.toString(), messages.get(i).is_outgoing, messages.get(i).has_been_received, messages.get(i).successfully_sent));
            }
            adapter = new ChatMessagesAdapter(main_act.getApplicationContext(), R.layout.chat_message_row, data);
            chatListView.setAdapter(adapter);
            chatListView.setSelection(adapter.getCount() - 1);
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        main_act = (MainActivity) getActivity();
        adapter = new ChatMessagesAdapter(getActivity(), R.layout.chat_message_row, new ArrayList<ChatMessages>(0));
        chatListView = (ListView) rootView.findViewById(R.id.chatMessages);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        chatListView.setStackFromBottom(true);
        chatListView.setAdapter(adapter);
        chatListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final CharSequence items[] = new CharSequence[]{"Delete"};//copy forward etc can be added here
                final ChatMessages item = (ChatMessages) parent.getAdapter().getItem(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(main_act);
                builder.setCancelable(true)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {

                                switch (index) {
                                    case 0:
                                        AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                                        db.deleteMessage(item.message_id);
                                        db.close();
                                        ChatMessagesAdapter chatAdapter = (ChatMessagesAdapter) chatListView.getAdapter();
                                        chatAdapter.data.remove(item);
                                        chatAdapter.notifyDataSetChanged();
                                        main_act.updateLeftPane();
                                        break;
                                }
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });

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
        main_act = (MainActivity) getActivity();
        main_act.chat = this;
        toxSingleton.rightPaneActive = true;
        main_act.updateChat(toxSingleton.activeFriendKey);
        if (toxSingleton.friendsList.getById(toxSingleton.activeFriendKey) == null) {
            main_act.activeTitle = "Toxer";
        } else if (toxSingleton.friendsList.getById(toxSingleton.activeFriendKey).getName() == null) {
            if (toxSingleton.friendsList.getById(toxSingleton.activeFriendKey).getId() == null) {
                main_act.activeTitle = "Toxer";
            }
            else {
                main_act.activeTitle = toxSingleton.friendsList.getById(toxSingleton.activeFriendKey)
                        .getId().substring(0, 7);
            }
        } else {
            main_act.activeTitle = toxSingleton.friendsList.getById(toxSingleton.activeFriendKey).getName();
        }
        main_act.pane.closePane();

        return rootView;
    }
}
