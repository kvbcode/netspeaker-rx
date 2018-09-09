/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.util;

import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;
/**
 *
 * @author CyberManic
 */
public class MapUtils {

    private MapUtils(){}
    
    public static List<String> toList(Map<? extends Object, ? extends Object> map){
        return map.entrySet()
            .stream()
            .map(e -> String.join("=", e.getKey().toString(), e.getValue().toString() ) )
            .collect(Collectors.toList());
    }
    
    public static String toText(Map<? extends Object, ? extends Object> map){
        return map.entrySet()
            .stream()
            .map(e -> String.join("=", e.getKey().toString(), e.getValue().toString() ) )
            .collect(Collectors.joining("\n"));        
    }
        
}
