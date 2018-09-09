/**
 * AudioPlayer.java
 * Created on 22-07-2013 07:26 PM
 * @author CyberManic
 * @version 1.0
 */
package com.cyber.audio;

import java.util.*;
import com.cyber.audio.dsp.*;
import com.cyber.storage.IProperties;
import java.io.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioPlayer extends Thread{
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected volatile boolean proceed = true;
    protected volatile boolean speedUpModeFlag = false;
    protected volatile boolean isStopped = false;

    protected final AudioFormat format;
    protected float volume = 0.0F;
    protected SourceDataLine outputLine;
    protected BlockingQueue<byte[]> playbackQueue;
    protected IProperties properties = null;

            
    protected static final int playbackQueueMax = 50;
    protected static final float speedUpFactor = 0.95F;
    protected long lastPlaybackTime = 0;
    
    protected INormalizer normalizer = null;
    
    /**
     * Создает плеер для звуковых данных формата format. Устанавливает приоритет
     * потока плеера и создает очередь для вопроизводимых данных.
     * @param format (AudioFormat получаемых данных для воспроизведения)
     */
    
    public AudioPlayer(AudioFormat format){
        setName("AudioPlayer-" + getId());        
        setPriority(Thread.MAX_PRIORITY);

        this.format = format;
        
        playbackQueue = new ArrayBlockingQueue(playbackQueueMax);        
    }

    public String toString(){
        return Thread.currentThread().getName();
    }

    public String getFullName(){
        return String.format("%s [%s]", this.toString(), format.toString());
    }
    
    public void setProperties(IProperties properties){
        this.properties = properties;
        
        setVolume( properties.getProperty("audio.player.volume", 0.0F) );
        setNormalization( properties.getProperty("audio.player.normalization", false) );
    }
        
    /**
     * Устанавливает флаг необходимости остановки плеера. Сама остановка
     * произойдет не моментально.
     */
    
    public void stopPlayer(){
        proceed = false;
        isStopped = true;
    }

    /**
     * Возвращает состояние плеера, был ли он остановлен.
     * @return boolean
     */
    
    public boolean isStopped(){
        return isStopped;
    }
    
    /**
     * Возвращает объекта, нормализующий громкость линии. Null если отключен.
     * @return Normalizer
     */
    
    public INormalizer getNormalizer(){
        return normalizer;
    }
    
    /**
     * Активирует или выключает нормализатор уровня сигнала.
     * @param value (флаг активации)
     */
    
    public void setNormalization(boolean value){
        properties.put("audio.player.normalization", value );
        
        if (value){
            if (normalizer==null){
                normalizer = new Normalizer2(format);
                normalizer.setProperties(properties);
                log.trace("{} {} - enabled", this, normalizer.getFullInfo());
            }
        }else{
            if (normalizer!=null){
                normalizer = null;
                log.trace("{} Normalizer disabled", this);                
            }
        }
    }

    
    /**
     * Пытается открыть линию для воспроизведения звуковых данных вида format.
     * Если звуковая карта не может воспроизводить звук вида format или микшер ОС
     * не может выделить свободный канал, то произойдет ошибка.
     * @return SourceDataLine (null при ошибке)
     */
    
    protected SourceDataLine openOutputLine(){

        if (outputLine==null){
            log.debug("openOutputLine()");
            try {
                outputLine = AudioSystem.getSourceDataLine(format);
                outputLine.open(format, outputLine.getBufferSize());
                setVolume(volume);
            } catch (Exception e) { 
                log.warn("openOutputLine() error: {}", e);
                outputLine = null;
            }
        }
        
        return outputLine;
    }

    /**
     * Останавливает и закрывает доступную линию воспроизведения аудиоданных.
     */
    
    protected void closeOutputLine(){
        if (outputLine!=null){
            if (outputLine.isActive()) outputLine.stop();
            log.debug("closeOutputLine()");
            outputLine.close();
            outputLine = null;
        }
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
            properties.put("audio.player.volume", this.volume);
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
     * Добавляет звуковые данные audiodata в очередь воспроизведения.
     * Длительность вычисляется автоматически.
     * Если очередь вопроизведения заполнена, то принимается решение об ускорении
     * вопроизведения и новые данные преобразуются функцией speedUp(). Такой способ
     * помогает быстрее добраться до момента синхронизации.
     * Если очередь полностью заполнена, то один устаревший пакет удаляется,
     * освобождая место для новых данных.
     * @param audiodata (при null возврат false без добавления)
     * @return boolean (успешность добавления в очередь)
     * @see AudioPlayer#speedUpMode() 
     * @see AudioPlayer#speedUp(byte[])
     */

    public boolean playDataChunk(final byte[] audiodata){
        return playDataChunk(audiodata, 0, audiodata.length);                
    }

    /**
     * Добавляет звуковые данные audiodata, начиная с offset, длительностью length
     * в очередь воспроизведения. Offset и length должны быть кратны размеру
     * audioFrame=((sampleBits/8)*channels).
     * Если очередь вопроизведения заполнена, то ко всем старым и новым данным,
     * применяется speedUp(), пока очередь не опустеет.
     * @param audiodata (при null возврат false без добавления)
     * @param offset (смещение фрагмента данных в audiodata)
     * @param length (длительность фрагмента данных. При length&lt;=0 возврат false
     * без добавления)
     * @return boolean (успешность добавления в очередь воспроизведения)
     * @see AudioPlayer#speedUpMode() 
     * @see AudioPlayer#speedUp(byte[])
     */
    
    public boolean playDataChunk(final byte[] audiodata, int offset, int length){        
        byte[] buf = null;

        //log.trace("queue [{}/{}]", String.valueOf(playbackQueue.size()), String.valueOf(playbackQueueMax));
        
        if (audiodata!=null){
            if (audiodata.length>0){
                buf = Arrays.copyOfRange(audiodata, offset, length);
                
                if (speedUpMode()){
                    addPlaybackData( speedUp( buf ) );                        
                }else{
                    addPlaybackData( buf );                                                 
                }
            }
        }
        return false;
    }

    
    /**
     * Принимает решение о необходимости ускорения. Если очередь playbackQueue
     * полностью заполнена, то все ее данные преобразуются в один ускоренный пакет
     * и устанавливается флаг. Когда очередь становится пуста, флаг снимается.
     * @return boolean
     * @see AudioPlayer#playDataChunk(byte[])
     * @see AudioPlayer#playDataChunk(byte[], int, int) 
     */
    
    public boolean speedUpMode(){        
        if (speedUpModeFlag){
            if (playbackQueue.isEmpty()){
                speedUpModeFlag = false;
                log.debug("{} speedUpMode off", this);
            }
        }else{
            if (isFull()){
                addPlaybackData( speedUp( getPlaybackDataArray() ) );
                speedUpModeFlag = true;
                log.debug("{} speedUpMode on", this);
            }
        }
        
        return speedUpModeFlag;
    }
    
    /**
     * Добавляет данные в очередь. Имитирует успешную вставку null данных.
     * Игнорирует ошибку добавления. Возвращает успешность операции.
     * @param audiodata (nullable)
     * @return boolean
     */
    
    protected boolean addPlaybackData(byte[] audiodata){
        if (audiodata==null) return true;
        return playbackQueue.offer(audiodata);        
    }
    
    /**
     * True если очередь plybackQueue уже содержит не меньше playbackQueueMax пакетов,
     * что указывает на полностью заполненную очередь. В остальных случаях False.
     * @return boolean
     */
    
    public boolean isFull(){
        return (playbackQueue.size() >= playbackQueueMax);
    }
        
    /**
     * Ускоряет аудиофрагмент.
     * @param audiodata (не null)
     * @return byte[] audiodata
     */
    
    protected byte[] speedUp(byte[] audiodata){
        if (audiodata==null) return null;
        
        //byte[] buf = AudioProcessor.setRate(audiodata, format, speedUpFactor);
        //byte[] buf = AudioProcessor.decimateFrames(audiodata, format, 50, 1);
        byte[] buf = AudioRoutines.decimateFramesThreshold(audiodata, format, 20);
        //byte[] buf = AudioProcessor.resampleAudioDataRate(audiodata, format, 1.05);
        
        return buf;        
    }                
    
    /**
     * Возвращает массив состоящий из объединения всех доступных элементов очереди
     * воспроизведения playbackQueue. При вызове очередь очищается от извлеченных данных.
     * @return массив байт аудио данных (или null если очередь пуста)
     */
    
    protected byte[] getPlaybackDataArray(){
        if (playbackQueue.isEmpty()) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try{
            while(!playbackQueue.isEmpty()){
                out.write(playbackQueue.poll());
            }
        }catch(IOException ex){
            log.trace("{} getPlaybackData() error:", this, ex.toString());
        }
            
        return out.toByteArray();
    }
    
    
    /**
     * Возвращает аудиофрагмент из очередь воспроизведения playbackQueue или null
     * при ошибке или когда очередь пуста.
     * @return byte[] audiodata
     */
    
    protected byte[] pollNextAudioFrame(){
        return playbackQueue.poll();
    }
    
    /**
     * Обновляет таймер события проигрывания последнего аудиофрагмента
     * @see AudioPlayer#getLastPlaybackTimeDelta()
     */
    
    public void updateLastPlaybackTime(){
        lastPlaybackTime = System.currentTimeMillis();
    }
    
    /**
     * Возвращает разницу между временем проигрывания последнего аудифрагмента
     * и текущим временем. Используется для определения неактивности.
     * @return long delta
     * @see AudioPlayer#updateLastPlaybackTime() 
     */
    
    public long getLastPlaybackTimeDelta(){
        return (System.currentTimeMillis() - lastPlaybackTime);
    }
    
    /**
     * Основная функция потока плеера для воспроизведения аудиофрагментов из очереди.
     * Активирует линию воспроизведения, берет аудиофрагмент и проигрывает его.
     * Если 2 такта подряд не было данных, то остатки буфера звуковой карты
     * воспроизводятся и он очищается (устраняет шипение и треск при обрыве связи).
     * При ошибках воспроизведения плеер останавливается и закрывает линию микшера.
     * @see AudioPlayer#playAudioBuffer(byte[]) 
     * @see AudioPlayer#stopPlayer()
     */
    
    public void run(){
        proceed = true;        
        
        boolean isSilence = false;
        boolean flushed = false;
                
        log.debug("start {}", this.getFullName());
        openOutputLine().start();
        log.debug("play audio");
        
        try{        
            while(proceed){                        
                if(!playbackQueue.isEmpty()){
                    playAudioBuffer( pollNextAudioFrame() );
                    isSilence = false;                                    
                    updateLastPlaybackTime();                                
                    flushed = false;
                }else{    
                    if (isSilence && getLastPlaybackTimeDelta() > 250){
                        if (!flushed){
                            //playAudioBuffer( getSilence(1) );
                            outputLine.flush();      
                            updateLastPlaybackTime();
                            flushed = true;
                        }
                    }
                    Thread.sleep(1);            
                    isSilence = true;                                    
                }
            }
        }catch(InterruptedException ex){
            log.debug("{} interrupted", this.toString());
        }
            
        log.debug("stop audio playing");		
        isStopped = true;
        closeOutputLine();
        Thread.interrupted();
    }
                
    /**
     * Основная функция проигрывания аудиофрагментов. Записывает audiodata
     * в outputLine. Длительность даных должа быть кратна format.getFrameSize().
     * @param audiodata (null игнорируется без ошибки)
     */
    
    protected void playAudioBuffer(byte[] audiodata){    
        if (audiodata==null) return;
        
        int bytesRemaining = audiodata.length;
        int bytesWritten = 0;
        int bytesPerFrame = format.getFrameSize();
        int available = 0;
        
        if (normalizer!=null)
            audiodata = normalizer.proceed(audiodata);
        
        //audiodata = AudioProcessor.cycle(audiodata, format);
        
        try{
            while (bytesRemaining > 0 ) {            
                available = outputLine.available();
                if ( available > bytesPerFrame ){
                    if (bytesRemaining > available){
                        bytesWritten = outputLine.write(audiodata, 0, available);
                    }else{
                        bytesWritten = outputLine.write(audiodata, 0, bytesRemaining);                    
                    }
                    if (bytesWritten==0){
                        break;
                    }else{
                        bytesRemaining -= bytesWritten;
                    }
                }
            }        
        }catch(IllegalArgumentException ex){
            // illegal request to write non-integral number of frames
            log.warn("playAudioBuffer() error: {}", ex.toString());
        }
    }

}
