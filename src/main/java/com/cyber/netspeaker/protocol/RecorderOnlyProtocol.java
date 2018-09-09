/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.netspeaker.protocol;

import com.cyber.audio.AudioRecorder;
import com.cyber.net.AProtocol;
import com.cyber.net.NetPacket;
import com.cyber.net.NetPacketType;
import com.cyber.net.SessionState;
import com.cyber.netspeaker.NetSpeaker;
import com.cyber.storage.IProperties;
import com.cyber.storage.SimpleProperties;
import com.cyber.util.MapUtils;
import com.cyber.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public class RecorderOnlyProtocol extends AProtocol{
    protected Logger log = LoggerFactory.getLogger( this.getClass() );
    protected static final int SEND_DELAY = 200;    
    
    protected AudioRecorder recorder = null;    
    protected ProtocolState state = ProtocolState.INIT;

    
    public RecorderOnlyProtocol(){

    }
    
    @Override
    public void connect(NetPacket p) {
        if (checkResponse(p, NetPacketType.RESET)) reset();
        
        switch(state){
            case STATUS_QUERY:
                if ( checkResponse( p, NetPacketType.READY, state.ordinal() ) ){
                    getSession().setState(SessionState.READY);         
                    state = ProtocolState.READY;
                    recorder = NetSpeaker.getInstance().getAudioEngine().getReadyAudioRecorder();                    
                    setConnected(true);
                }else{
                    getSession().sendPeriodically( new NetPacket( NetPacketType.STATUS, state.ordinal() ), SEND_DELAY);                    
                }
                break;
            case SET_PROPERTIES:
                if ( checkResponse( p, NetPacketType.OK, state.ordinal() ) ){
                    state = ProtocolState.STATUS_QUERY;
                }else{   
                    IProperties props = NetSpeaker.getInstance().getProperties();
                    String propsLines = TextUtils.filterStartsWith( MapUtils.toText( props ), "audio.player", "audio.format", "host" );
                    getSession().sendPeriodically( new NetPacket( 0, NetPacketType.SET, state.ordinal(), propsLines.getBytes() ), SEND_DELAY);
                }
                break;
            case INIT:
                if ( checkResponse( p, NetPacketType.OK, state.ordinal() ) ){
                    state = ProtocolState.SET_PROPERTIES;
                }else{
                    getSession().sendPeriodically( new NetPacket( NetPacketType.INIT, state.ordinal() ), SEND_DELAY);
                }
                break;
        }                
    }
    
    @Override
    public void onTick(){
        if (getSession().isReady()) sendAudioData();
        if (isConnected()) checkTimeout();

        try{
            if (!session.isWorked()) Thread.sleep(1);
        }catch(InterruptedException ex){
        }
    }
    
    @Override
    public void accept(NetPacket p){ 
        if (checkResponse(p, NetPacketType.RESET)) reset();
                
        if (getSession().isReady()){
            switch (p.getType()){
                case NetPacketType.KEEPALIVE:
                    System.out.print(".");
                    break;
                case NetPacketType.SET:
                    String propsLines = new String(p.getData());                    
                    log.trace("set properties:\n{}", propsLines);
                    NetSpeaker.getInstance().getProperties().putAll( SimpleProperties.fromLines(propsLines ) );
                    break;
            }
        }        
    }

    public void reset(){
        if (SessionState.RESET.equals(getSession().getState())) return;
        
        log.trace("{} reset()", this.getClass().getSimpleName());
        setConnected(false);
        state = ProtocolState.INIT;

        NetSpeaker.getInstance().getAudioEngine().stopAudioRecorder();
        recorder = null;
        
        getSession().resetCounters();
        getSession().flush();
        getSession().setState(SessionState.RESET);                        
    }
        
    public boolean checkResponse(NetPacket p, short ptype){
        boolean ret = false;
        if (p!=null && p.getType() == ptype){
            return true;
        }
        return ret;
    }

    public boolean checkResponse(NetPacket p, short ptype, int context){
        boolean ret = false;
        if (p!=null && p.getType() == ptype && p.getContext() == context){
            return true;
        }
        return ret;
    }

    public void checkTimeout(){
        if (getSession().isTimeout()){
            log.trace("{} timeout", this.getClass().getSimpleName());
            reset();
        }
        
    }
    
    public boolean sendAudioData(){
        byte[] audiodata;
        boolean worked = false;
        
        do{
             audiodata = getRecorder().pollDataChunk();
             if(audiodata!=null){
                 getSession().send( new NetPacket(NetPacketType.DATA, audiodata) );
                 worked = true;
             }
         } while (audiodata!=null);        
        
        return worked;
    }
    
    public AudioRecorder getRecorder(){
        if (recorder == null){
            recorder = NetSpeaker.getInstance().getAudioEngine().getReadyAudioRecorder();
        }
        return recorder;
    }
    
    @Override
    public void close(){
        NetSpeaker.getInstance().getAudioEngine().stopAudioRecorder();
        recorder = null;
        getSession().close();
    }
       
}
