/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.file;

import com.cyber.file.impl.FilesOnlyVisitor;
import com.cyber.file.impl.DirOnlyVisitor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author CyberManic
 */
public class FilenameUtils {

    private FilenameUtils(){
    
    }
    
    public static String getExtension(String fileName) {
        if (fileName==null) return "";        
        int len = fileName.length();
        char ch = fileName.charAt(len-1);        
        if ( len==0 || ch=='/' || ch=='\\' || ch=='.' ) return "";
        
        int dotInd = fileName.lastIndexOf('.');
        int sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        
        if( dotInd<=sepInd )
            return "";
        else
            return fileName.substring( dotInd+1 ).toLowerCase();
    }    
        
    public static boolean isExtension(String fileName, String extension){
        return extension.equals( getExtension(fileName) );
    }
    
    /**
     * Возвратить список абсолютных путей файлов включая поддиректории.
     * @param initDir
     * @return 
     */
    
    public static List<Path> getRecursiveFilesList(Path initDir){
        final List<Path> files = new ArrayList<>();
        try{
            Files.walkFileTree( initDir, new FilesOnlyVisitor( (file, attr) -> files.add(file.toAbsolutePath().normalize() ) ) );
        }catch(IOException ex){            
        }
        return files;
    }
    
    /**
     * Возвратить список абсолютных путей поддиректории
     * @param initDir
     * @return 
     */
    
    public static List<Path> getRecursiveDirList(Path initDir){
        final List<Path> dirs = new ArrayList<>();
        try{
            Files.walkFileTree( initDir, new DirOnlyVisitor( (dir, attr) -> dirs.add(dir.toAbsolutePath().normalize() ) ) );
        }catch(IOException ex){            
        }
        return dirs;
    }

    /**
     * Возвратить список относительных путей файлов включая поддиректории
     * @param initDir
     * @return 
     */
    
    public static List<Path> getRelativeFilesList(Path initDir){
        return relativize(getRecursiveFilesList(initDir), initDir.toAbsolutePath().normalize());
    }
    
    /**
     * Возвратить список относительных путей поддиректорий
     * @param initDir
     * @return 
     */
    
    public static List<Path> getRelativeDirList(Path initDir){
        return relativize(getRecursiveDirList(initDir), initDir.toAbsolutePath().normalize());
    }
    
    /**
     * Возвратить список путей из pathsList преобразованых относительно корневого пути rootPath
     * @param pathsList
     * @param rootPath
     * @return 
     */
    
    public static List<Path> relativize(List<Path> pathsList, final Path rootPath){                
        return pathsList
                .stream()
                .map( p -> rootPath.relativize(p) )
                .filter(p -> !p.toString().isEmpty())
                .collect(Collectors.toList());
    }
    
    
    public static String setUnixPathSeparator(String path){
        String ret = path.replace('\\', '/');
        return ret;
    }
    
    public static String getUserHomeDir(){
        return System.getProperty("user.home");
    }
    
    public static File getUserHomeFile(String appName, String filename){
        return Paths.get( getUserHomeDir(), appName, filename).toFile();
    }
}
