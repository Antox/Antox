package im.tox.antox.fragments

import android.os.{Build, Bundle}
import android.support.v4.app.{Fragment, FragmentManager}
import android.support.v4.view.ViewPager
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{FrameLayout, ImageView, TextView}
import com.astuetz.PagerSlidingTabStrip
import com.astuetz.PagerSlidingTabStrip.CustomTabProvider
import com.balysv.materialripple.MaterialRippleLayout
import im.tox.antox.pager.BetterFragmentPagerAdapter
import im.tox.antoxnightly.R

class AddPaneFragment extends Fragment {

  var pager: ViewPager = _

  class AddPagerAdapter(fm: FragmentManager) extends BetterFragmentPagerAdapter(fm) with CustomTabProvider {

    val ICONS: Array[Int] = Array(R.drawable.ic_person_add_white_24dp, R.drawable.ic_group_add_white_24dp)
    val LABELS: Array[String] = Array(getResources.getString(R.string.addpane_friend_label),
                                      getResources.getString(R.string.addpane_group_label))

    override def getCustomTabView(parent: ViewGroup, position: Int): View = {
      //disable the material ripple layout on pre-honeycomb devices
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        val customTabLayout: FrameLayout = LayoutInflater.from(getActivity)
          .inflate(R.layout.custom_tab_old, parent, false).asInstanceOf[FrameLayout]
        customTabLayout.findViewById(R.id.image).asInstanceOf[ImageView].setImageResource(ICONS(position))
        customTabLayout.findViewById(R.id.text).asInstanceOf[TextView].setText(LABELS(position))
        return customTabLayout
      } else {
        val materialRippleLayout: MaterialRippleLayout = LayoutInflater.from(getActivity)
          .inflate(R.layout.custom_tab, parent, false).asInstanceOf[MaterialRippleLayout]
        materialRippleLayout.findViewById(R.id.image).asInstanceOf[ImageView].setImageResource(ICONS(position))
        materialRippleLayout.findViewById(R.id.text).asInstanceOf[TextView].setText(LABELS(position))
        return materialRippleLayout
      }

      null
    }

    override def getPageTitle(position: Int): CharSequence = {
      position match {
        case 0 => return LABELS(0)
        case _ => return LABELS(1)
      }

      null
    }

    override def getItem(pos: Int): Fragment = pos match {
      case 0 => new AddFriendFragment()
      case _ => new AddGroupFragment()
    }

    override def getCount: Int = 2
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = inflater.inflate(R.layout.fragment_pane, container, false)
    pager = rootView.findViewById(R.id.pager).asInstanceOf[ViewPager]
    val tabs = rootView.findViewById(R.id.pager_tabs).asInstanceOf[PagerSlidingTabStrip]

    pager.setAdapter(new AddPagerAdapter(getFragmentManager))
    tabs.setViewPager(pager)

    rootView
  }

  def getSelectedFragment: Fragment = {
    pager.getAdapter.asInstanceOf[AddPagerAdapter].getActiveFragment(pager, pager.getCurrentItem)
  }
}
