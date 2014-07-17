package sh.jay.xposed.waze;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.Calendar;
import android.media.AudioManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

public class AudioEmphasis implements IXposedHookLoadPackage {
	// This current package.
	private static final String PACKAGE_NAME = "sh.jay.xposed.waze";
	
	// What the sound level was before we changed it.
	private int previousStreamVolume = 0;

	/**
	 * Constants useful for debugging mode.
	 */
	// Whether debugging mode is on or off - configurable via Settings.
	private boolean debugMode = false;
	
	// When in debugging mode, we restore the volume to this quiet setting,
	// so there should be a very clear difference between the volume level
	// of messages from Waze, and that of the other applications playing in
	// the background.
	private static final int QUIET_SOUND_LEVEL = 2;

	/**
	 * Some helpful constants that abstract away from the implementation details
	 * within Waze. We hook these and adjust the volume just before it's about
	 * to play some audio, and just after it has finished playing.
	 */
	// An internal Waze method that is called immediately before audio is played.
	private static final String WAZE_BEFORE_AUDIO_PLAYS_METHOD = "abandonAf";
	// An internal Waze method that is called immediately after audio has finished playing.
	private static final String WAZE_AFTER_AUDIO_PLAYS_METHOD = "requestAf";

	// The class that contains the above internal Waze methods.
	private static final String WAZE_PACKAGE = "com.waze";
	private static final String WAZE_SOUNDMANAGER_CLASS_NAME = WAZE_PACKAGE + ".NativeSoundManager";
	
	/**
	 * This is initially called by the Xposed Framework when we register this
	 * android application as an Xposed Module.
	 */
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
    	loadPreferences();

    	// This method is called once per package, so we only want to apply hooks to Waze.
        if (WAZE_PACKAGE.equals(lpparam.packageName)) {
            this.hookWazeRequestAudioFocus(lpparam.classLoader);
            this.hookWazeReleaseAudioFocus(lpparam.classLoader);
        }
    }
    
    /**
     * Load the preferences from our shared preference file.
     */
    private void loadPreferences() {
		XSharedPreferences prefApps = new XSharedPreferences(PACKAGE_NAME);
		prefApps.makeWorldReadable();

		this.debugMode = prefApps.getBoolean("waze_audio_emphasis_debug", false);
    }
    
    /**
     * Whenever Waze wants to output audio, it takes audio focus. Just *before*
     * it does that, we increase the volume.
     * 
     * @param classLoader ClassLoader for the com.waze package.
     */
    private void hookWazeRequestAudioFocus(final ClassLoader classLoader) {
        findAndHookMethod(WAZE_SOUNDMANAGER_CLASS_NAME, classLoader, WAZE_AFTER_AUDIO_PLAYS_METHOD, new AudioAware_XC_MethodHook() {
        	@Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        		AudioManager audioManager = this.getAudioManager(param);

            	debug("[START] beforeHookedMethod of " + param.method.getName());
        		if (null != audioManager) {
        			previousStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                	int maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                	audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxStreamVolume, generateSetStreamVolumeFlags());
                	debug("Updated audio to maximum (" + maxStreamVolume + ")");
                }
            	debug("[END] beforeHookedMethod of " + param.method.getName());
            }
        });
    }
    
    /**
     * Whenever Waze is finished outputting audio, it releases audio focus. It's
     * then safe for us to lower the volume *after* it has done that.
     * 
     * @param classLoader
     */
    private void hookWazeReleaseAudioFocus(final ClassLoader classLoader) {
        findAndHookMethod(WAZE_SOUNDMANAGER_CLASS_NAME, classLoader, WAZE_BEFORE_AUDIO_PLAYS_METHOD, new AudioAware_XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {           	
        		AudioManager mgr = this.getAudioManager(param);
            	debug("[START] afterHookedMethod of " + param.method.getName());
        		if (null != mgr) {
        			int previousStreamVolume = getPreviousStreamVolume();
        			mgr.setStreamVolume(AudioManager.STREAM_MUSIC, previousStreamVolume, generateSetStreamVolumeFlags());
                	debug("Updated audio to " + previousStreamVolume);
                }
            	debug("[END] afterHookedMethod of " + param.method.getName());
            }
        });
    }
    
    /**
     * Generate the flags for calling AudioManager.setStreamVolume().
     * 
     * @return int The flags to pass directly to AudioManager.setStreamVolume().
     */
    private int generateSetStreamVolumeFlags() {
    	if (this.debugMode) {
    		return (AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
    	} else {
    		return AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
    	}
    }
    
    /**
     * Get the stream volume we should restore the device to
     * (useful for after we've modified it).
     * 
     * @return int The sound level.
     */
    private int getPreviousStreamVolume() {
    	if (this.debugMode){
    		return QUIET_SOUND_LEVEL;
    	} else {
    		return previousStreamVolume;
    	}
    }
    
    /**
     * Capture debugging messages.
     * 
     * @param message
     */
    private void debug(String message) {
    	if (this.debugMode) {
    		String currentDateTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
    		XposedBridge.log(currentDateTime + ": " + message);
    	}
    }
}
