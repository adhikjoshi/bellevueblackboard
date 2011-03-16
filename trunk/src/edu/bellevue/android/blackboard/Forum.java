package edu.bellevue.android.blackboard;

import java.io.Serializable;

public class Forum implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String forumName;
	public String course_id;
	public String conf_id;
	public String forum_id;
	public String pCount;
	public String uCount;
	
	public Forum(String name, String postCount, String unreadCount, String course, String conf, String forum)
	{
		//CIS337-T201_2113_1: CIS337-T201 Web Scripting (2113-1)
		//EC202-T101_2115_1: EC202-T101 Microeconomics (2115-1) 
		
		forumName = name;
		pCount = postCount;
		uCount = unreadCount;
		course_id = course;
        conf_id = conf;
        forum_id = forum;
	}

	public String toString()
	{
		return forumName + " {" + pCount + "}  {" + uCount +"}";
	}
}