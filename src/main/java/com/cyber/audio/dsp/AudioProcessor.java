/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.dsp;

import com.cyber.storage.IProperties;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public abstract class AudioProcessor extends Object{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final ByteOrder bufOrder;
    protected final String name = this.getClass().getSimpleName();
    protected IProperties properties = null;

    
    public AudioProcessor(AudioFormat format){
        if (format.isBigEndian()){
            this.bufOrder = ByteOrder.BIG_ENDIAN;        
        }else{
            this.bufOrder = ByteOrder.LITTLE_ENDIAN;            
        }                
    }

    
    @Override
    public String toString(){
        return name;
    }

    
    public String getFullInfo(){
        return this.toString();
    }

    
    public void setProperties(IProperties properties){
        this.properties = properties;        
    }

    
    public byte[] proceed(byte[] input){
        // main routine        
        return input;
    }
    
}
