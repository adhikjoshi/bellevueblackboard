package edu.bellevue.android.blackboard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	
	public static Thread makeFromCompressedData(byte[] compressedData)
	{
		try{
	    ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
	    GZIPInputStream gzi = new GZIPInputStream(bais);
	    ObjectInputStream ois = new ObjectInputStream(gzi);
	    Thread t = (Thread) ois.readObject();
	    ois.close();
	    gzi.close();
	    bais.close();
	    return t;
		}catch (Exception e){e.printStackTrace();}
		return null;
	}

}