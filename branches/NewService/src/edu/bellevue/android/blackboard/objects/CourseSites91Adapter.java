package edu.bellevue.android.blackboard.objects;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import edu.bellevue.android.blackboard.Course;
import edu.bellevue.android.blackboard.Forum;
import edu.bellevue.android.blackboard.Message;
import edu.bellevue.android.blackboard.Thread;

public class CourseSites91Adapter implements BlackboardAdapter {

	private boolean _initialized = false;
	
	private boolean _loggedIn = false;
	private Hashtable<String, String> cookies = new Hashtable<String, String>();

	// URLs used throughout
	private static final String LOGIN_URL = "https://www.coursesites.com/webapps/login/";
	private static final String COURSES_URL = "https://www.coursesites.com/webapps/portal/execute/tabs/tabAction?action=refreshAjaxModule&modId=_4_1&tabId=_1_1&tab_tab_group_id=_1_1";
	private static final String DISCUSSION_BOARD_URL = "https://www.coursesites.com/webapps/discussionboard/do/conference?toggle_mode=read&action=list_forums&course_id=%s&nav=discussion_board_entry&mode=view";
	private static final String THREADS_URL = "https://www.coursesites.com/webapps/discussionboard/do/forum?action=list_threads&forum_id=%s&conf_id=%s&course_id=%s&nav=discussion_board_entry&forum_view=list&numResults=9000";
	private static final String MESSAGES_URL = "https://www.coursesites.com/webapps/discussionboard/do/message?action=message_tree&course_id=%s&conf_id=%s&forum_id=%s&message_id=%s&nav=discussion_board_entry&nav=discussion_board_entry&thread_id=%s";
	private static final String MESSAGE_URL = "https://www.coursesites.com/webapps/discussionboard/do/message?action=message_frame&course_id=%s&conf_id=%s&forum_id=%s&nav=db_thread_list_entry&nav=discussion_board_entry&message_id=%s&thread_id=%s";

	
	public void initializeAdapter() {
		// TODO Auto-generated method stub
		_loggedIn = false;
		System.setProperty("http.keepAlive", "false");
		_initialized = true;
	}

	
	public boolean isInitialized() {
		// TODO Auto-generated method stub
		return _initialized;
	}

	
	public boolean logIn(String userName, String password) {
		// TODO Auto-generated method stub
		Hashtable<String, String> params = new Hashtable<String, String>();
		cookies = new Hashtable<String, String>();
		params.put("action", "login");
		params.put("user_id", userName);
		params.put("new_loc", "");
		params.put("password", password);
		params.put("login", "Login");
		
		String result = executePost(LOGIN_URL, params);
		_loggedIn = result.contains("redirected");

		return _loggedIn;
	}

	
	public boolean isLoggedIn() {
		// TODO Auto-generated method stub
		return _loggedIn;
	}

	
	public List<Course> getCourses() {
		// TODO Auto-generated method stub
		ArrayList<Course> courses = new ArrayList<Course>();
		String response = executePost(COURSES_URL, null);
		response = response.replace("<![CDATA[", "");
		response = response.replace("]]>", "");
		Document doc = Jsoup.parse(response,"",Parser.xmlParser());
		Elements a = doc.select("a");
		for (int x = 0; x < a.size(); x++)
		{
			String name = a.get(x).text();
			String href = URLDecoder.decode(a.get(x).attr("href"));
			String courseId = getUrlAttribute(href, "&id");//href.substring(href.indexOf("e&id=")+5);
			//courseId = courseId.substring(0,courseId.indexOf("&"));
			
			Course c = new Course(name);
			c.courseId = courseId;
			courses.add(c);
		}
		return courses;
	}

	
	public List<Forum> getForums(String course_id) {
		// TODO Auto-generated method stub
		ArrayList<Forum> forums = new ArrayList<Forum>();
		String response = executePost(String.format(DISCUSSION_BOARD_URL,course_id), null);
		Document doc = Jsoup.parse(response);
		Elements rows = doc.select("table").select("tr[id^=listContainer_row:]");
		for (Element e : rows)
		{
			Element anchor = e.select("a").first();
			String href = URLDecoder.decode(anchor.attr("href"));	
			String forum_id = getUrlAttribute(href, "forum_id");
			String conf_id = getUrlAttribute(href,"conf_id");
			String forum_name = anchor.text();
			String unreadCount = e.select("span.unread-count").text();
			String totalCount = e.select("span.total-count").text();
			Forum f = new Forum(forum_name,totalCount,unreadCount,course_id,conf_id,forum_id);
			forums.add(f);
		}
		return forums;
	}

	
	public List<Thread> getThreads(String course_id, String forum_id,
			String conf_id) {
		ArrayList<Thread> threads = new ArrayList<Thread>();
		String name, date, author, postCount, unreadCount;
		
		String ourURL = String.format(THREADS_URL,forum_id,conf_id,course_id);
		String response = executePost(String.format(THREADS_URL,forum_id,conf_id,course_id), null);
		Document doc = Jsoup.parse(response);
		Elements rows = doc.select("table").select("tr[id^=listContainer_row:]");
		for (Element e : rows)
		{
			// Get Thread Name
			Elements el = e.select("a[href^=message?action=list_message");
			name = el.first().text();
			
			// Get Thread Date
			Element dataRow = e.select("td").get(2);
			if (dataRow.children().size() > 0)
			{
				date = dataRow.select("span").first().text();
			}else
			{
				date = dataRow.text();
			}
			
			// Get Thread Author
			dataRow = e.select("td").get(3);
			
			if (dataRow.children().size() > 0)
			{
				author = dataRow.select("span").first().text();
			}else
			{
				author = dataRow.text();
			}
			
			unreadCount = e.select("span.unread-count").text();
			postCount = e.select("span.total-count").text();
			
			// Get Thread ID
			String thread_id = getUrlAttribute(URLDecoder.decode(el.attr("href")),"message_id") ;
			
			
			Thread t = new Thread(name, date, author, postCount, unreadCount, course_id, conf_id, forum_id, thread_id);
			threads.add(t);
		}
		// TODO Auto-generated method stub
		return threads;
	}

	
	public Hashtable<String, String> getMessageIds(String forum_id,
			String course_id, String conf_id, String thread_id) {
		// TODO Auto-generated method stub
		Hashtable<String, String> msgIDs = new Hashtable<String, String>();
		
		String response = executePost(String.format(MESSAGES_URL, course_id, conf_id,
				forum_id, thread_id, thread_id), null);
		
		Document d = Jsoup.parse(response);
		Elements anchors = d.select("a[href^=javascript:display]");
		for (Element e : anchors)
		{
			String href = e.attr("href");
			href = href.substring(href.indexOf("(") + 1,href.indexOf(")"));
			href = href.replace("'", "");
			String[] pieces = href.split(",");
			msgIDs.put(pieces[1], pieces[0]);
		}
		
		return msgIDs;
	}

	
	public Message getMessage(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id) {
		// TODO Auto-generated method stub
		String name,date,author,body;
		
		String response = executePost(String.format(MESSAGE_URL, course_id,conf_id,forum_id,message_id,thread_id), null);
		Document doc = Jsoup.parse(response);
		
		name = doc.select("div.navButtons").select("span").get(1).text();
		Elements divs = doc.select("div[class^=dbThreadInfo]");
		Element ddTag = divs.select("dd").first();
		if (ddTag.children().size() > 0)
		{
			author = ddTag.select("a").first().text();
		}else
		{
			author = ddTag.text();
		}
		
		ddTag = divs.select("dd").get(1);
		date = ddTag.text();
		
		body = doc.select("div.vtbegenerated").last().text();
		body = body.replace("Â", "");
		return new Message(name, date, author, body, course_id, conf_id, forum_id, message_id, thread_id);
		
	}

	
	public void addThreadToWatch(Thread t) {
		// TODO Auto-generated method stub

	}

	
	public void removeThreadFromWatch(Thread t) {
		// TODO Auto-generated method stub

	}

	
	public boolean createNewThread(String course_id, String forum_id,
			String conf_id, String subject, String body, String attachedFile) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean createReply(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id,
			String subject, String body, String attachedFile) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean downloadAttachment(String url, String savePath) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean isThreadWatched(Thread t) {
		// TODO Auto-generated method stub
		return false;
	}

