package blockchain;

import blockchain.utils.RocksDBUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;


public class BlockChain {
    @Getter
    private String lastBlockHash;

    private BlockChain(String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    /**
     * 添加新区块
     *
     * @param data
     */
    public void addBlock(String data) throws Exception {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            throw new Exception("Fail to add block into blockchain ! ");
        }
        this.addBlock(Block.newBlock(data, lastBlockHash));
    }

    /**
     * 添加区块
     *
     * @param block
     */
    public void addBlock(Block block) {
        RocksDBUtil.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtil.getInstance().putBlock(block);
        this.lastBlockHash = block.getHash();
    }

    /**
     * <p> 创建区块链 </p>
     *
     * @return
     */
    public static BlockChain newBlockchain() {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            Block genesisBlock = Block.newGenesisBlock();
            lastBlockHash = genesisBlock.getHash();
            RocksDBUtil.getInstance().putBlock(genesisBlock);
            RocksDBUtil.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new BlockChain(lastBlockHash);
    }

}
