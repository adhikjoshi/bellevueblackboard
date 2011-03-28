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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardHelper;
import edu.bellevue.android.blackboard.Course;
import edu.bellevue.android.blackboard.Forum;

public class ForumActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	
	private List<Forum> forums;
	//private String courseId;
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
	    BlackboardHelper.setCourseId(extras.getString("course_id"));
	    friendlyName = extras.getString("name");
	    
	    setTitle(friendlyName + " - Forums");
	    
	    pd = ProgressDialog.show(this, "Please Wait", "Loading Forums...");
	    
	    Thread t = new Thread(new getForumsThread());
	    t.start();
	    
	}
	public void onListItemClick(ListView l, View v, int position, long id)
	{
    	super.onListItemClick(l, v, position, id);
    	if (ConnChecker.shouldConnect(prefs, ctx))
		{
        	Forum c = (Forum)l.getItemAtPosition(position);
        	Intent i = new Intent(this,ThreadActivity.class);
        	i.putExtra("name", c.forumName);
        	i.putExtra("course_id", c.course_id);
        	i.putExtra("conf_id", c.conf_id);
        	i.putExtra("forum_id", c.forum_id);
        	startActivity(i);
		}else
		{
			if (ConnChecker.getConnType(ctx).equals("NoNetwork"))
			{
				Toast.makeText(ForumActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
			}else
			{
				ConnChecker.showUnableToConnect(ForumActivity.this);
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
	
	protected class threadHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			pd.dismiss();
			switch(m.what)
			{
			case THREAD_COMPLETE:
				setListAdapter(new ForumAdapter(ctx, android.R.layout.simple_list_item_1,forums));
				break;
			case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(ForumActivity.this);
    			finish();
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(ForumActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			finish();
    		}
		}
	}
	
	protected class getForumsThread implements Runnable
	{

		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				forums = BlackboardHelper.getForums();
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
    private class ForumAdapter extends ArrayAdapter<Forum> {

        private List<Forum> items;

        public ForumAdapter(Context context, int textViewResourceId, List<Forum> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.forumrow, null);
                }
                Forum o = items.get(position);
                if (o != null) {
                        TextView tt = (TextView) v.findViewById(R.id.toptext);
                        TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText(o.forumName);                            }
                        if(bt != null){
                              bt.setText("Total Posts: "+ o.pCount + " Unread: " + o.uCount);
                        }
                }
                return v;
        }
    }  
}
