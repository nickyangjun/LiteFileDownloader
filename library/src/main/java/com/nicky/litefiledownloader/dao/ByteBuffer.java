package com.nicky.litefiledownloader.dao;

/**
 * Created by nickyang on 2018/4/14.
 */

public class ByteBuffer {
    int capacity;
    byte[] buffer;

    /**
     * 当前可以写入的位置
     */
    int position = 0;


    ByteBuffer(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException(" capacity cannot less than 0");
        buffer = new byte[capacity];
        this.capacity = capacity;
    }

    void put(byte[] dat, int offset, int counts) {
        System.arraycopy(dat, offset, buffer, position, counts);
        position += counts;
    }

    public void reset(){
        position = 0;
    }

    /**
     *
     * @return the number of remaining elements in this buffer.
     */
    final int remaining() {
        return capacity - position;
    }

    int size(){
        return position;
    }

    byte[] getBuffer(){
        return buffer;
    }
}
