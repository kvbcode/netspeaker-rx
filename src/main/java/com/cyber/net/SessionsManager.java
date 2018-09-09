/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.net;

import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public class SessionsManager implements Observer, Runnable{
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    final Map<Integer, ISession> sessions;
    final ISessionFactory sessionFactory;
    final ExecutorService executors;    
    final Thread thread;
    
    
    public SessionsManager(ISessionFactory sessionFactory){
        this.sessions = new ConcurrentHashMap<>();
        this.sessionFactory = sessionFactory;
        this.executors = Executors.newFixedThreadPool( Runtime.getRuntime().availableProcessors() );
        this.thread = new Thread( this, this.getClass().getSimpleName() );
    }

    @Override
    public String toString(){
        return "SessionsManager";
    }
    
    public ISession create(SocketAddress remoteSocketAddress ){
        int sid = generateSessionId( remoteSocketAddress );
        ISession session = sessions.get( sid );
        if (session!=null) remove( sid );

        session = sessionFactory.get( sid, remoteSocketAddress );
        sessions.put( sid, session );
        
        if (session instanceof Observable){
            ((Observable) session).addObserver(this);  
        }

        log.debug("new {}", session);
        
        return session;
    }
        
    public static int generateSessionId(SocketAddress remoteSocket){
        return Objects.hash( remoteSocket );
    }
            
    public void remove(int sid){
        ISession session = getSession(sid);
        if (session!=null) session.close();
        log.trace("remove {}", session);
        sessions.remove(sid);        
    }
    
    public ISession getSession(int sid){
        return sessions.get(sid);        
    }
            
    public Collection<ISession> getSessions(){
        return sessions.values();
    }

    @Override
    public void update(Observable o, Object arg) {
        ASession session = (ASession) o;

        if (arg instanceof SessionState){
            SessionState state = session.getState();
            log.trace("{} update {}, state = {}", this, session, state);
        }
    }
         
    public void start(){        
        thread.start();  
    }
    
    public void stop(){
        log.trace("{} shutdown", this);
        thread.interrupt();
        executors.shutdown();
        for(ISession session:getSessions()){
            remove(session.getSid());
        }
    }
    
    @Override
    public void run(){
        boolean worked;
        try{
            while(!thread.isInterrupted()){
                worked = false;
                for(ISession session:getSessions()){
                    if (session!=null){                        
                        if (session.isTimeout()){
                            session.close();
                        }
                        if (session.isClosed()){
                            remove(session.getSid());
                            continue;
                        }
                        if(!session.isLocked()){
                            executors.execute( session );
                            worked |= session.isWorked();
                        }
                    }
                }
                if (!worked) Thread.sleep(1);
            }
        }catch(InterruptedException ex){
        }
                    
    }
    
}
