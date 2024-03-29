/**
 * 
 */
package com.vuphone.tictactoe;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * @author Adam Albright, Ben Gotow
 * 
 */
public class SettingsActivity extends Activity implements OnClickListener {
	private Settings prefs_ = Settings.getInstance();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		((Button) findViewById(R.id.btnSave)).setOnClickListener(this);
		((Button) findViewById(R.id.btnCancel)).setOnClickListener(this);

		((TextView) findViewById(R.id.txtDisplayName)).setText(prefs_.getString(Settings.DISPLAY_NAME, ""));
		((CheckBox) findViewById(R.id.chkboxVibrate)).setChecked(prefs_.getBoolean(Settings.VIBRATE, true));
		((CheckBox) findViewById(R.id.chkboxKeepScreenOn)).setChecked(prefs_.getBoolean(Settings.KEEP_SCREEN_ON, true));
	}

	/**
	 * Called when Save or Cancel is clicked
	 */
	public void onClick(View v) {

		if (v.getId() == R.id.btnSave) {
			CharSequence displayName = ((TextView) findViewById(R.id.txtDisplayName))
					.getText();
			boolean vibrate = ((CheckBox) findViewById(R.id.chkboxVibrate))
					.isChecked();

			if (displayName.length() > 0)
				prefs_.putString(Settings.DISPLAY_NAME, displayName.toString());

			prefs_.putBoolean(Settings.VIBRATE, vibrate);
		}

		finish();
	}
}
