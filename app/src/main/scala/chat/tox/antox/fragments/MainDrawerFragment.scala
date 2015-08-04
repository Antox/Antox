package chat.tox.antox.fragments

import java.util.Random

import android.content.{Intent, SharedPreferences}
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.{Build, Bundle}
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.view.View.OnClickListener
import android.view.{LayoutInflater, MenuItem, View, ViewGroup}
import android.widget.{RelativeLayout, TextView, Toast}
import chat.tox.antox.R
import chat.tox.antox.activities.{AboutActivity, ProfileSettingsActivity, SettingsActivity}
import chat.tox.antox.callbacks.{AntoxOnSelfConnectionStatusCallback}
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.{BitmapManager, IconColor}
import chat.tox.antox.wrapper.FileKind.AVATAR
import chat.tox.antox.wrapper.{UserInfo, UserStatus}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.tox4j.core.enums.ToxConnection
import rx.lang.scala.Subscription
import rx.lang.scala.schedulers.AndroidMainThreadScheduler

class MainDrawerFragment extends Fragment {

  private var mDrawerLayout: DrawerLayout = _
  private var mNavigationView: NavigationView = _

  private var preferences: SharedPreferences = _

  private var userDetailsSubscription: Subscription = _

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    preferences = PreferenceManager.getDefaultSharedPreferences(getActivity)
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

    drawerHeader.setBackgroundColor(ThemeManager.primaryColorDark)

    userDetailsSubscription = State.userDb
      .userDetailsObservable(preferences.getString("active_account", ""))
      .combineLatestWith(AntoxOnSelfConnectionStatusCallback.connectionStatusSubject)((user, status) => (user, status))
      .observeOn(AndroidMainThreadScheduler())
      .subscribe((tuple) => {
      refreshDrawerHeader(tuple._1, tuple._2)
    })

    rootView
  }

  override def onResume(): Unit = {
    super.onResume()
  }

  def refreshDrawerHeader(userInfo: UserInfo, connectionStatus: ToxConnection): Unit = {
    val avatarView = getView.findViewById(R.id.avatar).asInstanceOf[CircleImageView]

    val avatar = AVATAR.getAvatarFile(userInfo.avatarName, getActivity)
    avatarView.setImageResource(R.color.grey_light)

    avatar.foreach(av => {
      avatarView.setImageURI(Uri.fromFile(av))
      BitmapManager.load(av, avatarView, isAvatar = true)
    })

    val nameView = getView.findViewById(R.id.name).asInstanceOf[TextView]
    nameView.setText(userInfo.nickname)

    val statusMessageView = getView.findViewById(R.id.status_message).asInstanceOf[TextView]
    statusMessageView.setText(userInfo.statusMessage)

    updateNavigationHeaderStatus(connectionStatus)
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

  def isDrawerOpen = mDrawerLayout.isDrawerOpen(GravityCompat.START)

  def openDrawer(): Unit = {
    mDrawerLayout.openDrawer(GravityCompat.START)
  }

  def closeDrawer(): Unit = {
    mDrawerLayout.closeDrawer(GravityCompat.START)
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
            val db = State.db

            db.addGroup(groupId, name, "")
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

  override def onPause(): Unit = {
    super.onPause()
  }

  override def onDestroy(): Unit = {
    super.onDestroy()

    userDetailsSubscription.unsubscribe()
  }
}
