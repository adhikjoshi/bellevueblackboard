package edu.bellevue.android.blackboard.objects;

import java.util.Hashtable;
import java.util.List;

import edu.bellevue.android.blackboard.Course;
import edu.bellevue.android.blackboard.Forum;
import edu.bellevue.android.blackboard.Message;
import edu.bellevue.android.blackboard.Thread;

public interface BlackboardAdapter {
	
	public abstract void initializeAdapter();
	public abstract boolean isInitialized();
	
	public abstract boolean logIn(String userName, String password);

	public abstract boolean isLoggedIn();

	public abstract List<Course> getCourses();

	public abstract List<Forum> getForums(String course_id);

	public abstract List<Thread> getThreads(String course_id, String forum_id,
			String conf_id);

	public abstract Hashtable<String, String> getMessageIds(String forum_id,
			String course_id, String conf_id, String thread_id);

	public abstract Message getMessage(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id);

	public abstract void addThreadToWatch(Thread t);

	public abstract void removeThreadFromWatch(Thread t);

	public abstract boolean createNewThread(String course_id, String forum_id,
			String conf_id, String subject, String body, String attachedFile);

	public abstract boolean createReply(String course_id, String forum_id,
			String conf_id, String thread_id, String message_id,
			String subject, String body, String attachedFile);

	public abstract boolean downloadAttachment(String url, String savePath);
	
	public abstract boolean isThreadWatched(Thread t);
}
