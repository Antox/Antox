package chat.tox.antox.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.Chronometer
import chat.tox.antox.R

class ActiveCallBarFragment extends Fragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    val rootView = inflater.inflate(R.layout.fragment_active_call_bar, container, false)

    rootView
  }

  def startChronometer(base: Long) = {
    val chronometer = getView.findViewById(R.id.call_bar_chronometer).asInstanceOf[Chronometer]
    chronometer.setBase(base)
    chronometer.start()
  }
}
