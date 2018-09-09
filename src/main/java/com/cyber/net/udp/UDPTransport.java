/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import com.cyber.net.SessionsManager;
import com.cyber.net.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public class UDPTransport{
    final protected static Logger log = LoggerFactory.getLogger("UDPTransport");

    DatagramSocket localSocket;
    DatagramSender sender;
    DatagramReceiver receiver;
    ISessionFactory sessionFactory;
    SessionsManager sessionManager;
    Consumer<DatagramPacket> dispatcher;    
    Supplier<IProtocol> protocolSupplier;
    UDPSession session = null;

    
    public UDPTransport(Supplier<IProtocol> protocolSupplier){
        if (protocolSupplier==null) throw new IllegalArgumentException("protocol is not defined");
        this.protocolSupplier = protocolSupplier;
    }
    
    public UDPTransport listen(DatagramSocket localSocket) throws SocketException{        
        this.localSocket = localSocket;
        
        sender = new DatagramSender(localSocket);      
        
        sessionFactory = new UDPSessionFactory(sender, protocolSupplier );
        sessionManager = new SessionsManager(sessionFactory);
        sessionManager.start();
        
        dispatcher = new DatagramDispatcher(sessionManager);                            
        receiver = new DatagramReceiver(localSocket, dispatcher);
        receiver.start();                               
        
        return this;
    }

    public UDPTransport listen(int port) throws SocketException{
        this.localSocket = new DatagramSocket(port);
        return listen(localSocket);
    }

    public ISession connect(String host, int port) throws SocketException{
        return connect(new InetSocketAddress(host, port));
    }
    
    public ISession connect(SocketAddress remoteAddress) throws SocketException{
        this.localSocket = new DatagramSocket();            
        
        sender = new DatagramSender(localSocket);      
        
        session = new UDPSession();
        session.setSender(sender);
        session.setRemoteAddress(remoteAddress);
        session.setProtocol(protocolSupplier.get());        
        
        receiver = new DatagramReceiver(localSocket, session);
        receiver.start();                               

        session.setName( getLocalHostName() );
        session.getProtocol().connect(null);
        
        return session;
    }           
    
    public static String getLocalHostName(){
        String ret = "";
                
        if (ret.isEmpty()){
            Map<String, String> env = System.getenv();
            if (env.containsKey("COMPUTERNAME"))
                ret = env.get("COMPUTERNAME");
            else if (env.containsKey("HOSTNAME"))
                ret = env.get("HOSTNAME");
        }            

        if (ret.isEmpty()){
            try {    
                InetAddress localhost=InetAddress.getLocalHost();
                ret = localhost.getHostName();
            } catch (UnknownHostException ex) {            
            }
        }
        return ret;
    }
    
    public void close(){
        if (sessionManager!=null) sessionManager.stop();
        if (session!=null) session.close();
        if (receiver!=null) receiver.stop();
        if (localSocket!=null) localSocket.close();
    }
    
    
    public ASession getSession(){
        return this.session;
    }
    
    
    public SessionsManager getSessionsManager(){
        return this.sessionManager;
    }
    
    
    public DatagramSocket getLocalSocket(){
        return this.localSocket;
    }
}
