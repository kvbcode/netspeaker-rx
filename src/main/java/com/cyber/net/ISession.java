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
public interface ISession extends Runnable, ISessionInfo{
    
    public NetPacket getData();
    
    public boolean send(NetPacket p);

    public boolean sendPeriodically(NetPacket sendPacket, long delayMillis);

    public boolean resend();

    public void updateActivity();

    public void resetCounters();

    public void flush();

    public void close();
    
}
