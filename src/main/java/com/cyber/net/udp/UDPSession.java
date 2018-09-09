/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import com.cyber.net.ASession;
import com.cyber.net.NetPacket;

import java.net.DatagramPacket;

/**
 *
 * @author CyberManic
 */
public class UDPSession extends ASession<DatagramPacket>{
    
    public UDPSession(){
        super();
    }               
           
    @Override    
    public void accept(DatagramPacket dg){        
        NetPacket p = NetPacket.fromBytes( dg.getData() );
        if (p==null) return;
        if (getRemoteAddress()==null) this.setRemoteAddress( dg.getSocketAddress() );

        putReceived(p);
    }
            
}
