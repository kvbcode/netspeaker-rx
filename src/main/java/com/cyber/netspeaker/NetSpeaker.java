/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.netspeaker;

import com.cyber.file.FilenameUtils;
import com.cyber.audio.*;
import com.cyber.net.*;
import com.cyber.netspeaker.protocol.*;
import com.cyber.net.udp.*;
import com.cyber.storage.*;
import com.cyber.io.IOUtilsLite;
import com.cyber.util.MapUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


public class NetSpeaker {
    protected static final String PROPERTIES_FILENAME = "app.properties";
    protected static final Logger log = LoggerFactory.getLogger("NetSpeaker");
    protected static int netPort = 13800;        
    private static volatile NetSpeaker instance = null;
    protected IProperties props = null;
    protected AudioEngine audioEngine = null;    
    
    protected SessionsManager sessionsManager = null;
    
    private NetSpeaker(){
        init();
    }
    
    public static NetSpeaker getInstance(){
        if (instance==null){
            instance = new NetSpeaker();
        }
        return instance;
    }
    
    public static void main(String[] args) throws IOException{        
        log.info("NetSpeakerCore", "");        
        
        if (args.length==0){
            new NetSpeaker().startServer(netPort);
        }else{
            new NetSpeaker().startClient(args[0], netPort);
        }                
        
        log.trace("exit Main()", "");
    }        
    
    
    public void init(){
        props = loadProperties();
        
        netPort =  props.getProperty("net.port", netPort);        
        
        audioEngine = new AudioEngine( props );
        
        saveProperties();
    }
    
    
    public IProperties loadProperties(){
        File propsUserFile = FilenameUtils.getUserHomeFile("NetSpeaker", PROPERTIES_FILENAME);
        
        IProperties retProps = null;
        
        try{
            if (propsUserFile.exists()){
                log.debug("loadProperties() from file: {}", propsUserFile.toString());
                retProps = SimpleProperties.fromFile(propsUserFile.toString());
            }
            
            if (retProps==null || retProps.isEmpty()){
                log.debug("loadProperties() from resource: {}", PROPERTIES_FILENAME);
                retProps = SimpleProperties.fromResource(PROPERTIES_FILENAME);                
            }
        }catch(Exception ex){
            log.trace("NetSpeaker error properties loading", "");
            retProps = new SimpleProperties();
        }
        
        return retProps;
    }

    
    public void saveProperties(){      
        File propsUserFile = FilenameUtils.getUserHomeFile("NetSpeaker", PROPERTIES_FILENAME);
        propsUserFile.getParentFile().mkdirs();
        log.debug("saveProperties() to {}", propsUserFile.toString());        
        if (!IOUtilsLite.writeText(propsUserFile, MapUtils.toText(props))){
            log.debug("saveProperties() failed", "");
        }        
    }

    
    public AudioEngine getAudioEngine(){
        return audioEngine;
    }

    
    public IProperties getProperties(){
        return props;
    }

    
    public SessionsManager getSessionsManager(){
        return sessionsManager;
    }

    
    public UDPTransport startServer(int port) throws IOException{
        UDPTransport channels = new UDPTransport( PlayerOnlyProtocol::new );

        channels.listen(port);     
        sessionsManager = channels.getSessionsManager();
        return channels;
    }

    
    public ISession startClient(String host, int port) throws IOException{
        UDPTransport channel = new UDPTransport( RecorderOnlyProtocol::new );

        final ISession session = channel.connect(host, port);        
        props.putIfAbsent("host.name", session.getName() );
        
        Thread t = new Thread(new Runnable(){
            @Override
            public void run(){
                while(!Thread.currentThread().isInterrupted()){
                    try{
                        session.run();
                    }catch(Exception ex){             
                        log.trace("session error: {}", ex.toString());
                    }
                }            
            }
        }, "NetSpeakerClient");
        t.start();
        
        return session;
    }
    
    
}
