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
import com.cyber.net.rx.UdpChannel;
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
    protected static final long SERVER_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
    protected final byte[] PING_BYTES = "PING".getBytes();
    
    public final AudioFormat format;
    public final AudioCodec codec;
    
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
        
    public static void main(String[] args) throws Exception{        
        log.info("NetSpeakerCore " + Arrays.toString(args));        
        
        String runMode = args[0].toLowerCase();

        switch (args.length) {
            case 2:{
                //client
                String addr = args[1];
                switch(runMode){
                    case "play":
                        new NetSpeakerRx().startClientPlayer(addr, netPort);
                        break;
                    case "record":
                        new NetSpeakerRx().startClientRecorder(addr, netPort);
                        break;
                }
                break;
            }
            case 1:{
                //server
                switch(runMode){
                    case "play":
                        new NetSpeakerRx().startServerPlayer(netPort);
                        break;
                    case "record":
                        new NetSpeakerRx().startServerRecorder(netPort);
                        break;
                    case "multicast":
                        new NetSpeakerRx().startServerMulticast(netPort);
                        break;
                    case "play_and_multicast":
                        new NetSpeakerRx().startServerPlayAndMulticast(netPort);
                        break;
                }
                break;
            }
            default:
                log.info("server mode:");
                log.info("netspeaker <play|record|multicast|play_and_multicast>");                
                log.info("client mode:");
                log.info("netspeaker <play|record> <serverIp>");
                break;
        }
        
    }        
    
    public static Disposable logSpeed(Observable<byte[]> flow, String title){
        return flow
            .map(b -> b.length)
            .buffer(10, TimeUnit.SECONDS)
            .map(values -> values.stream().reduce(0, (a, v) -> v+a ))
            .subscribe( d -> {
                if (d>0) log.debug("{}, {} ({} Kb/s)", title, d, String.format("%.2f", d / 10.0 / 1024.0));
            });
    }
    
    
    public void startServerPlayer(int port) throws IOException{
        log.info("start server (player) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);
        
        Supplier<AudioPlayerRx> playerFactory = () -> new AudioPlayerRx(format);

        server.observeConnection()
            .subscribe( conn -> {
                
                conn.getFlow()
                    .map( b -> codec.decode(b) )
                    .subscribeWith( playerFactory.get() );
            });
        
        server.observeConnection()                
            .subscribe(conn -> logSpeed(conn.getFlow(), "udp.in: " + conn.toString()) );
        
        server.observeConnection()
            .subscribe( conn -> log.info("new connection: {}", conn) );
        
    }

    public void startServerRecorder(int port) throws IOException{
        log.info("start server (recorder) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);        

        AudioRecorderRx recorder = new AudioRecorderRx(format);
        recorder.setRestartInterval( TimeUnit.HOURS.toSeconds(6) );
        recorder.setSilenceFilterValue(16);
        recorder.start();        
                
        server.observeConnection()
            .subscribe( conn -> {

                recorder.getFlow()
                    .map(rawAudioData -> codec.encode(rawAudioData))
                    .subscribeWith( conn );
                
            } );
                
        server.observeConnection()
            .subscribe( conn -> log.info("new connection: {}", conn) );
    }
    
    
    public void startServerMulticast(int port) throws IOException{
        log.info("start server (multicast) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);        
        
        server.observeConnection()
            .subscribe( conn -> {

                conn.getFlow()
                    .doOnTerminate( () -> log.info("connection closed: {}", conn ) )    
                    .subscribe(data -> {
                        server.getChannels().iterate()
                            .filter(e -> e.getValue()!= conn)
                            .subscribe(e -> e.getValue().onNext(data));
                    });
                
            } );
        
        server.observeConnection()
            .subscribe(conn -> log.info("new connection: {}", conn));
        
    }
    
    public void startServerPlayAndMulticast(int port) throws IOException{
        log.info("start server (play and multicast) on port: " + port);
        
        final UdpServer server = new UdpServer(port);
        server.setTimeout(SERVER_TIMEOUT);        

        Supplier<AudioPlayerRx> playerFactory = () -> new AudioPlayerRx(format);

        server.observeConnection()
            .subscribe( conn -> {

                conn.getFlow()
                    .filter( data -> !Arrays.equals( data, PING_BYTES) )
                    .map( b -> codec.decode(b) )
                    .subscribeWith( playerFactory.get() );

                conn.getFlow()
                    .doOnTerminate( () -> log.info("connection closed: {}", conn ) )    
                    .subscribe(data -> {
                        server.getChannels().iterate()
                            .filter(e -> e.getValue()!= conn)
                            .subscribe(e -> e.getValue().onNext(data));
                    });
                
            } );

        server.observeConnection()
            .subscribe(conn -> log.info("new connection: {}", conn));
        
    }
    
    public void startClientRecorder(String host, int port) throws IOException{
        SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
        
        log.info("client (recorder) connecting to " + remoteSocketAddress);

        UdpChannel conn = UdpClient.connect( remoteSocketAddress );
        
        AudioRecorderRx recorder = new AudioRecorderRx(format);
        recorder.setRestartInterval( TimeUnit.HOURS.toSeconds(6) );
        recorder.setSilenceFilterValue(16);
        recorder.start();        
        
        recorder.getFlow()
            .map(rawAudioData -> codec.encode(rawAudioData))
            .subscribeWith( conn );
        
    }

    /*
        В режиме мультикаста клиент-плеер никак не выдает свое состояние серверу
        поэтому необходимо отправить данные для начала коннекта
        и периодически уведомлять сервер о keep-alive
    */
    
    public void startClientPlayer(String host, int port) throws IOException{
        SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
        
        log.info("client (player) connecting to " + remoteSocketAddress);

        UdpChannel conn = UdpClient.connect( remoteSocketAddress );
        
        AudioPlayerRx player = new AudioPlayerRx(format);
                
        Disposable keepAlive = Observable.interval(SERVER_TIMEOUT/4, TimeUnit.MILLISECONDS)
            .subscribe(i -> {
                log.debug("send ping");
                conn.onNext( PING_BYTES );
            });

        conn.getFlow()
            .doOnTerminate( () -> keepAlive.dispose() )
            .map( data -> codec.decode(data) )
            .subscribeWith(player);                    
        
        // wait first data
        conn.onNext( PING_BYTES );
        conn.getFlow().blockingFirst();
        log.info("connection start");
        
    }
    
    
}
