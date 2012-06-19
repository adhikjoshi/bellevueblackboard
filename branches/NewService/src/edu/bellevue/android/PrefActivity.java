package edu.bellevue.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import edu.bellevue.android.blackboard.objects.BlackboardService;

public class PrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    // TODO Auto-generated method stub
	    addPreferencesFromResource(R.xml.mainprefs);
	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		if (arg1.equals("autologin"))
		{
			if (!arg0.getBoolean("autologin", false))
			{
				Editor e = arg0.edit();
				e.remove("username");
				e.remove("password");
				e.commit();
			}
		}
		if (arg1.equals("cachedata"))
		{
			BlackboardService.cacheData = arg0.getBoolean(arg1, false);
			if (BlackboardService.cacheData == true)
			{
				if (arg0.getString("cachelength", "").equals(""))
				{
					Editor e = arg0.edit();
					e.putString("cachelength", "1");
					e.commit();
				}
			}
		}
	}
}
