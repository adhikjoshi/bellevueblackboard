package edu.bellevue.android.blackboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableTag;
import org.htmlparser.util.NodeList;

import android.os.Environment;
import android.util.Log;

/* After some benchmark testing with Traceview it seems HtmlParser lib is better than HtmlCleaner lib
 * on average the HtmlParser version finishes 200ms faster and uses about 15k less memory than HtmlCleaner.
 */

public class BlackboardHelper {
	private static final String LOGTAG = "BB_HELPER";
	
	// URLS USED FOR PARSING
	private static final String LOGIN_URL = "https://cyberactive.bellevue.edu/webapps/login/";
	private static final String COURSES_URL = "https://cyberactive.bellevue.edu/webapps/portal/tab/_2_1/index.jsp";
	private static final String DISCUSSION_BOARD_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/conference?action=list_forums&course_id=%s&nav=discussion_board_entry";
	private static final String THREADS_URL = "https://cyberactive.bellevue.edu/webapps/discussionboard/do/forum?action=list_threads&forum_id=%s&conf_id=%s&course_id=%s&nav=discussion_board_entry&forum_view=list";
	private static HttpClient client = null;
	private static boolean _loggedIn = false;
	private static HttpResponse httpResponse = null;
	private static HttpPost httpPost = null;
	private static NodeList nodeList;
	private static Parser p = new Parser();
	
	// FILTERS USED FOR PARSING
	private static TagNameFilter tableTagFilter = new TagNameFilter("table");
	private static TagNameFilter anchorTagFilter = new TagNameFilter("a");
	private static TagNameFilter spanTagFilter = new TagNameFilter("span");
	
	// PUBLIC METHODS USED TO PERFORM BLACKBOARD OPERATIONS
	
	public static boolean logIn(String userName, String password)
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
	public static boolean isLoggedIn()
	{
		return _loggedIn;
	}
	public static List<Course> getCourses()
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
	public static List<Forum> getForums(String courseId)
	{
		List<Forum> forums = new ArrayList<Forum>();
		try
		{
			Log.i(LOGTAG,"Forums URL: " + String.format(DISCUSSION_BOARD_URL,courseId));
	        httpPost = new HttpPost(String.format(DISCUSSION_BOARD_URL,courseId));
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
	
	public static List<Thread> getThreads(String formid, String confid, String courseid)
	{
		List<Thread> threads = new ArrayList<Thread>();
		try
		{			
			Log.i("THREADS", String.format(THREADS_URL,formid,confid,courseid));
	        httpPost = new HttpPost(String.format(THREADS_URL,formid,confid,courseid));
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
					e.printStackTrace();
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
	
	public static boolean createNewThread(String courseid, String confid, String forumid, String subject, String body)
	{		
		// first we need to get this 'nonce' security thing (Session?)
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=create&do=create&type=thread&forum_id="+forumid+"&course_id="+courseid+"&nav=discussion_board_entry&conf_id="+confid);
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

		
		httpPost = new HttpPost("https://cyberactive.bellevue.edu/webapps/discussionboard/do/message?action=save&pageLink=list_messages&nav=discussion_board_entry&course_id="+courseid+"&nav=discussion_board_entry");
		
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
		multi.addPart("conf_id",new StringBody(confid));
		multi.addPart("do",new StringBody("create"));
		multi.addPart("course_id",new StringBody(courseid));
		multi.addPart("type",new StringBody("thread"));
		multi.addPart("forum_id",new StringBody(forumid));
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
	
	
	
	
	
	// PRIVATE HELPER METHODS

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
