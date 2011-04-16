package edu.bellevue.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.bellevue.android.blackboard.BlackboardService;

public class WatchUpdateReceiver extends BroadcastReceiver {
	private Context ctx = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    // TODO Auto-generated method stub
		ctx = context;
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.icon,"Checking for Updates",System.currentTimeMillis());
		n.defaults |= Notification.DEFAULT_SOUND;
		n.defaults |= Notification.DEFAULT_VIBRATE;
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		
		CharSequence contentTitle = "Checking for Updates";
		CharSequence contentText = "Checking for Thread Updates";
		Intent ni = new Intent(ctx, MainActivity.class);

		PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, ni, 0);
		
		n.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);
		nm.notify((int)System.currentTimeMillis(), n);
		BlackboardService.doCheck(ctx);
		System.gc();
	}

}
