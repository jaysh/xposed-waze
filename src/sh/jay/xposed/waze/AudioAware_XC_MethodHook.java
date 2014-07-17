package sh.jay.xposed.waze;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import android.media.AudioManager;
import de.robv.android.xposed.XC_MethodHook;

public class AudioAware_XC_MethodHook extends XC_MethodHook {
    protected AudioManager getAudioManager(MethodHookParam param) {       	
        if (null == param.thisObject) {
        	return null;
        } else {
        	return (AudioManager) callMethod(param.thisObject, "getAudioManager");
        }
    }
}
