package com.arsc.data;

/**
 *
 * Created by xueqiulxq on 30/07/2017.
 */

public abstract class BaseTypeChunk {

    public abstract String getChunkName();

    public abstract String buildEntry2String(ResStringPoolChunk typeStringPool, ResStringPoolChunk keyStringPool);
}
