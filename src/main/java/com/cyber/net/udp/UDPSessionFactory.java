/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import com.cyber.net.SessionsManager;
import com.cyber.net.*;
import java.net.SocketAddress;
import java.util.function.Supplier;
/**
 *
 * @author CyberManic
 */
public class UDPSessionFactory implements ISessionFactory{
    final DatagramSender sender;
    final Supplier<IProtocol> protocolSupplier;
            
    public UDPSessionFactory(DatagramSender sender, Supplier<IProtocol> protocolSupplier) {
        this.sender = sender;
        this.protocolSupplier = protocolSupplier;
    }
    
    @Override
    public ISession get(int sessionId, SocketAddress remoteSocketAddress){
        UDPSession newSession;

        newSession = new UDPSession();
        newSession.setSid( sessionId );
        newSession.setRemoteAddress( remoteSocketAddress );
        newSession.setSender( sender );
        newSession.setProtocol( protocolSupplier.get() );

        return newSession;
    }
            
}
