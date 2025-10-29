package p2pblockchain.config;

public class BlockchainConfig {
    public static final int INITIAL_DIFFICULTY = 1;
    public static final int DIFFICULTY_ADJUSTMENT_INTERVAL = 10; // In Blocks
    public static final long TARGET_BLOCK_TIME_SECONDS = 30; // In Seconds
    public static final double MINING_REWARDS = 10;
    public static final int MAX_TRANSACTIONS_PER_BLOCK = 32;

    // 0 = None
    // 1 = Errors only
    // 2 = Errors and Warnings
    // 3 = All logs
    public static int VERBOSE_LEVEL = 2;

    public void setVerboseLevel(int level) {
        VERBOSE_LEVEL = level;
    }

    public static int getVerboseLevel() {
        return VERBOSE_LEVEL;
    }
}