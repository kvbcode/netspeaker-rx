/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net.udp;

import java.net.*;
import java.io.*;
import java.util.function.Consumer;

/**
 *
 * @author CyberManic
 */
public class DatagramReceiver implements Runnable {

    final DatagramSocket localSocket;
    final Consumer<DatagramPacket> consumer;
    byte[] netbuf = new byte[4096];
    byte[] rawPacketData;
    final DatagramPacket dg = new DatagramPacket(netbuf, netbuf.length);
    final Thread thread;
    
    public DatagramReceiver(DatagramSocket localSocket, Consumer<DatagramPacket> consumer){
        this.localSocket = localSocket;        
        this.consumer = consumer;
        this.thread = new Thread( this, this.getClass().getSimpleName() );        
    }
    
    public DatagramSocket getSocket(){
        return localSocket;
    }
    
    public DatagramPacket getDatagram() throws IOException{                                        
        localSocket.receive(dg);                                                                                 // block thread and wait for data
        return dg;        
    }
    
    public void start(){
        thread.start();
    }
    
    public void stop(){
        thread.interrupt();
    }
    
    @Override
    public void run(){
        try{
            while(!thread.isInterrupted()){
                consumer.accept( getDatagram() );
            }
        }catch(IOException ex){            
        }
    }
    
}
