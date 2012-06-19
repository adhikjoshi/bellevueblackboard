package edu.bellevue.android.blackboard.objects;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import edu.bellevue.android.blackboard.Course;
import edu.bellevue.android.blackboard.Forum;
import edu.bellevue.android.blackboard.Message;
import edu.bellevue.android.blackboard.Thread;

public class BlackboardService {

	static boolean shouldPerformBackgroundCheck = true;
	private static final String LOGTAG = "BB_SERVICE";

	// variables for Properties
	private static String user_id = null;
	public static boolean cacheData = false;
	public static boolean offlineDemo = false;
	
	public static BlackboardAdapter adapter = null;
	

	// PUBLIC METHODS USED TO PERFORM BLACKBOARD OPERATIONS
	public static void setBlackboardAdapter(BlackboardAdapter ba)
	{
		if (!ba.isInitialized())
			ba.initializeAdapter();
		
		adapter = ba;
	}
	
	public static boolean logIn(String userName, String password) {
		if (adapter.logIn(userName,password))
		{
			user_id = userName;
			return true;
		}
		return false;
	}
	public static boolean isLoggedIn() {
		return adapter.isLoggedIn();
	}
	public static List<Course> getCourses() {
		shouldPerformBackgroundCheck = false;
		List<Course> courses = new ArrayList<Course>();
		courses = adapter.getCourses();
		shouldPerformBackgroundCheck = true;
		return courses;
	}
	public static List<Forum> getForums(String course_id) {
		shouldPerformBackgroundCheck = false;
		List<Forum> forums = new ArrayList<Forum>();
		
		forums = adapter.getForums(course_id);
		
		shouldPerformBackgroundCheck = true;
		return forums;

	}
	public static List<Thread> getThreads(String course_id, String forum_id,
			String conf_id) {
		List<Thread> threads = new ArrayList<Thread>();
		shouldPerformBackgroundCheck = false;
		
		threads = adapter.getThreads(course_id, forum_id, conf_id);
		
		shouldPerformBackgroundCheck = true;
		return threads;
	}
	public static Hashtable<String, String> getMessageIds(String forum_id,
			String course_id, String conf_id, String thread_id) {
		return adapter.getMessageIds(forum_id, course_id, conf_id, thread_id);
	}
	public static Message getMessage(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id) {
		shouldPerformBackgroundCheck = false;
		
		Message m = adapter.getMessage(course_id, forum_id, conf_id, thread_id, message_id);
		
		shouldPerformBackgroundCheck = true;
		
		return m;
	}
	public static void addThreadToWatch(Thread t) {
		//adapter.storeThread(t);
	}
	public static void removeThreadFromWatch(Thread t) {
		//adapter.removeThread(t);
	}
	public static boolean createNewThread(String course_id, String forum_id,
			String conf_id, String subject, String body, String attachedFile) {
		shouldPerformBackgroundCheck = false;
		
		adapter.createNewThread(course_id, forum_id, conf_id, subject, body, attachedFile);
		
		shouldPerformBackgroundCheck = true;
		return true;
	}
	public static boolean createReply(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id,
			String subject, String body, String attachedFile) {
		shouldPerformBackgroundCheck = false;
		adapter.createReply(course_id, forum_id, conf_id, thread_id, message_id, subject, body, attachedFile);
		shouldPerformBackgroundCheck = true;
		return true;
	}
	public static boolean downloadAttachment(String url, String savePath) {
		shouldPerformBackgroundCheck = false;
		boolean result = adapter.downloadAttachment(url, savePath);
		shouldPerformBackgroundCheck = true;
		return result;
	}

	// Database Methods
	public static boolean isThreadWatched(Thread t) {
		return adapter.isThreadWatched(t);
	}
}
