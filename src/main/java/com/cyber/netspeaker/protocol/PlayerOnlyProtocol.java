/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.netspeaker.protocol;

import com.cyber.audio.AudioEngine;
import com.cyber.audio.AudioPlayer;
import com.cyber.net.AProtocol;
import com.cyber.net.NetPacket;
import com.cyber.net.NetPacketType;
import com.cyber.net.SessionState;
import com.cyber.netspeaker.NetSpeaker;     
import com.cyber.storage.SimpleProperties;
import com.cyber.storage.IProperties;
import com.cyber.util.MapUtils;
import com.cyber.util.TextUtils;
import com.cyber.util.TimeoutTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public class PlayerOnlyProtocol extends AProtocol{
    protected final static int TIMEOUT = 5000;
    protected final Logger log = LoggerFactory.getLogger( this.getClass() );
    protected final TimeoutTimer connectionTimeout = new TimeoutTimer( TIMEOUT );

    private AudioPlayer player = null;
    
    ProtocolState state = ProtocolState.INIT;
    
    
    public PlayerOnlyProtocol(){
        connectionTimeout.update();
    }
    
    @Override
    public void connect(NetPacket p) {
        if (p==null) return;
        if (connectionTimeout.isTimeout()){
            log.trace("{}.{}: connection timeout", this.getClass().getSimpleName(), state);
            close();        
        }
        
        switch(state){
            case INIT:
                if (p.getType()==NetPacketType.INIT){
                    getSession().send( new NetPacket( NetPacketType.OK, p.getContext() ) );
                    state = ProtocolState.SET_PROPERTIES;
                }else{
                    sendReset();
                }
                break;
            case SET_PROPERTIES:
                if(p.getType()==NetPacketType.SET){
                    String propsLines = new String(p.getData());
                    if (propsLines!=null){
                        log.trace("setup properties:\n{}", propsLines);                
                        setup( SimpleProperties.fromLines(propsLines) );
                        getSession().send( new NetPacket( NetPacketType.OK, p.getContext() ) );
                        state = ProtocolState.STATUS_QUERY;
                    }else{
                        sendReset();
                    }
                }else{
                    getSession().resend();
                }
                break;
            case STATUS_QUERY:
                if (p.getType()==NetPacketType.STATUS){
                    if (player.isAlive()){
                        getSession().send( new NetPacket( NetPacketType.READY, p.getContext() ) );
                        getSession().setState( SessionState.READY );            
                        state = ProtocolState.READY;
                        setConnected(true);
                    }else{
                        sendReset();
                    }
                }else{
                    getSession().resend();                    
                }
                break;
        }
    }

    public void setup(IProperties props){
        IProperties curProps = NetSpeaker.getInstance().getProperties();
        
        getSession().setName( props.getProperty( "host.name", session.getName() ) );
        String codecName = props.getProperty("audio.format.codec", curProps.getProperty("audio.format.codec", "null"));
        player = getOrCreatePlayer(codecName);
        
        curProps.putAll( props );
        player.setProperties( curProps );
    }
    
    public void sendProperties(String...filterStartMasks){
        IProperties props = NetSpeaker.getInstance().getProperties();
        String propsLines = TextUtils.filterStartsWith( MapUtils.toText( props ), filterStartMasks );
        getSession().send( new NetPacket( 0, NetPacketType.SET, state.ordinal(), propsLines.getBytes() ));
    }
    
    @Override
    public void onTick() {
        
    }
    
    public void sendReset(){
        getSession().send( new NetPacket( Integer.MAX_VALUE, NetPacketType.RESET, state.ordinal(), null ) );
        getSession().resetCounters();        
    }
    
    @Override
    public void accept(NetPacket p){
        if (getSession().isReady()){
            switch (p.getType()){
                case NetPacketType.DATA:     
                    if (player!=null) player.playDataChunk( p.getData() );
                    break;
            }
        }        
    }
        
    public AudioPlayer getOrCreatePlayer(String codecName){
        if (player==null){
            AudioEngine audio = NetSpeaker.getInstance().getAudioEngine();
            player = audio.getReadyAudioPlayer( getSession().getSid(),  audio.getAudioCodecType(codecName));        
        }        
        return player;
    }

    public AudioPlayer getPlayer(){
        return player;
    }
    
    @Override
    public void close(){
        if (isConnected()){
            NetSpeaker.getInstance().getAudioEngine().stopAudioPlayer( getSession().getSid() );
            player = null;
        }
        getSession().close();
    }
    
}
