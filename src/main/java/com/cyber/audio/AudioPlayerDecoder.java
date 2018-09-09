/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio;

import com.cyber.audio.codec.*;
import javax.sound.sampled.AudioFormat;

/**
 *
 * @author CyberManic
 */
public class AudioPlayerDecoder extends AudioPlayer{

    AudioCodec decoder;
    
    public AudioPlayerDecoder(AudioFormat format, AudioCodecType decoder){
        super(format);        
        this.decoder = AudioCodecFactory.getCodec(decoder);
    }

    @Override
    public String getFullName(){
        return String.format("%s [%s]", this.toString(), decoder.getFullName());        
    }
    
    @Override
    public boolean playDataChunk(byte[] audiodata){
        byte[] buf = decoder.decode(audiodata);
        if (buf!=null){           
            return super.playDataChunk(buf);
        }
        return false;
    }

    
    
}
