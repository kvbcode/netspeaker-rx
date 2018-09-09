/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.storage;

import java.util.*;

/**
 *
 * @author CyberManic
 */
public abstract class AProperties extends Properties implements IProperties{

    @Override
    public Integer getProperty(String key, Integer defaultValue){
        String ret = getProperty(key);
        if (ret==null) return defaultValue;
        return Integer.valueOf(ret);        
    }    
    
    @Override
    public Float getProperty(String key, Float defaultValue){
        String ret = getProperty(key);
        if (ret==null) return defaultValue;
        return Float.valueOf(ret);        
    }    

    @Override
    public Double getProperty(String key, Double defaultValue){
        String ret = getProperty(key);
        if (ret==null) return defaultValue;
        return Double.valueOf(ret);        
    }    
    
    @Override
    public Boolean getProperty(String key, Boolean defaultValue){
        String val = getProperty(key);
        if (val==null) return defaultValue;
        
        val = val.toLowerCase();
        
        if (val.equals("true")) return true;
        if (val.equals("yes")) return true;
        if (val.equals("on")) return true;
        if (val.equals("1")) return true;
        
        return false;
    }    
    
}
