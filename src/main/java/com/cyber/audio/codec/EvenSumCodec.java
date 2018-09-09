/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvenSumCodec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    protected final ByteOrder bufOrder;

    // отношение 12 битныйх значений к 16 битным
    protected final double mult = (Math.pow(2, 12+1)-1) / (Math.pow(2, 16+1)-1);    
    
    public EvenSumCodec(AudioFormat format){
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
            ret += " [mono]";
        }else if (format.getChannels()==2){
            ret += " [stereo]";
        }
        return ret;
    }    
    
    public String toString(){
        return "EvenSum AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] output = input;
        output = encode_evensum_stereo(output);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }

    
    public byte[] decode(byte[] input){
        byte[] output = input;
        output = decode_evensum_stereo(output);
        
        System.out.printf("Decoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }
    
    
    public byte[] encode_evensum_stereo(byte[] input){
        
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length /2);
        out.order(bufOrder);
        
        short v0,v1, p0=0,p1=0;
        int i=0;
        
        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();
            
            if (i%2!=0){
                out.putShort((short)((v0+p0)/2));
                out.putShort((short)((v1+p1)/2));
            }else{
                //out.putShort((short)((v0+p0)/2));
                //out.putShort((short)((v1+p1)/2));                
            }
                        
            // store previous values
            p0 = v0;
            p1 = v1;
            i++;
        }
                
        out.flip();   
        return out.array();
    }
        
        
    public byte[] decode_evensum_stereo(byte[] input){
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length * 2);
        out.order(bufOrder);
        
        short v0,v1, p0=0,p1=0, pp0=0, pp1=0;
        int i=0;
        
        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();

            // здесь выводится основной фронт волны (большая часть энергии)
            out.putShort((short)(v0*1.25));
            out.putShort((short)(v1*1.25));                        
                        
            
            // здесь выводится затухание (остаток)
            // можно опустить значения до нулей, но тогда слышны "металлические" ноты и элетропомехи
            // при увеличении частоты до 48kHz помех почти не слышно
            // если превысить 2*v вместе с фронтом, то громкость будет увеличена и наоборот
            out.putShort((short)(v0));
            out.putShort((short)(v1));            
            
            
            //log.trace("{},{}", v0,v1);
            i++;
        }
                
        out.flip();        
        return out.array();
    }   

    
    public byte[] packShorts12bits(short n1, short n2){        
        int i1 = toUnsigned(n1);
        int i2 = toUnsigned(n2);
        
        i1 = (int)((double)i1 * mult);
        i2 = (int)((double)i2 * mult);
                        
        int ret = ((i1&0xFFF)<<12 | i2 & 0xFFF);
        
        byte[] buf = new byte[3];
        
        buf[0] = (byte)(ret & 0xFF);
        buf[1] = (byte)((ret>>8) & 0xFF);
        buf[2] = (byte)((ret>>16) & 0xFF);
                        
        return buf;
    }
    
    
    public short[] unpackShorts12bits(byte[] buf){        
        int input = buf[0] & 0xFF | ((buf[1] & 0xFF)<<8) | ((buf[2] & 0xFF)<<16);        
        
        int i1 =((input >> 12)  & 0xFFF);
        int i2 =(input  & 0xFFF);
        
        i1 = (int)((double)i1 / mult);
        i2 = (int)((double)i2 / mult);        
                
        short[] ret = new short[2];
                
        ret[0] = toSignedShort(i1);
        ret[1] = toSignedShort(i2);
        
        return ret;
    }        

    public int toUnsigned(short value){
        return (int)value + 0x8000;
    }
    
    
    public short toSignedShort(int value){
        return (short)(value - 0x8000);
    }    
           
}
