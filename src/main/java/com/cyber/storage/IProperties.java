/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.storage;

import java.util.Map;
/**
 *
 * @author CyberManic
 */
public interface IProperties extends Map<Object,Object>{

    public String getProperty(String key, String defaultValue);

    public Integer getProperty(String key, Integer defaultValue);
    
    public Float getProperty(String key, Float defaultValue);

    public Double getProperty(String key, Double defaultValue);
    
    public Boolean getProperty(String key, Boolean defaultValue);

    public Object setProperty(String key, String value);    
}
