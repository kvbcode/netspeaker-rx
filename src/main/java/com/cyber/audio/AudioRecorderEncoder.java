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
public class AudioRecorderEncoder extends AudioRecorder{

    AudioCodec encoder;
    
    public AudioRecorderEncoder(AudioFormat format, AudioCodecType encoder){
        super(format);
        this.encoder = AudioCodecFactory.getCodec(encoder);
    }

    public String getFullName(){
        return String.format("%s [%s]", this.toString(), encoder.getFullName());
    }    
    
    @Override
    public byte[] pollDataChunk(){
        byte[] buf = super.pollDataChunk();
        if (buf!=null){
            buf = encoder.encode(buf);
        }
        return buf;
    }

    @Override
    public byte[] takeDataChunk(){
        byte[] buf = super.takeDataChunk();
        if (buf!=null){
            buf = encoder.encode(buf);
        }
        return buf;
    }
    
    
}
