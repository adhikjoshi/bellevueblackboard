package edu.bellevue.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

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
			Toast.makeText(getApplicationContext(), "Autologin Changed", Toast.LENGTH_LONG).show();
			if (!arg0.getBoolean("autologin", false))
			{
				Editor e = arg0.edit();
				e.putString("username", "");
				e.putString("password", "");
				e.commit();
			}
		}
	}
}
