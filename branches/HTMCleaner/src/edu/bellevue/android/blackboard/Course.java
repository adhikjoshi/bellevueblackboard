package edu.bellevue.android.blackboard;

import java.io.Serializable;

public class Course implements Serializable{

	private static final long serialVersionUID = 1L;
	public String fullName;
	public String friendlyName;
	public String courseId;
	
	public Course(String courseName)
	{
		//CIS337-T201_2113_1: CIS337-T201 Web Scripting (2113-1)
		//EC202-T101_2115_1: EC202-T101 Microeconomics (2115-1) 
		
		fullName = courseName;
		friendlyName = courseName.substring(0, courseName.lastIndexOf("(") - 1);
        friendlyName = friendlyName.substring(friendlyName.lastIndexOf("-"));
        friendlyName = friendlyName.substring(friendlyName.indexOf(" ")+1);
        
	}
	
	public String toString()
	{
	
		return friendlyName;
	}
}