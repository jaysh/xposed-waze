xposed-waze
===========

Utilising the [Xposed framework](http://repo.xposed.info/module/de.robv.android.xposed.installer), this module adds tweaks to make Waze more useful to my. Currently, it only contains one tweak, but that could change as I get to use it more and more.

Audio Emphasis
--------------

I'll play music into my car using PowerAmp (which, in turn, passes through a FM Transmitter) which is great, because Waze will automatically quieten the music ([ake audio focus](http://developer.android.com/training/managing-audio/audio-focus.html)) when playing its messages. Unfortunately, they are not loud enough for me (when compared to the music).

This module is designed, very simply, to raise the volume of the music stream (which is the one PowerAmp and Waze play through) to maximum when Waze is saying something, and reducing it back the original level when it is done.

I hope the code is self-documenting in this respect as to how this works. If you find it is not clear, please do not hestitate to email me or file a pull request. I am not an Android/Java programmer by day, but I am always eager to learn how my code can be improved. At the heart, I was fortunate that Waze has two specific methods that are called directly before and directly after it is playing audio. Hooking those appropriately within Xposed gives us an easy opportunitiy to change the volume.
