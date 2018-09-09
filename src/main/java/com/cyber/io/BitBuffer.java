/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.io;

/**
 *
 * @author CyberManic
 */
public class BitBuffer extends Object{

    protected int buf = 0;
    protected int bufCount = 0;
    protected final int bufCapacity;
    
    protected static final int bitMask[] = {
        0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff,
        0x1ff,0x3ff,0x7ff,0xfff,0x1fff,0x3fff,0x7fff,0xffff,
        0x1ffff,0x3ffff,0x7ffff,0xfffff,0x1fffff,0x3fffff,
        0x7fffff,0xffffff,0x1ffffff,0x3ffffff,0x7ffffff,
        0xfffffff,0x1fffffff,0x3fffffff,0x7fffffff,0xffffffff
    };
    
    public BitBuffer(int capacity){
        bufCapacity = capacity;
    }

    public BitBuffer(int capacity, int bitData){
        buf = bitData;
        bufCapacity = capacity;
        bufCount = capacity;
    }
        
    public static BitBuffer wrap(int capacity, int bitData){
        return new BitBuffer( capacity, bitData);
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder(bufCount);
        int bit = 0;
        for( int i = bufCount - 1; i>=0; i--){
            bit = ( buf & ( bitMask[1] << i) )>>> i;
            if ( bit == 1 ){
                sb.append("1");
            }else{
                sb.append("0");
            }
        }
        return sb.toString();
    }
    
    protected int compositeAddBits( int srcBitsData, int srcBitsCount, int addBitsData, int addBitsCount ){        
        addBitsData &= bitMask[ addBitsCount ];
        addBitsData <<= srcBitsCount;
        srcBitsData |= addBitsData;
        return srcBitsData;    
    }

    protected int compositeInsertBits( int srcBitsData, int srcBitsCount, int addBitsData, int addBitsCount ){        
        srcBitsData <<= addBitsCount;
        addBitsData &= bitMask[ addBitsCount ];
        srcBitsData |= addBitsData;
        return srcBitsData;    
    }

    /**
     * Дополнить старшие разряды буфера новыми битами
     * @param bitsCount (int) - количество добавляемых бит
     * @param bitsData (int) - набор добавляемых бит
     * @return 
     */
    
    public int add( int bitsCount, int bitsData ){
        int stored = 0;
        int bufFreeBits = getFreeBits();
        
        if ( bufFreeBits > 0 ){
            if ( bufFreeBits >= bitsCount ){
                stored = bitsCount;
                buf = compositeAddBits( buf, bufCount, bitsData, bitsCount );
            }else{
                stored = bufFreeBits;
                buf = compositeAddBits( buf, bufCount, bitsData, bufFreeBits );
            }
        }
        bufCount += stored;
        
        return stored;
    }
    
    /**
     * Сдвинуть данные буфера влево на bitsCount и дополнить младшие разряды буфера новыми битами.
     * @param bitsCount (int) - количество добавляемых бит
     * @param bitsData (int) - набор вставляемых бит
     * @return 
     */
    
    public int insert( int bitsCount, int bitsData ){
        int stored = 0;
        int bufFreeBits = getFreeBits();
        
        if ( bufFreeBits > 0 ){
            if ( bufFreeBits >= bitsCount ){
                stored = bitsCount;
                buf = compositeInsertBits( buf, bufCount, bitsData, bitsCount );
            }else{
                stored = bufFreeBits;
                buf = compositeInsertBits( buf, bufCount, bitsData, bufFreeBits );
            }
        }
        bufCount += stored;
        
        return stored;
    }
    
    public int pull( int bitsCount ){
        int result = 0;
        
        if ( bitsCount <= bufCount ){
            result = buf & bitMask[bitsCount];
            shiftRight( bitsCount );
        }else{
            result = buf & bitMask[ bufCount ];
            clear();
        }
        
        return result;
    }
    
    public void shiftRight(int offset){
        if ( offset > 0 ){
            buf >>>= offset;
            bufCount -= offset;
        }
    }

    public void shiftLeft(int offset){
        if ( offset > 0 ){
            buf <<= offset;
            bufCount -= offset;
        }
    }
    
    public int getData(){
        return buf;
    }

    public void setData( int bitsData ){
        buf = bitsData & bitMask[bufCapacity];
        bufCount = bufCapacity;
    }
    
    public int getCapacity(){
        return bufCapacity;
    }
    
    public int getLength(){
        return bufCount;
    }
    
    public boolean isEmpty(){
        return ( bufCount == 0 );
    }
    
    public boolean isFull(){
        return ( getFreeBits() == 0 );
    }
        
    public int getFreeBits(){
        return ( bufCapacity - bufCount );
    }
    
    public void clear(){
        buf = 0;
        bufCount = 0;
    }
    
}
