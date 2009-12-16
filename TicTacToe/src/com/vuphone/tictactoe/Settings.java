package com.vuphone.tictactoe;

import android.content.SharedPreferences;

/**
 * This class is a singleton that holds the user's preference data
 * 
 * @author Adam Albright, Ben Gotow
 * 
 * 
 */
public class Settings {

	private SharedPreferences prefs_ = null;
	private static Settings instance_ = null;

	public static final String VIBRATE = "vibrate";
	public static final String PLAY_SOUNDS = "playSounds";
	public static final String DISPLAY_NAME = "displayName";
	public static final String FIRST_LAUNCH = "firstLaunch";

	protected Settings() {
	}

	public void loadPreferences(SharedPreferences p) {
		prefs_ = p;
	}

	public static Settings getInstance() {
		if (instance_ == null)
			instance_ = new Settings();

		return instance_;
	}

	public void putBoolean(String key, boolean value) {
		if (prefs_ == null)
			return;

		SharedPreferences.Editor editor = prefs_.edit();

		editor.putBoolean(key, value);
		editor.commit();
	}

	public void putString(String key, String value) {
		if (prefs_ == null)
			return;

		SharedPreferences.Editor editor = prefs_.edit();

		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * Defaults to FALSE if the preference doesn't exist!
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean getBoolean(String key, boolean defaultVal) {
		if (prefs_ == null)
			return defaultVal;

		return prefs_.getBoolean(key, defaultVal);
	}

	/**
	 * Defaults to FALSE if the preference doesn't exist!
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public String getString(String key, String defaultVal) {
		if (prefs_ == null)
			return defaultVal;

		return prefs_.getString(key, defaultVal);
	}
}
