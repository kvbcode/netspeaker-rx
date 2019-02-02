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
    protected static final Logger LOG = LoggerFactory.getLogger("NetSpeakerRx");
    protected static final long SERVER_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    protected static final byte[] PING_BYTES = "PING".getBytes();
    
    public static final AudioFormat format;
    public static final AudioCodec codec;
    
    public static Supplier<AudioPlayerRx> playerFactory;
    public static AudioRecorderRx recorder = null;

    static{
        LOG.info("NetSpeakerRx Core");
        
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        //float rate = 32000.0F;
        float rate = 44100.0F;
        int sampleBits = 16;
        int channels = 2;
        boolean bigEndian = false;
        
        format = new AudioFormat( encoding, rate, sampleBits, channels, (sampleBits/8)*channels, rate, bigEndian );
        LOG.info("AudioFormat: {}", format);
        
        //codec = new R16B12Codec(format);
        codec = new AdpcmCodec(format);
        LOG.info("Codec: {}", codec.getFullName());
        
        playerFactory = () -> new AudioPlayerRx(format);
                
    }

    synchronized public static AudioRecorderRx getAudioRecorder(){
        if (recorder==null){        
            recorder = new AudioRecorderRx(format);
            recorder.setRestartInterval( TimeUnit.HOURS.toSeconds(6) );
            recorder.setSilenceFilterValue(16);
            recorder.start();            
        }
        return recorder;
    }

    public static Disposable logSpeed(Observable<byte[]> flow, String title){
        return flow
            .map(b -> b.length)
            .buffer(10, TimeUnit.SECONDS)
            .map(values -> values.stream().reduce(0, (a, v) -> v+a ))
            .subscribe(d -> {
                if (d>0) LOG.debug("{}, {} ({} Kb/s)", title, d, String.format("%.2f", d / 10.0F / 1024.0F));
            });
    }
    
    public static class Server{
        final UdpServer server;

        public Server(int port) throws IOException{
            LOG.info("start server on port: " + port);

            server = new UdpServer(port);
            server.setTimeout(SERVER_TIMEOUT);

            server.observeConnection()
                .subscribe(conn -> LOG.info("new connection: {}", conn) );        
        }
        
        public void startPlayer(){            
            LOG.info("server mode: PLAY");
            
            server.observeConnection()
                .subscribe( conn -> {

                    conn.getFlow()
                        .map( b -> codec.decode(b) )
                        .subscribeWith( playerFactory.get() );
                });
        }

        public void startRecorder(){
            LOG.info("server mode: RECORD");
            
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
        }
        
        public void startRelay(){
            LOG.info("server mode: RELAY");

            server.observeConnection()
                .subscribe(conn -> {

                    conn.getFlow()
                        .doOnTerminate(() -> LOG.info("connection closed: {}", conn ) )    
                        .subscribe(data -> {
                            server.getChannels().iterate()
                                .filter(e -> e.getValue()!= conn)
                                .subscribe(e -> e.getValue().onNext(data));
                        });

                } );
            
        }
        
        public void startPlayAndRelay(){
            LOG.info("server mode: PLAY_AND_RELAY");            
            
            server.observeConnection()
                .subscribe(conn -> {

                    conn.getFlow()
                        .filter( data -> !Arrays.equals( data, PING_BYTES) )
                        .map( b -> codec.decode(b) )
                        .subscribeWith( playerFactory.get() );

                    conn.getFlow()
                        .doOnTerminate(() -> LOG.info("connection closed: {}", conn ) )    
                        .subscribe(data -> {
                            server.getChannels().iterate()
                                .filter(e -> e.getValue()!= conn)
                                .subscribe(e -> e.getValue().onNext(data));
                        });

                } ); 
            
        }
        
        public void enableSpeedLogging(){
            server.observeConnection()                
                .subscribe(conn -> logSpeed(conn.getFlow(), "udp.in: " + conn.toString()) );
        }
        
    }
    
    
    public static class Client{
        SocketAddress remoteSocketAddress;
        UdpChannel conn;
                
        public Client(String host, int port) throws IOException{
            remoteSocketAddress = new InetSocketAddress(host, port);
            LOG.info("start client, connecting to " + remoteSocketAddress);

            conn = UdpClient.connect( remoteSocketAddress );
            
        }
        
        public void startRecorder(){
            LOG.info("client mode: RECORDER");

            getAudioRecorder().getFlow()
                .map(rawAudioData -> codec.encode(rawAudioData))
                .subscribeWith( conn );
        }
        
        public void startPlayer(){
            LOG.info("client mode: PLAYER");
            
            AudioPlayerRx player = playerFactory.get();
            
            /*
            Клиент-плеер никак не выдает свое состояние серверу
            поэтому необходимо отправить данные для начала коннекта
            и периодически уведомлять сервер о keep-alive
            */

            Disposable keepAlive = Observable.interval(10, TimeUnit.SECONDS)
                .subscribe(i -> {
                    LOG.debug("send ping");
                    conn.onNext( PING_BYTES );
                });
            
            conn.getFlow()
                .doOnTerminate( () -> keepAlive.dispose() )
                .map( data -> codec.decode(data) )
                .subscribeWith(player);                    

            // wait first data
            conn.onNext( PING_BYTES );
            conn.getFlow().blockingFirst();
            LOG.info("connection start");
            
        }
        
    }

        
}
