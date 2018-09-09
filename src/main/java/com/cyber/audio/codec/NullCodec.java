/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullCodec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;

    public NullCodec(AudioFormat format){
        this.format = format;
        //log.trace("{} created", this.getFullName());
    }

    public String getFullName(){
        return this.toString() + " []";
    }    
    
    public String toString(){
        return "Null AudioCodec";
    }

    public byte[] encode(byte[] input){
        return input;
    }
    
    public byte[] decode(byte[] input){
        return input;
    }
    
}
