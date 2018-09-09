/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.net;

import java.net.SocketAddress;

/**
 *
 * @author CyberManic
 */
public interface ISessionInfo{
    
    public String getName();
    
    public void setName(String name);
    
    public SocketAddress getRemoteAddress();
       
    public ISessionInfo setSid(int key);
    
    public int getSid();
    
    public IProtocol getProtocol();
    
    public SessionState getState();

    public void setState(SessionState state);

    public int getLastSendId();

    public boolean isReady();

    public boolean isClosed();

    public boolean isLocked();

    public boolean isWorked();

    public boolean isTimeout();

    public boolean isTimeout(long timeDelta);

    public boolean isSendTimeout(long timeDelta);

}
