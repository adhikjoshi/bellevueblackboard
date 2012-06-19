package edu.bellevue.android;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import edu.bellevue.android.blackboard.Course;
import edu.bellevue.android.blackboard.objects.BlackboardService;

public class CourseActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	private List<Course> courses;
	private SharedPreferences prefs;
	private Context ctx;
	private Handler handler;
	private ProgressDialog pd;
	protected BlackboardService mBoundService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    GoogleAnalyticsTracker.getInstance().trackPageView("/CourseActivity");
	    setContentView(R.layout.listview);
	    ctx = this.getApplicationContext();
	    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    handler = new threadHandler();
	    
	    pd = ProgressDialog.show(CourseActivity.this, "Please Wait", "Loading Courses...");   
	    Thread t = new Thread(new getCoursesThread());
	    t.start();
	    
	}
	
	public void onListItemClick(ListView l, View v, int position, long id)
	{
    	super.onListItemClick(l, v, position, id);
    	if (ConnChecker.shouldConnect(prefs, ctx))
		{
        	Course c = (Course)l.getItemAtPosition(position);
        	Intent i = new Intent(this,ForumActivity.class);
        	i.putExtra("name", c.friendlyName);
        	i.putExtra("course_id", c.courseId);
        	startActivity(i);
		}else
		{
			if (ConnChecker.getConnType(ctx).equals("NoNetwork"))
			{
				Toast.makeText(CourseActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
			}else
			{
				ConnChecker.showUnableToConnect(CourseActivity.this);
			}
		}

	}
	
    public boolean onCreateOptionsMenu(Menu m)
    {
    	m.add("Settings");
    	return super.onCreateOptionsMenu(m);
    }
    
    public boolean onOptionsItemSelected(MenuItem mi)
    {
    	if (mi.getTitle().equals("Settings"))
    	{
    		Intent i = new Intent(this,PrefActivity.class);
    		startActivity(i);
    	}
    	return true;
    }
	
	protected class threadHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			pd.dismiss();
			switch(m.what)
			{
			case THREAD_COMPLETE:
				setListAdapter(new ArrayAdapter<Course>(ctx, android.R.layout.simple_list_item_1,courses));
				break;
    		case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(CourseActivity.this);
    			finish();
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(CourseActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			finish();
    		}
		}
	}	
	protected class getCoursesThread implements Runnable
	{

		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				courses = BlackboardService.getCourses();
				handler.sendEmptyMessage(THREAD_COMPLETE);
			}else
			{
    			if (ConnChecker.getConnType(ctx).equals("NoNetwork"))
    			{
    				handler.sendEmptyMessage(CONN_NOT_POSSIBLE);
    			}else
    			{
    				handler.sendEmptyMessage(CONN_NOT_ALLOWED);
    			}
			}
			
		}
		
	}
}
