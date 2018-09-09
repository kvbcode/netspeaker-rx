/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net;

import java.net.SocketAddress;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public abstract class ASession<T> extends Observable implements ISession, Consumer<T>{
    final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
        
    final static int INBOX_SIZE = 32;

    final BlockingQueue<NetPacket> inbox = new PriorityBlockingQueue<>(INBOX_SIZE);            
    final AtomicInteger recv_id = new AtomicInteger(Integer.MIN_VALUE);
    final AtomicInteger send_id = new AtomicInteger(Integer.MIN_VALUE);        
    final AtomicBoolean protocolExecutionActive = new AtomicBoolean( false );
    final AtomicBoolean workedFlag = new AtomicBoolean(false);
    
    IPacketSender sender;    
    int sid = 0;
    String name = "unnamed";
    SocketAddress remoteSocketAddress = null;
    IProtocol protocol;
    
    volatile NetPacket lastPacket = new NetPacket();
    volatile long lastActivity = System.currentTimeMillis();
    volatile long lastSendTime = System.currentTimeMillis();;
    long timeoutValue = 30_000;
    volatile SessionState state = SessionState.NEW;

    
    public ASession(){
    }

    @Override
    public void resetCounters(){
        lastPacket = new NetPacket();
        send_id.set(Integer.MIN_VALUE);
        recv_id.set(Integer.MIN_VALUE);
        lastSendTime = System.currentTimeMillis();
        updateActivity();
    }
    
    public ASession setTimeoutValue(long timeoutValue){
        this.timeoutValue = timeoutValue;
        return this;
    }
    
    public long getTimeoutValue(){
        return this.timeoutValue;
    }
        
    @Override
    public ASession setSid(int sid){
        this.sid = sid;
        return this;
    }
    
    @Override
    public int getSid(){
        return this.sid;
    }
    
    public ASession setSender(IPacketSender sender){
        if (sender==null)
            throw new RuntimeException("Session() sender is null on construct");
        this.sender = sender;
        return this;
    }
        
    public ASession setRemoteAddress(SocketAddress remoteSocketAddress){
        this.remoteSocketAddress = remoteSocketAddress;
        return this;
    }
    
    @Override
    public String toString(){
        return String.format("%s@%s (%s)", name, Integer.toHexString( hashCode() ), remoteSocketAddress.toString());
    }
    
    @Override
    public void setName(String name){
        if (name.isEmpty()){
            this.name = "unnamed";
        }else{
            this.name = name;
        }
    }
    
    @Override
    public String getName(){
        return name;
    }    
    
    @Override
    public SocketAddress getRemoteAddress(){
        return remoteSocketAddress;
    }
        
    @Override    
    public boolean send(NetPacket p){
        if (p!=lastPacket){
            if (p.getId()!=Integer.MAX_VALUE) p = p.setId( getNextSendId() );
            this.lastPacket = p;
        }
        this.lastSendTime = System.currentTimeMillis();
        workedFlag.set(true);        
//        log.trace("send: {}", p);
        return sender.send(p, remoteSocketAddress);
    }

    @Override
    public boolean sendPeriodically(NetPacket sendPacket, long delayMillis){
        if (isSendTimeout( delayMillis )){
            if ( sendPacket.similar( lastPacket ) ){
                return resend();
            }else{
                return send(sendPacket);                
            }
        }
        return false;
    }
        
    @Override
    public boolean resend(){
        return send(lastPacket);
    }

    @Override
    public int getLastSendId(){
        return lastPacket.getId();
    }
    
    private int getNextSendId(){
        int sid = send_id.get();
        
        if (sid<Integer.MAX_VALUE){
            sid = send_id.getAndIncrement();
        }else{
            sid = 0;
        }
        
        return sid;
    }
    
    @Override    
    public NetPacket getData(){
        NetPacket p = inbox.poll();
        int pid = 0;
        if (p!=null){
            pid = p.getId();
            if (pid!=Integer.MAX_VALUE) recv_id.set(pid);
        }
        return p;
    }

    @Override
    public SessionState getState(){
        return state;
    }
    
    @Override
    public void setState(SessionState state){
        if (this.state.equals(state)) return;
        this.state = state;
        this.setChanged();
        this.notifyObservers(state);
    }

    private boolean checkReceivedId(int packet_id){
        boolean ret = false;
        int cur_id = recv_id.get();
        
        if(packet_id >= cur_id){
            ret = true;
        }else{
            if (packet_id < 0 & cur_id > 0) ret = true;
        }
        
        return ret;
    }
    
    public boolean putReceived(NetPacket p){
        if ( !checkReceivedId( p.getId() ) ) return false;
        updateActivity();
        return inbox.offer(p);
    }
            
    public boolean isEmpty(){
        return inbox.isEmpty();
    }

    @Override
    public boolean isReady(){
        return SessionState.READY.equals( state );
    }    
    
    @Override
    public boolean isClosed(){
        return SessionState.CLOSED.equals( state );
    }    

    @Override
    public boolean isLocked(){
        return protocolExecutionActive.get();
    }

    @Override
    public boolean isWorked(){
        return this.workedFlag.get();
    }
    
    @Override
    public void updateActivity(){
        lastActivity = System.currentTimeMillis();
    }
    
    @Override
    public boolean isTimeout(){
        return isTimeout( getTimeoutValue() );
    }
    
    @Override
    public boolean isTimeout(long timeDelta){
        return (Math.abs(System.currentTimeMillis() - lastActivity) > timeDelta);
    }
            
    @Override
    public boolean isSendTimeout(long timeDelta){
        return (Math.abs(System.currentTimeMillis() - lastSendTime) > timeDelta);
    }
    
    public ASession setProtocol(IProtocol protocol){        
        if (protocol==null) throw new IllegalArgumentException(this + " no protocol worker specified");
        this.protocol = protocol;
        this.protocol.setSession(this);
        return this;
    }
    
    @Override
    public IProtocol getProtocol(){
        return this.protocol;
    }

    @Override
    public void close(){
        if (SessionState.CLOSED.equals( getState ())) return;

        setState(SessionState.CLOSED);
        deleteObservers();
        protocol.close();
        protocol = null;
        flush();
    }
        
    @Override
    public void flush(){
        inbox.clear();
    }
    
    @Override
    public void run(){
        workedFlag.set(false);
        protocolExecutionActive.set(true);
        protocol.run();
        protocolExecutionActive.set(false);
    }
}
