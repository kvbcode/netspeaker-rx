/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.netspeaker;

import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */

@Command(sortOptions = false, subcommands = {NetSpeakerLauncher.StartServer.class, NetSpeakerLauncher.StartClient.class})
public class NetSpeakerLauncher implements Runnable{
    protected static final Logger LOG = LoggerFactory.getLogger("Launcher");

    public static enum ServerMode{
        play(s -> s.startPlayer()),
        record(s -> s.startRecorder()),
        relay(s-> s.startRelay()),
        play_and_relay(s -> s.startPlayAndRelay());
    
        private final Consumer<NetSpeakerRx.Server> strategy;        
        ServerMode(Consumer<NetSpeakerRx.Server> strategy){ this.strategy = strategy; }   
        public void apply(NetSpeakerRx.Server server){ strategy.accept(server); };
    }
        
    public static enum ClientMode{
        play(c -> c.startPlayer()),
        record(c -> c.startRecorder());
        
        private final Consumer<NetSpeakerRx.Client> strategy;        
        ClientMode(Consumer<NetSpeakerRx.Client> strategy){ this.strategy = strategy; }
        public void apply(NetSpeakerRx.Client client){ strategy.accept(client); };
    }
    
    private NetSpeakerLauncher(){ }

    public static void main(String[] args) throws Exception{        
       
        CommandLine cmdRoot = new CommandLine(NetSpeakerLauncher.class);        
        
        ArrayDeque<CommandLine> cmdArr = new ArrayDeque<>(cmdRoot.parse(args));
        CommandLine subCmd = cmdArr.getLast();                 
        CommandLine.run(subCmd.getCommand(), args);
        
    }    

    // запуск если ни одной команды не было задано
    @Override
    public void run() {
        CommandLine cmdRoot = new CommandLine(NetSpeakerLauncher.class);        
        CommandLine.usage(NetSpeakerLauncher.class, System.out);
        cmdRoot.getSubcommands().forEach( (k,v) ->  v.usage(System.out));
    }

    // run если задана команда server
    @Command(name = "server", sortOptions = false)
    public static class StartServer implements Runnable{
        @Option(names = "server", hidden = true)
        boolean IsServerMode;
        
        @Option(names = "-m", paramLabel = "MODE", arity="1", required = true, description = "mode: <play|record|relay|play_and_relay>")
        ServerMode serverMode;
        
        @Option(names = "-p", paramLabel = "PORT", arity="1", defaultValue = "13801", description = "port (${DEFAULT-VALUE})")
        int port;

        @Override
        public void run() {
            LOG.info("start server: {} on port {}", serverMode, port );
            
            try{
                NetSpeakerRx.Server server = new NetSpeakerRx.Server(port);  
                serverMode.apply(server);
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }

    }

    // run если задана команда client
    @Command(name = "client", sortOptions = false)
    public static class StartClient implements Runnable{
        @Option(names = "client", hidden = true)
        boolean IsClientMode;

        @Option(names = "-m", paramLabel = "MODE", arity="1", required = true, description = "mode: <play|record>")
        ClientMode clientMode;
        
        @Option(names = "-r", paramLabel = "REMOTEHOST", arity="1", required = true, description = "remote hostname or ip")
        String host;
        
        @Option(names = "-p", paramLabel = "PORT", arity="1", defaultValue = "13801", description = "port (${DEFAULT-VALUE})")
        int port;

        @Override
        public void run() {
            LOG.info("start client: {}. Connect to {}:{}", clientMode, host, port );

            try{
                NetSpeakerRx.Client client = new NetSpeakerRx.Client(host, port);                
                clientMode.apply(client);
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
        
    }
    
    
}
