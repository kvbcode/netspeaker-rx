/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.dsp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Экспериментальный нормализатор громкости с компрессором.
 * @author CyberManic
 */
public class AutoGain{
    protected final Logger Log = LoggerFactory.getLogger(this.getClass().getSimpleName());    
    
    protected float gainAvg = 0.0F;
    protected final float gainMult = 0.025F;
    protected float gainMin = 0.1F;
    protected float gainMaxFinal = 5.0F;
    protected float gainMax = gainMaxFinal;
    protected float gain = 1.0F;
    protected float gainLast = gain;
    protected long gainMaxUpdateTime = System.currentTimeMillis();
    protected long gainUpdateTime = System.currentTimeMillis();    
    protected final long gainUpdateInterval = 1000;
    protected final long gainMaxUpdateInterval = 10000;
    protected final int silenceValue = 16;
    
    protected float avgDemandFloat = 0.10F;
    protected float avgDemandStep = 0.025F;    
    
    protected int vmaxPrev = silenceValue;
    protected float vmaxBoostPrev = 1.0F;
    protected float signalPrev = 0;
    protected int volumeStepsPrev = 0;
    protected int valuePrev = 0;    

    protected final int STEP = 15;
    protected final IntStatArray statArr = new IntStatArray(8*1024);
    protected ByteOrder bufOrder = ByteOrder.nativeOrder();

    
    private class IntStatArray{
        int[] arr;
        int pos = 0;
        int capacity = 0;
        
        IntStatArray(int capacity){
            this.capacity = capacity;
            arr = new int[capacity];
        }
        
        public void add(int value){
            arr[pos++] = value;
        }
        
        public void clear(){
            pos = 0;
            capacity = 0;
        }
    
        public int getMax(){
            return IntStream.of(arr).limit(pos).max().orElse(0);
        }
        
        public int getMathAvg(){
            return (int)IntStream.of(arr).limit(pos).average().orElse(0.0);
        }
        
    }    
    
    public AutoGain(boolean bigEndian){
        this.bufOrder = bigEndian? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN;
    }

    
    public String getFullInfo(){
        return String.format("%s [min=%.2f max=%.2f, avg=%.2f]", this.toString(), gainMin, gainMaxFinal, avgDemandFloat);
    }
        
    
    public byte[] proceed(byte[] input){
        
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length);
        out.order(bufOrder);

        int avgDemandValue = getAvgDemandAbs();   
        int compThreshold = (int)(avgDemandValue * 1.33);
        
        short v0,v1;
        short p0=0,p1=0;
        int d0=0,d1=0;
        int i = 0;
        int vmax=0;

        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();
                        
            v0 *= gain;
            v1 *= gain;
                        
            d0 = v0 - p0;
            d1 = v1 - p1;
            
            vmax = Math.max( vmax, Math.abs(d0));
            vmax = Math.max( vmax, Math.abs(d1));
            
            if (i%STEP==0){
                statArr.add(vmax);
            }
            
