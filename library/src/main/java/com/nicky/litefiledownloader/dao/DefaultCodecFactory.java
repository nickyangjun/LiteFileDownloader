package com.nicky.litefiledownloader.dao;

/**
 * Created by nickyang on 2018/4/16.
 */

public class DefaultCodecFactory implements CodecFactory {

    @Override
    public StreamCodec createCodec() {
        return new BufferRandomAccessFileCodec();
    }
}
