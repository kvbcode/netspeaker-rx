/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author CyberManic
 */
public class SimpleProperties extends AProperties{
    
    public SimpleProperties(){
    }
        
    public static SimpleProperties fromLines(String lines){
        SimpleProperties props = new SimpleProperties();
        props.addFromLines(lines);
        return props;
    }

    public static SimpleProperties fromMap(Map<? extends Object, ? extends Object> propertiesMap){
        SimpleProperties props = new SimpleProperties();
        props.putAll(propertiesMap);
        return props;
    }

    public static SimpleProperties fromStream(InputStream istream){
        SimpleProperties props = new SimpleProperties();
        
        try(InputStreamReader is = new InputStreamReader(istream)){
            props.load(is);
        }catch(IOException ex){
        }                
        return props;
    }
    
    
    public static SimpleProperties fromResource(String resourceName){
        try( InputStream istream = ClassLoader.getSystemResourceAsStream(resourceName) ){
            return SimpleProperties.fromStream(istream);
        }catch(Exception ex){
        }
        return new SimpleProperties();
    }

    
    public static SimpleProperties fromFile(String filename){
        try( InputStream istream = new FileInputStream(filename) ){
            return SimpleProperties.fromStream(istream);
        }catch(Exception ex){
        }
        return new SimpleProperties();
    }

    
    @Override
    public String toString(){
        return "SimpleProperties";
    }
    
    public void addFromLines(String multilineData){
        Stream.of( multilineData.split("\n") ).forEach( line -> {
            String[] part = line.split("=");
            this.setProperty(part[0].trim(), part[1].trim());            
        });
    }

    public String getFullName(){
        return String.format("%s [{}]", this, String.valueOf(this.size()));
    }            
    
}
