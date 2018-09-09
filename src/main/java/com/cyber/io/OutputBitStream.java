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
public class OutputBitStream {
    ByteBuffer byteBuffer;    
    BitBuffer tail = new BitBuffer(8);
    
    
    public OutputBitStream(int bytesCapacity){
        byteBuffer = ByteBuffer.allocate(bytesCapacity);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    
    public void write(int bitsCount, int groupBits) {
        BitBuffer src = new BitBuffer( bitsCount, groupBits );
        
        while( !src.isEmpty() ){
            if ( src.getLength() < tail.getFreeBits() ){
                tail.add( src.getLength(), src.pull( src.getLength() ) );                                
            }else{
                tail.add( tail.getFreeBits(), src.pull( tail.getFreeBits() ) );                
            }
            
            if (tail.isFull()){
                byteBuffer.put( (byte) tail.getData() );
                tail.clear();
            }
        }
    }

    
    public void writeSignedValue(int bitsCount, int value) {
        int signBitMask = 1 << bitsCount - 1;
        int valueMask = signBitMask - 1;
        int signedValue = value & valueMask;
        
        if (value < 0){
            signedValue ^= valueMask;
            signedValue +=1;
            signedValue |= signBitMask;
        }        
        
        write( bitsCount, signedValue );
    }
            
    
    /**
     * Создает дубликат буфера с общими данными. Остатки бит записываются в новый буфер перед
     * возвратом, но исходный буфер продолжает учитывать только целые байты т.к. position и limit
     * для них разные. Это позволяет продолжать работу с исходным буфером после чтения через дубликат.
     * @return ByteBuffer
     */
    
    public ByteBuffer toByteBuffer(){
        int oldPosition = byteBuffer.position();
        
        if (tail.getLength() > 0){
            byteBuffer.put((byte)tail.getData());            
        }
        
        ByteBuffer retBuf = byteBuffer.duplicate();
        retBuf.flip();
        
        byteBuffer.position( oldPosition );
        
        return retBuf;
    }
        
    
    public byte[] toBytes(){
        ByteBuffer buf = toByteBuffer();
        byte[] retArr = new byte[buf.limit()];
        buf.get( retArr, 0, retArr.length );
        return retArr;
    }
    
}
