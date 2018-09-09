/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cyber.util;

import java.util.function.Supplier;


/**
 *
 * @author CyberManic
 */
public class Cached<T> implements Supplier<T>{

    final TimeoutTimer timer;
    final Supplier<T> supplier;
    T obj = null;
    
    /**
     * Задает cacheTimеout в миллисекундах и поставщика объектов
     * @param cacheTimeoutMillis
     * @param supplier 
     */
    
    public Cached(int cacheTimeoutMillis, Supplier<T> supplier){
        this.timer = new TimeoutTimer(cacheTimeoutMillis);
        this.supplier = supplier;
    }
    
    /**
     * Возвращает кэшированную ссылку на объект. После cache timeout вызывает supplier и обновляет ссылку .
     * Если supplier вернул null то вернется null значение и cacheTimeout не сбросится.
     * @return Nullable
     */
    
    @Override
    public T get(){
        if (obj==null) return update();

        if (timer.isTimeout()){
            updateObject(supplier.get());
        }
        return obj;
    }
    
    /**
     * Возвращает кэшированную ссылку на объект. После cache timeout вызывает supplier и обновляет ссылку .
     * Если supplier вернул null то вернется предыдущее значение и cacheTimeout не сбросится.
     * @return Nullable
     */
    
    public T getOrPrevious(){
        if (obj==null) return update();
        
        if (timer.isTimeout()){
            T newObj = supplier.get();
            if (newObj!=null) updateObject(newObj);
        }
        return obj;        
    }

    protected T updateObject(T newObj){
        this.obj = newObj;
        if (newObj!=null) timer.update();
        return obj;
    }
            
    /**
     * Обновляет и возвращает ссылку на новый объект. Если supplier вернул null, то cache timeout не сбросится.
     * @return 
     */
    
    public T update(){
        return updateObject(supplier.get());        
    }
}
