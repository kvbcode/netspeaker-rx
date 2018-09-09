/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;

/**
 *
 * @author CyberManic
 */
public class Checksum {
    private final byte[] checksum;
    private final String checksumHexStr;
    private final String algorithm;
    private static final int BUFSIZE = 16*1024;
    
    private Checksum(byte[] checksum, String algorithm){
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.checksumHexStr = checksum!=null ? DatatypeConverter.printHexBinary(checksum).toLowerCase(): "";
    }
    
    
    public byte[] getBytes(){
        return checksum;
    }
    
    
    public String getAlgorythm(){
        return algorithm;
    }
    
    
    public boolean isEmpty(){
        return (checksum==null);
    }
        
    
    @Override
    public String toString(){
        return checksumHexStr;
    }
    
    
    public static Checksum ofArray(byte[] data, String algorithm){
        final MessageDigest digest;
        try {
            digest =  MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            return new Checksum(null, algorithm);
        }        
        return new Checksum( digest.digest( data ), algorithm );
    }
    
    
    public static Checksum ofStream(InputStream input, String algorithm){
        int readBytes = 0;
        byte[] buf = new byte[BUFSIZE];
        Checksum ret = new Checksum(null, algorithm);
        
        try{
            try (DigestInputStream inputDig = new DigestInputStream(input, MessageDigest.getInstance(algorithm))) {
                while((readBytes=inputDig.read(buf))!=-1){}
                ret = new Checksum(inputDig.getMessageDigest().digest(), algorithm);
            }
        } catch (NoSuchAlgorithmException | IOException ex) {
            return new Checksum(null, algorithm);
        }
        return ret;
    }

    
    public static Checksum ofByteBuffer(ByteBuffer input, String algorithm){
        final MessageDigest digest;

        try{            
            digest =  MessageDigest.getInstance(algorithm);
            digest.update(input);
        } catch (NoSuchAlgorithmException ex) {
            return new Checksum(null, algorithm);
        }
        return new Checksum( digest.digest(), algorithm );        
    }
            
    /**
     * Возвратить контрольную сумму путем чтения отображенного в память файла. Быстрый вариант,
     * но возможно ограничение на размер файла 4Gb в 32-bit версиях OS.
     * @param filepath
     * @param algorithm
     * @return
     */
    
    public static Checksum ofFileMap(String filepath, String algorithm){        
        try( RandomAccessFile file = new RandomAccessFile(filepath, "r") ){
            MappedByteBuffer mapBuf = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());            
            return Checksum.ofByteBuffer(mapBuf, algorithm);
        } catch (IOException ex) {
            return new Checksum(null, algorithm);
        }
    }
    
    /**
     * Рекомендуемый метод вычисления контрольных сумм для файла любого размера.
     * @param filepath
     * @param algorithm
     * @return 
     */
    
    public static Checksum ofFile(String filepath, String algorithm){
        Checksum ret = new Checksum(null, algorithm);
        try(FileInputStream inputFile = new FileInputStream(filepath)){
            ret = ofStream(inputFile, algorithm);
        }catch(IOException ex){
        }
        return ret;
    }
    
}
