/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author CyberManic
 */
public class InputBitStream extends Object{
    ByteBuffer byteBuffer;
    BitBuffer buf = new BitBuffer(8);    
        
    int positionBytes = 0;
    int positionBits = 0;

    
    public InputBitStream(ByteBuffer buf){
        byteBuffer = buf;
        readForwardBuffer(0);
    }

    
    public InputBitStream(byte[] data){
        byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        readForwardBuffer(0);
    }
    
    
    @Override
    public String toString(){
        return String.format("%s = [posBytes=%d, posBits=%d]", this.getClass().getSimpleName(), positionBytes, (buf.getCapacity() - buf.getLength()));
    }
    
    
    public int read( int bits ){
        int queryBitsCount = bits;
        BitBuffer result = new BitBuffer( bits );

        while( queryBitsCount > 0){
            if ( eof() )
                break;
            
            if ( queryBitsCount < buf.getLength() ){
                result.add( queryBitsCount, buf.pull( queryBitsCount ) );
                queryBitsCount = 0;                
            }else{
                queryBitsCount -= buf.getLength();
                result.add( buf.getLength(), buf.pull( buf.getLength() ) );
            }
                        
            if ( buf.isEmpty() ){
                readNext();                
            }
        }
        
        return result.getData();
    }
    
    
    public int readSignedValue(int bitsCount){
        if ( bitsCount == buf.getCapacity() ) return read( bitsCount );
                
        int groupBits = read( bitsCount );
        
        int signBitMask = 1<< (bitsCount -1);
        int signMinus = (groupBits & signBitMask);  
        
        int valueMask = signBitMask - 1;        
        int value = groupBits & valueMask;

        if ( signMinus > 0 ){
            if ( value == 0 ){
                // максимальное отрицательное значение для набора бит bitsCount
                value = valueMask + 1;
                value *= -1;
            }else{
                value -= 1;
                value ^= 0xFFFFFFFF;
            }
        }
        return value;
    }
    
    
    public boolean eof(){
        if (positionBytes >= byteBuffer.limit()){
            return true;
        }        
        return false;
    }
    
    
    protected int readNext(){
        positionBytes++;
        positionBits = 0;
                
        return readForwardBuffer( positionBits );
    }
    
    
    protected int readForwardBuffer(int offsetBits){
        if ( eof() ){
            buf.clear();
            return 0;
        }
        
        buf.setData( toUnsigned( byteBuffer.get() ) );       
        buf.shiftRight(offsetBits);        
        
        return buf.getData();
    }

            
    protected int toUnsigned(byte b){
        return b & 0xff;
    }    
    
        
    
}
