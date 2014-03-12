package im.tox.antox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import im.tox.antox.callbacks.AntoxOnMessageCallback;

/**
 * Created by ollie on 28/02/14.
 */
public class ChatFragment extends Fragment {
    private static String TAG = "im.tox.antox.ChatFragment";
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
        ChatMessages data[] = new ChatMessages[messages.size()];
        for (int i = 0; i<messages.size(); i++) {
            data[i] = new ChatMessages(messages.get(i).message, messages.get(i).timestamp.toString(), messages.get(i).is_outgoing);
        }
        adapter = new ChatMessagesAdapter(getActivity(), R.layout.chat_message_row, data);
        chatListView.setAdapter(adapter);
        chatListView.setSelection(adapter.getCount() - 1);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        adapter = new ChatMessagesAdapter(getActivity(), R.layout.chat_message_row, new ChatMessages[0]);
        chatListView = (ListView) rootView.findViewById(R.id.chatMessages);
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

        main_act = (MainActivity) getActivity();
        main_act.chat = this;
        main_act.updateChat(toxSingleton.activeFriendKey);

        AntoxDB db = new AntoxDB(getActivity().getApplicationContext());

        String activeName=null;
        if(main_act.toxSingleton.friendsList.getById(toxSingleton.activeFriendKey)!=null)
            activeName = main_act.toxSingleton.friendsList.getById(toxSingleton.activeFriendKey).getName();

        if(activeName==null){
            main_act.pane.openPane();
            Toast.makeText(main_act,"Cannot chat this person yet",Toast.LENGTH_SHORT).show();
        }
        else if(activeName.contains("(!)")) {
            db.updateFriendName(toxSingleton.activeFriendKey,activeName.substring(0,activeName.indexOf("(")));
        }
        db.close();

        return rootView;
    }
}
