/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.net;

import java.util.function.Consumer;

/**
 *
 * @author CyberManic
 */
public interface IProtocol extends Runnable, Consumer<NetPacket>{
    
    public void connect(NetPacket p);
    
    public void onTick();
    
    public boolean isConnected();
        
    public ISession getSession();
    
    public void setSession(ISession session);
        
    public void close();
}