	// Helper Methods

	private String executePost(String url, Hashtable<String, String> params) {
		// TODO Auto-generated method stub
		//url = "https://requestb.in/r0uku0r0";
		StringBuffer response = new StringBuffer();

		try {
			HttpsURLConnection conn = createHttpsUrlConnection(url);
			if (params != null)
				conn.setRequestMethod("POST");
			else
				conn.setRequestMethod("GET");
			
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			String queryString = buildQueryString(params);
			//conn.setRequestProperty("Content-Length",
					//Integer.toString(queryString.length()));
			conn.setFixedLengthStreamingMode(queryString.getBytes().length);
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Language", "en-US");
			String cookieString = buildCookieString(cookies);
			if (cookieString.length() > 0)
				conn.setRequestProperty("Cookie", buildCookieString(cookies));
			// Send Request
			OutputStream os = conn.getOutputStream();
			DataOutputStream wr = new DataOutputStream(os);
			wr.writeBytes(queryString);
			wr.flush();
			wr.close();
			// Get Response
			InputStream is = conn.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			int returnCode = conn.getResponseCode();
			if (conn.getErrorStream() != null)
			{
			int errStreamLen = conn.getErrorStream().available();
			errStreamLen++;
			}
			rd.close();
			setCookies(conn);
			conn.disconnect();
			return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "ERROR OCCURED DURING POST";
	}

	private void setCookies(HttpsURLConnection conn) {
		// TODO Auto-generated method stub

		for (int i = 0;; i++) {
			String headerName = conn.getHeaderFieldKey(i);
			String headerValue = conn.getHeaderField(i);

			if (headerName == null && headerValue == null) {
				// No more headers
				break;
			}
			if ("Set-Cookie".equalsIgnoreCase(headerName)) {
				// Parse cookie
				String[] fields = headerValue.split(";\\s*");

				String cookieValue = fields[0];
				boolean secure = false;

				// Parse each field
				for (int j = 1; j < fields.length; j++) {
					if ("secure".equalsIgnoreCase(fields[j])) {
						secure = true;
					}
				}
				
				if (secure)
				{
					String[] pieces = cookieValue.split("=");
					String cookieName = pieces[0];
					String value = pieces[1];
					
					if (cookies.containsKey(cookieName))
					{
						cookies.remove(cookieName);
					}
					cookies.put(cookieName, value);
				}
			}

		}
	}

	private HttpsURLConnection createHttpsUrlConnection(String url) {
		HttpsURLConnection conn = null;

		try {
			URL destURL = new URL(url);
			conn = (HttpsURLConnection) destURL.openConnection();
			conn.setDefaultUseCaches(false);
			conn.setUseCaches(false);
			conn.setHostnameVerifier(new AllowAllHostNameVerifier());
			conn.setRequestProperty("User-Agent","Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.13 (KHTML, like Gecko) Chrome/9.0.597.107 Safari/534.13");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}

	private String buildQueryString(Hashtable<String, String> params) {
		StringBuilder sb = new StringBuilder();
		if (params == null) return "";
		Enumeration<String> enumerator = params.keys();
		while (enumerator.hasMoreElements()) {
			String key = enumerator.nextElement();
			String value = params.get(key);
			try {
				sb.append(String.format("&%s=%s",
						URLEncoder.encode(key, "UTF-8"),
						URLEncoder.encode(value, "UTF-8")));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (sb.length() > 0)
			return sb.deleteCharAt(0).toString();
		else
			return "";
	}

	private String buildCookieString(Hashtable<String, String> cookies) {
		StringBuilder sb = new StringBuilder();

		Enumeration<String> enumerator = cookies.keys();
		while (enumerator.hasMoreElements()) {
			String key = enumerator.nextElement();
			String value = cookies.get(key);
			try {
				sb.append(String.format("%s=%s; ",
						URLEncoder.encode(key, "UTF-8"),
						URLEncoder.encode(value, "UTF-8")));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (sb.length() > 0)
		{
			sb.deleteCharAt(sb.lastIndexOf(";"));
			return sb.toString().trim();
		}
		else
		{
			return "";
		}
	}
	
	private String getUrlAttribute(String url, String attributeName)
	{
		String value = url.substring(url.indexOf(attributeName + "=") + attributeName.length() + 1);
		try
		{
		value = value.substring(0,value.indexOf("&"));
		}catch(Exception e)
		{}
		return value;
	}

}
