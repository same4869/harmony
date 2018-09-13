package blockchain.utils;

import blockchain.block.Block;
import blockchain.transaction.TXOutput;
import lombok.Getter;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 数据持久化，数据库
 */
public class RocksDBUtil {
    /**
     * 区块链数据文件
     */
    private static final String DB_FILE = "blockchain_harmony.db";
    /**
     * 区块桶前缀
     */
    private static final String BLOCKS_BUCKET_KEY = "blocks";
    /**
     * 链状态桶Key
     */
    private static final String CHAINSTATE_BUCKET_KEY = "chainstate";
    /**
     * 最新一个区块
     */
    private static final String LAST_BLOCK_KEY = "l";

    private volatile static RocksDBUtil instance;

    public static RocksDBUtil getInstance() {
        if (instance == null) {
            synchronized (RocksDBUtil.class) {
                if (instance == null) {
                    instance = new RocksDBUtil();
                }
            }
        }
        return instance;
    }

    private RocksDB db;

    /**
     * block buckets
     */
    private Map<String, byte[]> blocksBucket;
    /**
     * chainstate buckets
     */
    @Getter
    private Map<String, byte[]> chainstateBucket;

    private RocksDBUtil() {
        openDB();
        initBlockBucket();
        initChainStateBucket();
    }

    /**
     * 打开数据库
     */
    private void openDB() {
        try {
            db = RocksDB.open(DB_FILE);
        } catch (RocksDBException e) {
            throw new RuntimeException("Fail to open db ! ", e);
        }
    }

    /**
     * 初始化 blocks 数据桶
     */
    private void initBlockBucket() {
        try {
            byte[] blockBucketKey = SerializeUtil.serialize(BLOCKS_BUCKET_KEY);
            byte[] blockBucketBytes = db.get(blockBucketKey);
            if (blockBucketBytes != null) {
                blocksBucket = (Map) SerializeUtil.deserialize(blockBucketBytes);
            } else {
                blocksBucket = Maps.newHashMap();
                db.put(blockBucketKey, SerializeUtil.serialize(blocksBucket));
            }
        } catch (RocksDBException e) {
            throw new RuntimeException("Fail to init block bucket ! ", e);
        }
    }

    /**
     * 初始化 blocks 数据桶
     */
    private void initChainStateBucket() {
        try {
            byte[] chainstateBucketKey = SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY);
            byte[] chainstateBucketBytes = db.get(chainstateBucketKey);
            if (chainstateBucketBytes != null) {
                chainstateBucket = (Map) SerializeUtil.deserialize(chainstateBucketBytes);
            } else {
                chainstateBucket = Maps.newHashMap();
                db.put(chainstateBucketKey, SerializeUtil.serialize(chainstateBucket));
            }
        } catch (RocksDBException e) {
            LogUtil.d("Fail to init chainstate bucket ! " + e);
            throw new RuntimeException("Fail to init chainstate bucket ! ", e);
        }
    }

    /**
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) {
        try {
            blocksBucket.put(LAST_BLOCK_KEY, SerializeUtil.serialize(tipBlockHash));
            db.put(SerializeUtil.serialize(BLOCKS_BUCKET_KEY), SerializeUtil.serialize(blocksBucket));
        } catch (RocksDBException e) {
            throw new RuntimeException("Fail to put last block hash ! ", e);
        }
    }

    /**
     * 查询最新一个区块的Hash值
     *
     * @return
     */
    public String getLastBlockHash() {
        byte[] lastBlockHashBytes = blocksBucket.get(LAST_BLOCK_KEY);
        if (lastBlockHashBytes != null) {
            return (String) SerializeUtil.deserialize(lastBlockHashBytes);
        }
        return "";
    }

    /**
     * 保存区块
     *
     * @param block
     */
    public void putBlock(Block block) {
        try {
            blocksBucket.put(block.getHash(), SerializeUtil.serialize(block));
            db.put(SerializeUtil.serialize(BLOCKS_BUCKET_KEY), SerializeUtil.serialize(blocksBucket));
        } catch (RocksDBException e) {
            throw new RuntimeException("Fail to put block ! ", e);
        }
    }
    /**
     * 查询区块
     *
     * @param blockHash
     * @return
     */
    public Block getBlock(String blockHash) {
        return (Block) SerializeUtil.deserialize(blocksBucket.get(blockHash));
    }

    /**
     * 清空chainstate bucket
     */
    public void cleanChainStateBucket() {
        try {
            chainstateBucket.clear();
        } catch (Exception e) {
            LogUtil.d("Fail to clear chainstate bucket ! " +  e);
            throw new RuntimeException("Fail to clear chainstate bucket ! ", e);
        }
    }

    /**
     * 保存UTXO数据
     *
     * @param key   交易ID
     * @param utxos UTXOs
     */
    public void putUTXOs(String key, TXOutput[] utxos) {
        try {
            chainstateBucket.put(key, SerializeUtil.serialize(utxos));
            db.put(SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtil.serialize(chainstateBucket));
        } catch (Exception e) {
            LogUtil.d("Fail to put UTXOs into chainstate bucket ! key=" + key + " " + e);
            throw new RuntimeException("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
        }
    }


    /**
     * 查询UTXO数据
     *
     * @param key 交易ID
     */
    public TXOutput[] getUTXOs(String key) {
        byte[] utxosByte = chainstateBucket.get(key);
        if (utxosByte != null) {
            return (TXOutput[]) SerializeUtil.deserialize(utxosByte);
        }
        return null;
    }


    /**
     * 删除 UTXO 数据
     *
     * @param key 交易ID
     */
    public void deleteUTXOs(String key) {
        try {
            chainstateBucket.remove(key);
            db.put(SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtil.serialize(chainstateBucket));
        } catch (Exception e) {
            LogUtil.d("Fail to delete UTXOs by key ! key=" + key + " " + e);
            throw new RuntimeException("Fail to delete UTXOs by key ! key=" + key, e);
        }
    }


    /**
     * 关闭数据库
     */
    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            throw new RuntimeException("Fail to close db ! ", e);
        }
    }
}
