## 啟用docker版以太坊節點
```bash
# 建立docker環境
docker pull ethereum/client-go:alltools-v1.10.26
# 啟動節點
docker run -it -p 30303:30303 -p 8545:8545 -p 8551:8551 ethereum/client-go:alltools-v1.10.26
```

複製genesis.json到容器內
```bash
# 開啟另一個terminal
# 取得container_id
docker ps
# 複製檔案
docker cp ./genesis.json <container_id>:/genesis.json
```
初始化區塊鏈
```bash
geth --datadir ./ init genesis.json
```
啟動以太坊節點
```bash
geth  --datadir ./ --networkid 11330023 --http --http.addr 0.0.0.0 --http.port 8545 --http.api eth,web3,net,personal --http.corsdomain=https://remix.ethereum.org --allow-insecure-unlock console
```

測試節點是否啟動成功
```bash
# 看有沒有peer
admin.peers
```
## 創建/導入帳號
```bash
# 在 Geth console 中創建2隻新帳號
personal.newAccount("nycu")
personal.newAccount("nycu2")

# 確認帳號
eth.accounts
# 應該看到兩個地址
# ["0x851dE6089fdbdeE99562DD922A0aA74147F52b70", "0x1234567890abcdef..."]

# 設定挖礦使用的帳號
miner.setEtherbase(eth.accounts[0])
# 開始挖礦
miner.start()
# 停止挖礦
miner.stop()
# 挖一次
miner.start(1); admin.sleep(4); miner.stop()
```bash
# 查看帳號餘額
eth.getBalance(eth.accounts[0])

# 給新帳號轉一些 ETH（用於 gas 和賭注）
eth.sendTransaction({
    from: eth.accounts[0],
    to: eth.accounts[1],
    value: web3.toWei(10, "ether")
})

# 解鎖帳號供 Remix 使用
personal.unlockAccount(eth.accounts[0], "nycu", 0)
personal.unlockAccount(eth.accounts[1], "nycu2", 0)

# 查看帳號餘額
web3.fromWei(eth.getBalance(eth.accounts[0]), "ether")
web3.fromWei(eth.getBalance(eth.accounts[1]), "ether")
```

## 部署合約資訊

### 已部署的合約
- **合約名稱**: Gomoku
- **合約地址**: `0xC1a3dCD9178952DB33934Be3143a6413FADe7200`
- **Network ID**: 11330023
- **RPC URL**: http://127.0.0.1:8545

---

## Java 專案架構說明

本專案使用 Java + Web3j 實現區塊鏈五子棋遊戲的前端互動介面。

### 📁 專案結構

```
gomoku/src/
├── ethInfo/
│   └── EthBasis.java           # 區塊鏈連接設定與帳號管理
├── ethSC/
│   ├── Gomoku.java             # 智能合約包裝類（Web3j 自動生成）
│   ├── GomokuHandler.java      # 合約互動處理器
│   ├── ChatRoom.java           # 聊天室合約包裝類
│   └── ChatRoomHandler.java    # 聊天室處理器
├── gomokuApp/
│   └── GomokuCLI.java          # 五子棋命令列介面
└── chatRoomApp/
    ├── ChatRoomApp.java        # 聊天室主程式
    ├── ChatRoomClient.java     # 聊天客戶端
    ├── ChatRoomFileIO.java     # 檔案 I/O 處理
    └── ChatRoomManager.java    # 聊天室管理器
```

### 📄 核心檔案說明

#### 1. `ethInfo/EthBasis.java`
**功能**: 區塊鏈連接設定中心
- 定義 RPC URL (`http://127.0.0.1:8545`)
- 定義 Chain ID (`11330023`)
- 管理合約地址 (`gomokuAddress`)
- **支援雙帳號設定**:
  - `keystorePath1` + `password1`: 玩家 1 (黑方/創建者)
  - `keystorePath2` + `password2`: 玩家 2 (白方/加入者)

**使用方式**:
```java
// 所有其他類別透過靜態變數存取設定
String rpcUrl = EthBasis.rpcUrl;
String contractAddress = EthBasis.gomokuAddress;
```

---

#### 2. `ethSC/Gomoku.java`
**功能**: 智能合約的 Java 包裝類
- **自動生成**: 由 Web3j 工具從 Solidity 合約 + ABI 生成
- **提供類型安全的合約函數呼叫**
- 包含所有合約函數、事件、結構體的 Java 映射

**主要功能**:
```java
// 創建遊戲
TransactionReceipt receipt = gomoku.createGame(betAmount).send();

// 加入遊戲
gomoku.joinGame(gameId, betAmount).send();

// 下棋
gomoku.makeMove(gameId, x, y).send();

// 查詢遊戲資訊（唯讀，不需挖礦）
GameInfo info = gomoku.getGameInfo(gameId).send();
```

