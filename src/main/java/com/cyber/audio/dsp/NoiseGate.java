/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.dsp;

import com.cyber.storage.IProperties;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 *
 * @author CyberManic
 */
public class NoiseGate extends AudioProcessor{
    protected final short threshold;
    protected short lastMaxOutValue = 0;
    
    
    public NoiseGate(AudioFormat format, int threshold){
        super(format);
        this.threshold = (short)threshold;        
    }

    
    @Override
    public String toString(){
        return name + "(" + threshold + ")";
    }

    @Override
    public void setProperties(IProperties properties){
        super.setProperties(properties);
    }
    
    @Override
    public byte[] proceed(byte[] input){                
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length);
        out.order(bufOrder);
        
        short v0,v1;
        lastMaxOutValue = 0;

        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();
                     
            v0 = signalOffset(v0, threshold);
            v1 = signalOffset(v1, threshold);

            lastMaxOutValue = v0 > lastMaxOutValue? v0: lastMaxOutValue;            
            lastMaxOutValue = v1 > lastMaxOutValue? v1: lastMaxOutValue;

            out.putShort(v0);
            out.putShort(v1);            
        }
        
        out.flip();           
        return out.array();        
    }
    

    protected static short signalOffset(short val, short offset){
        if (val > 0){
            if (val > offset){
                val -= offset;
            }else{
                val = 0;
            }
        }else if (val < 0){
            if (val < -offset){
                val += offset;
            }else{
                val = 0;
            }
        }
        return val;
    }
    
    
    public int getLastOutValue(){
        return lastMaxOutValue;
    }
    
    
    public float getThreshold(){
        return threshold;
    }
    
}
