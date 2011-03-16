package edu.bellevue.android;

import android.app.Activity;
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
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardHelper;

public class MainActivity extends Activity {
	
	private Handler threadHandler = new msgHandler();
	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private ProgressDialog pd;
	private Context ctx;
	private SharedPreferences prefs;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.btnLogIn).setOnClickListener(new submitListener());
        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        
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
    			BlackboardHelper.logIn(user, pass);
    			threadHandler.sendEmptyMessage(THREAD_COMPLETE);
    		}else
    		{
    			threadHandler.sendEmptyMessage(CONN_NOT_ALLOWED);
    		}
			
		}
    	
    }
    private class msgHandler extends Handler
    {
    	public void handleMessage(Message m)
    	{
    		pd.dismiss();
    		if (m.what == THREAD_COMPLETE)
    		{
    			if (BlackboardHelper.isLoggedIn())
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
    		}else if (m.what == CONN_NOT_ALLOWED)
    		{
    			if (ConnChecker.getConnType(MainActivity.this).equals("NoNetwork"))
    			{
    				Toast.makeText(MainActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			}else
    			{
    				ConnChecker.showUnableToConnect(MainActivity.this);	
    			}
    			
    		}
    	}
    }
}
