package ethSC;

import java.math.BigInteger;
import java.nio.file.Paths;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import ethInfo.EthBasis;

/**
 * Small helper to load and call Gomoku.sol.
 * Fill EthBasis fields before running.
 */
public class GomokuHandler {
    private final Web3j web3j;
    private final Credentials credentials;
    private final Gomoku gomoku;

    /**
     * 預設建構子 - 使用帳號 1
     */
    public GomokuHandler() throws Exception {
        this(EthBasis.keystorePath1, EthBasis.password1);
    }
    
    /**
     * 參數化建構子 - 支援指定 keystore 和密碼
     * @param keystorePath keystore 檔案路徑
     * @param password 帳號密碼
     */
    public GomokuHandler(String keystorePath, String password) throws Exception {
        this.web3j = Web3j.build(new HttpService(EthBasis.rpcUrl));
        this.credentials = WalletUtils.loadCredentials(password, Paths.get(keystorePath).toFile());
        ContractGasProvider gas = new DefaultGasProvider();
        TransactionManager tm = new RawTransactionManager(web3j, credentials, EthBasis.chainID);
        this.gomoku = Gomoku.load(EthBasis.gomokuAddress, web3j, tm, gas);
        
        System.out.println("✓ Loaded account: " + credentials.getAddress());
    }

    public Gomoku getContract() {
        return gomoku;
    }

    public void joinGame(BigInteger gameId, BigInteger valueWei) throws Exception {
        gomoku.joinGame(gameId, valueWei).send();
    }

    public void makeMove(BigInteger gameId, int x, int y) throws Exception {
        gomoku.makeMove(gameId, x, y).send();
    }

    public void sendChat(BigInteger gameId, String message) throws Exception {
        gomoku.sendChat(gameId, message).send();
    }

    public void claimTimeout(BigInteger gameId) throws Exception {
        gomoku.claimTimeout(gameId).send();
    }

    public void surrender(BigInteger gameId) throws Exception {
        gomoku.surrender(gameId).send();
    }

    public Gomoku.GameInfo getGameInfo(BigInteger gameId) throws Exception {
        return gomoku.getGameInfo(gameId).send();
    }

    public int getPiece(BigInteger gameId, int x, int y) throws Exception {
        return gomoku.getPiece(gameId, x, y).send();
    }
}
