import blockchain.Block;
import blockchain.BlockChain;
import blockchain.ProofOfWork;
import blockchain.utils.LogUtil;

public class BlockchainTest {

    public static void main(String[] args) {
//        Block block = Block.newBlock("你好","000000");
//        LogUtil.d("prevhash --> " +block.getPrevBlockHash());
//        LogUtil.d("data --> " + block.getData());
//        LogUtil.d("timestamp --> " + block.getTimestamp() + "");
//        LogUtil.d("hash --> " + block.getHash());

//        BlockChain.newGenesisBlock();
//        BlockChain.addBlock("我是第一个区块");
//        BlockChain.addBlock("我是第二个区块");
//
//        for(Block block : BlockChain.getBlockList()){
//            LogUtil.d("timestamp:" + block.getTimestamp());
//            LogUtil.d("prevhash:" + block.getPrevBlockHash());
//            LogUtil.d("hash:" + block.getHash());
//            LogUtil.d("data:" + block.getData());
//            LogUtil.d("/////////////////////////////////////");
//        }

//        BlockChain.newGenesisBlock();
//
//        BlockChain.addBlock("Send 1 BTC to Ivan");
//        BlockChain.addBlock("Send 2 more BTC to Ivan");
//
//        for (Block block : BlockChain.getBlockList()) {
//            LogUtil.d("timestamp:" + block.getTimestamp());
//            LogUtil.d("prevhash:" + block.getPrevBlockHash());
//            LogUtil.d("hash:" + block.getHash());
//            LogUtil.d("data:" + block.getData());
//            LogUtil.d("nonce:" + block.getNonce());
//            LogUtil.d("/////////////////////////////////////");
//
//            ProofOfWork pow = ProofOfWork.newProofOfWork(block);
//            LogUtil.d("Pow valid: " + pow.validate() + "\n");
//        }

        BlockChain blockChain = BlockChain.newBlockchain();
        try {
            blockChain.addBlock("Send 1 BTC to Ivan");
            blockChain.addBlock("Send 2 BTC to kkkk");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
