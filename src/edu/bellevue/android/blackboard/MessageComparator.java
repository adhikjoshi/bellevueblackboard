package edu.bellevue.android.blackboard;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {

	public int compare(Message arg0, Message arg1) {
		return arg0.getDate()- arg1.getDate();
	}
}
