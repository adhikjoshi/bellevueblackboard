/**
 * 
 */
package edu.bellevue.android.blackboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.LinkRegexFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.bellevue.android.MessageActivity;
import edu.bellevue.android.R;

/**
 * @author TJ
 *
 */
public class BlackboardService extends Service {
	
	// This stuff is needed for the 'service' part of things
	// has nothing to do with blackboard really
	boolean shouldPerformBackgroundCheck = true;
	Notification n = null;
	NotificationManager nm;
	private final IBinder mBinder = new BlackboardServiceBinder();
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
    public class BlackboardServiceBinder extends Binder {
        public BlackboardService getService() {
            return BlackboardService.this;
        }
    }
    
    public void onDestroy()
    {
    	db.close();
    }
	public void onCreate()
	{
		/*nm =  (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		// init keep-alive (every 10 minutes)
		long delay = 1 * 60 * 1000;
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				if (shouldPerformBackgroundCheck == false)
				{
					return;
				}
				// Do something to keep our session valid
				// here is where we will eventually do the thread 'watching'
				logIn(user_id, password);
				
				int numWeeks = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(BlackboardService.this).getString("cachelength", "-1"));
				if (numWeeks > 0)
				{
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_YEAR, (-14) * numWeeks);
				
					Long expirationDate = cal.getTime().getTime();
				
					db.beginTransaction();
						db.delete("Messages", "storage_date <= '" + Long.toString(expirationDate)+"'", null);
						db.setTransactionSuccessful();
					db.endTransaction();
				}
				
				checkWatchedThreads();
			}
		}, 60 * 1000, delay);
		*/
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

	private final String LOGTAG = "BB_SERVICE";
	
	// URLS USED FOR PARSING
	private final String LOGIN_URL = "https://cyberactive.bellevue.edu/webapps/login/";
	private final String COURSES_URL = "https://cyberactive.bellevue.edu/webapps/portal/tab/_2_1/index.jsp";
	private final String DISCUSSION_BOARD_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/conference?action=list_forums&course_id=%s&nav=discussion_board_entry";
	private final String THREADS_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/forum?action=list_threads&forum_id=%s&conf_id=%s&course_id=%s&nav=discussion_board_entry&forum_view=list";
	private final String MESSAGES_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=list_messages&forum_id=%s&course_id=%s&nav=discussion_board_entry&conf_id=%s&message_id=%s";
	private final String TREE_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/";
	private final String DISPLAY_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/";
	private String displayUrl = null;
	
	//variables for Properties
	private boolean _loggedIn = false;
	private String user_id = null;
	private String password = null;
	
	// variables used throughout 
	private HttpClient client = null;
	private HttpResponse httpResponse = null;
	private HttpPost httpPost = null;
	private NodeList nodeList;
	private Parser p = new Parser();
	private SQLiteDatabase db = openDatabase();
	// FILTERS USED FOR PARSING
	private TagNameFilter tableTagFilter = new TagNameFilter("table");
	private TagNameFilter anchorTagFilter = new TagNameFilter("a");
	private TagNameFilter spanTagFilter = new TagNameFilter("span");
	private TagNameFilter scriptTagFilter = new TagNameFilter("script");

	// PUBLIC METHODS USED TO PERFORM BLACKBOARD OPERATIONS
	public boolean logIn(String userName, String password)
	{
		shouldPerformBackgroundCheck = false;
		Log.i(LOGTAG,"Loggin in with User: " + userName + " and Pass: xxxxx");
		try{

		httpPost = new HttpPost(LOGIN_URL);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		if (client != null){
			client.getConnectionManager().shutdown();
			client = null;
		}

		client = createHttpClient();

		// by parsing out the HTML page and looking at the code
		// we know that the password is base64 encoded twice (once ansii once unicode)
		// and that the password field itself is nulled out.
        nvps.add(new BasicNameValuePair("action", "login"));
        nvps.add(new BasicNameValuePair("user_id",userName));
        nvps.add(new BasicNameValuePair("encoded_pw",Base64.encodeBytes(password.getBytes(), 0)));
        
        // dont really have a Unicode base64 utility but blackboard seems OK with this
        nvps.add(new BasicNameValuePair("encoded_pw_unicode",Base64.encodeBytes(password.getBytes(), 0)));
        nvps.add(new BasicNameValuePair("remote-user", ""));
        nvps.add(new BasicNameValuePair("new_loc", ""));
        nvps.add(new BasicNameValuePair("auth_type", ""));
        nvps.add(new BasicNameValuePair("one_time_token", ""));
        nvps.add(new BasicNameValuePair("pass", ""));

        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        // try to log in
        httpResponse = client.execute(httpPost);
        
        // check for "redirected" showing that it succeeded.

        if (convertStreamToString(httpResponse.getEntity().getContent()).contains("redirected"))
		{ 
        	Log.i(LOGTAG, "Login Succeeded!");
			_loggedIn = true;
			this.user_id = userName;
			this.password = password;
        	
		}else
		{
			Log.i(LOGTAG, "Login Failed!");
        	_loggedIn = false;
		}
        
		}catch(Exception e)
		{
			Log.i(LOGTAG, "Exception while Logging In");
			e.printStackTrace();
			_loggedIn = false;
		}
		shouldPerformBackgroundCheck = true;
		return _loggedIn;
	}
	public boolean isLoggedIn()
	{
		return _loggedIn;
	}
	public List<Course> getCourses()
	{
		shouldPerformBackgroundCheck = false;
		List<Course> courses = new ArrayList<Course>();
		try
		{
	        httpPost = new HttpPost(COURSES_URL);
	        Log.i(LOGTAG,"Courses URL: " + COURSES_URL);
	        httpResponse = client.execute(httpPost);
	
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
	
			nodeList = p.extractAllNodesThatMatch(new LinkRegexFilter(" \\/webapps.*"));
	
			for (Node n : nodeList.toNodeArray())
			{
				Course c = new Course(((LinkTag)n).getLinkText());
				
				String courseId = URLDecoder.decode(((LinkTag)n).extractLink());
				courseId = courseId.substring(courseId.indexOf("e&id=")+5);
				courseId = courseId.substring(0,courseId.indexOf("&"));
				c.courseId = courseId;
				Log.i(LOGTAG , "Found Course With ID: " + c.courseId);
				courses.add(c);
			}
		}catch(Exception e){e.printStackTrace(); courses = null;}
		finally
		{
			System.gc();
		}
		shouldPerformBackgroundCheck = true;
		return courses;
	}
	public List<Forum> getForums(String course_id)
	{
		shouldPerformBackgroundCheck = false;
		List<Forum> forums = new ArrayList<Forum>();
		try
		{
			Log.i(LOGTAG,"Forums URL: " + String.format(DISCUSSION_BOARD_URL,course_id));
	        httpPost = new HttpPost(String.format(DISCUSSION_BOARD_URL,course_id));
	        httpResponse = client.execute(httpPost);	
	        
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
	
			nodeList = p.extractAllNodesThatMatch(tableTagFilter);
			TableTag forumTable = null;
			for (Node n : nodeList.toNodeArray())
			{
				if (((TableTag)n).getAttribute("summary") != null)
				{
					if(((TableTag)n).getAttribute("summary").equals("(Data Table)"))
					{
						forumTable = (TableTag)n;
						break;
					}
				}
			}
			
			org.htmlparser.tags.TableRow[] rows = forumTable.getRows();
			
			for (int x = 1; x < rows.length; x++)
			{
				String forumName;
				String pCount;
				String uCount;
				
				TableColumn[] cols = rows[x].getColumns();
				
				//Get Forum Name
				nodeList = cols[0].getChildren();
				CompositeTag myTag = (CompositeTag)(nodeList.extractAllNodesThatMatch(anchorTagFilter,true).toNodeArray()[0]);
				forumName = myTag.getStringText().trim();
				
				//Get Post Count
				nodeList = cols[1].getChildren();
				try{
					myTag = (CompositeTag)(nodeList.extractAllNodesThatMatch(spanTagFilter, true).toNodeArray()[1]);
				}catch(Exception e){
					myTag = (CompositeTag)(nodeList.extractAllNodesThatMatch(spanTagFilter, true).toNodeArray()[0]);
				}
				pCount = myTag.getStringText().trim();
				
				//Get Unread Count
				nodeList = cols[2].getChildren();
				try
				{
					myTag = (CompositeTag)(nodeList.extractAllNodesThatMatch(anchorTagFilter,true).extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
					uCount = myTag.getStringText().trim();
				}catch (Exception e){uCount = "0";}
				
				
				//Get Conf ID and Forum ID
				nodeList = cols[0].getChildren();
				myTag = (CompositeTag)(nodeList.extractAllNodesThatMatch(anchorTagFilter,true).toNodeArray()[0]);
				String theURL = URLDecoder.decode(((LinkTag)myTag).extractLink());
				
				String confid = theURL.substring(theURL.indexOf("conf_id=")+8);
				confid = confid.substring(0,confid.indexOf("&"));
				
				String forumid = theURL.substring(theURL.indexOf("forum_id=")+9);
				
				String courseid = theURL.substring(theURL.indexOf("course_id=")+10);
				courseid = courseid.substring(0,courseid.indexOf("&"));
				
				Forum f = new Forum(forumName,pCount,uCount,courseid,confid,forumid);
				forums.add(f);
			}			
		}catch(Exception e){e.printStackTrace(); forums = null;}
		shouldPerformBackgroundCheck = true;
		return forums;
		
	}
	public List<Thread> getThreads(String course_id, String forum_id, String conf_id)
	{
		shouldPerformBackgroundCheck = false;
		List<Thread> threads = new ArrayList<Thread>();
		try
		{			
			Log.i("THREADS", String.format(THREADS_URL,forum_id,conf_id,course_id));
	        httpPost = new HttpPost(String.format(THREADS_URL,forum_id,conf_id,course_id));
	        httpResponse = client.execute(httpPost);	
	        
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
			
	        nodeList = p.extractAllNodesThatMatch(tableTagFilter);
	        
			TableTag forumTable = null;
			for (Node n : nodeList.toNodeArray())
			{
				if (((TableTag)n).getAttribute("summary") != null)
				{
					if(((TableTag)n).getAttribute("summary").equals("(Data Table)"))
					{
						forumTable = (TableTag)n;
						break;
					}
				}
			}	        
			org.htmlparser.tags.TableRow[] rows = forumTable.getRows();
	        for (int x = 2; x < rows.length; x++)
			{
	        	String threadDate;
				String threadName;
				String threadAuthor;
				String pCount;
				String uCount;
				
				TableColumn[] cols = rows[x].getColumns();

				//Get Unread Count
				NodeList lst = cols[6 + (cols.length - 8)].getChildren();
				CompositeTag myTag;
				try
				{
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(anchorTagFilter,true).extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
					uCount = myTag.getStringText().trim();
				}catch (Exception e){
					
					uCount = "0";
					
				}
				
				//Get Thread Name
				lst = cols[3].getChildren();
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(anchorTagFilter,true).toNodeArray()[0]);
				threadName = myTag.getStringText().trim();
				
				//Get Thread Date
				lst = cols[2].getChildren();
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
				if (!uCount.equals("0"))
				{
					myTag = (CompositeTag)myTag.getChildren().extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0];
				}
				threadDate = myTag.getStringText().trim();
				
				//Get Thread Author
				lst = cols[4].getChildren();
				if (uCount.equals("0"))
				{
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
				}else
				{
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
					myTag = (CompositeTag)myTag.getChildren().extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0];
				}
				threadAuthor = myTag.getStringText().trim();
				
				//Get Post Count
				lst = cols[7].getChildren();
				try{
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter, true).toNodeArray()[1]);
				}catch(Exception e){
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter, true).toNodeArray()[0]);
				}
				pCount = myTag.getStringText().trim();
				
				//Get Conf ID and Forum ID
				lst = cols[3].getChildren();
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(anchorTagFilter,true).toNodeArray()[0]);
				String theURL = URLDecoder.decode(((LinkTag)myTag).extractLink());
				
				String confid = theURL.substring(theURL.indexOf("conf_id=")+8);
				confid = confid.substring(0,confid.indexOf("&"));
				
				String forumid = theURL.substring(theURL.indexOf("forum_id=")+9);
				forumid = forumid.substring(0,forumid.indexOf("&"));
				
				String courseid = theURL.substring(theURL.indexOf("course_id=")+10);
				courseid = courseid.substring(0,courseid.indexOf("&"));
				
				String messageid = theURL.substring(theURL.indexOf("message_id=")+11);
				
				Thread t = new Thread(threadName,threadDate,threadAuthor,pCount,uCount,courseid,confid,forumid,messageid);
				
				threads.add(t);
			}			
	        
		}catch(Exception e){e.printStackTrace();}
		shouldPerformBackgroundCheck = true;
		return threads;
	}	
	public Hashtable<String,String> getMessageIds(String forum_id, String course_id, String conf_id, String thread_id)
	{
		shouldPerformBackgroundCheck = false;
		Hashtable<String, String>msgIds = new Hashtable<String, String>(); // MessageID,ThreadID
		
		//load site that contains all replies, use this to get the IDs we need to get the details.
		try
		{
			// we need to get the URL for the message tree.
			String str = String.format(MESSAGES_URL,forum_id,course_id,conf_id,thread_id);
	        httpPost = new HttpPost(str);
	        httpResponse = client.execute(httpPost);	
	        
	        p = new Parser();	        
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
	        //WriteEntityToFile(forumsResponse.getEntity(), "MessagesOut.html");
	        nodeList = p.parse(scriptTagFilter);
	        String treeUrl= null;
	        //String displayUrl = null;
	        for (Node t:nodeList.toNodeArray())
	        {
	        	String scriptCode = ((ScriptTag)t).getScriptCode();
	        	if (scriptCode.contains("treeUrl"))
	        	{
	        		treeUrl = scriptCode.substring(scriptCode.indexOf("treeUrl = ")+11);
	        		treeUrl = treeUrl.substring(0,treeUrl.indexOf(";")-1);
	        		treeUrl = TREE_URL + treeUrl;
	        		
	        		displayUrl = scriptCode.substring(scriptCode.indexOf("displayUrl = ")+14);
	        		displayUrl = displayUrl.substring(0,displayUrl.indexOf(";")-1);
	        		displayUrl = DISPLAY_URL + displayUrl;
	        		break;
	        	}
	        }
	        
	        // now we need to get the form that contains all the messages:
	        httpPost = new HttpPost(treeUrl);
	        httpResponse = client.execute(httpPost);	
	        
	        p = new Parser();	        
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
	        
			nodeList = p.parse(null).extractAllNodesThatMatch(new HasAttributeFilter("name", "messageForm"),true);
			nodeList =((FormTag)nodeList.elementAt(0)).getChildren().extractAllNodesThatMatch(tableTagFilter);
			TableTag t = (TableTag)(nodeList.elementAt(0));
			TableRow[] rows = t.getRows();
			for (TableRow r : rows)
			{
				TableColumn[] cols = r.getColumns();
				TableColumn correctCol = cols[3];
				nodeList = correctCol.getChildren().extractAllNodesThatMatch(anchorTagFilter,true);
				String href = ((LinkTag)(nodeList.elementAt(1))).extractLink();
				href = href.substring(href.indexOf("(") + 1);
				href = href.substring(0,href.lastIndexOf(","));
				href = href.replace("'","");
				msgIds.put(href.substring(href.indexOf(",")+1),href.substring(0,href.indexOf(",")));
			}
		}catch(Exception e){}
		shouldPerformBackgroundCheck = true;
		return msgIds;
	}
	public Message getMessage(String course_id, String forum_id, String conf_id, String thread_id, String message_id)
	{
		shouldPerformBackgroundCheck = false;
		Message m = null;
		m = getMsgFromDb(course_id, message_id, thread_id);
		if (m != null)
		{
			return m;
		}
		try{
			TableTag t;
			TableRow[] rows;
	
			String msgUrl = displayUrl + "&message_id="+message_id+"&thread_id="+thread_id;
	
			httpPost = new HttpPost(msgUrl);
			httpResponse = client.execute(httpPost);
			
			p = new Parser();
			p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
			
			nodeList = p.parse(null).extractAllNodesThatMatch(new HasAttributeFilter("name", "messageForm"),true);
			nodeList =((FormTag)nodeList.elementAt(0)).getChildren().extractAllNodesThatMatch(tableTagFilter);
			
			t = (TableTag)(nodeList.elementAt(0));
			rows = t.getRows();
			Log.i("foo",msgUrl);
			
			// ROW 0 -> Subject Line.
			TableColumn[] cols = rows[0].getColumns();
			String subject = cols[0].getStringText().trim().replace("<strong>","").replace("</strong>", "");
			subject = subject.replace("Subject: ", "");
			subject = subject.replace("&nbsp;","");
			subject = "<b>" + subject + "</b>";
			// Get Author
			cols = rows[1].getColumns();
			String author = null;
			try{
				author =  ((LinkTag)((cols[0].getChildren().extractAllNodesThatMatch(anchorTagFilter,true)).elementAt(0))).getStringText();
			}catch (Exception e)
			{
				String selfAuthor = ((ParagraphTag)((cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("p"),true)).elementAt(0))).getStringText();
				selfAuthor = selfAuthor.substring(selfAuthor.indexOf("Author:") + 7);
				selfAuthor = selfAuthor.substring(0,selfAuthor.indexOf("<br>"));
				selfAuthor = selfAuthor.replace("\"", "");
				author = selfAuthor.replace("</strong>", "").trim();
			}
			
			// Get Posted Date
			String postedDate = ((ParagraphTag)((cols[0].getChildren().extractAllNodesThatMatch(new TagNameFilter("p"),true)).elementAt(0))).getStringText();
			postedDate = postedDate.substring(postedDate.indexOf("Posted date:") + 12);
			postedDate = postedDate.substring(0,postedDate.indexOf("<br>"));
			postedDate = postedDate.replace("</strong>", "").trim();
			
			// get actual message
			nodeList = t.getChildren().extractAllNodesThatMatch(tableTagFilter, true);
			t = (TableTag)nodeList.elementAt(1);
			String bodyinfo = null;
			if (thread_id.equals(message_id))
			{
				bodyinfo = t.getRow(0).getColumns()[0].getStringText();
			}else
			{
				bodyinfo = t.getRow(1).getColumns()[0].getStringText();
			}
			bodyinfo = "<h5>Body:</h5>"+bodyinfo;
			postedDate = "<i>" + postedDate + "</i>";
			m = new Message(subject, postedDate, author, bodyinfo, course_id, conf_id, forum_id, message_id, thread_id);
			
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cachedata", false))
			{
				storeMessage(m);
			}
			shouldPerformBackgroundCheck = true;
			return m;
		}catch(Exception e){return null;}
	}
	public void addThreadToWatch(Thread t)
	{
		storeThread(t);
	}
	public void removeThreadFromWatch(Thread t)
	{
		removeThread(t);
	}
	public boolean createNewThread(String course_id, String forum_id, String conf_id, String subject, String body, String attachedFile)
	{		
		shouldPerformBackgroundCheck = false;
		// first we need to get this 'nonce' security thing (Session?)
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=create&do=create&type=thread&forum_id="+forum_id+"&course_id="+course_id+"&nav=discussion_board_entry&conf_id="+conf_id);
		try {
			httpResponse = client.execute(httpPost);
			p = new Parser();
			p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
			nodeList = p.parse(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
				
		nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name","blackboard.platform.security.NonceUtil.nonce"), true);
		String nonceString = ((InputTag)nodeList.elementAt(0)).getAttribute("value");

		
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=save&pageLink=list_messages&nav=discussion_board_entry&course_id="+course_id+"&nav=discussion_board_entry");
		
		MultipartEntity multi = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		try{
		multi.addPart("blackboard.platform.security.NonceUtil.nonce",new StringBody(nonceString));
		multi.addPart("do",new StringBody("create"));
		multi.addPart("strHandle",new StringBody("db_thread_list_entry"));
		multi.addPart("title",new StringBody(subject));
		multi.addPart("description.type",new StringBody("H"));
		multi.addPart("textbox_prefix",new StringBody("description."));
		multi.addPart("description.text",new StringBody(body));
		multi.addPart("submit.x",new StringBody("27"));
		multi.addPart("submit.y",new StringBody("4"));
		multi.addPart("nav",new StringBody("discussion_board_entry"));
		multi.addPart("conf_id",new StringBody(conf_id));
		multi.addPart("do",new StringBody("create"));
		multi.addPart("course_id",new StringBody(course_id));
		multi.addPart("type",new StringBody("thread"));
		multi.addPart("forum_id",new StringBody(forum_id));
		if (attachedFile != null)
		{
			multi.addPart("attachmentFile",new FileBody(new File(attachedFile)));
		}
		}catch(Exception e){}
		
		httpPost.setEntity(multi);
		try {
			httpResponse = client.execute(httpPost);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shouldPerformBackgroundCheck = true;
		return true;
	}
	public boolean createReply(String course_id, String forum_id, String conf_id, String thread_id, String message_id, String subject, String body, String attachedFile)
	{
		shouldPerformBackgroundCheck = false;
		// first we need to get this 'nonce' security thing (Session)
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=create&do=create&type=thread&forum_id="+forum_id+"&course_id="+course_id+"&nav=discussion_board_entry&conf_id="+conf_id);
		try {
			httpResponse = client.execute(httpPost);
			p = new Parser();
			p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
			nodeList = p.parse(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
				
		nodeList = nodeList.extractAllNodesThatMatch(new HasAttributeFilter("name","blackboard.platform.security.NonceUtil.nonce"), true);
		String nonceString = ((InputTag)nodeList.elementAt(0)).getAttribute("value");

		
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=save&pageLink=list_messages&nav=discussion_board_entry&course_id="+course_id+"&nav=discussion_board_entry");
		
		MultipartEntity multi = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		try{
		multi.addPart("blackboard.platform.security.NonceUtil.nonce",new StringBody(nonceString));
		multi.addPart("do",new StringBody("reply"));
		multi.addPart("inWindow", new StringBody("false"));
		multi.addPart("strHandle",new StringBody("db_thread_list_entry"));
		multi.addPart("title",new StringBody(subject));
		multi.addPart("description.type",new StringBody("H"));
		multi.addPart("textbox_prefix",new StringBody("description."));
		multi.addPart("description.text",new StringBody(body));
		multi.addPart("submit.x",new StringBody("27"));
		multi.addPart("submit.y",new StringBody("4"));
		multi.addPart("nav",new StringBody("discussion_board_entry"));
		multi.addPart("conf_id",new StringBody(conf_id));
		multi.addPart("do",new StringBody("reply"));
		multi.addPart("course_id",new StringBody(course_id));
		multi.addPart("type",new StringBody("message"));
		multi.addPart("forum_id",new StringBody(forum_id));
		multi.addPart("message_id",new StringBody(message_id));
		multi.addPart("thread_id",new StringBody(thread_id));
		multi.addPart("showParent",new StringBody("false"));
		if (attachedFile != null)
		{
			multi.addPart("attachmentFile",new FileBody(new File(attachedFile)));
		}
		}catch(Exception e){}
		
		

		httpPost.setEntity(multi);
		try {
			httpResponse = client.execute(httpPost);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shouldPerformBackgroundCheck = true;
		return true;
	}
	
	public boolean downloadAttachment (String url, String savePath){
		shouldPerformBackgroundCheck = false;
		try
		{
			httpPost = new HttpPost(url);
	        httpResponse = client.execute(httpPost);
	        InputStream is = httpResponse.getEntity().getContent();
	        File f = new File(savePath);
	        f.mkdirs();
	        f.delete();
	        
	        FileOutputStream fos = new FileOutputStream(f);
	        
	        byte buf[]=new byte[1024];
	        int len;
	        while((len=is.read(buf))>0)
	        {
	        	fos.write(buf,0,len);
	        }
	        fos.close();
	        is.close();
	        shouldPerformBackgroundCheck = true;
	        return true;
		}catch (Exception e){
			shouldPerformBackgroundCheck = true;
			return false;
		}
		
	}
	
	// Database Methods
	
	public SQLiteDatabase openDatabase()
	{
		File dbFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blackboard/database.db3"); 
		return SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
	}
	public boolean isThreadWatched(Thread t)
	{
		Cursor c = db.query("Threads", new String[]{"thread_id"}, "thread_id='"+t.thread_id+"'", null, null, null, null);
		if (c.getCount() > 0)
		{
			c.close();
			return true;
		}else{
			c.close();
			return false;
		}
	}
	public void doCheck()
	{
		//Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
		//v.vibrate(2000);
		logIn(user_id,password);
		java.lang.Thread t = new java.lang.Thread(new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				
				checkWatchedThreads();
			}
		});
		t.start();
	}
	// PRIVATE HELPER METHODS
	private void checkWatchedThreads()
	{		
		NotificationManager nMan = (NotificationManager)this.getSystemService(NOTIFICATION_SERVICE);
		Cursor c = db.query("Threads", new String[]{"user_id","thread_data"}, "user_id='"+user_id+"'", null, null, null, null);
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			for (int x = 0; x < c.getCount(); x++)
			{
				byte[] blobData = c.getBlob(c.getColumnIndex("thread_data"));
				Thread t = Thread.makeFromCompressedData(blobData);

				int postCount = getMessageIds(t.forum_id,t.course_id,t.conf_id,t.thread_id).size();
				// changed to == for testing
				if (postCount > Integer.parseInt(t.pCount))
				{
					// make a notification
					n = new Notification(R.drawable.icon,"Discussion Thread Updated",System.currentTimeMillis());
					n.defaults |= Notification.DEFAULT_SOUND;
					n.defaults |= Notification.DEFAULT_VIBRATE;
					n.flags |= Notification.FLAG_AUTO_CANCEL;
					
					Context context = getApplicationContext();
					CharSequence contentTitle = "Thread Updated";
					CharSequence contentText = Integer.toString(postCount - Integer.parseInt(t.pCount)) + " New Post(s)";
					Intent ni = new Intent(BlackboardService.this, MessageActivity.class);
					ni.putExtra("name", t.threadName);
					ni.putExtra("fromNotification", true);
					ni.putExtra("conf_id", t.conf_id);
					ni.putExtra("course_id", t.course_id);
					ni.putExtra("forum_id", t.forum_id);
					ni.putExtra("thread_id", t.thread_id);
					ni.putExtra("message_id", t.thread_id);
					PendingIntent contentIntent = PendingIntent.getActivity(BlackboardService.this, 0, ni, 0);
					
					n.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
					nMan.notify((int)System.currentTimeMillis(), n);
					// update our stored thread
					ContentValues cv = new ContentValues();
					t.pCount = Integer.toString(postCount);
					cv.put("last_count", postCount);
					cv.put("thread_data", t.compressForStorage());
					db.beginTransaction();
						db.update("Threads", cv, "thread_id='"+t.thread_id+"'", null);
						db.setTransactionSuccessful();
					db.endTransaction();
				}
				c.moveToNext();
			}
		}
	}
	private void storeMessage(Message m)
	{		
		ContentValues cv = new ContentValues();
		Calendar cal = Calendar.getInstance();
		cv.put("storage_date",cal.getTime().getTime());
		cv.put("course_id",m.getCourseId());
		cv.put("message_id",m.getMessageId());
		cv.put("thread_id", m.getThreadId());
		cv.put("message_data", m.compressForStorage());
		try
		{
			db.beginTransaction();
				db.insert("Messages",null,cv);
				db.setTransactionSuccessful();
			db.endTransaction();
		}catch(Exception e){ e.printStackTrace(); db.endTransaction();}
		finally{}
	}
	private void storeThread(Thread t)
	{
		ContentValues cv = new ContentValues();
		
		cv.put("thread_id", t.thread_id);
		cv.put("user_id",user_id);
		cv.put("last_count",t.pCount);
		cv.put("thread_data", t.compressForStorage());
		try
		{
			db.beginTransaction();
				db.insert("Threads",null,cv);
				db.setTransactionSuccessful();
			db.endTransaction();
		}catch(Exception e){ e.printStackTrace(); db.endTransaction();}
		finally{}	
	}
	private void removeThread(Thread t)
	{
		db.beginTransaction();
			try{
				db.delete("Threads", "thread_id='" + t.thread_id + "'", null);
				db.setTransactionSuccessful();
			}catch(Exception e){}
		db.endTransaction();
	}
	private Message getMsgFromDb(String courseid, String mId, String tId) {
		// TODO Auto-generated method stub
		Cursor c = db.query("Messages", new String[]{"course_id","message_id","thread_id","message_data"}, "course_id='"+courseid+"' AND message_id='"+mId+"' AND thread_id='"+tId+"'", null, null, null, null);
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			byte[] blobData = c.getBlob(c.getColumnIndex("message_data"));
			c.close();
			return Message.makeFromCompressedData(blobData);
		}else{
			c.close();
			return null;
		}
	}
	
	private HttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
    {
		// This function will create the HttpClient we need to use for blackboard
		// this uses our MySSLSocketFactory to prevent cert checking
		
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", new MySSLSocketFactory(null), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        DefaultHttpClient d = new DefaultHttpClient(conMgr, params);
        
        // Blackboard refuses to allow mobile agents, so we spoof it to Firefox :P
        d.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.13 (KHTML, like Gecko) Chrome/9.0.597.107 Safari/534.13");
        return d;
    }
	private String convertStreamToString(InputStream is)
    throws IOException {
		// NOT MY CODE!
		// This function effectively converts the HTTP response into a string
		/*
		 * To convert the InputStream to String we use the
		 * Reader.read(char[] buffer) method. We iterate until the
		 * Reader return -1 which means there's no more data to
		 * read. We use the StringWriter class to produce the string.
		 */
		if (is != null) {
		    Writer writer = new StringWriter();
		
		    char[] buffer = new char[1024];
		    try {
		        Reader reader = new BufferedReader(
		                new InputStreamReader(is, "UTF-8"));
		        int n;
		        while ((n = reader.read(buffer)) != -1) {
		            writer.write(buffer, 0, n);
		        }
		    } finally {
		        is.close();
		    }
		    return writer.toString();
		} else {        
		    return "";
		}
    }
}
