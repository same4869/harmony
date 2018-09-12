package blockchain.pow;

import blockchain.block.Block;
import blockchain.utils.ByteUtil;
import blockchain.utils.LogUtil;
import org.apache.commons.codec.digest.DigestUtils;
import lombok.Data;

import java.math.BigInteger;

/**
 * 工作量证明
 */
@Data
public class ProofOfWork {
    /**
     * 难度目标位
     */
    public static final int TARGET_BITS = 20;

    /**
     * 区块
     */
    private Block block;
    /**
     * 难度目标值
     */
    private BigInteger target;

    private ProofOfWork(Block block, BigInteger target) {
        this.block = block;
        this.target = target;
    }

    /**
     * 创建新的工作量证明，设定难度目标值
     * <p>
     * 对1进行移位运算，将1向左移动 (256 - TARGET_BITS) 位，得到我们的难度目标值
     *
     * @param block
     * @return
     */
    public static ProofOfWork newProofOfWork(Block block) {
        BigInteger targetValue = BigInteger.valueOf(1).shiftLeft((256 - TARGET_BITS));
        return new ProofOfWork(block, targetValue);
    }

    /**
     * 准备数据
     * <p>
     * 注意：在准备区块数据时，一定要从原始数据类型转化为byte[]，不能直接从字符串进行转换
     *
     * @param nonce
     * @return
     */
    private byte[] prepareData(long nonce) {
        byte[] prevBlockHashBytes = {};
        if (this.getBlock().getPrevBlockHash() != null) {
            prevBlockHashBytes = new BigInteger(this.getBlock().getPrevBlockHash(), 16).toByteArray();
        }

//        LogUtil.d("1:" + Arrays.toString(prevBlockHashBytes) + " 2:" + Arrays.toString(this.getBlock().getData().getBytes()) + " 3:" + Arrays.toString(ByteUtil.longToByte(this.getBlock().getTimestamp())) + " 4:" + Arrays.toString(ByteUtil.intToByte(TARGET_BITS)) + " 5:" + Arrays.toString(ByteUtil.longToByte(nonce)));

        return ByteUtil.merge(
                prevBlockHashBytes,
                this.getBlock().hashTransaction(),
                ByteUtil.longToByte(this.getBlock().getTimestamp()),
                ByteUtil.intToByte(TARGET_BITS),
                ByteUtil.longToByte(nonce)
        );
    }

    /**
     * 运行工作量证明，开始挖矿，找到小于难度目标值的Hash
     *
     * @return
     */
    public PowResult run() {
        long nonce = 0;
        String shaHex = "";
        long startTime = System.currentTimeMillis();
        while (nonce < Long.MAX_VALUE) {
            byte[] data = this.prepareData(nonce);
            shaHex = DigestUtils.sha256Hex(data);
//            LogUtil.d("nonce:" + nonce + " shaHex:" + shaHex);
            if (new BigInteger(shaHex, 16).compareTo(this.target) == -1) {
                LogUtil.d("Elapsed Time:" + (float) (System.currentTimeMillis() - startTime) / 1000);
                LogUtil.d("correct hash Hex:" + shaHex + " nonce:" + nonce);
                break;
            } else {
                nonce++;
            }
        }
        return new PowResult(nonce, shaHex);
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public boolean validate() {
        byte[] data = this.prepareData(this.getBlock().getNonce());
        return new BigInteger(DigestUtils.sha256Hex(data), 16).compareTo(this.target) == -1;
    }
}
