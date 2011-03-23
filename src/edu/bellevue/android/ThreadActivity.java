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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardHelper;
import edu.bellevue.android.blackboard.Forum;

public class ThreadActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	
	private List<edu.bellevue.android.blackboard.Thread> threads;
	private String courseId;
	private String forumId;
	private String confId;
	private String friendlyName;
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
	    
	    Bundle extras = getIntent().getExtras();
	    courseId = extras.getString("course_id");
	    confId = extras.getString("conf_id");
	    forumId = extras.getString("forum_id");
	    friendlyName = extras.getString("name");
	    
	    setTitle(friendlyName + " - Threads");
	    
	    pd = ProgressDialog.show(this, "Please Wait", "Loading Threads...");
	    
	    Thread t = new Thread(new getThreadsThread());
	    t.start();
	    
	}
	
    public boolean onCreateOptionsMenu(Menu m)
    {
    	m.add("New Thread");
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
    	if (mi.getTitle().equals("New Thread"))
    	{
    		Toast.makeText(this, "Not Implemented Yet!", Toast.LENGTH_LONG).show();
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
				setListAdapter(new ThreadAdapter(ctx, android.R.layout.simple_list_item_1,threads));
				break;
			case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(ThreadActivity.this);
    			finish();
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(ThreadActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			finish();
    		}
		}
	}
	
	protected class getThreadsThread implements Runnable
	{
		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				threads = BlackboardHelper.getThreads(forumId,confId,courseId);
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
    private class ThreadAdapter extends ArrayAdapter<edu.bellevue.android.blackboard.Thread> {

        private List<edu.bellevue.android.blackboard.Thread> items;

        public ThreadAdapter(Context context, int textViewResourceId, List<edu.bellevue.android.blackboard.Thread> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.threadrow, null);
                }
                edu.bellevue.android.blackboard.Thread o = items.get(position);
                if (o != null) {
                    TextView tt = (TextView) v.findViewById(R.id.toptext);
                    
                    TextView mt = (TextView) v.findViewById(R.id.middletext);
                    TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                    if (tt != null) {
                          tt.setText(o.threadName); 
                    }
                    if (mt != null) {
                    	mt.setText("By: " + o.threadAuthor + " On: "+o.threadDate);
                    }
                    if(bt != null){
                          bt.setText("Total Posts: "+ o.pCount + " Unread: " + o.uCount);
                    }
            }
            return v;
        }
    }  
}
