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
import android.widget.ArrayAdapter;
import edu.bellevue.android.blackboard.BlackboardHelper;
import edu.bellevue.android.blackboard.Course;

public class CourseActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private List<Course> courses;
	private SharedPreferences prefs;
	private Context ctx;
	private Handler handler;
	private ProgressDialog pd;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.listview);
	    ctx = this.getApplicationContext();
	    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    handler = new threadHandler();
	    
	    pd = ProgressDialog.show(this, "Please Wait", "Loading Courses...");
	    
	    Thread t = new Thread(new getCoursesThread());
	    t.start();
	    
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
	
	private class threadHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			switch(m.what)
			{
			case THREAD_COMPLETE:
				pd.dismiss();
				setListAdapter(new ArrayAdapter<Course>(ctx, android.R.layout.simple_list_item_1,courses));
			case CONN_NOT_ALLOWED:
			}
		}
	}
	
	private class getCoursesThread implements Runnable
	{

		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				courses = BlackboardHelper.getCourses();
				handler.sendEmptyMessage(THREAD_COMPLETE);
			}else
			{
				ConnChecker.showUnableToConnect(CourseActivity.this);
				handler.sendEmptyMessage(CONN_NOT_ALLOWED);
			}
			
		}
		
	}
}
