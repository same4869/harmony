package blockchain;

import blockchain.utils.LogUtil;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public class BlockChain {
    @Getter
    private static List<Block> blockList;

    public BlockChain(List<Block> blockList) {
        this.blockList = blockList;
    }

    /**
     * 添加新区块
     * @param data
     */
    public static void addBlock(String data) {
        if (blockList.size() < 1) {
            LogUtil.d("添加区块失败，因为还没有创世区块");
            return;
        }
        Block previousBlock = blockList.get(blockList.size() - 1);
        blockList.add(Block.newBlock(data, previousBlock.getHash()));
    }

    /**
     * 创建创世区块
     */
    public static Block newGenesisBlock() {
        blockList = new LinkedList<>();
        Block block = Block.newBlock("I am Genesis Block", "0000000000000000");
        blockList.add(block);
        return block;
    }

}
