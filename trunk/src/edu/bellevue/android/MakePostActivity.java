package edu.bellevue.android;

import edu.bellevue.android.blackboard.BlackboardHelper;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MakePostActivity extends Activity {

	private String confid;
	private String courseid;
	private String forumid;
	private String method;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.messagemaker);
	    
	    
	    EditText bodyText = ((EditText)findViewById(R.id.txtThreadBody));
	    
	    Bundle extras = getIntent().getExtras();

	    method = extras.getString("method");
	    TextView header = ((TextView)findViewById(R.id.txtHeader));
	    ((Button)findViewById(R.id.btnBold)).setOnClickListener(new simpleFormatListener("<b>","</b>"));
	    ((Button)findViewById(R.id.btnItalic)).setOnClickListener(new simpleFormatListener("<i>","</i>"));
	    ((Button)findViewById(R.id.btnUnderline)).setOnClickListener(new simpleFormatListener("<u>","</u>"));
	    ((Button)findViewById(R.id.btnThreadSubmit)).setOnClickListener(new submitListener());
	    ((Button)findViewById(R.id.btnThreadCancel)).setOnClickListener(new cancelListener());
	    // load the necessary values
	    if (method == null)
	    {
	    	//shouldn't ever happen
	    	Toast.makeText(this, "No Method Specified.", Toast.LENGTH_LONG).show();
	    	finish();
	    }else if (method.equals("newthread"))
	    {
	    	header.setText("Create New Thread");
		    confid = extras.getString("confid");
		    courseid = extras.getString("courseid");
		    forumid = extras.getString("forumid");
	    }else if (method.equals("reply"))
	    {
	    	header.setText("Create a Reply");
	    	// finish later
	    }

	}

	class submitListener implements OnClickListener{

		public void onClick(View v) {
			// TODO Auto-generated method stub
			String subject = ((EditText)findViewById(R.id.txtThreadSubject)).getText().toString();
			String body = ((EditText)findViewById(R.id.txtThreadBody)).getText().toString();
			BlackboardHelper.createNewThread(courseid, confid, forumid, subject, body);
			setResult(1);
			finish();
			
		}
		
	}
	class cancelListener implements OnClickListener{

		public void onClick(View v) {
			setResult(0);
			finish();
		}
		
	}
	class simpleFormatListener implements OnClickListener{

		private String tagOpen;
		private String tagClose;
		public simpleFormatListener(String tOpen, String tClose)
		{
			tagOpen = tOpen;
			tagClose = tClose;
		}
		public void onClick(View v) {
			// Find out BODY text box
			
			EditText bodyText = ((EditText)findViewById(R.id.txtThreadBody));
			Editable ed = bodyText.getText();
			int selStart = bodyText.getSelectionStart();
			int selEnd = bodyText.getSelectionEnd();
			if (selEnd == selStart)
			{
				ed.insert(bodyText.getSelectionStart(), tagOpen + tagClose);
				bodyText.setText(ed);
				bodyText.setSelection(selStart+tagOpen.length());	
			}else
			{
				if (selStart > selEnd)
				{
					int tmp = selStart;
					selStart = selEnd;
					selEnd = tmp;
				}
				String selectedText = ed.toString().substring(selStart,selEnd);
				ed.replace(selStart, selEnd, tagOpen + selectedText + tagClose);
				bodyText.setText(ed);
				bodyText.setSelection(selStart + (tagOpen + selectedText + tagClose).length());
			}
			
		}
		
	}
}
