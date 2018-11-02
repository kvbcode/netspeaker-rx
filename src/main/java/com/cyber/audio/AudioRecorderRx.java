/**
 * AudioRecorder.java
 *
 * Created on 22-07-2013 07:45 PM
 *
 */
package com.cyber.audio;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.Arrays;
import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioRecorderRx extends Thread{
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected volatile boolean proceed = true;
    
    protected TargetDataLine inputLine;
    protected AudioFormat format;
    
    protected long lastOpenLineTime = 0L;
    protected long restartLineInterval = 0L;
    
    protected long lastRecordedSignalTime;
    protected int silenceThreshold = 16;
    protected int lastSignalValue = 0;
    
    protected final int minFrameSize = 0;
    protected int maxFrameSize = 1400;            // audioFrameSize and MTU tweak < max UDP packet size
    
    protected byte[] inputBuffer;
    
    private PublishSubject<byte[]> outputSlot = PublishSubject.create();
    
    public AudioRecorderRx(AudioFormat format){
        setName("AudioRecorder-" + getId());
        setPriority(Thread.MAX_PRIORITY);
             
        this.format = format;
                
        updateLastAudioSignalTime();
    }
    
    public Subject<byte[]> outputSlot(){
        return outputSlot;
    }
            
    
    public String toString(){
        return getName();
    }
   
    
    public String getFullName(){
        return String.format("%s [%s]", this.toString(), format.toString());
    }
    
    
    public void stopRecorder(){
        proceed = false;
    }
    
    
    public void setSilenceFilter(int threshold){
        this.silenceThreshold = threshold;
    }
        
    
    public boolean isSilenceFilterEnabled(){
        return (silenceThreshold > 0);
    }
        
    
    public void setRestartInterval(int tsec){
        this.restartLineInterval = tsec * 1000;
    }
    
    
    protected TargetDataLine openInputLine(){        
        if (inputLine==null){            
            log.debug("{} openInputLine()", this);
            try {
                inputLine = AudioSystem.getTargetDataLine(format);
                inputLine.open(format, inputLine.getBufferSize());
                
                inputBuffer = createLineByteBuffer(inputLine.getBufferSize(), format);
                if (inputBuffer.length < maxFrameSize) maxFrameSize = inputBuffer.length;     
                
                lastOpenLineTime = System.currentTimeMillis();
            } catch (Exception e) { 
                log.warn("openInputLine() error: {}", e);
                inputLine = null;
            }
        }
        
        return inputLine;
    }
        
    
    protected void closeInputLine(){
        if (inputLine!=null){
            log.trace("{} closeInputLine()", this);
            if (inputLine.isActive()){
                inputLine.flush();
                inputLine.stop();
            }
            inputLine.close();
            inputLine = null;
        }
    }    
        
    
    protected void checkRestartLine(){
        if ( restartLineInterval > 0 && System.currentTimeMillis() - lastOpenLineTime > restartLineInterval){
            if (getLastAudioSignalTimeDelta() > 3000){
                closeInputLine();
                openInputLine().start();
                lastOpenLineTime = System.currentTimeMillis();
            }
        }
    }
    
        
    protected void updateLastAudioSignalTime(){
        lastRecordedSignalTime = System.currentTimeMillis();
    }

    
    public long getLastAudioSignalTimeDelta(){
        return (System.currentTimeMillis() - lastRecordedSignalTime);
    }

    
    public boolean isAudioSignalDetected(){
        return lastSignalValue > silenceThreshold;
    }

    
    protected short bytes2shortBE(byte left, byte right){
        return (short) ((left & 0xff) << 8 | right & 0xff);        
    }
    
    
    protected short bytes2shortLE(byte left, byte right){
        return (short) (left & 0xff | (right & 0xff) << 8);        
    }
    
    
    public int detectAudioSignal(int silenceThreshold, byte[] data, int datalen){
        final int step = 4 * format.getFrameSize();
        final boolean isBigEndian = format.isBigEndian();
        
        int ret = 0;
        int val = 0;
        
        if (silenceThreshold<127){
            int istart = 0;
            if (isBigEndian) istart = 1;
            for (int i = istart; i<datalen; i+=step){
                val = data[i];
                ret = Math.max(ret, val);
            }
        }else{
            for(int i=0; i<(datalen-1); i+=step){
                if (isBigEndian){
                    val = bytes2shortBE(data[i], data[i+1]);
                }else{
                    val = bytes2shortLE(data[i], data[i+1]);
                }
                ret = Math.max(ret, val);
            }
        }
        return ret;
    }

    
    protected byte[] createLineByteBuffer(int bufferLengthInFrames, AudioFormat format){
        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInBytes = (bufferLengthInFrames / 8) * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        return data;
    }

    protected void pushRecordedAudioFrame(byte[] audiodata){
        pushRecordedAudioFrame(audiodata, audiodata.length);
    }
    
    
    protected void pushRecordedAudioFrame(byte[] audiodata, int length){
        if (length==0) return;
        byte[] buf = Arrays.copyOf(audiodata, length);        
        outputSlot.onNext(buf);
    }    
            
    public void run(){
        proceed = true;

        int available = 0;
        int numBytesRead;

        openInputLine().start();

        try{
            while(proceed){
                checkRestartLine();

                available = inputLine.available();            

                if (available > minFrameSize){                                
                    if (available > maxFrameSize) available = maxFrameSize;

                    if((numBytesRead = inputLine.read(inputBuffer, 0, available)) == -1) break;     

                    if (isSilenceFilterEnabled()){                    
                        lastSignalValue = detectAudioSignal(silenceThreshold, inputBuffer, numBytesRead);

                        if (isAudioSignalDetected()){
                            pushRecordedAudioFrame(inputBuffer, numBytesRead);                
                            updateLastAudioSignalTime();
                        }
                    }else{
                        pushRecordedAudioFrame(inputBuffer, numBytesRead);    
                        updateLastAudioSignalTime();
                    }
                }else{
                    Thread.sleep(1);
                }
            }
        }catch(InterruptedException ex){
            
        }        
        
        log.info("stop audio capturing");
        closeInputLine();        
    }

}
