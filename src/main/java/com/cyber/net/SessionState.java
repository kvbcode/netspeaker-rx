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
public enum SessionState {
    NEW,
    INIT,
    LISTEN,
    READY,
    RESET,
    CONNECTED,
    CLOSED,
    TERMINATED
}
