/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

import java.util.Arrays;
import java.util.zip.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuffmanCodec implements AudioCodec{
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final AudioFormat format;
    
    protected final Deflater compresser;
    protected final Inflater decompresser;
    
    public HuffmanCodec(AudioFormat format){
        this.format = format;
        compresser = new Deflater(Deflater.HUFFMAN_ONLY, true);
        decompresser = new Inflater(true);        
        //log.trace("{} created", this.getFullName());
    }

    public String getFullName(){
        return this.toString() + " []";
    }    
    
    public String toString(){
        return "Huffman AudioCodec";
    }
                
    public byte[] encode(byte[] input){
        byte[] output = input;
        output = encode_huffman(output);
        
        //System.out.printf("Encoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }

    
    public byte[] decode(byte[] input){
        byte[] output = input;
        output = decode_huffman(output);
        
        //System.out.printf("Decoded %d -> %d bytes%n", input.length, output.length);
        
        return output;
    }

    
    public byte[] encode_huffman(byte[] input){
        byte[] output = null;
        byte[] buf = new byte[4096];
        
        // Compress the bytes        
        compresser.reset();
        compresser.setInput(input);
        compresser.finish();
        int compressedDataLength = compresser.deflate(buf);

        output = Arrays.copyOf(buf, compressedDataLength);
        
        return output;
    }
    
    public byte[] decode_huffman(byte[] input){
        byte[] output = null;
        byte[] buf = new byte[4096];
        
        try {
            // Decompress the bytes
            decompresser.reset();
            decompresser.setInput(input, 0, input.length);
            int resultLength = decompresser.inflate(buf);

            output = Arrays.copyOf(buf, resultLength);

        } catch (DataFormatException ex) {
            output = null;            
        }
        
        return output;
    }
        
}
