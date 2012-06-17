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
import org.htmlparser.tags.DefinitionList;
import org.htmlparser.tags.DefinitionListBullet;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.Span;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import edu.bellevue.android.MessageActivity;
import edu.bellevue.android.R;
import com.google.android.apps.analytics.*;
/**
 * @author TJ
 *
 */
public class BlackboardService {
	
	// This stuff is needed for the 'service' part of things
	// has nothing to do with blackboard really
	static boolean shouldPerformBackgroundCheck = true;
	private static final String LOGTAG = "BB_SERVICE";
	
	// URLS USED FOR PARSING
	private static final String LOGIN_URL = "https://cyberactive.bellevue.edu/webapps/login/";
	private static final String CS_LOGIN_URL = "https://www.coursesites.com/webapps/login/";
	private static final String COURSES_URL = "https://cyberactive.bellevue.edu/webapps/portal/tab/_2_1/index.jsp";
	private static final String CS_COURSES_URL = "https://www.coursesites.com/webapps/portal/execute/tabs/tabAction?action=refreshAjaxModule&modId=_4_1&tabId=_1_1&tab_tab_group_id=_1_1";
	private static final String DISCUSSION_BOARD_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/conference?action=list_forums&course_id=%s&nav=discussion_board_entry";
	private static final String CS_DISCUSSION_BOARD_URL = "https://www.coursesites.com/webapps/discussionboard/do/conference?toggle_mode=read&action=list_forums&course_id=%s&nav=discussion_board_entry&mode=view";
	private static final String THREADS_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/forum?action=list_threads&forum_id=%s&conf_id=%s&course_id=%s&nav=discussion_board_entry&forum_view=list";
	private static final String CS_THREADS_URL = "https://www.coursesites.com/webapps/discussionboard/do/forum?action=list_threads&forum_id=%s&conf_id=%s&course_id=%s&nav=discussion_board_entry&forum_view=list&numResults=9000";
	private static final String MESSAGES_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=list_messages&forum_id=%s&course_id=%s&nav=discussion_board_entry&conf_id=%s&message_id=%s";
	private static final String CS_MESSAGES_URL = "https://www.coursesites.com/webapps/discussionboard/do/message?action=message_tree&course_id=%s&conf_id=%s&forum_id=%s&message_id=%s&nav=discussion_board_entry&nav=discussion_board_entry&thread_id=%s";
	private static final String CS_MESSAGE_URL = "https://www.coursesites.com/webapps/discussionboard/do/message?action=message_frame&course_id=%s&conf_id=%s&forum_id=%s&nav=db_thread_list_entry&nav=discussion_board_entry&message_id=%s&thread_id=%s";
	private static final String TREE_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/";
	private static final String DISPLAY_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/";
	private static String displayUrl = null;
	
	//variables for Properties
	private static boolean _loggedIn = false;
	private static String user_id = null;
	public static boolean cacheData = false;
	public static boolean offlineDemo = false;
	
	// variables used throughout 
	private static HttpClient client = null;
	private static HttpClient backclient = null;
	private static HttpResponse httpResponse = null;
	private static HttpPost httpPost = null;
	private static NodeList nodeList;
	private static Parser p = new Parser();
	private static SQLiteDatabase db = openDatabase();
	// FILTERS USED FOR PARSING
	private static TagNameFilter tableTagFilter = new TagNameFilter("table");
	private static TagNameFilter anchorTagFilter = new TagNameFilter("a");
	private static TagNameFilter spanTagFilter = new TagNameFilter("span");
    private static GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
    private static Context currentContext = null;
    
    public static void setContext(Context ctx)
    {
    	currentContext = ctx;
    }
    
