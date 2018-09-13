package blockchain.block;

import blockchain.pow.PowResult;
import blockchain.pow.ProofOfWork;
import blockchain.transaction.MerkleTree;
import blockchain.transaction.Transaction;
import blockchain.utils.ByteUtil;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 区块结构
 */
@Data
@NoArgsConstructor
public class Block {
    private long timestamp; //时间戳
    /**
     * 交易信息
     */
    private Transaction[] transactions;
    private String prevBlockHash; //上一个区块的hash
    private String hash; //当前区块hash
    private long nonce; //挖矿的nonce值

    public Block(long timestamp, Transaction[] transactions, String prevBlockHash, String hash) {
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.prevBlockHash = prevBlockHash;
        this.hash = hash;
    }

    public static Block newBlock(Transaction[] transactions, String prevBlockHash) {
        Block block = new Block(System.currentTimeMillis(), transactions, prevBlockHash, null);
        ProofOfWork pow = ProofOfWork.newProofOfWork(block);
        PowResult powResult = pow.run();
        block.setHash(powResult.getShaHex());
        block.setNonce(powResult.getNonce());
        return block;
    }

    public static Block newGenesisBlock(Transaction coinbase){
        return Block.newBlock(new Transaction[]{coinbase}, ByteUtil.ZERO_HASH);
    }

    /**
     * 对区块中的交易信息进行Hash计算
     *
     * @return
     */
    public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getTransactions().length][];
        for (int i = 0; i < this.getTransactions().length; i++) {
            txIdArrays[i] = this.getTransactions()[i].hash();
        }
        return new MerkleTree(txIdArrays).getRoot().getHash();
    }
}
