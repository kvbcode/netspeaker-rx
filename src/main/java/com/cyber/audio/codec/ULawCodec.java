/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ULawCodec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat decFormat;
    protected AudioFormat encFormat;
    
    protected final int MAX_BUFFER_SIZE = 4096;
    
    protected byte[] encbuf = new byte[MAX_BUFFER_SIZE];
    protected byte[] decbuf = new byte[MAX_BUFFER_SIZE];

    public ULawCodec(AudioFormat format){
        this.decFormat = format;
        this.encFormat = setDestinationAudioFormat(format);
        
        //log.trace("{} created", this.getFullName());
        
        if (AudioSystem.isConversionSupported(decFormat, encFormat)){
            log.trace("conversion with {} supported", this);
        }else{
            log.warn("conversion with {} is NOT supported", this);
        }
    }
    
    public String toString(){
        return "uLaw AudioCodec";
    }

    public String getFullName(){
        return String.format("%s [%s]", this.toString(), encFormat.toString());
    }    

    protected AudioFormat setDestinationAudioFormat(AudioFormat format){

        AudioFormat.Encoding encoding = AudioFormat.Encoding.ULAW;
        float rate = format.getFrameRate();
        int sampleSize = 8;
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        encFormat = new AudioFormat( encoding, rate, sampleSize, channels, (sampleSize/8)*channels, rate, bigEndian );
        
        return encFormat;
    }    
    
    public byte[] encode(byte[] input){
        byte[] buf = null;
        int buflen = 0;
        
        AudioInputStream src = getAudioStreamFromBytes(input, decFormat);
        AudioInputStream dst = AudioSystem.getAudioInputStream(encFormat, src);                
        
        try{
            buflen = dst.read(encbuf);
            buf = Arrays.copyOf(encbuf, buflen);
        }catch(IOException ex){
            log.trace("{} encode() error: {}", this, ex.toString());
        }                
                
        return buf;
    }
    
    public byte[] decode(byte[] input){
        byte[] buf = null;
        int buflen = 0;
        
        AudioInputStream src = getAudioStreamFromBytes(input, encFormat);
        AudioInputStream dst = AudioSystem.getAudioInputStream(decFormat, src);                
        
        try{
            buflen = dst.read(decbuf);
            buf = Arrays.copyOf(decbuf, buflen);
        }catch(IOException ex){
            log.trace("{} decode() error: {}", this, ex.toString());
        }                
        
        return buf;
    }

    protected AudioInputStream getAudioStreamFromBytes(final byte[] audiodata, AudioFormat format, long length){
        ByteArrayInputStream bis = new ByteArrayInputStream(audiodata); 
        AudioInputStream ais = new AudioInputStream(bis, format, length);

        return ais;
    }

    protected AudioInputStream getAudioStreamFromBytes(final byte[] audiodata, AudioFormat format){
        long length = audiodata.length / format.getFrameSize();
        return getAudioStreamFromBytes(audiodata, format, length);
    }
    
    
}
