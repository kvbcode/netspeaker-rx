/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import com.cyber.net.SessionsManager;
import com.cyber.net.ISession;

import java.net.SocketAddress;
import java.net.DatagramPacket;
import java.util.function.Consumer;
/**
 *
 * @author CyberManic
 */
public class DatagramDispatcher implements Consumer<DatagramPacket>{
    final SessionsManager sessions;
    
    public DatagramDispatcher(SessionsManager sessionManager){
        this.sessions = sessionManager;        
    }    
    
    @Override
    public void accept(DatagramPacket dg){
        SocketAddress remoteSocket = dg.getSocketAddress();        
        int sid = SessionsManager.generateSessionId( remoteSocket );
        
        ISession session = sessions.getSession(sid);
        
        if (session==null) session = sessions.create(remoteSocket);                    

        ( (UDPSession) session ).accept(dg);                
    }
    
}
