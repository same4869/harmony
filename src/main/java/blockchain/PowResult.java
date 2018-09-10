package blockchain;

import lombok.Data;

@Data
public class PowResult {
    private long nonce;
    private String shaHex;

    public PowResult(long nonce, String shaHex) {
        this.nonce = nonce;
        this.shaHex = shaHex;
    }
}
