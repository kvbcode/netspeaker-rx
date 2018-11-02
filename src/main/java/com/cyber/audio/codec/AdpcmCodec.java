/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.codec;

import javax.sound.sampled.AudioFormat;

/*
	A simple, good-enough-in-1995 quality audio compression algorithm.
	The basic algorithm encodes the difference between each sample into
	4 bits, increasing or decreasing the quantization step size depending
	on the size of the difference. A 16-bit stereo sample is neatly packed
	into 1 byte.

    based on ImaAdpcm codec (20101025 (c)2010 mumart@gmail.com)
    rewritten by CyberManic 20181101
*/


public class AdpcmCodec implements AudioCodec{

    protected final AudioFormat format;

    private static final int[] STEP_SIZE_TABLE = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

	private static final byte[] STEP_DELTA_TABLE = {
		8, 6, 4, 2, -1, -1, -1, -1, -1, -1, -1, -1, 2, 4, 6, 8
	};    

    
    public AdpcmCodec(AudioFormat format){
        this.format = format;
    }

    @Override
    public String getFullName() {
        return "ADPCM AudioCodec [16bit LE Stereo only]";
    }

    @Override
    public byte[] encode(byte[] input) {
		int inputIdx = 0, outputIdx = 0;
    	int lStepIdx=0, rStepIdx=0, lPredicted=0, rPredicted=0;		
        int count = input.length / 4;
        byte[] output = new byte[count];
        
        while( outputIdx < count ) {
			int lSam  = ( input[ inputIdx++ ] & 0xFF ) | ( input[ inputIdx++ ] << 8 );
			int rSam  = ( input[ inputIdx++ ] & 0xFF ) | ( input[ inputIdx++ ] << 8 );
			int lStep = STEP_SIZE_TABLE[ lStepIdx ];
			int rStep = STEP_SIZE_TABLE[ rStepIdx ];
			int lCode = ( ( lSam - lPredicted ) * 4 + lStep * 8 ) / lStep;
			int rCode = ( ( rSam - rPredicted ) * 4 + rStep * 8 ) / rStep;
			if( lCode > 15 ) lCode = 15;
			if( rCode > 15 ) rCode = 15;
			if( lCode <  0 ) lCode =  0;
			if( rCode <  0 ) rCode =  0;
			lPredicted += ( ( lCode * lStep ) >> 2 ) - ( ( 15 * lStep ) >> 3 );
			rPredicted += ( ( rCode * rStep ) >> 2 ) - ( ( 15 * rStep ) >> 3 );
			if( lPredicted >  32767 ) lPredicted =  32767;
			if( rPredicted >  32767 ) rPredicted =  32767;
			if( lPredicted < -32768 ) lPredicted = -32768;
			if( rPredicted < -32768 ) rPredicted = -32768;
			lStepIdx += STEP_DELTA_TABLE[ lCode ];
			rStepIdx += STEP_DELTA_TABLE[ rCode ];
			if( lStepIdx > 88 ) lStepIdx = 88;
			if( rStepIdx > 88 ) rStepIdx = 88;
			if( lStepIdx <  0 ) lStepIdx =  0;
			if( rStepIdx <  0 ) rStepIdx =  0;
			output[ outputIdx++ ] = ( byte ) ( ( lCode << 4 ) | rCode );
		}
        
        return output;
    }

    @Override
    public byte[] decode(byte[] input) {
    	int lStepIdx=0, rStepIdx=0, lPredicted=0, rPredicted=0;		
        int count = input.length;
        byte[] output = new byte[count*4];

        int inputIdx = 0, outputIdx = 0, outputEnd = count * 4;

        while( outputIdx < outputEnd ) {
			int lCode = input[ inputIdx++ ] & 0xFF;
			int rCode = lCode & 0xF;
			lCode = lCode >> 4;
			int lStep = STEP_SIZE_TABLE[ lStepIdx ];
			int rStep = STEP_SIZE_TABLE[ rStepIdx ];
			lPredicted += ( ( lCode * lStep ) >> 2 ) - ( ( 15 * lStep ) >> 3 );
			rPredicted += ( ( rCode * rStep ) >> 2 ) - ( ( 15 * rStep ) >> 3 );
			if( lPredicted >  32767 ) lPredicted =  32767;
			if( rPredicted >  32767 ) rPredicted =  32767;
			if( lPredicted < -32768 ) lPredicted = -32768;
			if( rPredicted < -32768 ) rPredicted = -32768;
			output[ outputIdx++ ] = ( byte )   lPredicted;
			output[ outputIdx++ ] = ( byte ) ( lPredicted >> 8 );
			output[ outputIdx++ ] = ( byte )   rPredicted;
			output[ outputIdx++ ] = ( byte ) ( rPredicted >> 8 );
			lStepIdx += STEP_DELTA_TABLE[ lCode ];
			rStepIdx += STEP_DELTA_TABLE[ rCode ];
			if( lStepIdx > 88 ) lStepIdx = 88;
			if( rStepIdx > 88 ) rStepIdx = 88;
			if( lStepIdx <  0 ) lStepIdx =  0;
			if( rStepIdx <  0 ) rStepIdx =  0;
		}
        return output;
    }

}
