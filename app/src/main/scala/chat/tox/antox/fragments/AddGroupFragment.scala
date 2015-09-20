package chat.tox.antox.fragments

import android.app.Activity
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View.OnClickListener
import android.view._
import android.widget.{Button, EditText, Toast}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{Constants, UiUtils}
import chat.tox.antox.wrapper.{ToxAddress, ToxKey}
import im.tox.tox4j.exceptions.ToxException

class AddGroupFragment extends Fragment with InputableID {

  var _groupKey: ToxKey = _

  var _originalUsername: String = ""

  var context: Context = _

  var text: CharSequence = _

  var duration: Int = Toast.LENGTH_SHORT

  var toast: Toast = _

  var groupKey: EditText = _

  var groupAlias: EditText = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    val rootView = inflater.inflate(R.layout.fragment_add_group, container, false)
    getActivity.overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out)

    context = getActivity.getApplicationContext

    text = getString(R.string.addgroup_group_added)
    groupKey = rootView.findViewById(R.id.addgroup_key).asInstanceOf[EditText]
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
        val key = new ToxKey(rawGroupKey)
        val alias = groupAlias.getText.toString //TODO: group aliases

        val db = State.db
        if (!db.doesContactExist(key)) {
          try {
            ToxSingleton.tox.joinGroup(key)
            Log.d("AddGroupFragment","joined group : " + groupKey)
            ToxSingleton.save()
          } catch {
            case e: ToxException[_] => e.printStackTrace()
          }
          Log.d("AddGroupKey", "Adding group to database")
          db.addGroup(key, UiUtils.trimId(key), topic = "")
        } else {
          toast = Toast.makeText(context, getResources.getString(R.string.addgroup_group_exists), Toast.LENGTH_SHORT)
          toast.show()
          false
        }
        toast = Toast.makeText(context, text, duration)
        toast.show()
        true
      } else {
        Log.d("AddGroupFragment","not validated")
        showToastInvalidID()
        false
      }
  }

  def addGroup(view: View) {
    if (groupKey.length == 64) {
      // Attempt to use ID as a Group ID
      val result = checkAndSend(groupKey.getText.toString, _originalUsername)
      if (result) {
        val update = new Intent(Constants.BROADCAST_ACTION)
        update.putExtra("action", Constants.UPDATE)
        LocalBroadcastManager.getInstance(getActivity).sendBroadcast(update)
        val i = new Intent()
        getActivity.setResult(Activity.RESULT_OK, i)
        getActivity.finish()
      }
    } else {
      Log.d("AddGroupFragment","length is not 64")
      showToastInvalidID()
    }
  }

  def showToastInvalidID(): Unit = {
    toast = Toast.makeText(context, getResources.getString(R.string.invalid_group_ID), Toast.LENGTH_SHORT)
    toast.show()
  }
}
