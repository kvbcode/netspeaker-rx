/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.array;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author CyberManic
 */
public class IntArray implements Iterable<Integer>, Iterator<Integer>{
    
    protected int[] data;
    protected int pos = 0;
    protected int iterCur = 0;
    
    public IntArray(int maxSize){
        data = new int[maxSize];
    }
    
    @Override
    public boolean hasNext() {
        if (iterCur < (pos-1)){
            return true;
        }  
        return false;
    }

    @Override
    public Integer next() {
        if (iterCur >= pos)
            throw new NoSuchElementException();
        
        iterCur++;
        return get(iterCur);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Integer> iterator() {
        iterCur = 0;
        return this;
    }
  
    public int get(int index){
        if (index<pos){
            return data[index];
        }
        throw new IndexOutOfBoundsException();
    }

    public void add(int value){
        data[pos++] = value;
    }

    public int size(){
        return pos;
    }

    public void clear(){
        pos = 0;
    }        

}
