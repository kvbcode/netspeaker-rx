/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvenSum128Codec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    protected final ByteOrder bufOrder;

    // отношение 12 битныйх значений к 16 битным
    protected static final double MULT = (Math.pow(2, 12+1)-1) / (Math.pow(2, 16+1)-1);    
    
    
    public EvenSum128Codec(AudioFormat format){
        this.format = format;
        //log.trace("{} created", this.getFullName());
        if (format.isBigEndian()){
            this.bufOrder = ByteOrder.BIG_ENDIAN;        
        }else{
            this.bufOrder = ByteOrder.LITTLE_ENDIAN;            
        }
        
    }

    public String getFullName(){
        String ret = this.toString();
        if (format.getChannels()==1){
            ret += " [12+8bit mono unsigned]";
        }else if (format.getChannels()==2){
            ret += " [12+8bit stereo unsigned]";
        }
        return ret;
    }    
    
    public String toString(){
        return "EvenSum128 AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] buf = input;
        buf = encode_evensum_128(buf);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }

    
    public byte[] decode(byte[] input){
        byte[] buf = input;
        buf = decode_evensum_128(buf);
        
        //System.out.printf("Decoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }

    
    public byte[] encode_evensum_128(byte[] input){
        
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length);
        out.order(bufOrder);
        
        short v0,v1, p0=0,p1=0;
        int i=0;
        
        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();
            
            if (i%2==0){
                out.put(packShorts12bits(v0, v1));
            }else{
                out.put((byte)((v0-p0)/2));
                out.put((byte)((v1-p1)/2));
            }
                        
            // store previous values
            p0 = v0;
            p1 = v1;
            i++;
        }

        out.flip();           
        return Arrays.copyOf(out.array(), out.limit());
    }
        
        
    public byte[] decode_evensum_128(byte[] input){
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length * 2);
        out.order(bufOrder);
        
        byte[] buf = new byte[3];
        short[] vbuf = new short[2];
        short v0=0,v1=0, p0=0,p1=0, pp0=0, pp1=0;
        int i=0;
        
        while(in.remaining()>=2){        
            
            if (i%2==0){
                buf[0] = in.get();
                buf[1] = in.get();
                buf[2] = in.get();
                
                vbuf = unpackShorts12bits(buf);
                v0 = vbuf[0];
                v1 = vbuf[1];                
                
                
                out.putShort(v0);
                out.putShort(v1);
            }else{
                v0 = (short)(in.get()*2 + p0);
                v1 = (short)(in.get()*2 + p1);

                out.putShort(v0);
                out.putShort(v1);
            }
                        
            // store previous values
            p0 = v0;
            p1 = v1;
            i++;

        }
                
        out.flip();           
        return Arrays.copyOf(out.array(), out.limit());
    }   

    protected static int toUnsigned(short value){
        return (int)value + 0x8000;
    }
    
    
    protected static short toSignedShort(int value){
        return (short)(value - 0x8000);
    }    
    
    
    protected static byte[] packShorts12bits(short n1, short n2){        
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
    
    
    protected static short[] unpackShorts12bits(byte[] buf){        
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
