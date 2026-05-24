package com.openquartz.easyarchive.common.entity;

import lombok.Data;

@Data
public class Pair<K,V> {

    private K key;
    private V value;

    private Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getLeft(){
        return key;
    }

    public V getRight(){
        return value;
    }

    public static <K,V> Pair<K,V> of(K k,V v){
        return new Pair<>(k,v);
    }
}
