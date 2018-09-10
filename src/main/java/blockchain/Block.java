package blockchain;

import blockchain.utils.ByteUtil;
import blockchain.utils.SHA256Util;
import lombok.Data;

import java.math.BigInteger;

/**
 * 区块结构
 */
@Data
public class Block {
    private long timestamp; //时间戳
    private String data; //区块数据
    private String prevBlockHash; //上一个区块的hash
    private String hash; //当前区块hash

    private Block(long timestamp, String data, String prevBlockHash, String hash) {
        this.timestamp = timestamp;
        this.data = data;
        this.prevBlockHash = prevBlockHash;
        this.hash = hash;
    }

    /**
     * 计算区块Hash
     * <p>
     * 注意：在准备区块数据时，一定要从原始数据类型转化为byte[]，不能直接从字符串进行转换
     */
    private void setHash() {
        byte[] prevBlockHashBytes = {};
        if (this.getPrevBlockHash() != null) {
            prevBlockHashBytes = new BigInteger(this.getPrevBlockHash(), 16).toByteArray();
        }

        byte[] headers = ByteUtil.byteMergerAll(
                prevBlockHashBytes,
                this.getData().getBytes(),
                ByteUtil.longToByte(this.getTimestamp()));

        this.setHash(SHA256Util.getSHA256StrJava(ByteUtil.byteArrayToStr(headers)));
    }

    public static Block newBlock(String data, String prevBlockHash) {
        Block block = new Block(System.currentTimeMillis(), data, prevBlockHash, null);
        block.setHash();
        return block;
    }
}
