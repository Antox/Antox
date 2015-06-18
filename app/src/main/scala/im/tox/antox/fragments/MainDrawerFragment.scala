package im.tox.antox.fragments

import android.content.{SharedPreferences, Intent}
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.View.OnClickListener
import android.view.{View, ViewGroup, LayoutInflater, MenuItem}
import android.widget.{Toast, TextView}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.antox.activities.{AboutActivity, SettingsActivity, ProfileSettingsActivity}
import im.tox.antox.callbacks.{SelfConnectionStatusChangeListener, AntoxOnSelfConnectionStatusCallback}
import im.tox.antox.data.State
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.{IconColor, BitmapManager}
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.antox.wrapper.UserStatus
import im.tox.antoxnightly.R
import im.tox.tox4j.core.enums.ToxConnection

class MainDrawerFragment extends Fragment {

  private var mDrawerLayout: DrawerLayout = _

  private var mNavigationView: NavigationView = _

  var preferences: SharedPreferences = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)

    AntoxOnSelfConnectionStatusCallback
      .addConnectionStatusChangeListener(new SelfConnectionStatusChangeListener {
      override def onSelfConnectionStatusChange(toxConnection: ToxConnection): Unit = {
        println("connection status change called " + toxConnection)
        updateNavigationHeaderStatus(toxConnection)
      }
    })
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val rootView = inflater.inflate(R.layout.fragment_main_drawer, container, false)

    // Set up the navigation drawer
    mDrawerLayout = rootView.findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
    mNavigationView = rootView.findViewById(R.id.left_drawer).asInstanceOf[NavigationView]

    mNavigationView.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener {
      override def onNavigationItemSelected(menuItem: MenuItem): Boolean = {
        selectItem(menuItem)
        true
      }
    })

    val drawerHeader = rootView.findViewById(R.id.drawer_header)
    drawerHeader.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val intent = new Intent(getActivity, classOf[ProfileSettingsActivity])
        startActivity(intent)
      }
    })

    rootView
  }

  override def onResume(): Unit = {
    super.onResume()
    // Update navigation drawer header in onResume because
    // nothing other than status can change without exiting the Activity
    val avatarView = getView.findViewById(R.id.avatar).asInstanceOf[CircleImageView]

    val avatar = AVATAR.getAvatarFile(preferences.getString("avatar", ""), getActivity)
    avatarView.setImageResource(R.color.grey_light)

    avatar.foreach(av => {
      BitmapManager.load(av, avatarView, isAvatar = true)
    })

    val nameView = getView.findViewById(R.id.name).asInstanceOf[TextView]
    nameView.setText(preferences.getString("nickname", ""))

    val statusMessageView = getView.findViewById(R.id.status_message).asInstanceOf[TextView]
    statusMessageView.setText(preferences.getString("status_message", ""))

    updateNavigationHeaderStatus(ToxSingleton.tox.getSelfConnectionStatus)
  }

  def updateNavigationHeaderStatus(toxConnection: ToxConnection): Unit = {
    val statusView = getView.findViewById(R.id.status)

    val status = UserStatus.getToxUserStatusFromString(preferences.getString("status", ""))
    val online = toxConnection != ToxConnection.NONE
    val drawable = getResources.getDrawable(IconColor.iconDrawable(online, status))

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      statusView.setBackground(drawable)
    } else {
      statusView.setBackgroundDrawable(drawable)
    }
  }

  def openDrawer(): Unit = {
    mDrawerLayout.openDrawer(GravityCompat.START)
  }

  private def selectItem(menuItem: MenuItem) {
    val id = menuItem.getItemId

    id match {
      case R.id.nav_profile_options =>
        val intent = new Intent(getActivity, classOf[ProfileSettingsActivity])
        startActivity(intent)

      case R.id.nav_settings =>
        val intent = new Intent(getActivity, classOf[SettingsActivity])
        startActivity(intent)

      case R.id.nav_create_group =>
        //TODO: uncomment for the future
        /* val dialog = new CreateGroupDialog(this)
        dialog.addCreateGroupListener(new CreateGroupListener {
          override def groupCreationConfimed(name: String): Unit = {
            val groupNumber = ToxSingleton.tox.newGroup(name)
            val groupId = ToxSingleton.tox.getGroupChatId(groupNumber)
            val db = new AntoxDB(getApplicationContext)

            db.addGroup(groupId, name, "")
            db.close()
            ToxSingleton.updateGroupList(getApplicationContext)
          }
        })
        dialog.showDialog()
        */
        Toast.makeText(getActivity, getResources.getString(R.string.main_group_coming_soon), Toast.LENGTH_LONG)
          .show()

      case R.id.nav_about =>
        val intent = new Intent(getActivity, classOf[AboutActivity])
        startActivity(intent)

      case R.id.nav_logout =>
        State.logout(getActivity)
    }

    mDrawerLayout.closeDrawer(mNavigationView)
  }
}
