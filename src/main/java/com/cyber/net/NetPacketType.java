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
public class NetPacketType{
        final public static short ERROR = -100;        
        final public static short WARNING = -1;
        final public static short NOP = 0;
        final public static short INIT = 1;
        final public static short STOP = 2;
        final public static short RETRY = 3;
        final public static short BUSY = 4;
        final public static short KEEPALIVE = 5;
        final public static short RESET = 6;
        final public static short STATUS = 99;
        final public static short OK = 100;
        final public static short READY = 101;        
        final public static short GET = 110;
        final public static short SET = 111;
        final public static short DATA = 120;
        final public static short TEXT = 140;
        final public static short OBJECT = 160;

    public NetPacketType(){

    }

}
