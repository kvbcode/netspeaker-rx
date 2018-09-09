/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net;

/**
 *
 * @author CyberManic
 */
public abstract class AProtocol extends Object implements IProtocol{
    protected static final int KEEPALIVE_INTERVAL = 10000;

    protected ISession session = null;
    protected boolean connectedFlag = false;
    
    public AProtocol(){

    }
        
    @Override
    public boolean isConnected(){
        return this.connectedFlag;
    }
    
    protected void setConnected(boolean value){
        this.connectedFlag = value;
    }
            
    @Override
    public ISession getSession(){
        return this.session;
    }
    
    @Override
    public void setSession(ISession session){
        this.session = session;
    }
        
    @Override
    public void close(){
        session.close();
    }
    
    @Override
    public void run(){
        NetPacket p = session.getData();
        if (!isConnected()){
            connect(p);
        }else{
            if (p!= null) accept(p);
        }
        onTick();
        keepAlive();
    }
        
    protected void keepAlive(){
        if (!getSession().isWorked()){
            getSession().sendPeriodically( new NetPacket( NetPacketType.KEEPALIVE, 0 ), KEEPALIVE_INTERVAL);            
        }
    }
    
}