/**
 * AudioPlayer.java
 * Created on 22-07-2013 07:26 PM
 * @author CyberManic
 * @version 1.0
 */
package com.cyber.audio;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;


public class AudioPlayerRx extends AudioPlayer implements Observer<byte[]>{
    
    private Disposable silenceCheck;    
    
    public AudioPlayerRx(AudioFormat format){
        super(format);        
    }
            
    @Override
    public void onSubscribe(Disposable d) {
        if( openOutputLine()!=null ){        
            silenceCheck = Observable.interval(0, SILENCE_CHECK_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(c -> checkFullSilence());              
        }
    }

    @Override
    public void onNext(byte[] audioData) {
        playAudioBuffer(audioData);
        updateLastPlaybackTime();
    }

    @Override
    public void onError(Throwable e) {
        log.error("player.err: " + e.toString());
        e.printStackTrace();
        closeOutputLine();
    }

    @Override
    public void onComplete() {
        closeOutputLine();
        silenceCheck.dispose();
    }

}