	// PUBLIC METHODS USED TO PERFORM BLACKBOARD OPERATIONS
	public static boolean logIn(String userName, String password)
	{
		
		if (userName.equals("demo") && password.equals("demo"))
		{
			user_id="demo";
			offlineDemo = true;
			_loggedIn = true;
		}else{
			offlineDemo = false;
		}
		
		if (offlineDemo)
			return true;
		//if (client == null)
		//{
			try{
				client = createHttpClient();
			}catch(Exception e){}
		//}
		return logIn(userName,password,client);
	}
	public static boolean logIn(String userName, String password, HttpClient client)
	{
		if (userName.equals("demo") && password.equals("demo"))
		{
			user_id="demo";
			offlineDemo = true;
			_loggedIn = true;
		}else{
			offlineDemo = false;
		}
		
		if (offlineDemo)
			return true;
		
		shouldPerformBackgroundCheck = false;
		Log.i(LOGTAG,"Loggin in with User: " + userName + " and Pass: xxxxx");
		try{

		httpPost = new HttpPost(CS_LOGIN_URL);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		if (client == null){
			try{
				client = createHttpClient();
			}catch(Exception e){}
		}


		// by parsing out the HTML page and looking at the code
		// we know that the password is base64 encoded twice (once ansii once unicode)
		// and that the password field itself is nulled out.
        nvps.add(new BasicNameValuePair("action", "login"));
        nvps.add(new BasicNameValuePair("user_id",userName));
        //nvps.add(new BasicNameValuePair("encoded_pw",Base64.encodeBytes(password.getBytes(), 0)));
        
        // dont really have a Unicode base64 utility but blackboard seems OK with this
        //nvps.add(new BasicNameValuePair("encoded_pw_unicode",Base64.encodeBytes(password.getBytes(), 0)));
        //nvps.add(new BasicNameValuePair("remote-user", ""));
        nvps.add(new BasicNameValuePair("new_loc", ""));
        //nvps.add(new BasicNameValuePair("auth_type", ""));
        //nvps.add(new BasicNameValuePair("one_time_token", ""));
        nvps.add(new BasicNameValuePair("password", password));
        nvps.add(new BasicNameValuePair("login","Login"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        // try to log in
        httpResponse = client.execute(httpPost);
        
        // check for "redirected" showing that it succeeded.
        String s = convertStreamToString(httpResponse.getEntity().getContent());
        if (s.contains("redirected"))
		{ 
        	Log.i(LOGTAG, "Login Succeeded!");
			_loggedIn = true;
			user_id = userName;
        	
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
		
		tracker.trackEvent("BlackboardService", "LogIn", "LogIn", _loggedIn == true ? 1 : 0 );
		
		return _loggedIn;
	}
	public static boolean isLoggedIn()
	{		
		return _loggedIn;
	}
	public static List<Course> getCourses()
	{
		shouldPerformBackgroundCheck = false;
		List<Course> courses = new ArrayList<Course>();
		if (offlineDemo)
		{
			Course c = new Course("CIS337-T201_2113_1: CIS337-T201 Course 1 (2113-1)");
			courses.add(c);
			c = new Course("CIS337-T201_2113_1: CIS337-T201 Course 2 (2113-1)");
			courses.add(c);
			c = new Course("CIS337-T201_2113_1: CIS337-T201 Course 3 (2113-1)");
			courses.add(c);
			c = new Course("CIS337-T201_2113_1: CIS337-T201 Course 4 (2113-1)");
			courses.add(c);
			return courses;
		}
		try
		{
	        httpPost = new HttpPost(CS_COURSES_URL);
	        Log.i(LOGTAG,"Courses URL: " + CS_COURSES_URL);
	        httpResponse = client.execute(httpPost);
	        String s = convertStreamToString(httpResponse.getEntity().getContent());
	        p.setInputHTML(s);
	
			nodeList = p.extractAllNodesThatMatch(new LinkRegexFilter(" /webapps.+"));
	
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
		tracker.trackEvent("BlackboardService", "GetCourses", "GetCourses", courses.size());
		return courses;
	}
	public static List<Forum> getForums(String course_id)
	{
		shouldPerformBackgroundCheck = false;
		List<Forum> forums = new ArrayList<Forum>();
		if (offlineDemo)
		{
			Forum f = new Forum("Forum 1","20","10","0","0","0");
			forums.add(f);
			f = new Forum("Forum 2","20","10","0","0","0");
			forums.add(f);
			f = new Forum("Forum 3","20","10","0","0","0");
			forums.add(f);
			f = new Forum("Forum 4","20","10","0","0","0");
			forums.add(f);
			return forums;
		}
		try
		{
			Log.i(LOGTAG,"Forums URL: " + String.format(CS_DISCUSSION_BOARD_URL,course_id));
	        httpPost = new HttpPost(String.format(CS_DISCUSSION_BOARD_URL,course_id));
	        httpResponse = client.execute(httpPost);	
	        String s = convertStreamToString(httpResponse.getEntity().getContent());
	        p.setInputHTML(s);
	
			nodeList = p.extractAllNodesThatMatch(tableTagFilter);
			TableTag forumTable = null;
			for (Node n : nodeList.toNodeArray())
			{
				if (((TableTag)n).getAttribute("summary") != null)
				{
					if(((TableTag)n).getAttribute("summary").equals("This is a table showing the attributes of a collection of items."))
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
				nodeList = rows[x].getHeaders()[0].getChildren();
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
				nodeList = rows[x].getHeaders()[0].getChildren();
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
		tracker.trackEvent("BlackboardService", "GetForums", "GetForums", forums.size());
		return forums;
		
	}
	public static List<Thread> getThreads(String course_id, String forum_id, String conf_id)
	{
		shouldPerformBackgroundCheck = false;
		List<Thread> threads = new ArrayList<Thread>();
		if (offlineDemo)
		{
			Thread t = new Thread("Thread 1","01/01/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 2","01/02/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 3","01/03/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 4","01/04/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 5","01/05/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 6","01/05/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 7","01/05/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			t = new Thread("Thread 8","01/05/01","DemoUser","10","0","0","0","0","0");
			threads.add(t);
			return threads;
			
		}
		try
		{			
			Log.i("THREADS", String.format(THREADS_URL,forum_id,conf_id,course_id));
	        httpPost = new HttpPost(String.format(CS_THREADS_URL,forum_id,conf_id,course_id));
	        httpResponse = client.execute(httpPost);	
	        
	        p.setInputHTML(convertStreamToString(httpResponse.getEntity().getContent()));
			
	        nodeList = p.extractAllNodesThatMatch(tableTagFilter);
	        
			TableTag forumTable = null;
			for (Node n : nodeList.toNodeArray())
			{
				if (((TableTag)n).getAttribute("summary") != null)
				{
					if(((TableTag)n).getAttribute("summary").equals("This is a table showing the attributes of a collection of items."))
					{
						forumTable = (TableTag)n;
						break;
					}
				}
			}	        
			org.htmlparser.tags.TableRow[] rows = forumTable.getRows();
	        for (int x = 1; x < rows.length; x++)
			{
	        	String threadDate;
				String threadName;
				String threadAuthor;
				String pCount;
				String uCount;
				
				TableColumn[] cols = rows[x].getColumns();

				//Get Unread Count
				//NodeList lst = cols[5 + (cols.length - 8)].getChildren();
				NodeList lst = cols[5].getChildren();
				CompositeTag myTag;
				try
				{
					myTag = (CompositeTag)(lst.extractAllNodesThatMatch(anchorTagFilter,true).extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
					uCount = myTag.getStringText().trim();
				}catch (Exception e){
					uCount = "0";	
				}
				
				//Get Thread Name
				lst = rows[x].getHeaders()[0].getChildren();
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(anchorTagFilter,true).toNodeArray()[0]);
				threadName = myTag.getStringText().trim();
				
				//Get Thread Date
				lst = cols[2].getChildren();
				try{
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
				threadDate = myTag.getStringText().trim();
				}
				catch(Exception e){threadDate = "Error Getting Date";}
				
				//Get Thread Author
				lst = cols[3].getChildren();
				
				try
				{
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter,true).toNodeArray()[0]);
				threadAuthor = myTag.getStringText().trim();
				}catch(Exception e){threadAuthor = "Error Getting Author";}
				
				//Get Post Count
				lst = cols[6].getChildren();
			
				myTag = (CompositeTag)(lst.extractAllNodesThatMatch(spanTagFilter, true).toNodeArray()[0]);
				
				pCount = myTag.getStringText().trim();
				
				//Get Conf ID and Forum ID
				lst = rows[x].getHeaders()[0].getChildren();
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
		tracker.trackEvent("BlackboardService", "GetThreads", "GetThreads", threads.size());
		return threads;
	}
	
	public static Hashtable<String,String> getMessageIds(String forum_id, String course_id, String conf_id, String thread_id)
	{
		return getMessageIds(forum_id,course_id, conf_id, thread_id,client);
	}
	
	public static Hashtable<String, String> getMessageIds(String forum_id,
			String course_id, String conf_id, String thread_id,
			HttpClient client) {
		shouldPerformBackgroundCheck = false;
		Hashtable<String, String> msgIds = new Hashtable<String, String>(); // MessageID,ThreadID
		if (offlineDemo) {
			msgIds.put("1", "1");
			msgIds.put("2", "2");
			msgIds.put("3", "3");
			msgIds.put("4", "4");
			msgIds.put("5", "5");
			return msgIds;
		}
		// load site that contains all replies, use this to get the IDs we need
		// to get the details.
		try {
			// we need to get the URL for the message tree.
			// course , conf, forum, message, thread
			//private static final String CS_MESSAGES_URL = "https://www.coursesites.com/webapps/discussionboard/do/message?action=message_tree&course_id=%s&conf_id=%s&forum_id=%s&message_id=%s&nav=discussion_board_entry&nav=discussion_board_entry&thread_id=%s";
			String str = String.format(CS_MESSAGES_URL, course_id, conf_id,
					forum_id, thread_id, thread_id);
			httpPost = new HttpPost(str);
			httpResponse = client.execute(httpPost);

			p = new Parser();
			String s = convertStreamToString(httpResponse.getEntity().getContent());
			p.setInputHTML(s);
			// WriteEntityToFile(forumsResponse.getEntity(),
			// "MessagesOut.html");
			
			nodeList = p.extractAllNodesThatMatch(anchorTagFilter);
			
			for (Node n : nodeList.toNodeArray())
			{
				String curHref = ((LinkTag)n).getAttribute("href");
				if (curHref.contains("javascript:display"))
				{
					curHref = curHref.replace("javascript:display(", "");
					curHref = curHref.replace("'", "");
					curHref = curHref.replace(")","");
					String[] values = curHref.split(",");
					msgIds.put(values[1],values[0]);
				}
			}
		}catch(Exception e){}
		shouldPerformBackgroundCheck = true;
		return msgIds;
	}

	public static Message getMessage(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id) {

		shouldPerformBackgroundCheck = false;
		Message m = null;
		if (offlineDemo) {
			m = new Message(
					"Message " + thread_id,
					"01/" + thread_id + "/01",
					"DemoUser",
					"Demo Body<br><a href='https://cyberactive.bellevue.edu/courses/foo'>DemoLink</a>",
					"0", "0", "0", "0", "0");
			return m;
		}
		try {
			TableTag t;
			TableRow[] rows;
			// course conf forum message thread
			String msgUrl = String.format(CS_MESSAGE_URL, course_id,conf_id,forum_id,message_id,thread_id);

			httpPost = new HttpPost(msgUrl);
			httpResponse = client.execute(httpPost);

			p = new Parser();
			String s = convertStreamToString(httpResponse.getEntity().getContent());
			p.setInputHTML(s);
			String author = "";
			nodeList = p.extractAllNodesThatMatch(new TagNameFilter("dl"));
			DefinitionList dl = (DefinitionList) nodeList.toNodeArray()[0];
			try
			{
				Node anchor = dl.getChildren().extractAllNodesThatMatch(anchorTagFilter, true).toNodeArray()[0];
				author = anchor.toPlainTextString();	
			}
			catch(Exception e){
				NodeList nl = dl.getChildren().extractAllNodesThatMatch(new TagNameFilter("dd"),true);
				author = ((DefinitionListBullet) nl.elementAt(0)).getStringText().trim();
			}
			
			int i = 0;
			nodeList = dl.getChildren().extractAllNodesThatMatch(new TagNameFilter("dd"),true);
			String postedDate = nodeList.elementAt(1).getFirstChild().getText().trim();
			p.setInputHTML(s);
			nodeList = p.parse(new TagNameFilter("div")).extractAllNodesThatMatch(new HasAttributeFilter("class", "navButtons"),true);
			nodeList = nodeList.toNodeArray()[0].getChildren();
			String subject = ((Span) nodeList.elementAt(3)).getStringText();
			
			// get actual message
			String bodyinfo = "";
			try
			{
			p.setInputHTML(s);
			nodeList = p.parse(null).extractAllNodesThatMatch(new HasAttributeFilter("class", "vtbegenerated"),true);
			bodyinfo  = ((Div)(nodeList.toNodeArray()[0])).getStringText();
			bodyinfo = "<h5>Body:</h5>" + bodyinfo;
			}
			catch (Exception e)
			{
				bodyinfo = "<h5>Body:</h5>" + "{blank}";
			}
			postedDate = "<i>" + postedDate + "</i>";
			m = new Message(subject, postedDate, author, bodyinfo, course_id,
					conf_id, forum_id, message_id, thread_id);

			if (cacheData) {
				// storeMessage(m);
			}
			shouldPerformBackgroundCheck = true;
			tracker.trackEvent("BlackboardService", "GetMessage", "GetMessage", 1);
			return m;
		} catch (Exception e) {
			tracker.trackEvent("BlackboardService", "GetMessage", "GetMessage", 0);
			return null; 
		}
	}

	public static void addThreadToWatch(Thread t)
	{
		storeThread(t);
		tracker.trackEvent("BlackboardService", "WatchThread", "AddToWatch", 1);
	}
	public static void removeThreadFromWatch(Thread t)
	{
		tracker.trackEvent("BlackboardService", "WatchThread", "RemoveFromWatch", 1);
		removeThread(t);
	}
	public static boolean createNewThread(String course_id, String forum_id, String conf_id, String subject, String body, String attachedFile)
	{		
		if (offlineDemo)
			return true;
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
	public static boolean createReply(String course_id, String forum_id, String conf_id, String thread_id, String message_id, String subject, String body, String attachedFile)
	{
		if (offlineDemo)
			return true;
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
	
	public static boolean downloadAttachment (String url, String savePath){
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
	
	public static SQLiteDatabase openDatabase()
	{
		File dbFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blackboard/database.db3"); 
		return SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
	}
	public static boolean isThreadWatched(Thread t)
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
	public static void doCheck(final Context ctx)
	{
		try {
			backclient = createHttpClient();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		logIn(prefs.getString("username", ""),prefs.getString("password", ""),backclient);
		//logIn(user_id,password);
		java.lang.Thread t = new java.lang.Thread(new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				NotificationManager nm = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
				checkWatchedThreads(ctx , nm, backclient);
				System.gc();
			}
		});
		t.start();
	}
	// PRIVATE HELPER METHODS
	private static void checkWatchedThreads(Context ctx, NotificationManager nm, HttpClient client)
	{		
		Cursor c = db.query("Threads", new String[]{"user_id","thread_data"}, "user_id='"+user_id+"'", null, null, null, null);
		if (c.getCount() > 0)
		{
			c.moveToFirst();
			for (int x = 0; x < c.getCount(); x++)
			{
				byte[] blobData = c.getBlob(c.getColumnIndex("thread_data"));
				Thread t = Thread.makeFromCompressedData(blobData);

				int postCount = getMessageIds(t.forum_id,t.course_id,t.conf_id,t.thread_id, client).size();

				if (postCount > Integer.parseInt(t.pCount))
				{

					// make a notification
					
					Notification n = new Notification(R.drawable.icon,"Discussion Thread Updated",System.currentTimeMillis());
					n.defaults |= Notification.DEFAULT_SOUND;
					n.defaults |= Notification.DEFAULT_VIBRATE;
					n.flags |= Notification.FLAG_AUTO_CANCEL;
					
					CharSequence contentTitle = "Thread Updated";
					CharSequence contentText = Integer.toString(postCount - Integer.parseInt(t.pCount)) + " New Post(s)";
					Intent ni = new Intent(ctx, MessageActivity.class);
					ni.putExtra("name", t.threadName);
					ni.putExtra("fromNotification", true);
					ni.putExtra("conf_id", t.conf_id);
					ni.putExtra("course_id", t.course_id);
					ni.putExtra("forum_id", t.forum_id);
					ni.putExtra("thread_id", t.thread_id);
					ni.putExtra("message_id", t.thread_id);
					PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, ni, 0);
					
					n.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);
					nm.notify((int)System.currentTimeMillis(), n);
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
		System.gc();
	}
	private static void storeMessage(Message m)
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
	private static void storeThread(Thread t)
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
	private static void removeThread(Thread t)
	{
		db.beginTransaction();
			try{
				db.delete("Threads", "thread_id='" + t.thread_id + "'", null);
				db.setTransactionSuccessful();
			}catch(Exception e){}
		db.endTransaction();
	}
	private static Message getMsgFromDb(String courseid, String mId, String tId) {
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
	
	private static HttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
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
	private static String convertStreamToString(InputStream is)
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
