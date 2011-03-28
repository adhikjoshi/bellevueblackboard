package edu.bellevue.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.h3r3t1c.filechooser.FileChooser;

//import edu.bellevue.android.ForumActivity.ForumAdapter;
import edu.bellevue.android.blackboard.BlackboardHelper;

public class MakePostActivity extends Activity {

	private String confid;
	private String courseid;
	private String forumid;
	private String method;
	private String attachedFile = null;
	private Handler handler = null;
	private ProgressDialog pd = null;
	/** Called when the activity is first created. */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode ==1)
		{
			attachedFile = data.getStringExtra("fileName");
			((TextView)findViewById(R.id.txtAttachedFile)).setText("Attached: " + attachedFile);
		}
	}
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.messagemaker);
	    handler = new MakePostHandler();
	    EditText bodyText = ((EditText)findViewById(R.id.txtThreadBody));
	    
	    Bundle extras = getIntent().getExtras();

	    method = extras.getString("method");
	    TextView header = ((TextView)findViewById(R.id.txtHeader));
	    ((Button)findViewById(R.id.btnBold)).setOnClickListener(new simpleFormatListener("<b>","</b>"));
	    ((Button)findViewById(R.id.btnItalic)).setOnClickListener(new simpleFormatListener("<i>","</i>"));
	    ((Button)findViewById(R.id.btnUnderline)).setOnClickListener(new simpleFormatListener("<u>","</u>"));
	    ((Button)findViewById(R.id.btnList)).setOnClickListener(new simpleFormatListener("<ul>\n", "\n</ul>"));
	    ((Button)findViewById(R.id.btnListItem)).setOnClickListener(new simpleFormatListener("<li>", "</li>"));
	    ((Button)findViewById(R.id.btnAttachFile)).setOnClickListener(new attachFileListener());
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
	class MakePostHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			setResult(1);
			pd.dismiss();
			finish();
		}
	}
	class MakePostThread implements Runnable{
		private String method;
		private String subject;
		private String body;
		private String attachedFile;
		
		public MakePostThread(String method, String subject, String body, String attachedFile)
		{
			this.method = method;
			this.subject = subject;
			this.body = body;
			this.attachedFile = attachedFile;
		}
		public void run() {
			// TODO Auto-generated method stub
			if (method.equals("createthread"))
			{
				BlackboardHelper.createNewThread(courseid, confid, forumid, subject, body, attachedFile);
				handler.sendEmptyMessage(0);
			}
		}
		
	}
	class submitListener implements OnClickListener{

		public void onClick(View v) {
			// TODO Auto-generated method stub
			String subject = ((EditText)findViewById(R.id.txtThreadSubject)).getText().toString();
			String body = ((EditText)findViewById(R.id.txtThreadBody)).getText().toString();
			pd = new ProgressDialog(MakePostActivity.this);
			pd.setMessage("Submitting post to BlackBoard");
			pd.setTitle("Please Wait...");
			pd.show();
			Thread t = new Thread(new MakePostThread("createthread", subject, body, attachedFile));
			t.start();
		}
		
	}
	class cancelListener implements OnClickListener{

		public void onClick(View v) {
			setResult(0);
			finish();
		}
		
	}
	class attachFileListener implements OnClickListener{

		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			Intent i = new Intent(MakePostActivity.this,FileChooser.class);
			startActivityForResult(i, 0);
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
