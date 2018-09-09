/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R16B12HCodec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    protected final AudioCodec r16b12 = AudioCodecFactory.getCodec(AudioCodecType.R16B12);
    protected final AudioCodec huffman = AudioCodecFactory.getCodec(AudioCodecType.HUFFMAN);
    
    
    public R16B12HCodec(AudioFormat format){
        this.format = format;
        //log.trace("{} created", this.getFullName());
    }

    public String getFullName(){
        String ret = this.toString();
        if (format.getChannels()==1){
            ret += " [12bit mono unsigned huffman]";
        }else if (format.getChannels()==2){
            ret += " [12bit stereo unsigned huffman]";
        }
        return ret;
    }    
    
    public String toString(){
        return "R16B12H AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] buf = input;
        buf = r16b12.encode(buf);
        buf = huffman.encode(buf);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }

    
    public byte[] decode(byte[] input){
        byte[] buf = input;
        buf = huffman.decode(buf);
        buf = r16b12.decode(buf);
        
        //System.out.printf("Decoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }
    
    
}
