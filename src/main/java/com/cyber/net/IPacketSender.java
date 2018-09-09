/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.net;

import com.cyber.net.NetPacket;
import java.net.SocketAddress;

/**
 *
 * @author CyberManic
 */
public interface IPacketSender{    
    public boolean send(NetPacket data, SocketAddress remoteSocketAddress);    
}
