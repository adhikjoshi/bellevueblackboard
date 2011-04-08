package edu.bellevue.android;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardService;

public class MainActivity extends Activity {
	
	private Handler handler = new msgHandler();
	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	private ProgressDialog pd;
	private Context ctx;
	private SharedPreferences prefs;
	protected BlackboardService mBoundService;
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mBoundService = ((BlackboardService.BlackboardServiceBinder)service).getService();
	        if (mBoundService.isLoggedIn())
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

	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	        
	    }
	};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	unbindService(mConnection);
    	
    }
    public void onResume()
    {
    	super.onResume();
    	bindService(new Intent(MainActivity.this,BlackboardService.class),mConnection,Context.BIND_AUTO_CREATE);
    }
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        startService(new Intent(MainActivity.this,edu.bellevue.android.blackboard.BlackboardService.class));
        
        bindService(new Intent(MainActivity.this,BlackboardService.class),mConnection,Context.BIND_AUTO_CREATE);
       
                
        
        setContentView(R.layout.main);
        findViewById(R.id.btnLogIn).setOnClickListener(new submitListener());
        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor e = prefs.edit();
        if (prefs.getBoolean("firstRun", true))
        {
        	e.putBoolean("wifi",true);
        	e.putBoolean("mobile", true);
        }
        
        e.putBoolean("firstRun", false);
        e.commit();
        // AutoLogin stuff        
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
    			mBoundService.logIn(user, pass);
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
    			if (mBoundService.isLoggedIn())
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
