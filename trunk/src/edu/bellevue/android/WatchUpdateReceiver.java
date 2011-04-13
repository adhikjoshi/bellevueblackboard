package edu.bellevue.android;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Vibrator;
import android.widget.Toast;
import edu.bellevue.android.blackboard.BlackboardService;
import edu.bellevue.android.blackboard.BlackboardService.BlackboardServiceBinder;

public class WatchUpdateReceiver extends BroadcastReceiver {
	protected BlackboardService mBoundService;
	private Context ctx = null;
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        Toast.makeText(ctx, "service connected", Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	    	Toast.makeText(ctx, "service disconnected", Toast.LENGTH_SHORT).show();
	        mBoundService = null;
	        
	    }
	};

	
	@Override
	public void onReceive(Context context, Intent intent) {
	    // TODO Auto-generated method stub
		ctx = context;
		Toast.makeText(context, "broadcast received", Toast.LENGTH_LONG).show();
		BlackboardServiceBinder bs = (BlackboardServiceBinder) peekService(context,new Intent(context,edu.bellevue.android.blackboard.BlackboardService.class));
		
		if (bs != null)
		{
			Toast.makeText(context, "Made Connection to the service :)", Toast.LENGTH_LONG).show();
			BlackboardService bbs = bs.getService();
			bbs.doCheck();
		}else
		{
			// something went wrong, TRY to start the service and recover :)
			context.startService(new Intent(context,edu.bellevue.android.blackboard.BlackboardService.class));
			bs = (BlackboardServiceBinder) peekService(context,new Intent(context,edu.bellevue.android.blackboard.BlackboardService.class));
			Toast.makeText(context, "Made Connection to the service :)", Toast.LENGTH_LONG).show();
			BlackboardService bbs = bs.getService();
			bbs.doCheck();
			Toast.makeText(context, "had to start it manually though", Toast.LENGTH_LONG).show();
		}
		int x = 1;
		int y = x;
		
	}

}
