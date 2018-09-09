/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.audio.dsp;

import com.cyber.storage.IProperties;

/**
 *
 * @author CyberManic
 */
public interface INormalizer {

    public String getFullInfo();
    
    public void setProperties(IProperties properties);

    public byte[] proceed(byte[] input);    
    
    public void gainUp();
            
    public void gainDown();

    public void gainInfo();
    
    
    
}
