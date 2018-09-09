/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.util;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author CyberManic
 */
public class TextUtils {

    private TextUtils(){}

    public static String filter(String multilineText, Predicate<String> predicate){
        return Stream.of( multilineText.split("\n") ).filter(predicate).collect(Collectors.joining("\n"));
    }

    public static String filterStartsWith(String multilineText, String...masks){        
        //return filter(multilineText, line -> Arrays.stream(masks).anyMatch( m -> line.startsWith(m) ));
        return filter(multilineText, line -> Stream.of(masks).anyMatch(m -> line.startsWith(m) ) );
    }
    
    public static String filterNonEmpty(String multilineText){
        return filter(multilineText, line -> !(line.trim().isEmpty()) );
    }
    
}
