###harmony开发流程日志1.0
---
####2018.9.10

区块链原型

- 新增Block类：区块基本属性，生成新的区块
- 新增BlockChain类：区块链添加新区块，创建创世区块
- 新增SHA256Util类，ByteUtil类，ByteUtil类

####2018.9.10

POW

- Block类增加nonce字段
- 新增ProofOfWork类：拼接区块数据，POW挖矿，返回PowResult

####2018.9.11

持久化

- 新增RocksDBUtil类，数据库使用RocksDB
- 改造BlockChain类中方法

