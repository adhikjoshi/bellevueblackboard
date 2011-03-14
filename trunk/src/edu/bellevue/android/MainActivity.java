package edu.bellevue.android;

import edu.bellevue.android.blackboard.BlackboardHelper;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViewById(R.id.btnLogIn).setOnClickListener(new submitListener());
    }
    
    // Listener for the submit button
    private class submitListener implements OnClickListener
    {
    	public void onClick(View v) {
    		// get references to the Edit Boxes
    		EditText userName = ((EditText)findViewById(R.id.txtUserName));
    		EditText password = ((EditText)findViewById(R.id.txtPassword));
    		
    		// Attempt to log in
    		BlackboardHelper.logIn(userName.getText().toString(), password.getText().toString());
    		
    		if (BlackboardHelper.isLoggedIn())
    		{
    			// Display if successful
    			Toast.makeText(MainActivity.this, "Logged In!", Toast.LENGTH_LONG).show();
    		}else
    		{
    			Toast.makeText(MainActivity.this, "Login Failed!", Toast.LENGTH_LONG).show();
    		}
    	}
    }
}