**不要手動修改此檔案** - 如果合約更新，重新生成即可。

---

#### 3. `ethSC/GomokuHandler.java`
**功能**: 合約互動的便捷處理器
- 封裝 Web3j 連接邏輯
- 管理帳號憑證 (Credentials)
- 提供簡化的合約操作方法

**主要特點**:
```java
// 支援兩種建構方式

// 方式 1: 使用預設帳號（帳號 1）
GomokuHandler handler = new GomokuHandler();

// 方式 2: 指定 keystore 和密碼
GomokuHandler handler = new GomokuHandler(keystorePath, password);
```

**提供的方法**:
- `joinGame(gameId, betAmount)` - 加入遊戲
- `makeMove(gameId, x, y)` - 下棋
- `sendChat(gameId, message)` - 發送聊天
- `claimTimeout(gameId)` - 宣告超時
- `surrender(gameId)` - 投降
- `getContract()` - 取得原始合約物件

---

#### 4. `gomokuApp/GomokuCLI.java`
**功能**: 五子棋遊戲的命令列使用者介面
- **互動式選單**: 提供 9 種操作選項
- **支援雙帳號**: 啟動時可選擇使用哪個帳號
- **即時監聽**: 可訂閱區塊鏈事件

**使用流程**:
```
1. 啟動程式
2. 選擇帳號 (1: 黑方, 2: 白方)
3. 選擇操作 (創建/加入/下棋/查詢等)
4. 在 Geth console 挖礦確認交易
5. 查看結果
```

**選單操作**:
```
1) createGame      - 創建新遊戲（可設定賭注）
2) joinGame        - 加入現有遊戲
3) makeMove        - 下棋（輸入座標）
4) sendChat        - 發送聊天訊息
5) claimTimeout    - 宣告對手超時獲勝
6) surrender       - 投降
7) getGameInfo     - 查看遊戲資訊（唯讀）
8) tail MoveMade   - 監聽下棋事件（即時）
9) quit            - 退出程式
```

---

#### 5. `chatRoomApp/*.java`
**功能**: 聊天室相關功能（獨立於遊戲）
- `ChatRoomApp.java` - 聊天室主程式
- `ChatRoomClient.java` - 客戶端實現
- `ChatRoomManager.java` - 聊天室管理
- `ChatRoomFileIO.java` - 聊天記錄檔案處理

可用於遊戲中的聊天功能擴展。

---

## 🎮 使用 Java 程式進行遊戲

### 前置準備

#### 1. 確保 Geth 節點運行
```bash
# 在 Docker 容器中
geth --datadir ./ --networkid 11330023 \
  --http --http.addr 0.0.0.0 --http.port 8545 \
  --http.api eth,web3,net,personal \
  --http.corsdomain "*" \
  --http.vhosts "*" \
  --allow-insecure-unlock console
```

#### 2. 創建並解鎖兩個帳號
```javascript
// 在 Geth console 中
personal.newAccount("nycu")   // 帳號 1
personal.newAccount("nycu")   // 帳號 2

// 挖礦讓帳號 1 獲得 ETH
miner.start()
// 等待區塊增加
miner.stop()

// 轉帳給帳號 2
personal.unlockAccount(eth.accounts[0], "nycu", 0)
eth.sendTransaction({
    from: eth.accounts[0],
    to: eth.accounts[1],
    value: web3.toWei(10, "ether")
})
miner.start(1); admin.sleep(2); miner.stop()

// 解鎖兩個帳號
personal.unlockAccount(eth.accounts[0], "nycu", 0)
personal.unlockAccount(eth.accounts[1], "nycu", 0)
```

#### 3. 匯出 Keystore 檔案
```bash
# 在新 terminal 中
docker ps
docker cp <container_id>:/keystore ./keystore
ls ./keystore
# 會看到兩個 UTC-- 開頭的檔案
```

#### 4. 更新 `EthBasis.java`
```java
// 修改為實際的 keystore 路徑
public static String keystorePath1 = "./keystore/UTC--...--address1";
public static String keystorePath2 = "./keystore/UTC--...--address2";
```

---

### 🎯 雙人對戰流程

#### Terminal 1: 玩家 A (黑方/創建者)

```bash
# 運行 Java 程式
java -cp ... gomokuApp.GomokuCLI

# 選擇帳號
選擇玩家帳號:
1) Player 1 (Black/創建者)
2) Player 2 (White/加入者)
> 1

# 創建遊戲
=== Gomoku Tester ===
> 1
Bet amount in wei (0 for none): 1000000000
```

