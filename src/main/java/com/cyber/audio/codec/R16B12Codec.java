/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R16B12Codec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    
    // отношение 12 битныйх значений к 16 битным
    public static final double MULT = (Math.pow(2, 12+1)-1) / (Math.pow(2, 16+1)-1);    

    public R16B12Codec(AudioFormat format){
        this.format = format;
        //log.trace("{} created", this.getFullName());
    }

    public String getFullName(){
        String ret = this.toString();
        if (format.getChannels()==1){
            ret += " [12bit mono unsigned]";
        }else if (format.getChannels()==2){
            ret += " [12bit stereo unsigned]";
        }
        return ret;
    }    
    
    public String toString(){
        return "R16B12 AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] output = input;
        output = encode_r16b12(output);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }

    
    public byte[] decode(byte[] input){
        byte[] output = input;
        output = decode_r16b12(output);
        
        //System.out.printf("Decoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }
    
    
    public byte[] encode_r16b12(byte[] input){
        byte[] output = null;              
        byte[] buf;                        
        
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(input));
                ByteArrayOutputStream rawout = new ByteArrayOutputStream(input.length);
                DataOutputStream out = new DataOutputStream(rawout);)
        {
            while(in.available()>=4){
                short left;
                short right;
                
                if (format.isBigEndian()){
                    left = in.readShort();
                    right = in.readShort();
                }else{
                    left = Short.reverseBytes(in.readShort());
                    right = Short.reverseBytes(in.readShort());
                }

                //System.out.printf("%d, %d\t%n", left, right);
                
                buf = packShorts12bits(left, right);
                                
                // Transfer left12bits + right12bits = 3 bytes
                out.writeByte(buf[0]);
                out.writeByte(buf[1]);
                out.writeByte(buf[2]);
            }
            output = rawout.toByteArray();
        }catch(IOException ex){
            log.trace("{} error: {}", this, ex.toString());
        }
        
        return output;
    }
    
    
    public byte[] decode_r16b12(byte[] input){
        byte[] output = null;
        byte[] buf = new byte[3];
        
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(input));
                ByteArrayOutputStream rawout = new ByteArrayOutputStream(input.length);
                DataOutputStream out = new DataOutputStream(rawout);){
            while(in.available()>=3){
                buf[0] = in.readByte();
                buf[1] = in.readByte();
                buf[2] = in.readByte();
                
                short[] audiodata = unpackShorts12bits(buf);
                short left = audiodata[0];
                short right = audiodata[1];
                
                //System.out.printf("%d, %d\t%n", left, right);                
                
                if (format.isBigEndian()){
                    out.writeShort(left);
                    out.writeShort(right);
                }else{
                    out.writeShort(Short.reverseBytes(left));
                    out.writeShort(Short.reverseBytes(right));                    
                }
                
            }
            output = rawout.toByteArray();
        }catch(IOException ex){
            log.trace("{} error: {}", this, ex.toString());
        }
                
        return output;
    }

    
    public static int toUnsigned(short value){
        return (int)value + 0x8000;
    }
    
    
    public static short toSignedShort(int value){
        return (short)(value - 0x8000);
    }    
    
    
    public static byte[] packShorts12bits(short n1, short n2){        
        int i1 = toUnsigned(n1);
        int i2 = toUnsigned(n2);
        
        i1 = (int)((double)i1 * MULT);
        i2 = (int)((double)i2 * MULT);
                        
        int ret = ((i1&0xFFF)<<12 | i2 & 0xFFF);
        
        byte[] buf = new byte[3];
        
        buf[0] = (byte)(ret & 0xFF);
        buf[1] = (byte)((ret>>8) & 0xFF);
        buf[2] = (byte)((ret>>16) & 0xFF);
                        
        return buf;
    }
    
    
    public static short[] unpackShorts12bits(byte[] buf){        
        int input = buf[0] & 0xFF | ((buf[1] & 0xFF)<<8) | ((buf[2] & 0xFF)<<16);        
        
        int i1 =((input >> 12)  & 0xFFF);
        int i2 =(input  & 0xFFF);
        
        i1 = (int)((double)i1 / MULT);
        i2 = (int)((double)i2 / MULT);        
                
        short[] ret = new short[2];
                
        ret[0] = toSignedShort(i1);
        ret[1] = toSignedShort(i2);
        
        return ret;
    }        
    
}
