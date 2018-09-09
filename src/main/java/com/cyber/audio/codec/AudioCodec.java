/**
 * AudioCodec.java
 *
 * Created on 23-07-2013 04:54 AM
 *
 */

package com.cyber.audio.codec;

public interface AudioCodec {

    public String getFullName();
    
    public byte[] encode(final byte[] input);

    public byte[] decode(final byte[] input);	

}
