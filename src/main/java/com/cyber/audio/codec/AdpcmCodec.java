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
    protected final int headerSize = 8;
    protected int lPredictedEnc = 0, rPredictedEnc = 0;
    protected int lStepIdxEnc = 0, rStepIdxEnc = 0;
    

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
        int count = input.length / 4;        
		int outputEnd = count + headerSize;
        byte[] output = new byte[ outputEnd ];
        int inputIdx = 0, outputIdx = headerSize;
                
        //write header data
        output[0] = (byte)  lPredictedEnc;
        output[1] = (byte) (lPredictedEnc >> 8);
        output[2] = (byte)  rPredictedEnc;
        output[3] = (byte) (rPredictedEnc >> 8);
        output[4] = (byte)  lStepIdxEnc;
        output[5] = (byte)  rStepIdxEnc;

        while( outputIdx < outputEnd ) {
			int lSam  = ( input[ inputIdx++ ] & 0xFF ) | ( input[ inputIdx++ ] << 8 );
			int rSam  = ( input[ inputIdx++ ] & 0xFF ) | ( input[ inputIdx++ ] << 8 );
			int lStep = STEP_SIZE_TABLE[ lStepIdxEnc ];
			int rStep = STEP_SIZE_TABLE[ rStepIdxEnc ];
			int lCode = ( ( lSam - lPredictedEnc ) * 4 + lStep * 8 ) / lStep;
			int rCode = ( ( rSam - rPredictedEnc ) * 4 + rStep * 8 ) / rStep;
			if( lCode > 15 ) lCode = 15;
			if( rCode > 15 ) rCode = 15;
			if( lCode <  0 ) lCode =  0;
			if( rCode <  0 ) rCode =  0;
			lPredictedEnc += ( ( lCode * lStep ) >> 2 ) - ( ( 15 * lStep ) >> 3 );
			rPredictedEnc += ( ( rCode * rStep ) >> 2 ) - ( ( 15 * rStep ) >> 3 );
			if( lPredictedEnc >  32767 ) lPredictedEnc =  32767;
			if( rPredictedEnc >  32767 ) rPredictedEnc =  32767;
			if( lPredictedEnc < -32768 ) lPredictedEnc = -32768;
			if( rPredictedEnc < -32768 ) rPredictedEnc = -32768;
			lStepIdxEnc += STEP_DELTA_TABLE[ lCode ];
			rStepIdxEnc += STEP_DELTA_TABLE[ rCode ];
			if( lStepIdxEnc > 88 ) lStepIdxEnc = 88;
			if( rStepIdxEnc > 88 ) rStepIdxEnc = 88;
			if( lStepIdxEnc <  0 ) lStepIdxEnc =  0;
			if( rStepIdxEnc <  0 ) rStepIdxEnc =  0;
			output[ outputIdx++ ] = ( byte ) ( ( lCode << 4 ) | rCode );
		}
                                
        return output;
    }

    @Override
    public byte[] decode(byte[] input) {
        int count = input.length - headerSize;
        byte[] output = new byte[ count * 4 ];

        int inputIdx = headerSize, outputIdx = 0, outputEnd = count * 4;

        // read header
        int lPredicted  = ( input[ 0 ] & 0xFF ) | ( input[ 1 ] << 8 );
        int rPredicted  = ( input[ 2 ] & 0xFF ) | ( input[ 3 ] << 8 );        
        int lStepIdx = input[4] & 0xFF;
        int rStepIdx = input[5] & 0xFF;
        
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
