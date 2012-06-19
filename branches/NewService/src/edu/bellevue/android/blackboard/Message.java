package edu.bellevue.android.blackboard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Message implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String msgName;
	private String course_id;
	private String conf_id;
	private String forum_id;
	private String thread_id;
	private String message_id;
	private String body;
	private String msgDate;
	private String msgAuthor;
	
	public Message(String mName, String mDate, String mAuthor, String body, String course, String conf, String forum, String message, String thread)
	{
		//CIS337-T201_2113_1: CIS337-T201 Web Scripting (2113-1)
		//EC202-T101_2115_1: EC202-T101 Microeconomics (2115-1) 
		
		msgName = mName;
		msgDate = mDate;
		msgAuthor = mAuthor;
		message_id = message;
		thread_id = thread;
		course_id = course;
        conf_id = conf;
        forum_id = forum;
        this.body = body;
	}

	public String getMsgName()
	{
		return msgName;
	}

	public int getDate()
	{
		String tmp = msgDate.replace("<i>", "");
		tmp = tmp.replace("</i>", "");
		return ((int)Date.parse(tmp));
	}
	public String getForumId()
	{
		return forum_id;
	}
	public String getCourseId()
	{
		return course_id;
	}
	public String getConfId()
	{
		return conf_id;
	}

	public String getThreadId() {
		return thread_id;
	}

	public String getMessageId() {
		return message_id;
	}

	public String getBody() {
		return body;
	}

	public String getMsgDate() {
		return msgDate;
	}

	public String getMsgAuthor() {
		return msgAuthor;
	}
	
	public byte[] compressForStorage()
	{
		byte[] compressedData = null;
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gz = new GZIPOutputStream(baos);
		    ObjectOutputStream oos = new ObjectOutputStream(gz);
		    oos.writeObject(this);
		    oos.flush();
		    oos.close();
		    compressedData = baos.toByteArray();
		    baos.close();
		    
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return compressedData;
	}
	
	public static Message makeFromCompressedData(byte[] compressedData)
	{
		try{
	    ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
	    GZIPInputStream gzi = new GZIPInputStream(bais);
	    ObjectInputStream ois = new ObjectInputStream(gzi);
	    Message m = (Message) ois.readObject();
	    ois.close();
	    gzi.close();
	    bais.close();
	    return m;
		}catch (Exception e){e.printStackTrace();}
		return null;
	}
}