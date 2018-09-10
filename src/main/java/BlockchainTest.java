import blockchain.Block;
import blockchain.BlockChain;
import blockchain.utils.LogUtil;

public class BlockchainTest {

    public static void main(String[] args) {
//        Block block = Block.newBlock("你好","000000");
//        LogUtil.d("prevhash --> " +block.getPrevBlockHash());
//        LogUtil.d("data --> " + block.getData());
//        LogUtil.d("timestamp --> " + block.getTimestamp() + "");
//        LogUtil.d("hash --> " + block.getHash());

        BlockChain.newGenesisBlock();
        BlockChain.addBlock("我是第一个区块");
        BlockChain.addBlock("我是第二个区块");

        for(Block block : BlockChain.getBlockList()){
            LogUtil.d("timestamp:" + block.getTimestamp());
            LogUtil.d("prevhash:" + block.getPrevBlockHash());
            LogUtil.d("hash:" + block.getHash());
            LogUtil.d("data:" + block.getData());
            LogUtil.d("/////////////////////////////////////");
        }
    }

}
