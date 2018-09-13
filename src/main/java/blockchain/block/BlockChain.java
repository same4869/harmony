package blockchain.block;

import blockchain.transaction.SpendableOutputResult;
import blockchain.transaction.TXInput;
import blockchain.transaction.TXOutput;
import blockchain.transaction.Transaction;
import blockchain.utils.RocksDBUtil;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class BlockChain {
    @Getter
    private String lastBlockHash;

    private BlockChain(String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }

    /**
     * 打包交易，进行挖矿
     *
     * @param transactions
     */
    public Block mineBlock(Transaction[] transactions) throws Exception {
        // 挖矿前，先验证交易记录
        for (Transaction tx : transactions) {
            if (!this.verifyTransactions(tx)) {
                throw new Exception("ERROR: Fail to mine block ! Invalid transaction ! ");
            }
        }
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to get last block hash ! ");
        }
        Block block = Block.newBlock(transactions, lastBlockHash);
        this.addBlock(block);
        return block;
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
     * 从 DB 从恢复区块链数据
     *
     * @return
     * @throws Exception
     */
    public static BlockChain initBlockchainFromDB() throws Exception {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to init blockchain from db. ");
        }
        return new BlockChain(lastBlockHash);
    }

    /**
     * 创建区块链，没有创世区块的话会创建
     *
     * @return
     */
    public static BlockChain newBlockchain(String address) {
        String lastBlockHash = RocksDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, "");
            Block genesisBlock = Block.newGenesisBlock(coinbaseTX);
            lastBlockHash = genesisBlock.getHash();
            RocksDBUtil.getInstance().putBlock(genesisBlock);
            RocksDBUtil.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new BlockChain(lastBlockHash);
    }

    public BlockchainIterator getBlockchainIterator() {
        return new BlockchainIterator(lastBlockHash);
    }


    /**
     * 查找所有的 unspent transaction outputs
     *
     * @return
     */
    public Map<String, TXOutput[]> findAllUTXOs() {
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs();
        Map<String, TXOutput[]> allUTXOs = Maps.newHashMap();
        // 再次遍历所有区块中的交易输出
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            for (Transaction transaction : block.getTransactions()) {

                String txId = Hex.encodeHexString(transaction.getTxId());

                int[] spentOutIndexArray = allSpentTXOs.get(txId);
                TXOutput[] txOutputs = transaction.getOutputs();
                for (int outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }
                    TXOutput[] UTXOArray = allUTXOs.get(txId);
                    if (UTXOArray == null) {
                        UTXOArray = new TXOutput[]{txOutputs[outIndex]};
                    } else {
                        UTXOArray = ArrayUtils.add(UTXOArray, txOutputs[outIndex]);
                    }
                    allUTXOs.put(txId, UTXOArray);
                }
            }
        }
        return allUTXOs;
    }

    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出
     *
     * @return 交易ID以及对应的交易输出下标地址
     */
    private Map<String, int[]> getAllSpentTXOs() {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        Map<String, int[]> spentTXOs = Maps.newHashMap();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            for (Transaction transaction : block.getTransactions()) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TXInput txInput : transaction.getInputs()) {
                    String inTxId = Hex.encodeHexString(txInput.getTxId());
                    int[] spentOutIndexArray = spentTXOs.get(inTxId);
                    if (spentOutIndexArray == null) {
                        spentOutIndexArray = new int[]{txInput.getTxOutputIndex()};
                    } else {
                        spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                    }
                    spentTXOs.put(inTxId, spentOutIndexArray);
                }
            }
        }
        return spentTXOs;
    }

//    /**
//     * 查找钱包地址对应的所有UTXO
//     *
//     * @return
//     */
//    public TXOutput[] findUTXO(byte[] pubKeyHash) throws Exception {
//        Transaction[] unspentTxs = this.findUnspentTransactions(pubKeyHash);
//        TXOutput[] utxos = {};
//        if (unspentTxs == null || unspentTxs.length == 0) {
//            return utxos;
//        }
//        for (Transaction tx : unspentTxs) {
//            for (TXOutput txOutput : tx.getOutputs()) {
//                if (txOutput.isLockedWithKey(pubKeyHash)) {
//                    utxos = ArrayUtils.add(utxos, txOutput);
//                }
//            }
//        }
//        return utxos;
//    }

//    /**
//     * 寻找能够花费的交易
//     *
//     * @param amount  花费金额
//     */
//    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) throws Exception {
//        Transaction[] unspentTXs = this.findUnspentTransactions(pubKeyHash);
//        int accumulated = 0;
//        Map<String, int[]> unspentOuts = new HashMap<>();
//        for (Transaction tx : unspentTXs) {
//
//            String txId = Hex.encodeHexString(tx.getTxId());
//
//            for (int outId = 0; outId < tx.getOutputs().length; outId++) {
//
//                TXOutput txOutput = tx.getOutputs()[outId];
//
//                if (txOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
//                    accumulated += txOutput.getValue();
//
//                    int[] outIds = unspentOuts.get(txId);
//                    if (outIds == null) {
//                        outIds = new int[]{outId};
//                    } else {
//                        outIds = ArrayUtils.add(outIds, outId);
//                    }
//                    unspentOuts.put(txId, outIds);
//                    if (accumulated >= amount) {
//                        break;
//                    }
//                }
//            }
//        }
//        return new SpendableOutputResult(accumulated, unspentOuts);
//    }

    /**
     * 依据交易ID查询交易信息
     *
     * @param txId 交易ID
     * @return
     */
    private Transaction findTransaction(byte[] txId) throws Exception {
        for (BlockchainIterator iterator = this.getBlockchainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            for (Transaction tx : block.getTransactions()) {
                if (Arrays.equals(tx.getTxId(), txId)) {
                    return tx;
                }
            }
        }
        throw new Exception("ERROR: Can not found tx by txId ! ");
    }

    /**
     * 进行交易签名
     *
     * @param tx         交易数据
     * @param privateKey 私钥
     */
    public void signTransaction(Transaction tx, BCECPrivateKey privateKey) throws Exception {
        // 先来找到这笔新的交易中，交易输入所引用的前面的多笔交易的数据
        Map<String, Transaction> prevTxMap = new HashMap<>();
        for (TXInput txInput : tx.getInputs()) {
            Transaction prevTx = this.findTransaction(txInput.getTxId());
            prevTxMap.put(Hex.encodeHexString(txInput.getTxId()), prevTx);
        }
        tx.sign(privateKey, prevTxMap);
    }

    /**
     * 交易签名验证
     *
     * @param tx
     */
    private boolean verifyTransactions(Transaction tx) throws Exception {
        if (tx.isCoinbase()) {
            return true;
        }
        Map<String, Transaction> prevTx = new HashMap<>();
        for (TXInput txInput : tx.getInputs()) {
            Transaction transaction = this.findTransaction(txInput.getTxId());
            prevTx.put(Hex.encodeHexString(txInput.getTxId()), transaction);
        }
        return tx.verify(prevTx);
    }

}