**在 Geth console 挖礦**:
```javascript
miner.start(1); admin.sleep(2); miner.stop()
```

#### Terminal 2: 玩家 B (白方/加入者)

```bash
# 運行第二個 Java 程式實例
java -cp ... gomokuApp.GomokuCLI

# 選擇帳號 2
> 2

# 加入遊戲
> 2
gameId: 0
bet amount: 1000000000
```

**在 Geth console 挖礦**:
```javascript
miner.start(1); admin.sleep(2); miner.stop()
```

#### 玩家 A 下第一步棋

```
> 3
gameId: 0
x (0-14): 7
y (0-14): 7
```

**挖礦**: `miner.start(1); admin.sleep(2); miner.stop()`

#### 玩家 B 回應

```
> 3
gameId: 0
x: 7
y: 8
```

**挖礦**: `miner.start(1); admin.sleep(2); miner.stop()`

---

### 💡 實用技巧

#### 查看遊戲狀態（不需挖礦）
```
> 7
gameId: 0

輸出:
blackPlayer: 0x851dE6089fdbdeE99562DD922A0aA74147F52b70
whitePlayer: 0x1b325b09c712f993f49550da3f3c9c288e4adc50
currentPlayer: 1 (1=黑方, 2=白方)
state: 1 (0=Waiting, 1=Playing, 2=BlackWin, 3=WhiteWin, 4=Draw)
betWei: 1000000000
moveCount: 2
```

#### 即時監聽遊戲事件
```
> 8
Subscribing to MoveMade from latest...
Press Enter to stop tailing...

Move game=0 player=0x851d... x=7 y=7 piece=1
Move game=0 player=0x1b32... x=7 y=8 piece=2
```

#### 發送聊天訊息
```
> 4
gameId: 0
message: Good game!
[挖礦]
```

---

## ⚠️ 重要注意事項

### 每次交易後必須挖礦
```javascript
// 在 Geth console 中
miner.start(1)    // 啟動挖礦
admin.sleep(2)    // 等待 2 秒
miner.stop()      // 停止挖礦
```

**便捷函數**（推薦）:
```javascript
// 在 Geth console 定義一次
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

// 每次交易後執行
m()
```

### 常見錯誤處理

**錯誤: "You are already in this game"**
- 原因: 用同一個帳號加入自己創建的遊戲
- 解決: 使用不同的帳號（切換到帳號 2）

**錯誤: "Not your turn"**
- 原因: 不是您的回合
- 解決: 等待對手下棋，或用選項 7 查看 `currentPlayer`

**錯誤: "Position already occupied"**
- 原因: 該位置已有棋子
- 解決: 選擇其他座標

**錯誤: "authentication needed"**
- 原因: 帳號未解鎖
- 解決: 在 Geth console 執行 `personal.unlockAccount(...)`

---

## 📊 技術架構

```
使用者
  ↓
GomokuCLI.java (命令列介面)
  ↓
GomokuHandler.java (業務邏輯)
  ↓
Gomoku.java (合約包裝)
  ↓
Web3j (Java 區塊鏈庫)
  ↓
HTTP-RPC (port 8545)
  ↓
Geth 節點 (私有鏈)
  ↓
Gomoku.sol (智能合約)
  ↓
區塊鏈狀態儲存
```

**資料流向**:
1. 使用者輸入 → Java CLI
2. CLI 呼叫 Handler
3. Handler 打包交易
4. Web3j 發送 RPC 請求
5. Geth 執行合約
6. 合約驗證 + 更新狀態
7. 返回結果 → 顯示給使用者

---

## 🔧 開發擴展

### 如何重新生成合約包裝類

當 Solidity 合約更新後：

```bash
# 1. 編譯合約獲得 ABI 和 Bytecode
# 在 Remix 中編譯，複製 ABI 到 Gomoku.abi.json

# 2. 使用 Web3j 命令行工具生成
web3j generate solidity \
  -a Gomoku.abi.json \
  -b Gomoku.bin \
  -o ./src/ethSC \
  -p ethSC
```

### 如何添加新功能

1. 在 `Gomoku.sol` 中添加新函數
2. 重新部署合約或使用 `upgradeable` 模式
3. 重新生成 `Gomoku.java`
4. 在 `GomokuHandler.java` 中添加便捷方法
5. 在 `GomokuCLI.java` 選單中添加新選項

---

## 📚 參考資料

- **Web3j 官方文檔**: https://docs.web3j.io/
- **Solidity 文檔**: https://docs.soliditylang.org/
- **Geth 文檔**: https://geth.ethereum.org/docs/

---

## 合約地址

**Gomoku 合約**: `0xC1a3dCD9178952DB33934Be3143a6413FADe7200`