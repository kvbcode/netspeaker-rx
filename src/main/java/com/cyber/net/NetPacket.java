/**
 * NetPacket.java
 *
 * Created on 18-07-2013 06:59 PM
 *
 */
package com.cyber.net;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;


public class NetPacket implements Externalizable, Comparable<NetPacket>{
    protected int cachedFullHashCode = 0;
    protected int dataHashCode = 0;
    protected volatile boolean validHashCode = false;
    protected volatile boolean validDataHashCode = false;
    
    final static int HEADER_SIZE = (Short.BYTES * 2) + (Integer.BYTES * 3);
    
    final public short version = 3;
    protected int id = Integer.MIN_VALUE;
    protected short type = 0;
    protected int context = 0;
    protected int datalen = 0;
    protected byte[] data = null;

        
    public NetPacket(){
    }

    public NetPacket(short type){
        setType(type);
    }

    public NetPacket(short type, int context){
        setType(type);
        setContext(context);
    }
    
    public NetPacket(byte[] data){
        setData(data);
    }
    
    public NetPacket(short type, byte[] data){
        setType(type);
        setData(data);
    }

    public NetPacket(int id, short type, int context, byte[] data){
        this();
        setId(id);
        setType(type);
        setContext(context);
        setData(data);
    }
    
    @Override
    public String toString(){
        return String.format("NetPacket[id=%d, type=%d, context=%d, datalen=%d]", id, type, context, datalen);
    }

    @Override
    public int compareTo(NetPacket p){
        if (this.id < p.id)
        {
            return -1;
        }
        if (this.id > p.id)
        {
            return 1;
        }
        return 0;
    }   
    
    public static NetPacket fromBytes(byte[] rawpacket){
        NetPacket ret = null;
        if (rawpacket==null) return ret;
        
        try(ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rawpacket));){
            ret = (NetPacket)in.readObject();
        }catch(IOException | ClassNotFoundException ex){
        }
        return ret;
    }
    
    public byte[] toBytes(){
        byte[] ret = null;
        try(ByteArrayOutputStream barout = new ByteArrayOutputStream(HEADER_SIZE + datalen);
            ObjectOutputStream objout = new ObjectOutputStream(barout);){
            objout.writeObject(this);
            ret = barout.toByteArray();
        }catch(IOException ex){
        }
        return ret;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);
        
        out.writeInt(id);
        out.writeShort(type);
        out.writeInt(context);
        out.writeInt(datalen);
        if (datalen>0) out.write(data, 0, datalen);        
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        short version = in.readShort();
        if (this.version!=version) throw new IOException("incorrect protocol version");
        
        id = in.readInt();
        type = in.readShort();
        context = in.readInt();
        datalen = in.readInt();
        if (datalen>0){
            data = new byte[datalen];        
            in.readFully(data, 0, datalen);
        }
    }

    @Override
    public boolean equals(Object obj){
        if (obj==this) return true;
        if (obj==null || !(this.getClass().equals(obj.getClass()))) return false;
        
        NetPacket p = (NetPacket)obj;
        return ( (id == p.getId() ) && similar(p) );
    }
    
    public boolean similar(NetPacket p){
        return type == p.getType() &&
                context == p.getContext() &&
                datalen == p.getDataLength() &&
                getDataHashCode() == p.getDataHashCode();
    }
    
    @Override
    public int hashCode(){
        if (!validHashCode) updateHashCode();
        return cachedFullHashCode;
    }

    public int getDataHashCode(){
        if (!validDataHashCode) updateDataHashCode();        
        return dataHashCode;
    }
    
    protected void updateHashCode(){        
        cachedFullHashCode = Objects.hash( id, type, context, datalen, getDataHashCode() );        
        validHashCode = true;
    }

    protected void updateDataHashCode(){        
        dataHashCode = Arrays.hashCode(data);
        validDataHashCode = true;
    }
        
    public NetPacket setId(int id){
        this.id = id;
        validHashCode = false;
        return this;
    }

    public int getId(){
        return id;
    }
    
    public NetPacket setType(short type){
        this.type = type;
        validHashCode = false;
        return this;
    }

    public int getType(){
        return type;
    }

    public NetPacket setContext(int contextId){
        this.context = contextId;
        validHashCode = false;
        return this;
    }
    
    public int getContext(){
        return this.context;
    }
    
    public NetPacket setData(byte[] source, int offset, int length){
        if (source!=null){
            data = Arrays.copyOfRange(source, offset, length);
            datalen = length;
        }else{
            datalen = 0;
            data = null;
        }
        validDataHashCode = false;
        validHashCode = false;
        return this;
    }

    public NetPacket setData(byte[] source){
        if (source!=null){
            setData(source, 0, source.length);
        }else{
            datalen = 0;
            data = null;
            validDataHashCode = false;
            validHashCode = false;
        }
        return this;
    }

    public byte[] getData(){
        return data;
    }
    
    public int getDataLength(){
        return datalen;
    }
    
    
    
    
        
}
