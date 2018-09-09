/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.io;

import com.cyber.array.ByteArray;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
/**
 *
 * @author CyberManic
 */
public class InputStreamScanner{
    private final InputStream in;
    private static final int DEFAULT_BUFSIZE = 4096;
    private byte[] delim;
    private boolean EOF = false;
    private final ByteArray out = new ByteArray();
    
    public InputStreamScanner(InputStream input, byte[] delim){
        this.in = input;
        this.delim = delim;
    }

    public InputStreamScanner(InputStream input){
        this(input, "\n".getBytes());
    }
        
    public void setEOF(boolean val){
        EOF = val;
    }
    
    public boolean isEOF(){
        return EOF;
    }
    
    public byte[] getDelim(){
        return delim;
    }
    
    public void setDelim(byte[] delim){
        this.delim = delim;
    }
    
    public byte[] next(){
        return readUntil(delim);
    }
    
    public byte[] readBytes(int dataSize){
        if (isEOF()) return null;

        out.reset();  
        int c = 0;
        int dataLeave = dataSize;
        int bufsize = dataSize<DEFAULT_BUFSIZE? dataSize : DEFAULT_BUFSIZE;
        
        byte[] data = new byte[bufsize];
        try{
            while(dataLeave>0){
                bufsize = Math.min(dataLeave, bufsize);
                c = in.read(data, 0, bufsize);
                if (c==-1){
                    setEOF(true);
                    break;
                }
                out.add(data, 0, c);
                dataLeave -= c;
            }
        }catch(IOException ex){            
        }
        
        return out.get();
    }
    
    public byte[] readUntil(byte[] delim){        
        if (isEOF()) return null;
        
        out.reset();        
        
        int dataByte;
        int delimSize = delim.length;
        int delimLastByte = delim[delimSize-1];        
                
        try{
            while( true ){
                dataByte=in.read();
                
                if (dataByte==-1){
                    setEOF(true);
                    break;                    
                }
                
                out.add(dataByte);
                if (dataByte==delimLastByte && out.size()>=delimSize && Arrays.equals(out.getLast(delimSize), delim)){
                    out.rewind(delimSize);
                    break;
                }
            }            
        }catch(IOException ex){
        }        
        
        return out.get();
    }
    
    
    public byte[] readAvailable(){
        if (isEOF()) return null;
        
        out.reset();        

        int bytesCount;
        byte[] buf = new byte[DEFAULT_BUFSIZE];        
        
        try{
            while(in.available()>0){
                bytesCount = in.read(delim);
                out.add(buf, 0, bytesCount);
            }
        }catch(IOException ex){            
        }
        
        return out.get();
    }
    
}
