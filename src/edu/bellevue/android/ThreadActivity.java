package edu.bellevue.android;

import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.objects.BlackboardService;

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
	
	/** Called when the activity is first created. */
	@Override
    public void onDestroy()
    {
    	super.onDestroy();
    	
    }
    public void onResume()
    {
    	super.onResume();
    }
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.threadviewlayout);
	    ctx = this.getApplicationContext();
	    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    handler = new threadHandler();
	    
	    Bundle extras = getIntent().getExtras();
	    
	    friendlyName = extras.getString("name");	    
	    setTitle(friendlyName + " - Threads");

	    ((Button)findViewById(R.id.btnNewThread)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent i = new Intent(ThreadActivity.this,MakePostActivity.class);
	    		i.putExtra("method", "newthread");
	    		Bundle extras = getIntent().getExtras();
	    		i.putExtra("course_id", extras.getString("course_id"));
	    		i.putExtra("forum_id", extras.getString("forum_id"));
	    		i.putExtra("conf_id", extras.getString("conf_id"));    		
	    		startActivityForResult(i, 0);
			}
		});
	    
	    pd = ProgressDialog.show(ThreadActivity.this, "Please Wait", "Loading Threads...");
	    
	    Thread t = new Thread(new getThreadsThread());
	    t.start();
	    
	}

	public void onListItemClick(ListView l, View v, int position, long id)
	{
    	super.onListItemClick(l, v, position, id);
    	if (ConnChecker.shouldConnect(prefs, ctx))
		{
        	edu.bellevue.android.blackboard.Thread t = (edu.bellevue.android.blackboard.Thread)threads.get(position);
        	Intent i = new Intent(ThreadActivity.this,MessageActivity.class);
        	i.putExtra("name", t.threadName);
        	i.putExtra("course_id", t.course_id);
        	i.putExtra("forum_id", t.forum_id);
        	i.putExtra("conf_id", t.conf_id);
        	i.putExtra("thread_id", t.thread_id);
        	i.putExtra("message_id", t.thread_id);
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
    	return super.onOptionsItemSelected(mi);
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
				Bundle extras = getIntent().getExtras();
				threads = BlackboardService.getThreads(extras.getString("course_id"),extras.getString("forum_id"),extras.getString("conf_id"));
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
        private Context ctx;
        public ThreadAdapter(Context context, int textViewResourceId, List<edu.bellevue.android.blackboard.Thread> items) {
                super(context, textViewResourceId, items);
                this.items = items;
                ctx = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = null;
                //if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.threadrow, null);
                //}
                v.setId(position);
                edu.bellevue.android.blackboard.Thread o = items.get(position);
                if (o != null) {
                    TextView tt = (TextView) v.findViewById(R.id.toptext);
                    TextView mt = (TextView) v.findViewById(R.id.middletext);
                    TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                    
                    if (tt != null) {
                        tt.setText(" " + o.threadName);
                        if (BlackboardService.isThreadWatched(o))
                        {
                        	tt.setTextColor(Color.RED);

                        }else
                        {
                        	tt.setTextColor(Color.WHITE);
                        }
                        
                    }
                    if (mt != null) {
                    	mt.setText("     By: " + o.threadAuthor + " On: "+o.threadDate);
                    }
                    if(bt != null){
                    	bt.setText("     Total Posts: "+ o.pCount + " Unread: " + o.uCount);
                    }
                    
                    Button b = (Button) v.findViewById(R.id.btnWatch);
                    b.setId(position);
                    if (BlackboardService.isThreadWatched(threads.get(position)))
                    {
                    	b.setText("Unwatch This Thread");
                    }else
                    {
                    	b.setText("Watch This Thread");
                    }
                    b.setOnClickListener(new OnClickListener() {
						
						public void onClick(View v) {
							// TODO Auto-generated method stub
							
							edu.bellevue.android.blackboard.Thread t = threads.get(v.getId());
							if (((Button) v).getText().toString().equals("Watch This Thread"))
					    	{
						    	BlackboardService.addThreadToWatch(t);
						    	((Button)v).setText("Unwatch This Thread");
						    	RelativeLayout rl = (RelativeLayout) v.getParent();
						    	TextView tv = ((TextView)rl.findViewById(R.id.toptext));
						    	//TextView tv = ((TextView)getListView().getChildAt(v.getId()).findViewById(R.id.toptext));
						    	tv.setTextColor(Color.RED);
					    	}else
					    	{
					    		// unwatch thread
					    		BlackboardService.removeThreadFromWatch(t);
					    		((Button)v).setText("Watch This Thread");
					    		RelativeLayout rl = (RelativeLayout) v.getParent();
						    	TextView tv = ((TextView)rl.findViewById(R.id.toptext));
					    		//TextView tv = ((TextView)getListView().getChildAt(v.getId()).findViewById(R.id.toptext));
						    	tv.setTextColor(Color.WHITE);
					    	}
						}
					});
                    v.setOnClickListener(new OnClickListener() {
						
						public void onClick(View v) {
							// TODO Auto-generated method stub
							v.requestFocus();
							onListItemClick(getListView(), v, v.getId(), v.getId());
							
						}
					});
                    
                    return v;
            }
            return v;
        }
    }  
}
