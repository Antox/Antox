package im.tox.antox.activities;

import android.app.Activity
import android.app.AlertDialog
import android.content.CursorLoader
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.List
import java.util.Random
import java.util.concurrent.TimeUnit
import im.tox.antox.R
import im.tox.antox.adapters.ChatMessagesAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.ChatMessages
import im.tox.antox.utils.Constants
import im.tox.antox.utils.FileDialog
import im.tox.antox.utils.FriendInfo
import im.tox.antox.utils.IconColor
import im.tox.antox.utils.Tuple
import im.tox.jtoxcore.ToxException
import im.tox.jtoxcore.ToxUserStatus
import rx.Observable
import rx.Observer
import rx.Subscriber
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions

class ChatActivity extends Activity {
    val TAG: String = "im.tox.antox.fragments.ChatFragment"
    //var ARG_CONTACT_NUMBER: String = "contact_number"
    //var chatListView: ListView
    //var adapter: ChatMessagesAdapter
    //var messageBox: EditText
    //var isTypingBox: TextView
    //var statusTextBox: TextView
    //var toxSingleton: ToxSingleton = ToxSingleton.getInstance()
    //var messagesSub: Subscription
    //var progressSub: Subscription
    //var activeKeySub: Subscription
    //var titleSub: Subscription
    //var typingSub: Subscription
    //var chatMessages: ArrayList<ChatMessages>
    //var activeKey: String
    //var scrolling: boolean = false
    //var antoxDB: AntoxDB
    //var photoPath: String
    override def onCreate(savedInstanceState: Bundle) = {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
    }
}
