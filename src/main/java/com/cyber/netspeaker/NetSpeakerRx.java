/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.netspeaker;

import com.cyber.audio.*;
import com.cyber.audio.codec.AdpcmCodec;
import com.cyber.audio.codec.AudioCodec;
import com.cyber.audio.codec.R16B12Codec;
import com.cyber.net.rx.UdpConnection;
import com.cyber.net.rx.impl.UdpClient;
import com.cyber.net.rx.impl.UdpServer;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.sound.sampled.AudioFormat;


public class NetSpeakerRx {    
    protected static final String PROPERTIES_FILENAME = "app.properties";
    protected static final Logger log = LoggerFactory.getLogger("NetSpeakerRx");
    protected static int netPort = 13801;        
    protected static final long SERVER_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    
    public final AudioFormat format;
    public final AudioCodec codec;
    
    private static volatile NetSpeakerRx instance = null;
    
    private NetSpeakerRx(){
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        //float rate = 32000.0F;
        float rate = 44100.0F;
        int sampleBits = 16;
        int channels = 2;
        boolean bigEndian = false;
        
        format = new AudioFormat( encoding, rate, sampleBits, channels, (sampleBits/8)*channels, rate, bigEndian );
        log.info("AudioFormat: {}", format);
        
        //codec = new R16B12Codec(format);
        codec = new AdpcmCodec(format);
        log.info("Codec: {}", codec.getFullName());
    }
    
    
    public static NetSpeakerRx getInstance(){
        if (instance==null){
            instance = new NetSpeakerRx();
        }
        return instance;
    }
    
    public static void main(String[] args) throws IOException{        
        log.info("NetSpeakerCore " + Arrays.toString(args));        
        
        if (args.length==2){
            //client
            String runMode = args[0].toLowerCase();
            String addr = args[1];
            
            switch(runMode){
                case "play":
                    new NetSpeakerRx().startClientPlayer(addr, netPort);                                    
                    break;
                case "record":
                    new NetSpeakerRx().startClientRecorder(addr, netPort);                    
                    break;
            }            
        }else if (args.length==1){
            //server
            String runMode = args[0].toLowerCase();

            switch(runMode){
                case "play":
                    new NetSpeakerRx().startServerPlayer(netPort);                                
                    break;
                case "multicast":
                    new NetSpeakerRx().startServerMulticast(netPort);                                
                    break;
            }                        
        }else{
            log.info("server mode:");
            log.info("netspeaker <play|record|multicast>");
            log.info("client mode:");
            log.info("netspeaker <play|record> <serverIp>");
        }                
        
        log.trace("exit Main()", "");
    }        
    
    
    public void startServerPlayer(int port) throws IOException{
        log.info("start server (player) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);
        
        Supplier<AudioPlayerRx> playerFactory = () -> new AudioPlayerRx(format);

        server.observeConnection()
            .subscribe( conn -> {
                
                conn.getDownstream()                        
                    .map( b -> codec.decode(b) )
                    .subscribeWith( playerFactory.get() );

                conn.getDownstream()
                    .map(b -> b.length)
                    .buffer(10, TimeUnit.SECONDS)
                    .map(values -> values.stream().reduce(0, (a, v) -> v+a ))
                    .subscribe( d -> {
                        if (d>0) log.debug("{}, udp.in: {} ({} Kb/s)", conn, d, String.format("%.2f", d / 10.0 / 1024.0));
                    });
                
            } );
        
        
        server.observeConnection()
            .subscribe(conn -> log.info("new connection: {}", conn));
        
    }

    public void startServerMulticast(int port) throws IOException{
        log.info("start server (multicast) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);        
        
        server.observeConnection()
            .subscribe( conn -> {

                conn.getDownstream()
                    .subscribe(data -> {
                        server.getConnectionStorage().iterate()
                            .filter(e -> e.getValue()!= conn)
                            .subscribe(e -> e.getValue().getUpstream().onNext(data));
                    });
                
                conn.getDownstream()
                    .map(b -> b.length)
                    .buffer(10, TimeUnit.SECONDS)
                    .map(values -> values.stream().reduce(0, (a, v) -> v+a ))
                    .subscribe( d -> {
                        if (d>0) log.debug("{}, udp.in: {} ({} Kb/s)", conn, d, String.format("%.2f", d / 10.0 / 1024.0));
                    });
                
            } );
        
        server.observeConnection()
            .subscribe(conn -> log.info("new connection: {}", conn));
        
    }
    
    
    public void startClientRecorder(String host, int port) throws IOException{
        SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
        
        log.info("client (recorder) connect to " + remoteSocketAddress);

        UdpConnection conn = UdpClient.connect( remoteSocketAddress );
        
        AudioRecorderRx recorder = new AudioRecorderRx(format);
        recorder.setRestartInterval( 6*60*60 );
        recorder.setSilenceFilter(16);
        recorder.start();        
        
        recorder.outputSlot()
            .map(rawAudioData -> codec.encode(rawAudioData))
            .subscribeWith( conn.getUpstream() );
        
    }

    // prototype
    public void startClientPlayer(String host, int port) throws IOException{
        SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
        
        log.info("client (player) connect to " + remoteSocketAddress);

        UdpConnection conn = UdpClient.connect( remoteSocketAddress );
        
        AudioPlayerRx player = new AudioPlayerRx(format);

        final byte[] pingBytes = new byte[]{0,0,0,0};

        
        Disposable connInitSub = Observable.interval(1000, TimeUnit.MILLISECONDS)
            .doOnTerminate(() -> log.debug("end connection init"))
            .doOnNext(i -> {
                log.debug("conn.start");
                conn.getUpstream().onNext(pingBytes);
            })
            .subscribe(i -> log.debug("conn complete"),
                        err -> log.error("conn error: " + err));

        // wait first data
        conn.getDownstream().blockingFirst();

        connInitSub.dispose();
        
        conn.getDownstream()
            .map( data -> codec.decode(data) )
            .subscribeWith(player);            

        Observable.interval(SERVER_TIMEOUT/4, TimeUnit.MILLISECONDS)
            .subscribe(i -> {
                log.debug("send ping");
                conn.getUpstream().onNext(pingBytes);
            });
        
        
    }
    
    
}
