package edu.bellevue.android.blackboard;

import java.io.Serializable;

public class Thread implements Serializable{
	/**
	 * 
	 */
	public static final long serialVersionUID = 1L;
	public String threadName;
	public String threadDate;
	public String threadAuthor;
	
	public String course_id;
	public String conf_id;
	public String forum_id;
	public String message_id;
	public String pCount;
	public String uCount;
	
	public Thread(String name, String date, String author, String postCount, String unreadCount, String course, String conf, String forum, String messageid)
	{
		threadName = name.replace("&nbsp;","");
		threadDate = date;
		threadAuthor = author;
		pCount = postCount;
		uCount = unreadCount;
		course_id = course;
        conf_id = conf;
        forum_id = forum;
        message_id = messageid;
	}


}