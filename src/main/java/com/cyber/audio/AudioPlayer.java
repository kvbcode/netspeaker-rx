/**
 * AudioPlayer.java
 * Created on 22-07-2013 07:26 PM
 * @author CyberManic
 * @version 1.0
 */
package com.cyber.audio;

import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioPlayer{
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected volatile boolean proceed = true;
    protected volatile boolean isStopped = false;

    protected final AudioFormat format;
    protected float volume = 0.0F;
    protected SourceDataLine outputLine;
            
    protected static final long SILENCE_CHECK_INTERVAL = 200L;
    protected long lastPlaybackTime = 0;
        
    /**
     * Создает плеер для звуковых данных формата format. Устанавливает приоритет
     * потока плеера и создает очередь для вопроизводимых данных.
     * @param format (AudioFormat получаемых данных для воспроизведения)
     */
    
    public AudioPlayer(AudioFormat format){
        this.format = format;        
    }
    
    /**
     * Пытается открыть линию для воспроизведения звуковых данных вида format.
     * Если звуковая карта не может воспроизводить звук вида format или микшер ОС
     * не может выделить свободный канал, то произойдет ошибка.
     * @return SourceDataLine (null при ошибке)
     */
    
    public SourceDataLine openOutputLine(){
        if (outputLine==null){
            log.debug("openOutputLine()");
            try {
                outputLine = AudioSystem.getSourceDataLine(format);
                outputLine.open(format, outputLine.getBufferSize());
                setVolume(volume);
                outputLine.start();
                                
            } catch (Exception e) { 
                log.error("openOutputLine() error: {}", e);
                outputLine = null;
            }
        }
        
        return outputLine;
    }

    /**
     * Останавливает и закрывает доступную линию воспроизведения аудиоданных.
     */
    
    public void closeOutputLine(){
        if (outputLine!=null){
            if (outputLine.isActive()) outputLine.stop();
            log.debug("closeOutputLine()");
            outputLine.close();
            outputLine = null;
        }
    }
    
    public boolean checkFullSilence(){
        if (getLastPlaybackTimeDelta() > SILENCE_CHECK_INTERVAL){
            outputLine.flush();
            return true;
        }        
        return false;
    }
    
    /**
     * Возвращает установленную громкость линии плеера.
     * @return float
     */
    
    public float getVolume(){
        return volume;
    }
    
    /**
     * Устанавливает громкость заранее открытой линии в микшере.
     * Реализация зависит от возможностей микшера.
     * Пример значений: -80.0 dB до +6.0dB
     * @param value (величина усиления)
     */
    
    public void setVolume(float value){
        float oldValue = this.volume;
        this.volume = value;
        
        FloatControl vol = getLineControl();
        if (vol!=null){
            try{
                vol.setValue(value);            
                log.trace("{}", vol.toString());
            }catch(IllegalArgumentException ex){
                log.warn("{} setVolume() warning: out of range. {}", this, ex.toString());
                this.volume = oldValue;
            }
        }                        
    }
    
    /**
     * Возвращает объект управления уровнем сигнал для заранее открытой линии.
     * Доступны величина и границы громкости,а также баланс. Возможности управления и конкретные
     * значения зависят от возможностей микшера в системе.
     * @return FloatControl object (или null)
     */
    
    public FloatControl getLineControl(){
        if (outputLine==null) return null;
        FloatControl control = (FloatControl)outputLine.getControl(FloatControl.Type.MASTER_GAIN);
        return control;
    }
            
    /**
     * Обновляет таймер события проигрывания последнего аудиофрагмента
     * @see AudioPlayerRx#getLastPlaybackTimeDelta()
     */
    
    public void updateLastPlaybackTime(){
        lastPlaybackTime = System.currentTimeMillis();
    }
    
    /**
     * Возвращает разницу между временем проигрывания последнего аудифрагмента
     * и текущим временем. Используется для определения неактивности.
     * @return long delta
     * @see AudioPlayerRx#updateLastPlaybackTime() 
     */
    
    public long getLastPlaybackTimeDelta(){
        return (System.currentTimeMillis() - lastPlaybackTime);
    }
                    
    /**
     * Основная функция проигрывания аудиофрагментов. Записывает audiodata
     * в outputLine. Длительность даных должа быть кратна format.getFrameSize().
     * @param audiodata (null игнорируется без ошибки)
     */
    
    protected void playAudioBuffer(byte[] audiodata){            
        if (audiodata.length > outputLine.available()){
            log.warn("line.buffer.overflow!");
        }
        final int dataSize = audiodata.length;            
        int available, chunkSize;
        
        for(int i=0; i<dataSize;){
            available = outputLine.available();
            if ( available > 0 ){
                chunkSize = Math.min( dataSize - i, available );
                i+= outputLine.write( audiodata, i, chunkSize );
                updateLastPlaybackTime();
            }
        }            
    }
        
}
