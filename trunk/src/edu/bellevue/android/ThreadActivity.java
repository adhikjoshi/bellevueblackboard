package edu.bellevue.android;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardService;

public class ThreadActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	
	private List<edu.bellevue.android.blackboard.Thread> threads;
	private String friendlyName;
	private SharedPreferences prefs;
	private Context ctx;
	private Handler handler;
	private ProgressDialog pd;
	
	protected BlackboardService mBoundService;
	
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        mBoundService = ((BlackboardService.BlackboardServiceBinder)service).getService();
	        Bundle extras = getIntent().getExtras();
	        mBoundService.setCourseId(extras.getString("course_id"));
	        mBoundService.setConfId(extras.getString("conf_id"));
	        mBoundService.setForumId(extras.getString("forum_id"));
		    friendlyName = extras.getString("name");
		    
		    setTitle(friendlyName + " - Threads");
		    
		    pd = ProgressDialog.show(ThreadActivity.this, "Please Wait", "Loading Threads...");
		    
		    Thread t = new Thread(new getThreadsThread());
		    t.start();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        mBoundService = null;
	    }
	};
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.listview);
	    ctx = this.getApplicationContext();
	    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    handler = new threadHandler();
	    
	    bindService(new Intent(ThreadActivity.this,BlackboardService.class),mConnection,Context.BIND_AUTO_CREATE);
	    
	}
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		
    	super.onListItemClick(l, v, position, id);
    	if (ConnChecker.shouldConnect(prefs, ctx))
		{
        	edu.bellevue.android.blackboard.Thread t = (edu.bellevue.android.blackboard.Thread)threads.get(position);
        	mBoundService.setThreadId(t.message_id);
        	mBoundService.setMessageId(t.message_id);
        	Intent i = new Intent(ThreadActivity.this,MessageActivity.class);
        	i.putExtra("name", t.threadName);
        	startActivity(i);
		}else
		{
			if (ConnChecker.getConnType(ctx).equals("NoNetwork"))
			{
				Toast.makeText(ThreadActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
			}else
			{
				ConnChecker.showUnableToConnect(ThreadActivity.this);
			}
		}		
	}
    public boolean onCreateOptionsMenu(Menu m)
    {
    	m.add("New Thread");
    	m.add("Settings");
    	return super.onCreateOptionsMenu(m);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if (resultCode == 1)
    	{
    		pd = ProgressDialog.show(this, "Please Wait", "Refreshing Threads...");
    	    
    	    Thread t = new Thread(new getThreadsThread());
    	    t.start();	
    	}
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
    		Intent i = new Intent(ThreadActivity.this,MakePostActivity.class);
    		i.putExtra("method", "newthread");
    		startActivityForResult(i, 0);
    		
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
				threads = mBoundService.getThreads();
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
