package im.tox.antox;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import im.tox.antox.callbacks.AntoxOnMessageCallback;

/**
 * Created by ollie on 28/02/14.
 */
public class ChatFragment extends Fragment {


    public static String ARG_CONTACT_NUMBER = "contact_number";
    private ListView chatListView;
    private int counter = 0;

    private ChatMessages chat_messages[] = new ChatMessages[counter];
    private ChatMessagesAdapter adapter;
    private EditText messageBox;

    public ChatFragment() {

    }

    public void sendMessage(View view) {
        EditText tmp = (EditText) getView().findViewById(R.id.yourMessage);

        chat_messages = Arrays.copyOf(chat_messages, chat_messages.length + 1);

        SimpleDateFormat time = new SimpleDateFormat("HH:mm");

        chat_messages[counter] = new ChatMessages(tmp.getText().toString(),
                time.format(new Date()), true);

        adapter = new ChatMessagesAdapter(getActivity(),
                R.layout.chat_message_row, chat_messages);


        chatListView.setAdapter(adapter);

        tmp.setText("");
        counter++;
        chatListView.setSelection(adapter.getCount() - 1);
    }


    public void setContact(int position, String contact) {
        if (contact != null) {

        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        adapter = new ChatMessagesAdapter(getActivity(),
                R.layout.chat_message_row, chat_messages);
        chatListView = (ListView) rootView.findViewById(R.id.chatMessages);
        chatListView.setAdapter(adapter);
        messageBox = (EditText) rootView.findViewById(R.id.yourMessage);
        messageBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                chatListView.setSelection(adapter.getCount() - 1);
            }
        });

        return rootView;
    }
}
