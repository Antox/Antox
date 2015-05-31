package im.tox.antox.fragments

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View.OnClickListener
import android.view._
import android.widget.{Button, EditText, Toast}
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{Constants, UIUtils}
import im.tox.antoxnightly.R
import im.tox.tox4j.exceptions.ToxException

class AddGroupFragment extends Fragment with InputableID {

  var _groupID: String = ""

  var _originalUsername: String = ""

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var groupID: EditText = _

  var groupAlias: EditText = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_group, container, false)
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addgroup_group_added)
    groupID = rootView.findViewById(R.id.addgroup_key).asInstanceOf[EditText]
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

  override def onPause() = {
    super.onPause()
  }

  def inputID(input: String) {
    val addGroupKey = getView.findViewById(R.id.addgroup_key).asInstanceOf[EditText]
    val groupKey = (if (input.toLowerCase.contains("tox:")) input.substring(4) else input)
      .replaceAll("\uFEFF", "").replace(" ", "") //remove start-of-file unicode char and spaces
    if (validateGroupKey(groupKey)) {
      addGroupKey.setText(groupKey)
    } else {
      showToastInvalidID()
    }
  }

  private def checkAndSend(groupId: String, originalUsername: String): Int = {
      if (validateGroupKey(groupId)) {
        val alias = groupAlias.getText.toString //TODO: group aliases

        val db = new AntoxDB(getActivity.getApplicationContext)
        if (!db.doesGroupExist(groupId)) {
          try {
            ToxSingleton.tox.joinGroup(groupId)
            println("joined group : " + groupId)
            ToxSingleton.save()
          } catch {
            case e: ToxException[_] => e.printStackTrace()
          }
          Log.d("AddGroupID", "Adding group to database")
          db.addGroup(groupId, UIUtils.trimIDForDisplay(groupId), topic = "")
        } else {
          db.close()
          toast = Toast.makeText(context, getResources.getString(R.string.addgroup_group_exists), Toast.LENGTH_SHORT)
          toast.show()
          -2
        }
        db.close()
        toast = Toast.makeText(context, text, duration)
        toast.show()
        0
      } else {
        println("not validated")
        showToastInvalidID()
        -1
      }
  }

  def addGroup(view: View) {
    if (groupID.length == 64) {
      // Attempt to use ID as a Group ID
      val result = checkAndSend(groupID.getText.toString, _originalUsername)
      if (result == 0) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
        val i = new Intent()
        getActivity.setResult(Activity.RESULT_OK, i)
        getActivity.finish()
      }
    } else {
      println("length is not 64")
      showToastInvalidID()
    }
  }

  def showToastInvalidID(): Unit = {
    toast = Toast.makeText(context, getResources.getString(R.string.invalid_group_ID), Toast.LENGTH_SHORT)
    toast.show()
  }

  //TODO move this to somewhere sane (ToxAddress class)
  private def validateGroupKey(groupKey: String): Boolean = {
    !(groupKey.length != 64 || groupKey.matches("[[:xdigit:]]"))
  }
}
