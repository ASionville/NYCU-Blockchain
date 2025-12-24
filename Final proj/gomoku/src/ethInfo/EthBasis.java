package ethInfo;

/**
 * 區塊鏈連接設定與帳號管理
 */
public class EthBasis {
	// Chain ID
	public static final long chainID = 11330023L;
	
	// RPC 連接 URL
    public static String rpcUrl = "http://127.0.0.1:8545";
    
    // 部署的 Gomoku 合約地址
	public static String gomokuAddress = "0xC1a3dCD9178952DB33934Be3143a6413FADe7200";
	
	// 帳號 1 (黑方/創建者)
    public static String keystorePath1 = "/Users/nimab/Library/Mobile Documents/com~apple~CloudDocs/Desktop/nycu/Blockchain-and-Smart-Contract/Final proj/keystore/UTC--2025-12-22T08-47-56.417895929Z--851de6089fdbdee99562dd922a0aa74147f52b70";
    public static String password1 = "nycu";
    
    // 帳號 2 (白方/加入者)
    public static String keystorePath2 = "/Users/nimab/Library/Mobile Documents/com~apple~CloudDocs/Desktop/nycu/Blockchain-and-Smart-Contract/Final proj/keystore/UTC--2025-12-23T13-41-05.975450132Z--1b325b09c712f993f49550da3f3c9c288e4adc50";
    public static String password2 = "nycu2";
}