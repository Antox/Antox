package chat.tox.antox.fragments

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.View.OnClickListener
import android.view._
import android.widget.{Button, EditText, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.{GroupKey, ToxAddress, ToxKey}
import im.tox.tox4j.exceptions.ToxException

class AddGroupFragment extends Fragment with InputableID {

  var groupKey: GroupKey = _

  var originalUsername: String = ""

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var groupKeyView: EditText = _

  var groupAlias: EditText = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_group, container, false)
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addgroup_group_added)
    groupKeyView = rootView.findViewById(R.id.addgroup_key).asInstanceOf[EditText]
    groupAlias = rootView.findViewById(R.id.addgroup_groupAlias).asInstanceOf[EditText]

    rootView.findViewById(R.id.add_group_button).asInstanceOf[Button].setOnClickListener(new OnClickListener {
      override def onClick(view: View): Unit = {
        //TODO: Uncomment this for the future
        //addGroup(view)
        Toast.makeText(getActivity, getActivity.getResources.getString(R.string.main_group_coming_soon), Toast.LENGTH_LONG)
          .show()
      }
    })
    rootView
  }

  override def onPause(): Unit = {
    super.onPause()
  }

  def inputID(input: String) {
    val addGroupKey = getView.findViewById(R.id.addgroup_key).asInstanceOf[EditText]
    val groupKey = UiUtils.sanitizeAddress(ToxAddress.removePrefix(input))

    if (ToxKey.isKeyValid(groupKey)) {
      addGroupKey.setText(groupKey)
    } else {
      showToastInvalidID()
    }
  }

  private def checkAndSend(rawGroupKey: String, originalUsername: String): Boolean = {
      if (ToxKey.isKeyValid(rawGroupKey)) {
        val key = new GroupKey(rawGroupKey)
        val alias = groupAlias.getText.toString //TODO: group aliases

        val db = State.db
        if (!db.doesContactExist(key)) {
          try {
            ToxSingleton.tox.joinGroup(key)
            AntoxLog.debug("joined group : " + groupKeyView)
            ToxSingleton.save()
          } catch {
            case e: ToxException[_] => e.printStackTrace()
          }
          db.addGroup(key, UiUtils.trimId(key), topic = "")

          //prevent already-added group from having an existing group invite
          db.deleteGroupInvite(key)
          AntoxNotificationManager.clearRequestNotification(key)
        } else {
          toast = Toast.makeText(context, getResources.getString(R.string.addgroup_group_exists), Toast.LENGTH_SHORT)
          toast.show()
          false
        }
        toast = Toast.makeText(context, text, duration)
        toast.show()
        true
      } else {
        showToastInvalidID()
        false
      }
  }

  def addGroup(view: View) {
    if (groupKeyView.length == 64) {
      // Attempt to use ID as a Group ID
      val result = checkAndSend(groupKeyView.getText.toString, originalUsername)
      if (result) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
        val i = new Intent()
        getActivity.setResult(Activity.RESULT_OK, i)
        getActivity.finish()
      }
    } else {
      showToastInvalidID()
    }
  }

  def showToastInvalidID(): Unit = {
    toast = Toast.makeText(context, getResources.getString(R.string.invalid_group_ID), Toast.LENGTH_SHORT)
    toast.show()
  }
}
