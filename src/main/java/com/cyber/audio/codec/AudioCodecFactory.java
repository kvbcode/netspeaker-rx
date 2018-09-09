/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioCodecFactory {
    protected static final String name = "AudioCodecFactory";
    protected static final Logger log = LoggerFactory.getLogger(name);        
    protected static AudioFormat format = null;
    
    public static void setAudioFormat(AudioFormat newAudioFormat){
        format = newAudioFormat;
    }
        
    public static AudioCodec getCodec(int codec_id){
        return getCodec(AudioCodecType.getById(codec_id));
    }
    
    public static AudioCodec getCodec(AudioCodecType codec){
        AudioCodec ret = null;
        switch(codec){
            case NULL:
                return new NullCodec(format);
            case ULAW:
                return new ULawCodec(format);
            case R16B12:
                return new R16B12Codec(format);
            case R16B12H:
                return new R16B12HCodec(format);
            case EVENSUM:
                return new EvenSumCodec(format);
            case EVENSUM128:
                return new EvenSum128Codec(format);
            case HUFFMAN:
                return new HuffmanCodec(format);
            case DEFLATE:
                return new DeflateCodec(format);
            case DELTA2:
                return new Delta2Codec(format);
            case LZF:
                //return new LzfCodec(format);
            case SPEEX:
                //return new SpeexCodec(format);
            default:
                log.warn("AudioCodecFactory getCodec() error: Unknown codec #{}", name, String.valueOf(codec.ordinal()));
        }
        return ret;
    }

}
