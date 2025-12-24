# Gomoku on Private Ethereum

End-to-end notes for the Java + Web3j CLI and the browser UI that interact with the `Gomoku.sol` smart contract on a private Geth network.

## Table of Contents
- Overview
- Prerequisites
- Start a local Geth node (Docker)
- Create and fund accounts
- Contract info
- Project layout
- Java CLI workflow
- Web UI workflow
- Mining helper
- Troubleshooting
- Regenerating the contract wrapper
- References

## Overview
- Private chain running in Dockerized Geth (HTTP RPC on 8545, network ID 11330023).
- Two-player Gomoku with optional bet amount; includes on-chain chat.
- Two clients: Java CLI (Web3j) and a browser UI (Web3.js + canvas board + chat).

## Prerequisites
- Docker with network access to map ports 30303, 8545, 8551.
- JDK 11+ for the Java tools.
- Basic `npm` or Python if you want to host the web UI with a simple static server.

## Start a local Geth node (Docker)
```bash
# Pull tooling image
docker pull ethereum/client-go:alltools-v1.10.26

# Start the container (exposes P2P + HTTP RPC)
docker run -it -p 30303:30303 -p 8545:8545 -p 8551:8551 ethereum/client-go:alltools-v1.10.26
```

Copy the genesis file into the container and initialize:
```bash
# In another terminal
container_id=$(docker ps -q --filter ancestor=ethereum/client-go:alltools-v1.10.26)
docker cp ./genesis.json ${container_id}:/genesis.json

# Inside the container
geth --datadir ./ init genesis.json
```

Start Geth with HTTP RPC (CORS enabled for Remix and the web UI):
```bash
geth --datadir ./ \
  --networkid 11330023 \
  --http --http.addr 0.0.0.0 --http.port 8545 \
  --http.api eth,web3,net,personal \
  --http.corsdomain "*" --http.vhosts "*" \
  --allow-insecure-unlock console
```

Verify the node is up:
```javascript
admin.peers
```

## Create and fund accounts
Inside the Geth console:
```javascript
// Create two accounts
personal.newAccount("nycu")
personal.newAccount("nycu2")

// Show accounts
eth.accounts
// Example: ["0x851dE6089fdbdeE99562DD922A0aA74147F52b70", "0x1234..."]

// Set miner coinbase and mine
miner.setEtherbase(eth.accounts[0])
miner.start(); admin.sleep(4); miner.stop();

// Fund player 2
eth.sendTransaction({
  from: eth.accounts[0],
  to: eth.accounts[1],
  value: web3.toWei(10, "ether")
})

// Unlock for Remix/UI
personal.unlockAccount(eth.accounts[0], "nycu", 0)
personal.unlockAccount(eth.accounts[1], "nycu2", 0)
```
Check balances:
```javascript
web3.fromWei(eth.getBalance(eth.accounts[0]), "ether")
web3.fromWei(eth.getBalance(eth.accounts[1]), "ether")
```

## Contract info
- Name: Gomoku
- Deployed address: `0xC1a3dCD9178952DB33934Be3143FADe7200`
- Network ID: 11330023
- RPC URL: http://127.0.0.1:8545

## Project layout
```
gomoku/src/
├─ ethInfo/
│  └─ EthBasis.java        # RPC URL, chain ID, contract address, keystore paths
├─ ethSC/
│  ├─ Gomoku.java          # Web3j-generated wrapper (do not edit manually)
│  ├─ GomokuHandler.java   # Convenience contract calls
│  ├─ ChatRoom.java        # Chat contract wrapper
│  └─ ChatRoomHandler.java # Chat helper
├─ gomokuApp/
│  └─ GomokuCLI.java       # Interactive CLI for Gomoku
└─ chatRoomApp/
   ├─ ChatRoomApp.java     # Chat app entry
   ├─ ChatRoomClient.java
   ├─ ChatRoomFileIO.java
   └─ ChatRoomManager.java

web-ui/
├─ index.html    # Browser UI
├─ style.css     # Styling
├─ config.js     # RPC/contract config + ABI
├─ app.js        # Main UI logic
├─ board.js      # Canvas board rendering
└─ chat.js       # Chat logic
```

