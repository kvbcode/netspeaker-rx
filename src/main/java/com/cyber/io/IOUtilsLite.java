/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.io;

import java.io.*;

/**
 *
 * @author CyberManic
 */
public class IOUtilsLite {
    private static final int BUFSIZE = 4096;
    
    private IOUtilsLite(){ }

    
    public static long copy(InputStream input, OutputStream output) throws IOException{
        long totalBytesCounter = 0;
        byte[] buf = new byte[BUFSIZE];
        int numBytes = 0;
        while( (numBytes = input.read(buf))!=-1 ){
            output.write(buf, 0, numBytes);
            totalBytesCounter += numBytes;
        }
        
        return totalBytesCounter;
    }
    

    public static boolean writeText( File file, String textData ){
        try(PrintWriter out = new PrintWriter(file)){
            out.write(textData);
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }
    
}
