package gomokuApp;

import ethSC.GomokuHandler;
import ethInfo.EthBasis;
import java.math.BigInteger;
import java.util.Scanner;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple6;

/**
 * Console interface for Gomoku blockchain game
 * Supports dual-account mode for testing
 */
public class GomokuCLI {
    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        System.out.println("\n=== Gomoku Blockchain Game ===");
        System.out.println("Using RPC: " + EthBasis.rpcUrl);
        System.out.println("Contract: " + EthBasis.gomokuAddress);
        
        // Select account
        System.out.println("\nSelect player account:");
        System.out.println("1) Player 1 (Black/Creator)");
        System.out.println("2) Player 2 (White/Joiner)");
        System.out.print("> ");
        
        String choice = SC.nextLine().trim();
        String keystorePath;
        String password;
        String playerName;
        
        if (choice.equals("2")) {
            keystorePath = EthBasis.keystorePath2;
            password = EthBasis.password2;
            playerName = "Player 2 (White)";
        } else {
            keystorePath = EthBasis.keystorePath1;
            password = EthBasis.password1;
            playerName = "Player 1 (Black)";
        }
        
        System.out.println("\nUsing account: " + playerName);
        GomokuHandler handler = new GomokuHandler(keystorePath, password);
        System.out.println("\n[!] Reminder: After each transaction, run in Geth console:");
        System.out.println("    miner.start(1); admin.sleep(3); miner.stop()");
        