## Java CLI workflow
1. **Export keystores** from the container (new terminal on host):
   ```bash
   docker cp ${container_id}:/keystore ./keystore
   ls keystore  # two UTC--* files
   ```

2. **Point the client to your keys** in `EthBasis.java`:
   ```java
   public static String keystorePath1 = "./keystore/UTC--...--address1"; // black/creator
   public static String keystorePath2 = "./keystore/UTC--...--address2"; // white/joiner
   public static String rpcUrl = "http://127.0.0.1:8545";
   public static String chainId = "11330023";
   public static String gomokuAddress = "0xC1a3dCD9178952DB33934Be3143FADe7200";
   ```

3. **Run the CLI** (two terminals for two players):
   ```bash
   java -cp ... gomokuApp.GomokuCLI
   ```
   - Choose account (1 = black/creator, 2 = white/joiner).
   - Menu options include `createGame`, `joinGame`, `makeMove`, `sendChat`, `claimTimeout`, `surrender`, `getGameInfo`, `tail MoveMade`, `quit`.
   - After every transaction, mine a block in the Geth console: `miner.start(1); admin.sleep(2); miner.stop();`.

4. **Sample flow**
   - Player A: option `1` createGame → mine.
   - Player B: option `2` joinGame with the same gameId → mine.
   - Alternate `makeMove` (x, y between 0–14) → mine after each move.
   - Use option `7` to inspect game state (does not require mining).
   - Use option `8` to tail `MoveMade` events live.

## Web UI workflow
1. **Ensure Geth is running** with HTTP RPC (see above) and both accounts are unlocked:
   ```javascript
   personal.unlockAccount(eth.accounts[0], "nycu", 0)
   personal.unlockAccount(eth.accounts[1], "nycu2", 0)
   ```

2. **Serve the static files** (pick one):
   ```bash
   # Python
   cd web-ui
   python -m http.server 8000
   # or Node.js
   http-server -p 8000 --cors
   ```
   Open http://localhost:8000.

3. **Use the interface**
   - Select an account from the dropdown.
   - Create or join a game (bet amount in wei can be 0).
   - Mine after each transaction: `miner.start(1); admin.sleep(3); miner.stop();`.
   - Click the 15x15 board to place stones; chat in the side panel.

4. **Config tweaks** (web-ui/config.js):
   ```javascript
   const RPC_URL = "http://localhost:8545";
   const CONTRACT_ADDRESS = "0xC1a3dCD9178952DB33934Be3143FADe7200";
   const ACCOUNTS = { player1: "0x...", player2: "0x..." };
   ```

## Mining helper
Define once in the Geth console to mine whenever pending transactions exist:
```javascript
function m() {
  var pending = txpool.status.pending;
  if (pending > 0) {
    console.log("Mining " + pending + " tx...");
    miner.start(1);
    admin.sleep(3);
    miner.stop();
    console.log("✓ Block " + eth.blockNumber);
  } else {
    console.log("No pending tx");
  }
}

// Call after each transaction
m()
```

## Troubleshooting
- "You are already in this game": use a different account when joining.
- "Not your turn": wait for the opponent; check `currentPlayer` via option `getGameInfo`.
- "Position already occupied": choose another coordinate.
- "authentication needed": unlock accounts in the Geth console with `personal.unlockAccount(...)`.
- UI cannot connect: confirm Geth HTTP RPC is running, CORS/vhosts allow all, and port 8545 is reachable.
- Transaction stuck: run the mining helper or `miner.start(1); admin.sleep(3); miner.stop();`.

## Regenerating the contract wrapper
If `Gomoku.sol` changes:
```bash
# Compile in Remix to get ABI + bytecode (Gomoku.abi.json, Gomoku.bin)
web3j generate solidity \
  -a Gomoku.abi.json \
  -b Gomoku.bin \
  -o ./src/ethSC \
  -p ethSC
```
Then rebuild the Java project and redeploy the contract as needed.

## References
- Web3j docs: https://docs.web3j.io/
- Solidity docs: https://docs.soliditylang.org/
- Geth docs: https://geth.ethereum.org/docs/
