package edu.bellevue.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.bellevue.android.blackboard.BlackboardService;

public class WatchUpdateReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    // TODO Auto-generated method stub
		BlackboardService.doCheck(context);
		System.gc();
	}

}
