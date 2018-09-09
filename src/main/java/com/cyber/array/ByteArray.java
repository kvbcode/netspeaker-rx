/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.array;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
/**
 *
 * @author CyberManic
 */
public class ByteArray {
    private static final int DEFAULT_SIZE = 4096;
    private static final float BUF_RESIZE_MULT = 1.33F;
    
    private byte[] buf;
    private volatile int limit = 0;
    private final AtomicInteger pos = new AtomicInteger(0);
    
    public ByteArray(int size){
        this.limit = size;
        buf = new byte[size];
    }

    public ByteArray(){
        this(DEFAULT_SIZE);
    }
    
    /**
     * Подготовить буфер для новых данных. Увеличивать размер в BUF_RESIZE_MULT раз пока буфер
     * не сможет добавить dataSize байт
     * @param dataSize 
     */
    
    private void prepareForData(int dataSize){
        int oldCapacity = capacity();
        int newCapacity = oldCapacity;
        while( (pos.get()+dataSize) >= newCapacity){
            newCapacity = Math.round( newCapacity * BUF_RESIZE_MULT);
        }
        if (newCapacity!=oldCapacity){
            resizeArray(newCapacity);
        }
    }
    
    /**
     * Изменить размер внутреннего буфера до newSize с копированием старых данных
     * @param newSize 
     */
    
    private synchronized void resizeArray(int newSize){
        buf = Arrays.copyOf(buf, newSize);
        limit = newSize;
        if (pos.get()>limit) pos.set(limit);
    }
    
    /**
     * Добавить байт dataByte в массив
     * @param dataByte 
     */
    
    public void add(int dataByte){
        add((byte)dataByte);
    }
    
    /**
     * Добавить байт dataByte в массив
     * @param dataByte 
     */

    public synchronized void add(byte dataByte){
        prepareForData(1);
        buf[pos.getAndIncrement()] = dataByte;
    }

    /**
     * Добавить все данные из data в массив
     * @param data 
     */    
    
    public void add(byte[] data){
        add(data, 0, data.length);
    }
    
    /**
     * Добавить данные из data, начиная с dataOffset, размером dataLen в массив
     * При копировании данных границы data не проверяются.
     * @param data
     * @param dataOffset
     * @param dataLen 
     */
    
    public synchronized void add(byte[] data, int dataOffset, int dataLen){
        prepareForData(dataLen);
        System.arraycopy(data, dataOffset, buf, pos.get(), dataLen);
        pos.addAndGet(dataLen);
    }

    /**
     * Переместить текущую позицию курсора на bytesCount назад. Равнозначно удалению bytesСount
     * из конца массива.
     * @param bytesCount 
     */
    
    public void rewind(int bytesCount){        
        pos.addAndGet(-bytesCount);
        if (pos.get() < 0 ) reset();
    }
    
    /**
     * Сбросить позицию курсора на начало массива. Равнозначно удалению всех данных из массива.
     */
    
    public void reset(){
        pos.set(0);
    }
    
    /**
     * Удалить данные из начала массива длиной beginningBytes и сдвинуть оставшиеся данные и позицию курсора.
     * Размер массива size() уменьшится, но емкость capacity() останется прежней.
     * @param beginningBytes 
     */
    
    public synchronized void remove(int beginningBytes){
        byte[] newdata = new byte[buf.length];
        System.arraycopy(buf, beginningBytes, newdata, 0, size());
        rewind(beginningBytes);
        buf = newdata;
    }
    
    /**
     * Попытаться уменьшить емкость capacity() массива до размера фактических данных в нем.
     * Массив не может быть меньше исходного размера по умолчанию.
     */
    
    public synchronized void trim(){
        if (size()>DEFAULT_SIZE){
            resizeArray(size());
        }
    }
    
    /**
     * Возвратить фактический размер данных в массиве (текущую позицию курсора)
     * @return 
     */
    
    public int size(){
        return pos.get();
    }
    
    /**
     * Возвратить внутреннюю емкость массива (динамическая, обычно превышает размер данных)
     * @return 
     */
    
    public int capacity(){
        return limit;
    }
    
    /**
     * Получить копию данных массива от начала и до текущей позиции. Сдвиг курсора не происходит.
     * @return 
     */
    
    public byte[] get(){
        return Arrays.copyOfRange(buf, 0, pos.get());
    }
    
    /**
     * Получить байт по индексу index. Сдвиг курсора не происходит. Границы не проверяются.
     * @param index
     * @return 
     */
    
    public byte get(int index){
        return buf[index];
    }
    
    /**
     * Получить копию данных начиная с индекса indexStart до позиции курсора, размером до length байт.
     * Сдвиг курсора не происходит.
     * @param indexStart
     * @param length
     * @return 
     */
    
    public byte[] get(int indexStart, int length){
        int bytesCount = length;
        int size = size();
        if ((indexStart+length) > size) bytesCount = size - indexStart;

        byte[] data = new byte[bytesCount];
        System.arraycopy(buf, indexStart, data, 0, bytesCount);
        return data;
    }
    
    /**
     * Получить копию последних bytesCount байт до позиции курсора (фактически от конца данных).
     * Границы не проверяются. Сдиг курсора не происходит.
     * @param bytesCount
     * @return 
     */
    
    public byte[] getLast(int bytesCount){
        int lengthEnd = size();
        int indexStart = lengthEnd - bytesCount;
        return Arrays.copyOfRange(buf, indexStart, lengthEnd);
    }
    
}
