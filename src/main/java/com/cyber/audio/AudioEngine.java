/**
 * Audio.java
 *
 * Created on 22-07-2013 04:09 PM
 *
 */
package com.cyber.audio;

//import org.slf4j.*;
import com.cyber.audio.codec.*;
import com.cyber.storage.*;
import java.nio.ByteOrder;
import javax.sound.sampled.*;
import java.util.concurrent.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioEngine {
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected final Map<Integer,AudioPlayer> players;    
    protected final AudioCodecType codecType;
    protected IProperties properties;
    protected AudioRecorder recorder;    
    protected AudioFormat format = null;    
    
    /**
     * Создает экземпляр и использует IProperties для компонентов {@link AudioRecorder}, {@link AudioPlayer},
     * {@link AudioCodecFactory}.
     * Следует выбирать наиболее совместимый и гарантированно работающий {@link AudioFormat}
     * для большинства звуковых карт (44100 Гц, 16 бит, стерео).
     * @param properties (null параметр задает значения по умолчанию)
     * @see AProperties
     */
        
    public AudioEngine( IProperties properties ){
        this.properties = properties;            
        
        format = getAudioFormat(
            properties.getProperty("audio.format.sample.rate", 44100),
            properties.getProperty("audio.format.sample.bits", 16),
            properties.getProperty("audio.format.channels", 2)
        );
        
        AudioCodecFactory.setAudioFormat(format);   
        codecType = getAudioCodecType( this.properties.getProperty("audio.format.codec", "NULL") );
        players = new ConcurrentHashMap<>();        
        
        log.debug("{} CodecType = {}", this, codecType);
        log.debug("{} AudioFormat = {}", this, format);       
        log.debug("{} NativeOrder = {}", this, ByteOrder.nativeOrder());
        
    }

    /**
     * Создает экземпляр используя временные {@link SimpleProperties} для компонентов {@link AudioRecorder}, {@link AudioPlayer},
     * {@link AudioCodecFactory}.
     * Следует выбирать наиболее совместимый и гарантированно работающий {@link AudioFormat}
     * для большинства звуковых карт (44100 Гц, 16 бит, стерео).
     * @see AProperties
     */
    
    public AudioEngine(){
        this(new SimpleProperties());
    }
    
    public AudioEngine setProperties(IProperties properties){
        this.properties = properties;                    
        return this;
    }
    
    public IProperties getProperties(){
        return properties;
    }
    
    @Override
    public String toString(){
        return "AudioEngine";
    }
        
    /**
     * Кодирует массив аудиоданных (input) с помощью кодека (codec_id).
     * Метод эффективен только для аудиоданных значительного размера т.к.
     * при каждом вызове создается новый экземпляр кодека.
     * @param codec_id ({@link AudioCodecType#ordinal()})
     * @param input (звуковые данные)
     * @return массив закодированных аудиоданных или null при ошибке
     */
    
    public byte[] encode(int codec_id, byte[] input){
        return AudioCodecFactory.getCodec(codec_id).encode(input);
    }
    
    /**
     * Декодирует массив аудиоданных (input) с помощью кодека (codec_id).
     * Метод эффективен только для аудиоданных значительного размера т.к.
     * при каждом вызове создается новый экземпляр кодека.
     * @param codec_id ({@link AudioCodecType#ordinal()})
     * @param input (звуковые данные)
     * @return массив раскодированных аудиоданных или null при ошибке
     */

    public byte[] decode(int codec_id, byte[] input){
        return AudioCodecFactory.getCodec(codec_id).decode(input);
    }
    
    /**
     * Возвращает заданый {@link AudioFormat}
     * Если значение не было предварительно задано, то инициализирует значением по умолчанию
     * (44100 Гц, 16 бит, стерео, Little-endian)
     * @return {@link AudioFormat}
     */
    
    public AudioFormat getAudioFormat(){

        if (format==null){
            AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
            float rate = 44100.0F;
            int sampleBits = 16;
            int channels = 2;
            boolean bigEndian = false;
            format = new AudioFormat( encoding, rate, sampleBits, channels, (sampleBits/8)*channels, rate, bigEndian );
        }        
        return format;
    }

    /**
     * Упрощенный метод создания {@link AudioFormat} по нескольким параметрам
     * @param sampleRate (22050, 44100, 48000 и т.д.)
     * @param sampleBits (8, 16 и т.д)
     * @param channels (1, 2 и т.д. для моно или стерео)
     * @return new {@link AudioFormat}
     */
    
    public static AudioFormat getAudioFormat(int sampleRate, int sampleBits, int channels ){
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        float rate = (float)sampleRate;
        int sampleSize = sampleBits;
        boolean bigEndian = false;
        return new AudioFormat( encoding, rate, sampleSize, channels, (sampleSize/8)*channels, rate, bigEndian );        
    }    
    
    /**
     * Возвращает тип кодека, заданного в по-умолчанию. Используется для {@link AudioRecorderEncoder}.
     * @return {@link AudioCodecType}
     */
    
    public AudioCodecType getAudioCodecType(){
        return this.codecType;
    }
    
    /**
     * Возвращает тип кодека по его имени. Не зависит от регистра. При ошибке выбрасывает
     * RuntimeException
     * @param codecName (строковое название кодека)
     * @return {@link AudioCodecType}
     */
    
    public AudioCodecType getAudioCodecType(String codecName){
        return AudioCodecType.getByName(codecName);
    }
        
    /**
     * Возвращает AudioPlayer для идентификатора id. Создает новый AudioPlayer
     * при необходимости. Плеер будет работать с несжатыми звуковыми данными.
     * Каждый новый экземпляр регистрируется в ConcurrentHashMap players.
     * Запуск плеера необходимо производить вручную вызовом start().
     * @param id (любой идентификатор плеера)
     * @return {@link AudioPlayer}
     * @see AudioEngine#stopAudioPlayer(int)
     */
    
    public AudioPlayer getAudioPlayer(int id){
        AudioPlayer player = players.get(id);

        if (player!=null && player.isStopped){
            players.remove(id);
            player = null;
        }

        if (player==null){
            log.trace("new AudioPlayer({})", id);
            player = new AudioPlayer(this.format);        
            setupAudioPlayer(player);
            players.put(id, player);
        }
        return player;
    }

    /**
     * Возвращает {@link AudioPlayer} для идентификатора id, Создает новый {@link AudioPlayerDecoder}
     * при необходимости. Плеер будет работать со сжатыми данными через вызов кодека decoder.
     * Каждый новый экземпляр регистрируется в {@link #players}.
     * Запуск плеера необходимо производить вручную вызовом start().
     * @param id (любой идентификатор плеера)
     * @param decoder (кодек для воспроизведения)
     * @return {@link AudioPlayer}
     * @see AudioEngine#stopAudioPlayer(int)
     * @see AudioCodecType
     */

    public AudioPlayer getAudioPlayer(int id, AudioCodecType decoder){
        AudioPlayer player = players.get(id);
        
        if (player != null && player.isStopped){
            players.remove(id);
            player = null;
        }
        
        if (player==null){
            log.trace("create AudioPlayerDecoder({}) for '{}'", decoder, id);
            player = new AudioPlayerDecoder(this.format, decoder);        
            setupAudioPlayer(player);
            players.put(id, player);
        }
        
        return player;    
    }    
    
    /**
     * Возвращает рекордер. Создает новый {@link AudioRecorder} при необходимости.
     * Рекордер будет работать с несжатыми звуковыми данными. Запуск рекордера
     * необходимо производить вручную вызовом start()
     * @return {@link AudioRecorder}
     * @see AudioRecorderEncoder
     */
    
    public AudioRecorder getAudioRecorder(){
        if (recorder==null){
            recorder = new AudioRecorder(this.format);
            setupAudioRecorder();
        }
        return recorder;
    }

    /**
     * Возвращает рекордер. Создает новый AudioRecorderEncoder при необходимости.
     * Рекордер будет работать со сжатыми данными, кодируя их через вызов кодека
     * encoder. Запуск рекордера необходимо производить вручную вызовом start().
     * @param encoder (кодек для записи)
     * @return AudioRecorder
     * @see AudioEngine#stopAudioRecorder()
     * @see AudioRecorderEncoder
     * @see AudioCodecType
     */

    public AudioRecorder getAudioRecorder(AudioCodecType encoder){
        if (recorder==null){
            recorder = new AudioRecorderEncoder(this.format, encoder);
            setupAudioRecorder();
        }
        
        return recorder;
    }    
    
    /**
     * Применяет доступные настройки из {@link #properties} к существующему {@link #recorder}.
     */
    
    public void setupAudioRecorder(){
        recorder.setProperties(properties);
    }

    /**
     * Применяет доступные настройки из {@link #properties} к существующему {@link AudioPlayer}.
     * @param player (существующий экземпляр AudioPlayer)
     */
    
    public void setupAudioPlayer(AudioPlayer player){
        player.setProperties(properties);
    }
    
    
    /**
     * Возвращает активный, готовый к записи {@link AudioRecorder}. Создает и запускает
     * неактивный экземпляр при необходимости.
     * @param encoder (кодек для записи {@link AudioCodecType})
     * @return {@link AudioRecorder}
     * @see AudioEngine#stopAudioRecorder()
     * @see AudioCodecType
     */
    
    public AudioRecorder getReadyAudioRecorder(AudioCodecType encoder){
        if (!getAudioRecorder(encoder).isAlive()) recorder.start();
        return recorder;
    }
        
    /**
     * Возвращает активный, готовый к записи {@link AudioRecorder}. Создает и запускает экземпляр
     * при необходимости. Используется параметр 'audio.format.codec' из {@link #properties} или NULL
     * для кодирования.
     * @return {@link AudioRecorder}
     * @see AudioEngine#stopAudioRecorder()
     */
    
    public AudioRecorder getReadyAudioRecorder(){

        if (recorder==null){
            if (AudioCodecType.NULL.equals( codecType )){
                recorder = getAudioRecorder();
            }else{
                recorder = getAudioRecorder( codecType );
            }
        }
        
        if (!recorder.isAlive()) recorder.start();
        return recorder;
    }
        
    
    /**
     * Возвращает активный, готовый к воспроизведению {@link AudioPlayer} для идентификатора
     * id. Создает и запускает экземпляр при необходимости. Регистрирует каждый
     * новый плеер в {@link #players}. Плеер будет работать с несжатыми данными.
     * @param id (идентификатор для поиска или создания)
     * @return {@link AudioPlayer}
     * @see AudioEngine#stopAudioPlayer(int)
     */
    
    public AudioPlayer getReadyAudioPlayer(int id){
        AudioPlayer player = getAudioPlayer(id);
        if (!player.isAlive()) player.start();
        return player;
    }

    /**
     * Возвращает активный, готовый к воспроизведению {@link AudioPlayer} для идентификатора
     * id. Создает и запускает экземпляр при необходимости. Регистрирует каждый
     * новый плеер в {@link #players}. Плеер будет работать со сжатыми данными через вызов кодека
     * decoder.
     * @param id (идентификатор для поиска или создания)
     * @param decoder (кодек для воспроизведения)
     * @return {@link AudioPlayer}
     * @see AudioEngine#stopAudioPlayer(int) 
     * @see AudioCodecType
     */

    public AudioPlayer getReadyAudioPlayer(int id, AudioCodecType decoder){
        AudioPlayer player = getAudioPlayer(id, decoder);
        if (!player.isAlive()) player.start();
        return player;
    }
    
    /**
     * Останавливает активный рекордер и очищает ссылку на него
     * @see AudioRecorder
     */
    
    public void stopAudioRecorder(){
        if (recorder!=null && recorder.isAlive()){
            recorder.stopRecorder();
            recorder = null;
        }
    }
    
    /**
     * Останавливает активный плеер с идентификатором id и очищает ссылку на него
     * из {@link #players}.
     * @param id (идентификатор для поиска)
     * @see AudioPlayer
     */
    
    public void stopAudioPlayer(int id){
        AudioPlayer player = getAudioPlayer(id);
        if (player!=null && player.isAlive()){
            player.stopPlayer();
            players.remove(id);
        }
    }
    
    /**
     * Возвращает ссылку на объект управления плеерами. Используется для применения настроек к списку
     * активных плееров.
     * @return {@link #players} map
     */
    
    public Map<Integer, AudioPlayer> getAudioPlayers(){
        return players;
    }
    
}
