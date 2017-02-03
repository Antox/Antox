package chat.tox.antox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by zoff99 on 29.01.2017.
 */

public class CrashActivity extends AppCompatActivity
{
    ImageView bug_button = null;
    View CrashView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        System.out.println("CrashActivity:onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);


        CrashView = (View) this.findViewById(R.id.CrashView);
        CrashView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent e)
            {
                try
                {
                    int action = e.getAction();
                    if (action == MotionEvent.ACTION_DOWN)
                    {
                        CrashView.setBackgroundColor(Color.parseColor("#ee0000"));
                        return true;
                    }
                    else
                    {
                        if (action == MotionEvent.ACTION_UP)
                        {
                            CrashView.setBackgroundColor(Color.parseColor("#FF9900"));
                            restart_app();
                            finish();
                            return true;
                        }
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                return false;
            }

        });


        bug_button = (ImageView) this.findViewById(R.id.bugButton);
        bug_button.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View arg0, MotionEvent e)
            {
                try
                {
                    int action = e.getAction();
                    if (action == MotionEvent.ACTION_DOWN)
                    {
                        bug_button.setLeft(bug_button.getLeft() + 10);
                        bug_button.setTop(bug_button.getTop() + 10);
                        return true;
                    }
                    else
                    {
                        if (action == MotionEvent.ACTION_UP)
                        {
                            bug_button.setLeft(bug_button.getLeft() - 10);
                            bug_button.setTop(bug_button.getTop() - 10);
                            restart_app();
                            finish();
                            return true;
                        }
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                return false;
            }

        });
    }

    public void restart_app()
    {
        PendingIntent intent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(getApplicationContext(), chat.tox.antox.activities.LoginActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 300, intent); // restart app after 1 second delay
    }

}
