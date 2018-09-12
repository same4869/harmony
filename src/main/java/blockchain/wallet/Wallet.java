package blockchain.wallet;

import blockchain.utils.Base58Check;
import blockchain.utils.BtcAddressUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;

/**
 * 钱包
 */
@Data
@AllArgsConstructor
public class Wallet implements Serializable {
    // 校验码长度
    private static final int ADDRESS_CHECKSUM_LEN = 4;
    /**
     * 私钥
     */
    private BCECPrivateKey privateKey;
    /**
     * 公钥
     */
    private byte[] publicKey;

    public Wallet() {
        initWallet();
    }

    /**
     * 初始化钱包
     */
    private void initWallet() {
        try {
            KeyPair keyPair = newECKeyPair();
            BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
            BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();

            byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);

            this.setPrivateKey(privateKey);
            this.setPublicKey(publicKeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建新的密钥对
     *
     * @return
     * @throws Exception
     */
    private KeyPair newECKeyPair() throws Exception {
        // 注册 BC Provider
        Security.addProvider(new BouncyCastleProvider());
        // 创建椭圆曲线算法的密钥对生成器，算法为 ECDSA
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        // 椭圆曲线（EC）域参数设定
        // bitcoin 为什么会选择 secp256k1，详见：https://bitcointalk.org/index.php?topic=151120.0
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        g.initialize(ecSpec, new SecureRandom());
        return g.generateKeyPair();
    }

    /**
     * 获取钱包地址
     *
     * @return
     */
    public String getAddress() throws Exception {
        // 1. 获取 ripemdHashedKey
        byte[] ripemdHashedKey = BtcAddressUtil.ripeMD160Hash(this.getPublicKey());

        // 2. 添加版本 0x00
        ByteArrayOutputStream addrStream = new ByteArrayOutputStream();
        addrStream.write((byte) 0);
        addrStream.write(ripemdHashedKey);
        byte[] versionedPayload = addrStream.toByteArray();

        // 3. 计算校验码
        byte[] checksum = BtcAddressUtil.checksum(versionedPayload);

        // 4. 得到 version + paylod + checksum 的组合
        addrStream.write(checksum);
        byte[] binaryAddress = addrStream.toByteArray();

        // 5. 执行Base58转换处理
        return Base58Check.rawBytesToBase58(binaryAddress);
    }

}
