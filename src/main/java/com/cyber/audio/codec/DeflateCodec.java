/**
 * DeflateCodec.java
 *
 * Created on 22-07-2013 10:41 PM
 *
 */
package com.cyber.audio.codec;

import java.io.*;
import java.util.zip.*;
import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeflateCodec implements AudioCodec{
        protected final Logger log = LoggerFactory.getLogger(this.getClass());
	protected AudioFormat format;    

        protected final int MAX_BUFFER_SIZE = 4096;

        
	public DeflateCodec(AudioFormat format){            
            //log.debug("{} created", this.getFullName());
	}
	
	public String toString(){
            return "DeflateAudioCodec";
	}
	
        public String getFullName(){
            return this.toString() + " []";
        }
        
        
	public byte[] encode(final byte[] input){		
            byte[] encoded = null;

            Deflater compressor = new Deflater();
            compressor.setLevel(Deflater.BEST_SPEED);

            compressor.setInput(input);
            compressor.finish();

            try(ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);){
                byte[] buf = new byte[MAX_BUFFER_SIZE];
                while (!compressor.finished()) {
                    int count = compressor.deflate(buf);
                    bos.write(buf, 0, count);
                }
                bos.close();
                encoded = bos.toByteArray();		
            }catch(IOException ex){
                log.trace("{} error: {}", this, ex.toString());
            }

            return encoded;
	}
	
	
	public byte[] decode(final byte[] input){
            byte[] decoded = null;

            Inflater decompressor = new Inflater();
            decompressor.setInput(input);

            try(ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length*2)){
                byte[] buf = new byte[MAX_BUFFER_SIZE];
                while (!decompressor.finished()) {
                    int count = decompressor.inflate(buf);
                    bos.write(buf, 0, count);
                }
                bos.close();
                decoded = bos.toByteArray();
            }catch(IOException ex){
                log.trace("{} error: {}", this, ex.toString());
            }catch(DataFormatException ex){
                log.trace("{} error: {}", this, ex.toString());
            }	

            return decoded;
	}
	
}
