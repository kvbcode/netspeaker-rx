/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import com.cyber.io.*;

import javax.sound.sampled.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delta2Codec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    protected final ByteOrder bufOrder;
    protected final AudioCodec huffman = AudioCodecFactory.getCodec(AudioCodecType.HUFFMAN);
    protected final int valueBits = 10;
    protected final int deltaBits = 6;
    protected final static float scale10Mult = (float)((1 << 10)-1) / ((1 << 16)-1);    
    protected final static float scale9Mult = (float)((1 << 9)-1) / ((1 << 16)-1);    
    protected final static float scale8Mult = (float)((1 << 8)-1) / ((1 << 16)-1);    
    protected final static float scale7Mult = (float)((1 << 7)-1) / ((1 << 16)-1);    
    protected final static float scale6Mult = (float)((1 << 6)-1) / ((1 << 16)-1);    
    
    
    public Delta2Codec(AudioFormat format){
        this.format = format;
        //log.trace("{} created", this.getFullName());
        //log.trace("scaleMult = {}", scaleMult);
        
        if (format.isBigEndian()){
            this.bufOrder = ByteOrder.BIG_ENDIAN;        
        }else{
            this.bufOrder = ByteOrder.LITTLE_ENDIAN;            
        }
    }

    public String getFullName(){
        StringBuilder sb = new StringBuilder();
        sb.append( this.toString() );
        sb.append(" [");
        sb.append(valueBits + "bit");
        sb.append(" ");        
        if (format.getChannels()==1){
            sb.append("mono");
        }else if (format.getChannels()==2){
            sb.append("stereo");
        }
        sb.append(" ");        
        sb.append("signed");        
        sb.append(" ");        
        sb.append("huffman");        
        sb.append("]");
        return sb.toString();
    }    
    
    public String toString(){
        return "Delta2 AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] buf = input;
        buf = encode_delta2_stereo(buf);
        buf = huffman.encode(buf);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }

    
    public byte[] decode(byte[] input){
        byte[] buf = input;
        buf = huffman.decode(buf);
        buf = decode_delta2_stereo(buf);
        
        //System.out.printf("Decoded %d -> %d bytes%n", input.length, buf.length);
        
        return buf;
    }
    

    protected ByteBuffer encodeChannelBits(ByteBuffer in, float stepValue){
        OutputBitStream out = new OutputBitStream( 4 + in.limit() );
        
        if (stepValue<1.0F) stepValue = 1.0F;
        
        out.write( 16, in.limit() );
        out.write( 16, (int)(stepValue*100) );
        
        float prev = 0;
        float delta = 0;
        float v = 0;
        float result = 0;
        int i = 0;
        
        while( in.hasRemaining()){
            v = in.getShort();
            
            if (i%2==0){
                result = v * scale10Mult;
                out.writeSignedValue(valueBits, (int)result);                            
            }else{
                result = v * scale6Mult;
                //delta = v - prev;
                //result = delta * scale7Mult;
                //result = delta / stepValue;
                out.writeSignedValue(deltaBits, (int)result);                            
            }
            prev = v;
            i++;
        }

        return out.toByteBuffer();
    }

    
    protected ByteBuffer decodeChannelBits( ByteBuffer in ){        
        InputBitStream ibs = new InputBitStream( in );

        int decDataSize = ibs.read(16);
        float stepValue = (float)ibs.read(16) / 100;
        
        //log.trace("decode(), alloc = {}, startPos = {}, startNeg = {}", decDataSize, startPositive, startNegative);        
        //if (stepValue > 1.0) log.trace("dataSize = {}, stepValue = {}", decDataSize, stepValue );
        
        ByteBuffer out = ByteBuffer.allocate( decDataSize );        

        float v = 0;
        float prev = 0;
        float result = 0;
        int i=0;        
        
        while( !ibs.eof() && out.hasRemaining() ){            
            
            if (i%2==0){
                v = ibs.readSignedValue(valueBits);
                result = v / scale10Mult;
            }else{
                v = ibs.readSignedValue(deltaBits);
                result = v / scale6Mult;
                //result = v * stepValue;
                //result = prev + result;
            }
            out.putShort((short)result);                
            prev = result;
            i++;
        }
        
        out.flip();
        return out;
    }
    
        
    
    public byte[] encode_delta2_stereo(byte[] input){
        
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);

        int channels = 2;
        int channelDataLength = input.length / channels;
        
        ByteBuffer outLeft = ByteBuffer.allocate(channelDataLength);
        outLeft.order(bufOrder);

        ByteBuffer outRight = ByteBuffer.allocate(channelDataLength);
        outRight.order(bufOrder);
                
        short v0,v1;
        short p0=0,p1=0;
        int deltaLeft = 0;
        int deltaRight = 0;
        int maxDeltaLeft = 0;
        int maxDeltaRight = 0;
        int i = 0;
        
        // разделяем данные на 2 канала и собираем статистику
        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();
            
            if (i%2==1){
                deltaLeft = v0 - p0;
                deltaRight = v1 - p1;
                maxDeltaLeft = Math.max(maxDeltaLeft, deltaLeft);
                maxDeltaRight = Math.max(maxDeltaRight, deltaRight);
            }                        
                        
            outLeft.putShort(v0);
            outRight.putShort(v1);                   
            
            p0 = v0;
            p1 = v1;
            i++;
        }        
                
        float deltaRange = 1<<(deltaBits-1) - 1;
        float deltaStepLeft = (float)maxDeltaLeft / deltaRange;
        float deltaStepRight = (float)maxDeltaRight / deltaRange;
        
        //log.trace("maxDeltaLeft = {}, range = {}, step = {}", maxDeltaLeft, deltaRange, deltaStepLeft);
        
        outLeft.rewind();        
        ByteBuffer encLeft = encodeChannelBits( outLeft, deltaStepLeft );
        
        outRight.rewind();        
        ByteBuffer encRight = encodeChannelBits( outRight, deltaStepRight);
        
        ByteBuffer out = ByteBuffer.allocate( encLeft.limit() + encRight.limit());
        out.order(bufOrder);
        
        out.put( encLeft );
        out.put( encRight );
        
        out.flip();   
        return out.array();
    }
        
        
    public byte[] decode_delta2_stereo(byte[] input){
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        int channels = 2;
        int channelDataLength = input.length / channels;
                
        // locate left channel data area in buffer
        ByteBuffer inLeft = in.slice();
        inLeft.order(in.order());
        inLeft.limit( channelDataLength );
        inLeft.rewind();
        
        // locate right channel data area in buffer
        in.position( channelDataLength );
        ByteBuffer inRight = in.slice();       
        inRight.order(in.order());
        inRight.limit( channelDataLength );
        inRight.rewind();
        
               
        //log.trace("L={},{}, R={},{}, IN={}", inLeft.limit(), inLeft.order(), inRight.limit(), inRight.order(), in.order());
        
        ByteBuffer decLeft = decodeChannelBits(inLeft);
        ByteBuffer decRight = decodeChannelBits(inRight);

        ByteBuffer out = ByteBuffer.allocate( decLeft.limit() + decRight.limit() );
        out.order(bufOrder);        
        
        short v0,v1;
        while(decLeft.hasRemaining() && decRight.hasRemaining()){
            v0 = decLeft.getShort();
            v1 = decRight.getShort();
            
            out.putShort(v0);
            out.putShort(v1);
        }
        
        out.flip();        
        return out.array();
    }   

    
}
