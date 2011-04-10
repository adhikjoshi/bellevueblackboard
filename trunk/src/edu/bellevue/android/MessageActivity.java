package edu.bellevue.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
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
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardService;
import edu.bellevue.android.blackboard.MessageComparator;

public class MessageActivity extends ListActivity {

	private static final int THREAD_COMPLETE = 1;
	private static final int CONN_NOT_ALLOWED = 2;
	private static final int CONN_NOT_POSSIBLE = 3;
	private static final int DOWNLOAD_COMPLETE = 4;
	private static final int MSG_UPDATE_STATUS = 5;
	private static final int MSG_INCREMENT_COUNT = 6;
	
	private List<edu.bellevue.android.blackboard.Message> messages;
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
		    friendlyName = extras.getString("name");		    
		    setTitle(friendlyName + " - Messages");    
		    pd = new ProgressDialog(MessageActivity.this);
		    pd.setTitle("Please Wait");
		    pd.setMessage("Getting Message List...");
		    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    pd.show();
		    Thread t = new Thread(new getMessagesThread());
		    t.start();
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
	
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
    	if (resultCode == 1)
    	{
    		pd = ProgressDialog.show(this, "Please Wait", "Refreshing Messages...");
    	    
    	    Thread t = new Thread(new getMessagesThread());
    	    t.start();	
    	}
    }
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.listview);
	    ctx = this.getApplicationContext();
	    prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    handler = new threadHandler();
	    
	    bindService(new Intent(MessageActivity.this,BlackboardService.class),mConnection,Context.BIND_AUTO_CREATE);
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
    
    public boolean onContextItemSelected(MenuItem aItem)
    {    	
    	edu.bellevue.android.blackboard.Message m = messages.get(aItem.getItemId());
    	
		Intent i = new Intent(MessageActivity.this,MakePostActivity.class);
		Bundle extras = getIntent().getExtras();
		extras.putString("method", "reply");
		extras.putString("message_id", m.getMessageId());
		extras.putString("thread_id", m.getThreadId());
		i.putExtras(extras);
		startActivityForResult(i, 1);
		return true;
    }
    
	private List<edu.bellevue.android.blackboard.Message> getAllMessages()
	{
		List<edu.bellevue.android.blackboard.Message> msgs = new ArrayList<edu.bellevue.android.blackboard.Message>();
		// if we were started from a notification
		Bundle extras = getIntent().getExtras();
		Hashtable <String,String> msgIds = mBoundService.getMessageIds(extras.getString("forum_id"),extras.getString("course_id"),extras.getString("conf_id"),extras.getString("thread_id"));
		Message m = new Message();
		m.what = MSG_UPDATE_STATUS;
		m.arg1 = msgIds.size();
		handler.sendMessage(m);
		Enumeration<String> keyEnum = msgIds.keys();
		while(keyEnum.hasMoreElements())
		{
			String mId = keyEnum.nextElement();
			String tId = msgIds.get(mId);
			msgs.add(mBoundService.getMessage(extras.getString("course_id"),extras.getString("forum_id"),extras.getString("conf_id"),tId,mId));
			handler.sendEmptyMessage(MSG_INCREMENT_COUNT);
		}
		//end debug stuff//
		return msgs;
	}
	protected class threadHandler extends Handler
	{
		public void handleMessage(Message m)
		{
			if (m.what != MSG_UPDATE_STATUS && m.what != MSG_INCREMENT_COUNT)
				pd.dismiss();
			switch(m.what)
			{
			case THREAD_COMPLETE:
				setListAdapter(new MessageAdapter(ctx, android.R.layout.simple_list_item_1,messages));
				break;
			case DOWNLOAD_COMPLETE:
				Toast.makeText(MessageActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
				Toast.makeText(MessageActivity.this, "Downloaded to /sdcard/Downloads/BU/", Toast.LENGTH_SHORT).show();
				break;
			case CONN_NOT_ALLOWED:
    			ConnChecker.showUnableToConnect(MessageActivity.this);
    			finish();
    			break;
    		case CONN_NOT_POSSIBLE:
    			Toast.makeText(MessageActivity.this, "No Active Network Found", Toast.LENGTH_SHORT).show();
    			finish();
    		case MSG_UPDATE_STATUS:
    			pd.dismiss();
    			pd = new ProgressDialog(MessageActivity.this);
    			pd.setTitle("Please Wait");
    			pd.setMessage("Loading Messages");
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pd.setMax(m.arg1);
				pd.show();
    		case MSG_INCREMENT_COUNT:
    			pd.setProgress(pd.getProgress()+1);
    			pd.setSecondaryProgress(pd.getSecondaryProgress()+1);
    		}
		}
	}
	
	class getMessagesThread implements Runnable
	{

		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				
				messages = getAllMessages();
				Collections.sort(messages, new MessageComparator());
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
                        WebView bt = (WebView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText(Html.fromHtml(o.getMsgName())); 
                        }
                        if (mt != null) {
                        	mt.setText("By: " + o.getMsgAuthor() + "\nOn: "+Html.fromHtml(o.getMsgDate()));
                        }
                        if(bt != null){
                        	bt.loadDataWithBaseURL("https://cyberactive.bellevue.edu", "<html><body><font color='white'>" + o.getBody() + "</font></body></html>", "text/html", "UTF-8", null);
                        	bt.setBackgroundColor(0);
                        	bt.setWebViewClient(new myWebClient());
                            //bt.setText(Html.fromHtml(o.getBody()));
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
					}
				});
                return v;
        }
    }   
    class downloadThread implements Runnable{
    	private String url;
    	public downloadThread(String url){
    		this.url = url;
    	}
		public void run() {
			if (ConnChecker.shouldConnect(prefs, ctx))
			{
				mBoundService.downloadAttachment(url, "/sdcard/Downloads/BU"+url.substring(url.lastIndexOf("/")));
				handler.sendEmptyMessage(DOWNLOAD_COMPLETE);
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
    class myWebClient extends WebViewClient{
    	public boolean shouldOverrideUrlLoading(WebView wv,String url)
    	{
    		if (url.startsWith("https://cyberactive.bellevue.edu/courses"))
    		{
    			pd.setTitle("Please Wait");
    			pd.setMessage("Downloading...");
    			pd.show();
    			Thread t = new Thread(new downloadThread(url));
    			t.start();
    		}
    		
    		return true;
    	}
    }
    
}