            p0 = v0;
            p1 = v1;
            i++;
        }
        
        updateGain(vmax);
        in.rewind();
        
        while(in.remaining()>=4){        
            v0 = in.getShort();
            v1 = in.getShort();

            gain = clippingProtection(v0, gain);
            
            v0 *= gain;
            v1 *= gain;
                                
            v0 = compressor(v0, compThreshold);
            v1 = compressor(v1, compThreshold);
            
            out.putShort(v0);
            out.putShort(v1);            
        }

        gainLast = gain;
        
        out.flip();           
        return out.array();        
    }
    
    
    protected float clippingProtection(short signalValue, float gain){
        // оперативное снижение усиления до 95% от исходного за раз, для предотвращения перегрузки сигнала
        while(signalValue * gain > (Short.MAX_VALUE * 0.9)){
            gain *= 0.95;

            if (gainMax < gainMin) gainMax = gainMin;

            if (gain < gainMin){
                Log.info("{} warning: clipping! gain decreased to gainMin {}", this, String.format("%.4f", gainMin));
                gain = gainMin;
                break;
            }
            Log.info("{} warning: clipping! gain decreased to {}", this, String.format("%.4f", gain));
        }
        return gain;
    }
    
    protected short compressor(short signalValue, int threshold){
        int sign = signalValue<0 ? -1 : 1;
        int value = Math.abs(signalValue);
        int overValue = value - threshold;
        
        if (overValue<0) return signalValue;
        
        overValue /= 4;        
        value = sign * (threshold + overValue);

        //log.trace("in={}, out={}", signalValue, value);
        
        return (short)value;
    }
    
    
    protected void updateGain(int shortMax){        
        int value = shortMax;
        int vmax = value;
        int vavg = value;
        int avgDemandValue = getAvgDemandAbs();   

        if (shortMax < avgDemandValue){        
            if ((System.currentTimeMillis() - gainUpdateTime) < gainUpdateInterval) return;
            vmax = statArr.getMax();
            if (vmax < silenceValue){
                statArr.clear();
                return;                
            }
        }
        
        
        vavg = statArr.getMathAvg();
        value = (vavg + vmax) / 2;
        statArr.clear();  
        gainUpdateTime = System.currentTimeMillis();                        
        
        float vmaxBoost = (float)vmax / vmaxPrev;
        float signal = 1.0F - (float)vavg / value;
        int volumeSteps = 0;
                
        // Noise detect
        /*if (signal < 150 && signalPrev < 150){
            return;
        }*/       
        
        
        if (value < avgDemandValue){
            if (vmaxBoost < 0.55){
                if (volumeSteps<0){
                    volumeSteps = 4;
                    gain = (float)gainAvg;
                }else{
                    volumeSteps = 1;                
                    gain *= 1.0 + gainMult;                                    
                }
            }else{
                if (value < avgDemandValue * 0.40){
                    if (volumeStepsPrev>=2){
                        if (valuePrev < avgDemandValue * 0.40){
                            volumeSteps = 5;
                            gain *= 1.5;
                            gainAvg = gain;
                        }else{
                            volumeSteps = 3;
                            gain *= 1.0 + gainMult * 3;                                            
                        }                        
                    }else{
                        volumeSteps = 2;
                        gain *= 1.0 + gainMult * 2;
                    }
                }else if (value < avgDemandValue * 0.75){
                    if (volumeStepsPrev>0){
                        volumeSteps = 2;                
                        gain *= 1.0 + gainMult * 2;                
                    }else{
                        volumeSteps = 2;                
                        gain *= 1.0 + gainMult * 1.5;
                    }
                 }else{
                    volumeSteps = 1;                
                    gain *= 1.0 + gainMult;
                 }       
            }
        }else{
            if (vmaxBoost > 2.0){
                float gainBoostMult = (float)avgDemandValue / value;
                if (gainBoostMult < 0.75F) gainBoostMult = 0.75F;
                gain *= gainBoostMult;
                volumeSteps = -5;     
            }else{
                if (value > avgDemandValue * 1.5){
                    volumeSteps = -3;                
                    gain *= 1.0 - gainMult * 2;                
                }else if (value > avgDemandValue * 1.25){
                    volumeSteps = -2;                
                    gain *= 1.0 - gainMult * 1.5;                
                 }else{            
                    volumeSteps = -1;                
                    gain *= 1.0 - gainMult;
                 }            
            }
        }
        
        
        gain = gain > gainMax ? gainMax : gain;
        gain = gain < gainMin ? gainMin : gain;

        
        if ((System.currentTimeMillis() - gainMaxUpdateTime) > gainMaxUpdateInterval){
            if (gainMax < gainMaxFinal){
                gainMax *= 1.0 + gainMult;
                if (gainMax > gainMaxFinal){
                    gainMax = gainMaxFinal;
                }
            }
            gainMaxUpdateTime = System.currentTimeMillis();
        }

        updateGainAvg(gain);
        
        Log.debug("gain={} ({}), gainAvg={}, value={} ({}), vavg={}, vmax={}, vmaxBoost={}, signal={}, steps = {}",
                            String.format("%.4f", gain), String.format("%.2f", gainMax), String.format("%.4f", gainAvg),
                            String.format("%5d", value), String.format("%5d", avgDemandValue),
                            String.format("%5d",vavg), String.format("%5d",vmax), String.format("%.2f", vmaxBoost),
                            String.format("%.4f", signal), getVolumeStepsText(volumeSteps));

        valuePrev = value;
        vmaxPrev = vmax;
        vmaxBoostPrev = vmaxBoost;
        signalPrev = signal;
        volumeStepsPrev = volumeSteps;
    }
    
    
    protected float updateGainAvg(float gainValue){
        if (gainAvg == 0.0){
            gainAvg = gainValue;
        }else{
            gainAvg = (gainAvg*2 + gainValue) / 3;
        }
        
        return gainAvg;
    }
    
    public void setTargetGainValue(float targetValue){
        this.avgDemandFloat = targetValue;
    }
    
    protected String getVolumeStepsText(int value){
        if (value==0) return "0";
        
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<Math.abs(value); i++){
            if (value<0){
                sb.append("-");
            }else{
                sb.append("+");
            }
        }
        return sb.toString();
    }
    
    public void gainUp(){
        avgDemandFloat += avgDemandStep;
    }
            
    public void gainDown(){
        avgDemandFloat -= avgDemandStep;
    }
    
    protected int getAvgDemandAbs(){
        return (int)(Short.MAX_VALUE * avgDemandFloat);            
    }
            
    public String gainInfo(){
        return String.format("{} threshold({}, {})", this, String.format("%.4f", avgDemandFloat), getAvgDemandAbs());        
    }
        
}
