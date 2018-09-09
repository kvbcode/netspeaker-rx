/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import com.cyber.net.NetPacket;
import java.net.*;
import java.io.IOException;
import com.cyber.net.IPacketSender;

/**
 *
 * @author CyberManic
 */
public class DatagramSender implements IPacketSender{
    
    final DatagramSocket socket;    
    
    public DatagramSender(final DatagramSocket localSocket){
        this.socket = localSocket;
    }
        
    public DatagramSocket getSocket(){
        return socket;
    }    
    
    @Override
    public boolean send(NetPacket p, SocketAddress remoteSocketAddress){
        if (remoteSocketAddress==null) return false;        
        
        return send(p.toBytes(), remoteSocketAddress);        
    }

    public boolean send(byte[] data, SocketAddress remoteSocketAddress){
        if (remoteSocketAddress==null) return false;        
        
        DatagramPacket dg = new DatagramPacket(data, data.length, remoteSocketAddress);
        
        try{
            socket.send(dg);
        }catch(SocketException ex){
            return false;
        }catch(IOException ex){
            return false;
        }
        return true;
    }
    
}
