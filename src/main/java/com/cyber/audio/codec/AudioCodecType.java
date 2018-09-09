/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cyber.audio.codec;

/**
 *
 * @author CyberManic
 */
public enum AudioCodecType {
    NULL,
    ULAW,
    R16B12,
    R16B12H,
    EVENSUM,
    EVENSUM128,
    HUFFMAN,
    DEFLATE,
    DELTA2,
    LZF,
    SPEEX;    
    
    public static AudioCodecType getById(int codec_id){
        for (AudioCodecType type: AudioCodecType.values()) {
            if (type.ordinal() == codec_id) {
                return type;
            }
        }
        throw new RuntimeException(String.format("unknown AudioCodecType: %d", codec_id));
    }

    public static AudioCodecType getByName(String codecName) throws RuntimeException{
        switch (codecName.toLowerCase()){
            case "null":
                return NULL;
            case "ulaw":
                return ULAW;
            case "r16b12":
                return R16B12;
            case "r16b12h":
                return R16B12H;
            case "evensum":
                return EVENSUM;
            case "evensum128":
                return EVENSUM128;
            case "huffman":
                return HUFFMAN;
            case "deflate":
                return DEFLATE;
            case "delta2":
                return DELTA2;
            case "lzf":
                return LZF;
            case "speex":
                return SPEEX;
            default:
                throw new RuntimeException(String.format("unknown AudioCodecType: %s", codecName));
        }        
    }

}
