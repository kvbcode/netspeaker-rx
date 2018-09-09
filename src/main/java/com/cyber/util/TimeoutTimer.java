/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.util;

/**
 *
 * @author CyberManic
 */
public class TimeoutTimer {
    private final static long NANOSINMILLIS = 1000000L;
    final long timeoutNanos;    
    volatile long lastActivity;
    
    public TimeoutTimer(long timeoutMillis){
        lastActivity = System.nanoTime();
        this.timeoutNanos = timeoutMillis*NANOSINMILLIS;
    }

    public TimeoutTimer(){
        this(0);
    }
    
    public void update(){
        lastActivity = System.nanoTime();
    }
    
    public boolean isTimeout(long millis){
        return ((System.nanoTime() - lastActivity) > millis*NANOSINMILLIS);
    }
    
    public boolean isTimeout(){
        return ((System.nanoTime() - lastActivity) > timeoutNanos);
    }
}
