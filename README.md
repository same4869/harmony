### harmony开发流程日志1.0
---
#### 2018.9.10

区块链原型

- 新增Block类：区块基本属性，生成新的区块
- 新增BlockChain类：区块链添加新区块，创建创世区块
- 新增SHA256Util类，ByteUtil类./

#### 2018.9.10

POW

- Block类增加nonce字段
- 新增ProofOfWork类：拼接区块数据，POW挖矿，返回PowResult

#### 2018.9.11

持久化

- 新增序列化工具类SerializeUtil
- 新增数据工具类RocksDBUtil，数据库使用RocksDB
- 改造BlockChain类中方法

#### 2018.9.11

遍历打印区块链

- BlockChain类中赠加迭代器内部类，增加遍历打印区块链方法

#### 2018.9.11

CLI命令行以及sh脚本

- 包简单整理
- cli命令支持
- 添加sh脚本使得支持在命令行中运行

#### 2018.9.11

UTXO,余额与转账

- 新增transaction包
- 区块数据data变成transaction
- 实现获取余额getbalance和转账send的命令逻辑

#### 2018.9.12

钱包，加密算法

- 新增wallet包
- 交易数据结构中检查方法修改
- 新增创建钱包，查看钱包逻辑与命令
- 必须使用创建好的钱包地址作为地址参数

#### 2018.9.12

交易签名及验证

- Transaction，BlockChain中加入签名与验证方法

#### 2018.9.13

UTXO池，挖矿奖励

- 新增UTXOSet类，获取UTXO等逻辑修改

#### 2018.9.13

默克尔树

- 新增默克尔树，更改交易哈希为默克尔树根节点的hash
