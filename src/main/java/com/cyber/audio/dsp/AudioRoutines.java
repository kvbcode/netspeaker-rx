/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.dsp;

import java.io.*;
import java.util.Arrays;
import javax.sound.sampled.*;
import java.nio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioRoutines {
    protected static String name = "AudioRoutines";
    protected static final Logger log = LoggerFactory.getLogger(name);
    
    /**
     * Возвращает AudioInputStream, представляя audiodata в виде потока звуковых
     * данных format. Длительность потока указывается во фреймах.
     * @param audiodata (массив байт аудиоданных)
     * @param format (AudioFormat данных буфера и получаемого потока)
     * @param framesLength (длительность аудиоданных во фреймах)
     * @return new AudioInputStream
     */
    
    public static AudioInputStream getAudioStreamFromBytes(final byte[] audiodata, AudioFormat format, long framesLength){
        ByteArrayInputStream bis = new ByteArrayInputStream(audiodata); 
        AudioInputStream ais = new AudioInputStream(bis, format, framesLength);

        return ais;
    }

    /**
     * Возвращает AudioInputStream, представляя audiodata в виде потока звуковых
     * данных format. Длительность рассчитывается автоматически.
     * @param audiodata (массив байт аудиоданных)
     * @param format (AudioFormat данных буфера и получаемого потока)
     * @return new AudioInputStream
     */
    
    public static AudioInputStream getAudioStreamFromBytes(final byte[] audiodata, AudioFormat format){
        long length = audiodata.length / format.getFrameSize();
        return getAudioStreamFromBytes(audiodata, format, length);
    }
    
    /**
     * Возвращает AudioInputStream, пытаясь представить audiodata как поток
     * звковых данных с указанным форматом (звуковой файл). Невозможность
     * определить формат вызовет ошибку.
     * @param audiodata (массив байт аудиоданных)
     * @return new AudioInputStream (null при ошибке)
     */
    
    public static AudioInputStream getAudioStreamFromBytes(final byte[] audiodata){
        ByteArrayInputStream bis = new ByteArrayInputStream(audiodata); 
        AudioInputStream ais = null;
        
        try{
            ais = AudioSystem.getAudioInputStream(bis);
        }catch(UnsupportedAudioFileException ex){
            //log.warn("getAudioStreamFromBytes() error: {}", ex);
        }catch(IOException ex){
            //log.warn("getAudioStreamFromBytes() error:", ex);
        }

        return ais;
    }
    
    
    /**
     * Ускоряет аудиофрагмент преобразуя его sampleRate, так чтобы его длительность
     * соответствовала factor процентов от оригинала (factor=0.95 ускорит на 5%).
     * При этом звук значительно искажается.
     * @param audiodata (массив байт аудиоданных, не null)
     * @param format (формат аудиоданных)
     * @param factor (множитель длительности от оригинала)
     * @return byte[] audiodata
     */
    
    public static byte[] setRate(byte[] audiodata, AudioFormat format, float factor){
        if (audiodata==null) return null;
        
        byte[] encbuf = new byte[audiodata.length];
        byte[] buf = null;
        int buflen = 0;        
        float newRate = format.getFrameRate() * factor;
                
        //log.trace("{} speedUp({})", this, String.valueOf(factor) );
        
        AudioFormat targetFormat = new AudioFormat(
                format.getEncoding(),
                newRate,
                format.getSampleSizeInBits(),
                format.getChannels(),
                (format.getSampleSizeInBits()/8)*format.getChannels(),
                newRate,
                false
        );        
        AudioInputStream src = getAudioStreamFromBytes(audiodata, format);
        AudioInputStream dst = AudioSystem.getAudioInputStream(targetFormat, src);                
        
        try{
            buflen = dst.read(encbuf);
            buf = Arrays.copyOf(encbuf, buflen);
        }catch(IOException ex){
            log.trace("{} setRate() error: {}", name);
        }                
                
        return buf;        
    }                

    /**
     * Прореживает байтовый массив с аудиофреймами. Сохраняет keepFrames фреймов в новом потоке,
     * затем пропускает delFrames фреймов пока не достигнет конца. Возвращает массив сохраненных
     * аудиофреймов и возможные остатки байтов.
     * @param audiodata (массив байт аудиоданных, не null)
     * @param format (формат аудиоданных)
     * @param keepFrames (число сохраняемых фреймов)
     * @param delFrames (число удаляемых фреймов)
     * @return byte[] audiodate
     */
    
    public static byte[] decimateFrames(byte[] audiodata, AudioFormat format, int keepFrames, int delFrames){
        if (audiodata==null) return null;
                
        //log.trace("{} decimate() in={}", name, String.valueOf(audiodata.length));
        
        byte[] ret = null;
        final int frameSize = format.getFrameSize();
        final int copyBytes = keepFrames * frameSize;
        final int skipBytes = delFrames * frameSize;
        byte[] buf = new byte[copyBytes];
        
        try( ByteArrayInputStream in = new ByteArrayInputStream(audiodata);
                ByteArrayOutputStream out = new ByteArrayOutputStream(audiodata.length);)
        {
            
            while(true){
                //log.trace("{} decimate() in.available={} (copy={}, skip={})", name, String.valueOf(in.available()), String.valueOf(copyBytes), String.valueOf(skipBytes));
                // copy
                if (in.available() > copyBytes){                
                    in.read(buf, 0, copyBytes);
                    out.write(buf, 0, copyBytes);
                }else{
                    if (in.available() > frameSize){
                        //log.trace("{} decimate, copy tail={} (out.size={})", name, String.valueOf(in.available()), String.valueOf(out.size()));
                        for(int i=0; i<in.available(); i+=frameSize){
                            in.read(buf, 0, frameSize);
                            out.write(buf, 0, frameSize);
                        }
                    }
                    break;
                }

                // skip
                if (in.available() > skipBytes){                
                    in.skip(skipBytes);
                }else{
                    break;
                }                                
            }
            
            ret = out.toByteArray();
            
        }catch(IOException ex){
            log.trace("{} decimate error: {}", name, ex.toString());
        }        

        //log.trace("{} decimate() out={}", name, String.valueOf(ret.length));
        
        return ret;
    }

    /**
     * Прореживает audiodata, удаляя повторяющиеся сэмплы значением меньше, чем threshold. Поддерживает
     * размер сэмпла 8 или 16 бит на канал BigEndian или LittleEndian, Моно или Стерео.
     * @param audiodata (массив байт аудиоданных, не null)
     * @param format (формат аудиоданных)
     * @param threshold (порог значения аудиосигнала)
     * @return audiodata
     */
    
    public static byte[] decimateFramesThreshold(byte[] audiodata, AudioFormat format, int threshold){
        if (audiodata==null) return null;
                
        //log.trace("{} decimateFramesThreshold() in={}", name, String.valueOf(audiodata.length));
        
        final int frameSize = format.getFrameSize();
        final int channels = format.getChannels();
        final int sampleBits = format.getSampleSizeInBits();
        final boolean isBigEndian = format.isBigEndian();
        
        byte[] ret = null;        
        byte[] curFrame = new byte[frameSize];
        byte[] prevFrame = new byte[frameSize];
        
        boolean skipFrame = false;
        Arrays.fill(prevFrame, (byte)0);
        
        try( ByteArrayInputStream in = new ByteArrayInputStream(audiodata);
                ByteArrayOutputStream out = new ByteArrayOutputStream(audiodata.length);)
        {
            if (sampleBits==8){
                if (channels==1){
                    // 8-bit Mono
                    while(true){
                        if (in.read(curFrame, 0, frameSize) == -1) break;

                        short val = get16bitSampleValue(curFrame[0], curFrame[1], isBigEndian);

                        if (val < threshold){
                            if (!skipFrame) out.write(curFrame, 0, frameSize);
                            skipFrame = true;
                        }else{
                            out.write(curFrame, 0, frameSize);
                            skipFrame = false;
                        }                        
                     }                                
                }else if(channels==2){
                    // 8-bit Stereo
                    while(true){
                        if (in.read(curFrame, 0, frameSize) == -1) break;

                        short valLeft = get16bitSampleValue(curFrame[0], curFrame[1], isBigEndian);
                        short valRight = get16bitSampleValue(curFrame[2], curFrame[3], isBigEndian);

                        if (Math.abs(valLeft) < threshold && Math.abs(valRight) < threshold){
                            if (!skipFrame) out.write(curFrame, 0, frameSize);
                            skipFrame = true;
                        }else{
                            out.write(curFrame, 0, frameSize);
                            skipFrame = false;
                        }                        
                     }                                
                }
                
            }else  if (sampleBits==16){          
                if (channels==1){
                    // 16-bit Mono
                    while(true){
                        if (in.read(curFrame, 0, frameSize) == -1) break;

                        short val = get16bitSampleValue(curFrame[0], curFrame[1], isBigEndian);

                        if (val < threshold){
                            if (!skipFrame) out.write(curFrame, 0, frameSize);
                            skipFrame = true;
                        }else{
                            out.write(curFrame, 0, frameSize);
                            skipFrame = false;
                        }                        
                     }                                
                }else if(channels==2){
                    // 16-bit Stereo
                    while(true){
                        if (in.read(curFrame, 0, frameSize) == -1) break;

                        short valLeft = get16bitSampleValue(curFrame[0], curFrame[1], isBigEndian);
                        short valRight = get16bitSampleValue(curFrame[2], curFrame[3], isBigEndian);

                        if (Math.abs(valLeft) < threshold && Math.abs(valRight) < threshold){
                            if (!skipFrame) out.write(curFrame, 0, frameSize);
                            skipFrame = true;
                        }else{
                            out.write(curFrame, 0, frameSize);
                            skipFrame = false;
                        }                        
                     }                                
                }

            }else{
                log.error("{}  decimateFramesThreshold() error: {}", "supported 8 or 16-bit samples only");
            }

            ret = out.toByteArray();
            
        }catch(IOException ex){
            log.trace("{} decimateFramesThreshold error: {}", name, ex.toString());
        }        

        //log.trace("{} decimateFramesThreshold() out={}", name, String.valueOf(ret.length));
        
        return ret;
    }
        
    public static short get16bitSampleValue(byte b0, byte b1, boolean isBigEndian){
        short val = 0;

            if (isBigEndian){
                val = (short) ((b0 & 0xff) << 8 | b1 & 0xff);        
            }else{
                val = (short) (b0 & 0xff | (b1 & 0xff) << 8);                    
            }
        return val;
    }
    
    
    public static int getSubValueLinear(int v1, int v2, float cx){
        int k = v2 - v1;
        float cy = cx*k + v1;

        //log.trace("getSubValueLinear({},{}) x {} = {}", String.valueOf(v1), String.valueOf(v2), String.valueOf(cx), String.valueOf((int)cy));
        
        return (int)cy;        
    }
    
    
    public static int getSubValueNearest(int v1, int v2, float cx){
        if (cx < 0.5F){
                return v1;
        }
        return v2;
    }
            
    public static short[] getAudioFrame16bitStereo(byte[] audiodata, int frameIndex, boolean isBigEndian){
        final int channels = 2;
        final int frameSize = 4;
        
        int startBytesOffset = frameIndex * frameSize;
        short[] ret = new short[channels];

        log.trace("getAudioFrame16bitStereo({})", String.valueOf(frameIndex));
        //log.trace("getAudioFrame16bitStereo ({},{}) from {}", startBytesOffset, startBytesOffset+3, audiodata.length);
        
        ret[0] = get16bitSampleValue(audiodata[startBytesOffset], audiodata[startBytesOffset+1], isBigEndian);
        ret[1] = get16bitSampleValue(audiodata[startBytesOffset+2], audiodata[startBytesOffset+3], isBigEndian);                
        
        return ret;
    }

    public static byte[] short2Bytes(short s) {
            return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
    }    
    
    /**
     * Записывает в поток AudioSample учитывая нужен ли реверс порядка байт (reverseBytes).
     * Не обрабатывает исключения.
     * @param out (OutputStream)
     * @param sampleValue (16-bit audio sample value)
     * @param reverseBytes (reverse bytes flag)
     * @throws IOException (ошибка записи в OutputStream)
     */
    
    public static void writeAudioSample(OutputStream out, short sampleValue, boolean reverseBytes) throws IOException{
        byte[] ret = null;
        short val = 0;        
        
        if (reverseBytes){
            val = Short.reverseBytes(sampleValue);
        }
        
        ret = short2Bytes( sampleValue );
        out.write(ret, 0, 2);
        
        //log.trace("writeAudioSample({}) reverse={}", sampleValue, String.valueOf(reverseBytes));
    }
        
    
    public static byte[] resampleAudioDataRate(byte[] audiodata, AudioFormat format, float step){
        if (audiodata==null) return null;
        
        final int frameSize = format.getFrameSize();
        final int maxFrames = audiodata.length / frameSize;
        final int channels = format.getChannels();
        final int sampleBits = format.getSampleSizeInBits();
        final boolean isBigEndian = format.isBigEndian();        
        
        int bufSize = (int)(audiodata.length / step) + frameSize;
        byte[] ret = null;
        short[] audioSample = null;
        short[] audioSampleNext = null;
        
        int i = 0;
        float floatIndex = 0.0F;
        float offset = 0.0F;
        int frameIndex = 0;        
        
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(bufSize);){
            while((frameIndex+1) < maxFrames){
                floatIndex = step * i;
                frameIndex = (int)Math.floor(floatIndex);
                offset = floatIndex - frameIndex;
                
                if (Double.compare(0.0, offset)==0){
                    // точно попали в индекс, дополнительных вычислений не требуется
                    audioSample = getAudioFrame16bitStereo(audiodata, frameIndex, !isBigEndian);
                    writeAudioSample(out, audioSample[0], isBigEndian);
                    writeAudioSample(out, audioSample[1], isBigEndian);                    
                }else{
                    // мы между frameIndex и frameIndex+1, коэффициент смещения между ними равен offset
                    // происходит чтение следующих 2 фреймов начинающихся с frameIndex
                    // и вычисление промежуточных значений с учетом коэффициента offset
                    if ((frameIndex+2) >= maxFrames ) break;
                    
                    audioSample = getAudioFrame16bitStereo(audiodata, frameIndex, isBigEndian);
                    audioSampleNext = getAudioFrame16bitStereo(audiodata, frameIndex+1, isBigEndian);
                    writeAudioSample(out, (short)getSubValueLinear(audioSample[0], audioSampleNext[0], offset), !isBigEndian);
                    writeAudioSample(out, (short)getSubValueLinear(audioSample[1], audioSampleNext[1], offset), !isBigEndian);
                }                
                
                i++;
            }            
            ret = out.toByteArray();
        }catch(IOException ex){
            log.trace("{} resampleAudioDataRate() error: {}", name, ex.toString());
        }        
        
        return ret;
    }
    
    
    public static byte[] cycle(byte[] audiodata, AudioFormat format){
        //if (true) return audiodata;
        
        
        ByteOrder bufOrder = ByteOrder.LITTLE_ENDIAN;
        if (format.isBigEndian()) bufOrder = ByteOrder.BIG_ENDIAN;
        
        ByteBuffer buf = ByteBuffer.wrap(audiodata);        
        buf.order(bufOrder);
        
        ByteBuffer res = ByteBuffer.allocate(audiodata.length);
        res.order(bufOrder);
                
        
        short v0,v1;
        short p0 = 0, p1 = 0;
        int d0=0, d1=0;
        
        int vmax=0,vmaxd=0;
        int i=0;

        int offset = 150;
        
        while(buf.remaining()>=4){        
            v0 = buf.getShort();
            v1 = buf.getShort();
              
            if (v0 > 0){
                if (v0 > offset){
                    v0 -= offset;
                }else{
                    v0 = 0;
                }
            }else if (v0 < 0){
                if (v0 < offset){
                    v0 += offset;
                }else{
                    v0 = 0;
                }
            }
            
            if (v1 > 0){
                if (v1 > offset){
                    v1 -= offset;
                }else{
                    v1 = 0;
                }
            }else if (v1 < 0){
                if (v1 < offset){
                    v1 += offset;
                }else{
                    v1 = 0;
                }
            }

            res.putShort(v0);
            res.putShort(v1);
            
            d0 = v0 - p0;
            
            p0 = v0;
            p1 = v1;
            i++;
            /*
            if (i%100==0){
                log.trace("v0 = %5d, d0 = %5d", v0, d0);
            }
            */
        }
                
        res.flip();
        
        /*
        log.trace("pos=( %5d, %5d )( %5d ) neg = ( %5d, %5d )( %5d ) ",
                leftPosStat.getMin(), leftPosStat.getMax(), leftPosStat.getAvg(),
                leftNegStat.getMin(), leftNegStat.getMax(), leftNegStat.getAvg() );                        
        */
        
        //log.trace("min = %5d, avg = %5d, max = %5d", leftStat.getMin(), leftStat.getAvgMath(), leftStat.getMax());
        
        return res.array();
    }

    public static int toUnsigned(short value){
        return (int)value + 0x8000;
    }
    
    
    public static short toSignedShort(int value){
        return (short)(value - 0x8000);
    }    

    
    public static byte[] gain(byte[] audiodata, AudioFormat format, float gain){
        
        ByteOrder bufOrder = ByteOrder.LITTLE_ENDIAN;
        if (format.isBigEndian()) bufOrder = ByteOrder.BIG_ENDIAN;
        
        ByteBuffer buf = ByteBuffer.wrap(audiodata);        
        buf.order(bufOrder);
        
        ByteBuffer res = ByteBuffer.allocate(audiodata.length);
        res.order(bufOrder);
                
        
        short v0,v1, p0=0,p1=0;
        int vmax=0,vmaxd=0;
        int i=0;
        
        while(buf.remaining()>=4){        
            v0 = buf.getShort();
            v1 = buf.getShort();
            
            v0 *= gain;
            v1 *= gain;
            
            res.putShort(v0);
            res.putShort(v1);
            
        }
                
        res.flip();
        return res.array();
    }

    /**
     * Возвращает аудиобуфер заполненный нулевыми значениями
     * @param format (формат создаваемых аудиоданных)
     * @param frames (длительность во фреймах)
     * @return audiodata (массив нулевых значений)
     */
    
    public static byte[] getSilence(AudioFormat format, int frames){
        byte[] buf = new byte[frames * format.getFrameSize()];        
        Arrays.fill(buf, (byte)0);        
        return buf;
    }

    
}
