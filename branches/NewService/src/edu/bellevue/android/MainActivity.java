package edu.bellevue.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import edu.bellevue.android.blackboard.objects.BlackboardService;
import edu.bellevue.android.blackboard.objects.CourseSites91Adapter;

public class MainActivity extends Activity {
	
	private Handler handler = new msgHandler();
	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	private ProgressDialog pd;
	private Context ctx;
	private SharedPreferences prefs;
	private GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	CourseSites91Adapter adapter = new CourseSites91Adapter();
    	adapter.initializeAdapter();
    	BlackboardService.setBlackboardAdapter(adapter);
		tracker.startNewSession("UA-32710526-1",this);
		tracker.setAnonymizeIp(true);
		
    	tracker.trackPageView("/LoginPage");
    	ensureDBExists();

        // get a Calendar object with current time
        Calendar cal = Calendar.getInstance();
        // add 5 minutes to the calendar object
        cal.add(Calendar.MINUTE, 1);
        Intent intent = new Intent(MainActivity.this, WatchUpdateReceiver.class);
        // In reality, you would want to have a static variable for the request code instead of 192837
        PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get the AlarmManager service
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),AlarmManager.INTERVAL_FIFTEEN_MINUTES, sender);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 1 * 60 * 1000, sender);
        setContentView(R.layout.main);
        findViewById(R.id.btnLogIn).setOnClickListener(new submitListener());
        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        BlackboardService.cacheData = prefs.getBoolean("cachedata", false);
        Editor e = prefs.edit();
        if (prefs.getBoolean("firstRun", true))
        {
        	e.putBoolean("wifi",true);
        	e.putBoolean("mobile", true);
        }
        
        e.putBoolean("firstRun", false);
        e.commit();        
        
    	if (BlackboardService.isLoggedIn())
        {
        	// switch to Courses view since we're already logged in :)
        	handler.sendEmptyMessage(THREAD_COMPLETE);
        }
    	
        if (prefs.getBoolean("autologin", false))
        {
        	EditText userName = ((EditText)findViewById(R.id.txtUserName));
    		EditText password = ((EditText)findViewById(R.id.txtPassword));
    		userName.setText(prefs.getString("username", ""));
    		password.setText(prefs.getString("password", ""));
    		
    		// call the submit button's click handler :)
    		new submitListener().onClick(null);
    		
        }
    }
     
    private void ensureDBExists() 
    {
    	File dbFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blackboard/database.db3");
    	if (!dbFile.exists())
    	{
			try {
				AssetManager am = getResources().getAssets();
				InputStream is = am.open("blackboard.db");
				dbFile.getParentFile().mkdirs();
				dbFile.createNewFile();
				FileOutputStream fos = new FileOutputStream(dbFile);
				byte[] buf = new byte[1024];
				int len;
				while ((len = is.read(buf)) > 0) {
					fos.write(buf, 0, len);
				}	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
    
    // Listener for the submit button
    private class submitListener implements OnClickListener
    {
    	public void onClick(View v) {
    		
    		pd = ProgressDialog.show(MainActivity.this, "Please Wait", "Logging In...");
    		Thread t = new Thread(new loginThread());
    		t.start();
    	}
    }
    private class loginThread implements Runnable
    {

		public void run() {
			//get references to the Edit Boxes
    		EditText userName = ((EditText)findViewById(R.id.txtUserName));
    		EditText password = ((EditText)findViewById(R.id.txtPassword));
    		
    		// Trim the username
    		String user = userName.getText().toString().trim();
    		String pass = password.getText().toString();
    		
    		// Attempt to log in
    		if (ConnChecker.shouldConnect(prefs, ctx))
    		{
    			BlackboardService.logIn(user, pass);
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
    private class msgHandler extends Handler
    {
    	public void handleMessage(Message m)
    	{
    		if (pd!=null)
    		{
    			pd.dismiss();
    			pd = null;
    		}
    		switch (m.what)
    		{
    		case THREAD_COMPLETE:
    			if (BlackboardService.isLoggedIn())
    			{
    				// Display if successful
    				Toast.makeText(MainActivity.this, "Logged In!", Toast.LENGTH_SHORT).show();
    				// Display Courses
    				Intent i = new Intent(MainActivity.this,CourseActivity.class);
    				startActivity(i);
    			}else
    			{
    				Toast.makeText(MainActivity.this, "Login Failed!", Toast.LENGTH_LONG).show();
    			}
    			break;
    		case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(MainActivity.this);
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(MainActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    		}
    	}
    }
}
