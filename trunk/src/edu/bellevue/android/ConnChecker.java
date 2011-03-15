package edu.bellevue.android;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

public final class ConnChecker {
		public static boolean shouldConnect(SharedPreferences prefs, Context ctx)
	    {
	    	ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    	int netType = connMan.getActiveNetworkInfo().getType();
	    	
	    	if (netType == ConnectivityManager.TYPE_WIFI) 
	    	{
	    	    return prefs.getBoolean("wifi", false);
	    	} else if (netType == ConnectivityManager.TYPE_MOBILE)
	    	{
	    		if (prefs.getBoolean("mobile",false))
	    		{
	    			TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
	    			if (tm.isNetworkRoaming())
	    			{
	    				return (prefs.getBoolean("roaming",false));
	    			}else
	    			{
	    				return true;
	    			}
	    			
	    		}else 
	    		{
	    			return false;
	    		}
	    	}else
	    	{
	    		return false;
	    	}
	    }
		
		public static String getConnType(Context ctx) {
			// TODO Auto-generated method stub
			ConnectivityManager connMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			switch (connMan.getActiveNetworkInfo().getType())
			{
			case ConnectivityManager.TYPE_WIFI:
				return "Wireless";
			case ConnectivityManager.TYPE_MOBILE:
				TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
				if (tm.isNetworkRoaming())
				{
					return "Roaming";
				}else
				{
					return "Mobile";
				}
			default:
				return "Unknown";
			}
		}

		public static void showUnableToConnect(Context ctx) {
			AlertDialog.Builder b = new Builder(ctx);
			b.setMessage("Connecting while on a " + ConnChecker.getConnType(ctx) + " network has been disabled.\r\n\r\nPlease adjust your settings!");
			b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}
			});
			AlertDialog ad = b.create();
			ad.setTitle("Connection Failed");
			ad.setIcon(R.drawable.icon);
			ad.show();
		}
	
}
