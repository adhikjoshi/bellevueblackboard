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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * @author TJ
 *
 */
public class BlackboardService extends Service {
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
	public void onCreate()
	{
		// init keep-alive (every 10 minutes)
		long delay = 10 * 60 * 1000;
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				// Do something to keep our session valid
				getCourses();
			}
		}, 0, delay);
	}
	
	public void onDestroy()
	{
		Toast.makeText(this, "onDestroy()", Toast.LENGTH_SHORT).show();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
		Toast.makeText(this, "onStartCommand()", Toast.LENGTH_SHORT).show();
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
	//Properties used
	private boolean _loggedIn = false;
	
	private String course_id = null;
	private String forum_id = null;
	private String conf_id = null;
	private String thread_id = null;
	private String message_id = null;
	
	
	// variables used throughout 
	private HttpClient client = null;
	private HttpResponse httpResponse = null;
	private HttpPost httpPost = null;
	private NodeList nodeList;
	private Parser p = new Parser();
	
	// FILTERS USED FOR PARSING
	private TagNameFilter tableTagFilter = new TagNameFilter("table");
	private TagNameFilter anchorTagFilter = new TagNameFilter("a");
	private TagNameFilter spanTagFilter = new TagNameFilter("span");
	private TagNameFilter scriptTagFilter = new TagNameFilter("script");
	
	// SETTER METHODS
	public  void setCourseId(String courseId){
		course_id = courseId;
		forum_id = null;
		conf_id = null;
		thread_id = null;
		message_id = null;
	}
	public void setForumId(String forumId){
		forum_id = forumId;
		thread_id = null;
		message_id = null;
	}
	public void setConfId(String confId){
		conf_id = confId;
		thread_id = null;
		message_id = null;
	}
	public void setThreadId(String threadId){
		thread_id = threadId;
		message_id = null;
	}
	public void setMessageId(String messageId){
		message_id = messageId;
	}

	// PUBLIC METHODS USED TO PERFORM BLACKBOARD OPERATIONS
	public boolean logIn(String userName, String password)
	{
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
		return _loggedIn;
	}
	public boolean isLoggedIn()
	{
		return _loggedIn;
	}
	public List<Course> getCourses()
	{
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
		return courses;
	}
	public List<Forum> getForums()
	{
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
				
				String conf_id = theURL.substring(theURL.indexOf("conf_id=")+8);
				conf_id = conf_id.substring(0,conf_id.indexOf("&"));
				
				String forum_id = theURL.substring(theURL.indexOf("forum_id=")+9);
				
				String course_id = theURL.substring(theURL.indexOf("course_id=")+10);
				course_id = course_id.substring(0,course_id.indexOf("&"));
				
				Forum f = new Forum(forumName,pCount,uCount,course_id,conf_id,forum_id);
				forums.add(f);
			}			
		}catch(Exception e){e.printStackTrace(); forums = null;}
		
		return forums;
		
	}
	public List<Thread> getThreads()
	{
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
				
				String conf_id = theURL.substring(theURL.indexOf("conf_id=")+8);
				conf_id = conf_id.substring(0,conf_id.indexOf("&"));
				
				String forum_id = theURL.substring(theURL.indexOf("forum_id=")+9);
				forum_id = forum_id.substring(0,forum_id.indexOf("&"));
				
				String course_id = theURL.substring(theURL.indexOf("course_id=")+10);
				course_id = course_id.substring(0,course_id.indexOf("&"));
				
				String message_id = theURL.substring(theURL.indexOf("message_id=")+11);
				
				Thread t = new Thread(threadName,threadDate,threadAuthor,pCount,uCount,course_id,conf_id,forum_id,message_id);
				
				threads.add(t);
			}			
	        
		}catch(Exception e){e.printStackTrace();}
		return threads;
	}	
	public Hashtable<String,String> getMessageIds()
	{
		
		Hashtable<String, String>msgIds = new Hashtable<String, String>(); // MessageID,ThreadID
		
		//load site that contains all replies, use this to get the IDs we need to get the details.
		try
		{
			// we need to get the URL for the message tree.
			String str = String.format(MESSAGES_URL,forum_id,course_id,conf_id,message_id);
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
		return msgIds;
	}
	public Message getMessage()
	{
		try{
		TableTag t;
		TableRow[] rows;
		Message m;

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
		return m;
		}catch(Exception e){return null;}
	}

	
	public boolean createNewThread(String subject, String body, String attachedFile)
	{		
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
		return true;
	}
	public boolean downloadAttachment (String url, String savePath){
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
	        return true;
		}catch (Exception e){return false;}
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
