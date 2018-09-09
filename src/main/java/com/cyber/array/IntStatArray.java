/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.array;

import java.util.Arrays;

/**
 *
 * @author CyberManic
 */
public class IntStatArray extends IntArray{

    public IntStatArray(int maxSize){
        super(maxSize);
    }

    
    public int getMax(){
        int vmax = 0;
        
        for(int i=0; i<this.size(); i++){
            vmax = Math.max(vmax, data[i]);
        }        
        
        return vmax;
    }
    
    
    public int getMathAvg(){
        int vavg = 0;
        long sum = 0L;
                
        for(int i=0; i<this.size(); i++){
            sum += data[i];
        }        
        
        vavg = (int)(sum / (this.size()+1));
        return vavg;
    }
    
    /**
     * Создает диапазоны по полученным точкам и подсчитывает количество попаданий значений
     * внутреннего массива в каждый из них. Например 2 точки {100,200} создаст 3 диапазона:
     * до 100; от 100 до 200; после 200.
     * 
     * @param rangePoint (точки деления пространства на диапазоны)
     * @return int[] rangeValuesCount (массив попаданий значений в диапазоны)     
     * 
     * @see getRangeValuesPercent
     */
    
    public int[] getRangeValuesCount(final int rangePoint[]){
        int[] ret = new int[rangePoint.length+1];
        Arrays.fill(ret, 0);
        
        int value = 0;
        for(int i=0; i<this.size(); i++){
            value = data[i];
            
            for(int r=0; r<ret.length; r++){
                if (r<rangePoint.length){
                    if (value<rangePoint[r]){
                        ret[r]++;
                        break;
                    }
                }else{
                    ret[r]++;     
                }
            }
        }    
        
        return ret;
    }

    /**
     * Создает диапазоны по полученным точкам и подсчитывает количество попаданий значений
     * внутреннего массива в каждый из них. Например 2 точки {100,200} создаст 3 диапазона:
     * до 100; от 100 до 200; после 200.
     * 
     * @param rangePoint (точки деления пространства на диапазоны)
     * @return int[] rangeValuesCount (массив процентов попаданий значений в диапазоны)
     * 
     * @see getRangeValuesCount
     */
    
    public float[] getRangeValuesPercent(final int rangePoint[]){
        float[] ret = new float[rangePoint.length+1];
        Arrays.fill(ret, 0);

        int[] rangeValuesCount = getRangeValuesCount(rangePoint);
        
        for(int i=0; i<rangeValuesCount.length; i++){
            ret[i] = 100.0F * rangeValuesCount[i] / this.size();
        }
        
        return ret;
    }
    
}