        menu(handler);
    }

    private static void menu(GomokuHandler handler) throws Exception {
        boolean running = true;
        while (running) {
            System.out.println("\n=== Gomoku Game Menu ===");
            System.out.println("1) createGame (with optional bet)");
            System.out.println("2) joinGame");
            System.out.println("3) makeMove");
            System.out.println("4) sendChat");
            System.out.println("5) claimTimeout");
            System.out.println("6) surrender");
            System.out.println("7) getGameInfo");
            System.out.println("8) getGameCounter");
            System.out.println("9) tail MoveMade events (live)");
            System.out.println("0) quit");
            System.out.print("> ");
            String choice = SC.nextLine().trim();
            switch (choice) {
                case "1":
                    createGame(handler);
                    break;
                case "2":
                    joinGame(handler);
                    break;
                case "3":
                    makeMove(handler);
                    break;
                case "4":
                    sendChat(handler);
                    break;
                case "5":
                    claimTimeout(handler);
                    break;
                case "6":
                    surrender(handler);
                    break;
                case "7":
                    getGameInfo(handler);
                    break;
                case "8":
                    getGameCounter(handler);
                    break;
                case "9":
                    tailMoves(handler);
                    break;
                default:
                    running = false;
                    break;
            }
        }
    }

    private static void createGame(GomokuHandler handler) throws Exception {
        System.out.print("Bet amount in wei (0 for none): ");
        BigInteger bet = new BigInteger(SC.nextLine());
        
        // Query current game counter before creating
        BigInteger currentCounter = handler.getContract().gameCounter().send();
        
        // Send transaction
        TransactionReceipt receipt = handler.getContract().createGame(bet).send();
        System.out.println("\n[v] Transaction sent: " + receipt.getTransactionHash());
        System.out.println("[v] Status: " + (receipt.isStatusOK() ? "Success" : "Failed"));
        
        if (receipt.isStatusOK()) {
            System.out.println("\n*** Game Created! ***");
            System.out.println("    Game ID: " + currentCounter);
            System.out.println("    Bet Amount: " + bet + " wei");
            System.out.println("\n[!] Remember this Game ID: " + currentCounter);
            System.out.println("    Your opponent needs it to join!");
        } else {
            System.out.println("\n[X] Transaction failed!");
        }
        
        System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
    }

    private static void joinGame(GomokuHandler handler) throws Exception {
        System.out.print("gameId: ");
        BigInteger gid = new BigInteger(SC.nextLine());
        
        try {
            // 使用 GameInfo 結構體
            ethSC.Gomoku.GameInfo info = handler.getContract().getGameInfo(gid).send();
            
            System.out.println("\n=== Game Info ===");
            System.out.println("Creator (Black): " + info.blackPlayer);
            System.out.println("Required Bet: " + info.betAmountWei + " wei");
            System.out.println("Game State: " + getStateString(info.state));
            
            if (info.state != 0) {
                System.out.println("\n[X] Error: Game is not waiting for players!");
                return;
            }
            
            System.out.print("\nConfirm join? Must pay " + info.betAmountWei + " wei (y/n): ");
            String confirm = SC.nextLine().trim().toLowerCase();
            
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                System.out.println("Cancelled");
                return;
            }
            
            // Join with correct bet amount
            handler.joinGame(gid, info.betAmountWei);
            System.out.println("\n[v] joinGame transaction sent");
            System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
            
        } catch (Exception e) {
            System.out.println("\n[X] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void makeMove(GomokuHandler handler) throws Exception {
        try {
            System.out.print("gameId: ");
            String gameIdInput = SC.nextLine().trim();
            if (gameIdInput.isEmpty()) {
                System.out.println("\n[X] Error: Game ID cannot be empty");
                return;
            }
            
            BigInteger gid;
            try {
                gid = new BigInteger(gameIdInput);
                if (gid.compareTo(BigInteger.ZERO) < 0) {
                    System.out.println("\n[X] Error: Game ID must be non-negative");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[X] Error: Invalid game ID format. Please enter a number.");
                return;
            }
            
            System.out.print("x (0-14): ");
            String xInput = SC.nextLine().trim();
            if (xInput.isEmpty()) {
                System.out.println("\n[X] Error: X coordinate cannot be empty");
                return;
            }
            
            int x;
            try {
                x = Integer.parseInt(xInput);
                if (x < 0 || x > 14) {
                    System.out.println("\n[X] Error: X coordinate must be between 0 and 14");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[X] Error: Invalid X coordinate. Please enter a number between 0 and 14.");
                return;
            }
            
            System.out.print("y (0-14): ");
            String yInput = SC.nextLine().trim();
            if (yInput.isEmpty()) {
                System.out.println("\n[X] Error: Y coordinate cannot be empty");
                return;
            }
            
            int y;
            try {
                y = Integer.parseInt(yInput);
                if (y < 0 || y > 14) {
                    System.out.println("\n[X] Error: Y coordinate must be between 0 and 14");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[X] Error: Invalid Y coordinate. Please enter a number between 0 and 14.");
                return;
            }
            
            // 確認座標
            System.out.println("\n[i] Making move at position (" + x + ", " + y + ") in game " + gid);
            
            handler.makeMove(gid, x, y);
            System.out.println("\n[v] move transaction sent");
            System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
            
        } catch (Exception e) {
            System.out.println("\n[X] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendChat(GomokuHandler handler) throws Exception {
        System.out.print("gameId: ");
        BigInteger gid = new BigInteger(SC.nextLine());
        System.out.print("message: ");
        String msg = SC.nextLine();
        
        handler.sendChat(gid, msg);
        System.out.println("\n[v] chat transaction sent");
        System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
    }

    private static void claimTimeout(GomokuHandler handler) throws Exception {
        System.out.print("gameId: ");
        BigInteger gid = new BigInteger(SC.nextLine());
        
        handler.claimTimeout(gid);
        System.out.println("\n[v] claimTimeout transaction sent");
        System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
    }

    private static void surrender(GomokuHandler handler) throws Exception {
        System.out.print("gameId: ");
        BigInteger gid = new BigInteger(SC.nextLine());
        
        handler.surrender(gid);
        System.out.println("\n[v] surrender transaction sent");
        System.out.println("\n[!] Now run in Geth console: miner.start(1); admin.sleep(2); miner.stop()");
    }

    private static void getGameInfo(GomokuHandler handler) throws Exception {
        System.out.print("gameId: ");
        BigInteger gid = new BigInteger(SC.nextLine());
        
        try {
            // 使用 GameInfo 結構體
            ethSC.Gomoku.GameInfo info = handler.getContract().getGameInfo(gid).send();
            
            System.out.println("\n=== Game Info ===");
            System.out.println("Game ID: " + gid);
            System.out.println("Black Player: " + info.blackPlayer);
            System.out.println("White Player: " + info.whitePlayer);
            System.out.println("Current Turn: " + (info.currentPlayer == 1 ? "Black (1)" : "White (2)"));
            System.out.println("Game State: " + getStateString(info.state));
            System.out.println("Bet Amount: " + info.betAmountWei + " wei");
            System.out.println("Move Count: " + info.moveCount);
            
        } catch (Exception e) {
            System.out.println("\n[X] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void getGameCounter(GomokuHandler handler) throws Exception {
        BigInteger counter = handler.getContract().gameCounter().send();
        System.out.println("\n=== Game Statistics ===");
        System.out.println("Total Games Created: " + counter);
        
        if (counter.intValue() > 0) {
            System.out.println("Latest Game ID: " + (counter.intValue() - 1));
            System.out.println("\nTip: Game IDs start from 0");
            System.out.println("  - First game: ID = 0");
            System.out.println("  - Second game: ID = 1");
            System.out.println("  - Latest game: ID = " + (counter.intValue() - 1));
        } else {
            System.out.println("No games created yet");
        }
    }
    
    private static String getStateString(int state) {
        switch (state) {
            case 0: return "Waiting";
            case 1: return "Playing";
            case 2: return "Black Wins";
            case 3: return "White Wins";
            case 4: return "Draw";
            default: return "Unknown (" + state + ")";
        }
    }

    private static void tailMoves(GomokuHandler handler) {
        System.out.println("Subscribing to MoveMade events from latest block...");
        System.out.println("(Listening for real-time move events)");
        
        handler.getContract()
                .moveMadeEventFlowable(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                .subscribe(ev -> {
                    System.out.printf("\n[*] Move Event | Game=%s Player=%s Position=(%d,%d) Piece=%d%n",
                            ev.gameId, ev.player, ev.x, ev.y, ev.piece);
                }, err -> {
                    System.err.println("[X] Subscription error: " + err.getMessage());
                });
        
        System.out.println("Press Enter to stop listening...");
        SC.nextLine();
    }
}
