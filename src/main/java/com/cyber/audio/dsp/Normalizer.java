/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.audio.dsp;

import com.cyber.array.IntArray;
import com.cyber.storage.IProperties;
import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

/**
 *
 * @author CyberManic
 */
public class Normalizer extends AudioProcessor implements INormalizer{
    protected float gainAvg = 0.0F;
    protected final float gainMult = 0.025F;
    protected float gainMin = 0.1F;
    protected float gainMaxFinal = 25.0F;
    protected float gainMax = gainMaxFinal;
    protected float gain = 1.0F;
    protected float gainLast = gain;
    protected long gainMaxUpdateTime = System.currentTimeMillis();
    protected long gainUpdateTime = System.currentTimeMillis();    
    protected final long gainUpdateInterval = 1000;
    protected final long gainMaxUpdateInterval = 10000;
    protected final int silenceValue = 16;
    
    protected final IntArray statArr = new IntArray(5000);
    protected float avgDemandFloat = 0.200F;
    protected float avgDemandStep = 0.025F;    
    
    protected int vmaxPrev = silenceValue;
    protected float vmaxBoostPrev = 1.0F;
    protected float signalPrev = 0;
    protected int volumeStepsPrev = 0;
    protected int valuePrev = 0;    
    
    
    public Normalizer(AudioFormat format){
        super(format);
    }

    @Override
    public String getFullInfo(){
        return String.format("%s [min=%.2f max=%.2f, avg=%.2f]", this.toString(), gainMin, gainMaxFinal, avgDemandFloat);
    }
    
    @Override
    public void setProperties(IProperties properties){
        super.setProperties(properties);
        
        this.gainMaxFinal = properties.getProperty("audio.player.normalization.max", gainMaxFinal);
        this.gainMax = this.gainMaxFinal;
        this.gainMin = properties.getProperty("audio.player.normalization.min", gainMin);
        this.avgDemandFloat = properties.getProperty("audio.player.normalization.avg", avgDemandFloat);
    }
    
    @Override
    public byte[] proceed(byte[] input){
        
        ByteBuffer in = ByteBuffer.wrap(input);        
        in.order(bufOrder);
        
        ByteBuffer out = ByteBuffer.allocate(input.length);
        out.order(bufOrder);
        
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
            
            if (i%15==0){
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

            clippingProtection(v0);
            
            v0 *= gain;
            v1 *= gain;
                                    
            out.putShort(v0);
            out.putShort(v1);            
        }

        gainLast = gain;
        
        out.flip();           
        return out.array();        
    }
    
    
    protected void clippingProtection(short signalValue){
            // оперативное снижение усиления до 95% от исходного за раз, для предотвращения перегрузки сигнала
            while(signalValue * gain > (Short.MAX_VALUE * 0.9)){
                gain *= 0.95;
                gainAvg *= 0.95;
                //gainMax = gain;
                
                if (gainMax < gainMin) gainMax = gainMin;
                
                if (gain < gainMin){
                    log.debug("{} warning: clipping! gain decreased to gainMin %.4f", this, gainMin);
                    gain = gainMin;
                    break;
                }
                log.debug("{} warning: clipping! gain decreased to %.4f", this, gain);
            }
    }
    
    
    protected void updateGain(int shortMax){        
        int value = shortMax;
        int vmax = value;
        int vavg = value;
        int avgDemandValue = getAvgDemandAbs();   

        if (shortMax < avgDemandValue){        
            if ((System.currentTimeMillis() - gainUpdateTime) < gainUpdateInterval) return;
            vmax = getMax(statArr);
            if (vmax < silenceValue){
                statArr.clear();
                return;                
            }
        }
        
        vavg = getMathAvg(statArr);
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
        
        log.debug("gain=%.4f (%.2f), gainAvg=%.4f, value=%5d (%5d), vavg=%5d, vmax=%5d, vmaxBoost=%.2f, signal=%.4f, steps = %s",
                            gain, gainMax, gainAvg, value, avgDemandValue, vavg, vmax, vmaxBoost, signal, getVolumeStepsText(volumeSteps));

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
            
    public void gainInfo(){
        log.trace("{%s} threshold(%.4f, %d)", this, avgDemandFloat, getAvgDemandAbs());        
    }
    
    protected int getMax(IntArray arr){
        int vmax = 0;
        
        for(int item:arr){
            vmax = Math.max(vmax, item);
        }        
        
        return vmax;
    }
    
    
    protected int getMathAvg(IntArray arr){
        int vavg = 0;
        long sum = 0L;
        int count = 0;
                
        for(int value:arr){
            sum += value;
            count ++;
        }        
        
        vavg = (int)(sum / count);
        return vavg;
    }
    
}
