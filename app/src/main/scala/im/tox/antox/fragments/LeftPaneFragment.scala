package im.tox.antox.fragments

import android.os.{Build, Bundle}
import android.support.v4.app.{Fragment, FragmentManager, FragmentPagerAdapter}
import android.support.v4.view.ViewPager
import android.view.ViewGroup.LayoutParams
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{FrameLayout, ImageView, RelativeLayout}
import com.astuetz.PagerSlidingTabStrip
import com.astuetz.PagerSlidingTabStrip.CustomTabProvider
import com.balysv.materialripple.MaterialRippleLayout
import im.tox.antox.activities.MainActivity
import im.tox.antoxnightly.R

class LeftPaneFragment extends Fragment {

  class LeftPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) with CustomTabProvider {

    val ICONS: Array[Int] = Array(R.drawable.ic_chat_white_24dp, R.drawable.ic_person_white_24dp)

    override def getCustomTabView(parent: ViewGroup, position: Int): View = {
      //hack to center the image only for left pane
      val params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
      params.addRule(RelativeLayout.CENTER_HORIZONTAL)
      params.addRule(RelativeLayout.CENTER_VERTICAL)

      //disable the material ripple layout on pre-honeycomb devices
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
        val customTabLayout: FrameLayout = LayoutInflater.from(getActivity).inflate(R.layout.custom_tab_old, parent, false).asInstanceOf[FrameLayout]
        val imageView = customTabLayout.findViewById(R.id.image).asInstanceOf[ImageView]
        imageView.setImageResource(ICONS(position))
        imageView.setLayoutParams(params)
        return customTabLayout
      } else {
        val materialRippleLayout: MaterialRippleLayout = LayoutInflater.from(getActivity).inflate(R.layout.custom_tab, parent, false).asInstanceOf[MaterialRippleLayout]
        val imageView = materialRippleLayout.findViewById(R.id.image)
        imageView.asInstanceOf[ImageView].setImageResource(ICONS(position))
        imageView.setLayoutParams(params)
        return materialRippleLayout
      }

      null
    }

    override def getPageTitle(position: Int): CharSequence = {
      position match {
        case 0 => return "Recent"
        case _ => return "Contacts"
      }

      null
    }

    override def getItem(pos: Int): Fragment = pos match {
      case 0 => new RecentFragment()
      case _ => new ContactsFragment()
    }

    override def getCount: Int = 2
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val thisActivity = this.getActivity.asInstanceOf[MainActivity]
    val actionBar = thisActivity.getSupportActionBar
    val rootView = inflater.inflate(R.layout.fragment_leftpane, container, false)
    val pager = rootView.findViewById(R.id.pager).asInstanceOf[ViewPager]
    val tabs = rootView.findViewById(R.id.pager_tabs).asInstanceOf[PagerSlidingTabStrip]

    pager.setAdapter(new LeftPagerAdapter(getFragmentManager))
    tabs.setViewPager(pager)

    rootView
  }
}
