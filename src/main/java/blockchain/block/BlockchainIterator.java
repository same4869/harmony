package blockchain.block;

import blockchain.pow.ProofOfWork;
import blockchain.utils.ByteUtil;
import blockchain.utils.LogUtil;
import blockchain.utils.RocksDBUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 区块链迭代器,倒序遍历
 */
public class BlockchainIterator {
    private String currentBlockHash; //当前迭代器指向区块的hash

    public BlockchainIterator(String currentBlockHash) {
        this.currentBlockHash = currentBlockHash;
    }

    /**
     * 是否有下一个区块
     *
     * @return
     */
    public boolean hashNext() {
        if (StringUtils.isBlank(currentBlockHash) || currentBlockHash.equals(ByteUtil.ZERO_HASH)) {
            return false;
        }
        Block lastBlock = RocksDBUtil.getInstance().getBlock(currentBlockHash);
        if (lastBlock == null) {
            return false;
        }
        // 创世区块直接放行
        if (ByteUtil.ZERO_HASH.equals(lastBlock.getPrevBlockHash())) {
            return true;
        }
        return RocksDBUtil.getInstance().getBlock(lastBlock.getPrevBlockHash()) != null;
    }


    /**
     * 返回区块
     *
     * @return
     */
    public Block next(){
        Block currentBlock = RocksDBUtil.getInstance().getBlock(currentBlockHash);
        if (currentBlock != null) {
            this.currentBlockHash = currentBlock.getPrevBlockHash();
            return currentBlock;
        }
        return null;
    }

    /**
     * 遍历打印整条区块链信息
     *
     * @param blockChain
     */
    public void printBlockChain(BlockChain blockChain) {
        for (BlockchainIterator iterator = blockChain.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = null;
            try {
                block = iterator.next();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (block != null) {
                boolean validate = ProofOfWork.newProofOfWork(block).validate();
                LogUtil.d(block.toString() + ", validate = " + validate);
            }
        }
    }
}