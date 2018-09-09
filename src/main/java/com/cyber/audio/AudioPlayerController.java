/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio;

import com.cyber.audio.AudioPlayer;
import com.cyber.audio.dsp.INormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author CyberManic
 */
public class AudioPlayerController{
    protected final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    protected final AudioPlayer player;
    
    public AudioPlayerController(AudioPlayer player){
        this.player = player;
    }

    public void normalizerGainDown(){        
        INormalizer norm = player.getNormalizer();
        if (norm != null){
            norm.gainDown();
            norm.gainInfo();
        }
    }
    
    public void normalizerGainUp(){    
        INormalizer norm = player.getNormalizer();
        if (norm != null){
            norm.gainUp();
            norm.gainInfo();
        }
    }
    
    public void setNormalization(boolean val){
        player.setNormalization(val);
    }

    public void toggleNormalization(){
        if (player.getNormalizer()!=null){
            player.setNormalization(false);
        }else{
            player.setNormalization(true);
        }
    }
    
    public void setVolume(float val){
        player.setVolume(val);
    }

    public void volumeDown(float delta){
        player.setVolume(player.getVolume() - delta);
    }

    public void volumeUp(float delta){
        player.setVolume(player.getVolume() + delta);
    }
    
    public void stopPlayer(){
        player.stopPlayer();
    }
    
    public void exec(String cmdString){
        if (cmdString.isEmpty()) return;
        
        String[] buf = cmdString.split(":");
        String cmd = buf[0];        
        
        log.trace("cmd={}", cmd);
        
        switch(cmd){
            case "0":
                this.setVolume(0.0F);
                break;
            case "stop":
                this.stopPlayer();
                break;
            case "v+":
                this.volumeUp(2.0F);
                break;
            case "+":
                this.volumeUp(2.0F);
                break;
            case "++":
                this.volumeUp(4.0F);
                break;
            case "+++":
                this.volumeUp(6.0F);
                break;
            case "v-":
                this.volumeDown(2.0F);
                break;
            case "-":
                this.volumeDown(2.0F);
                break;
            case "--":
                this.volumeDown(4.0F);
                break;
            case "---":
                this.volumeDown(6.0F);
                break;
            case "n":
                this.toggleNormalization();
                break;
            case "n0":
                this.setNormalization(false);
                break;
            case "n1":
                this.setNormalization(true);
                break;
            case "n+":
                this.normalizerGainUp();
                break;
            case "n-":
                this.normalizerGainDown();
                break;         
            default:
                log.trace("cmd='{}' not recognized", cmd);
        }
    }
    
}
