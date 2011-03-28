package edu.bellevue.android;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
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
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardHelper;

public class MessageActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	
	private List<edu.bellevue.android.blackboard.Message> messages;
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
	    friendlyName = extras.getString("name");
	    
	    setTitle(friendlyName + " - Messages");
	    
	    pd = ProgressDialog.show(this, "Please Wait", "Loading Messages...");
	    
	    Thread t = new Thread(new getMessagesThread());
	    t.start();
	    
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
	private List<edu.bellevue.android.blackboard.Message> getAllMessages()
	{
		List<edu.bellevue.android.blackboard.Message> msgs = new ArrayList<edu.bellevue.android.blackboard.Message>();
		Hashtable <String,String> msgIds = BlackboardHelper.getMessageIds();
		Enumeration<String> keyEnum = msgIds.keys();
		while(keyEnum.hasMoreElements())
		{
			String mId = keyEnum.nextElement();
			String tId = msgIds.get(mId);
			BlackboardHelper.setMessageId(mId);
			msgs.add(BlackboardHelper.getMessage());		
		}
		//end debug stuff//
		return msgs;
	}
	protected class threadHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			pd.dismiss();
			switch(m.what)
			{
			case THREAD_COMPLETE:
				setListAdapter(new MessageAdapter(ctx, android.R.layout.simple_list_item_1,messages));
				break;
			case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(MessageActivity.this);
    			finish();
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(MessageActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			finish();
    		}
		}
	}
	
	class getMessagesThread implements Runnable
	{

		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				
				messages = getAllMessages();
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
    private class MessageAdapter extends ArrayAdapter<edu.bellevue.android.blackboard.Message> {

        private List<edu.bellevue.android.blackboard.Message> items;

        public MessageAdapter(Context context, int textViewResourceId, List<edu.bellevue.android.blackboard.Message> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.messagerow, null);
                }
                edu.bellevue.android.blackboard.Message o = (edu.bellevue.android.blackboard.Message)(items.get(position));
                if (o != null) {
                        TextView tt = (TextView) v.findViewById(R.id.toptext);
                        
                        TextView mt = (TextView) v.findViewById(R.id.middletext);
                        TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText(Html.fromHtml(o.getMsgName())); 
                        }
                        if (mt != null) {
                        	mt.setText("By: " + o.getMsgAuthor() + "\nOn: "+Html.fromHtml(o.getMsgDate()));
                        }
                        if(bt != null){
                              bt.setText(Html.fromHtml(o.getBody()));
                        }
                }
                v.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
					// TRY USING THE VIEW, AND ITERATE TO FIND THE CORRECT ONE?
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						// TODO Auto-generated method stub
						int x = getListView().indexOfChild(v); 
						menu.setHeaderTitle("Options...");
						menu.add(ContextMenu.NONE,x,ContextMenu.NONE,"Reply...");
						menu.add(Integer.toString(x));
					}
				});
                return v;
        }
    }    
}
