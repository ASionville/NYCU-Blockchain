// 全局變數
let web3;
let contract;
let currentAccount = null;
let currentGameId = null;
let eventSubscriptions = [];
let board; // 棋盤實例

// 初始化應用
async function init() {
    try {
        // 初始化 Web3
        web3 = new Web3(RPC_URL);
        
        // 初始化合約實例
        contract = new web3.eth.Contract(CONTRACT_ABI, CONTRACT_ADDRESS);
        
        // 載入帳號列表
        await loadAccounts();
        
        // 初始化棋盤
        board = new GomokuBoard('gameBoard');
        board.onCellClick = handleBoardClick;
        
        // 初始化聊天室
        initChat();
        
        updateConnectionStatus(true);
        showToast("✓ 已連接到區塊鏈節點", "success");
        
        logEvent("系統", "應用程式初始化完成");
        
        // 🔥 更新總遊戲數
        await updateGameCounter();
        
        // 🔥 嘗試恢復上次的遊戲會話
        await restoreGameSession();
        
    } catch (error) {
        console.error("初始化失敗:", error);
        updateConnectionStatus(false);
        showToast("✗ 連接失敗: " + error.message, "error");
    }
}

// 載入帳號列表
async function loadAccounts() {
    try {
        const accounts = await web3.eth.getAccounts();
        const select = document.getElementById('accountSelect');
        select.innerHTML = '<option value="">請選擇帳號...</option>';
        
        accounts.forEach((account, index) => {
            const option = document.createElement('option');
            option.value = account;
            option.textContent = `帳號 ${index + 1}: ${account}`;
            select.appendChild(option);
        });
        
        // 監聽帳號變更
        select.addEventListener('change', async (e) => {
            currentAccount = e.target.value;
            if (currentAccount) {
                document.getElementById('accountAddress').textContent = 
                    `當前帳號: ${formatAddress(currentAccount)}`;
                
                // 🔥 保存當前帳號到 localStorage
                localStorage.setItem('currentAccount', currentAccount);
                
                // 解鎖帳號 (嘗試多個密碼)
                try {
                    await web3.eth.personal.unlockAccount(currentAccount, "nycu", 0);
                    showToast(`✓ 帳號已解鎖: ${formatAddress(currentAccount)}`, "success");
                } catch (error) {
                    try {
                        await web3.eth.personal.unlockAccount(currentAccount, "nycu2", 0);
                        showToast(`✓ 帳號已解鎖: ${formatAddress(currentAccount)}`, "success");
                    } catch (error2) {
                        showToast("⚠ 帳號解鎖失敗，請在 Geth console 手動解鎖", "warning");
                    }
                }
                
                // 🔥 自動檢查並載入當前遊戲
                await checkAndLoadCurrentGame();
            }
        });
        
    } catch (error) {
        console.error("載入帳號失敗:", error);
        showToast("✗ 無法載入帳號列表", "error");
    }
}

// 🔥 更新遊戲計數器顯示
async function updateGameCounter() {
    try {
        const gameCounter = await contract.methods.gameCounter().call();
        document.getElementById('totalGames').textContent = gameCounter;
        logEvent("系統", `當前有 ${gameCounter} 個遊戲 (ID: 0-${gameCounter - 1})`);
    } catch (error) {
        console.error("無法獲取遊戲計數:", error);
        document.getElementById('totalGames').textContent = "錯誤";
    }
}

// 創建遊戲
async function createGame() {
    if (!currentAccount) {
        showToast("請先選擇帳號", "warning");
        return;
    }
    
    try {
        const betAmount = document.getElementById('betAmount').value || "0";
        
        showToast("⏳ 正在創建遊戲...", "info");
        logEvent("系統", `正在創建遊戲 (賭注: ${betAmount} wei)`);
        
        const receipt = await contract.methods.createGame().send({
            from: currentAccount,
            value: betAmount,
            gas: 3000000
        });
        
        // 從事件中獲取遊戲 ID
        const gameId = receipt.events.GameCreated.returnValues.gameId;
        
        showToast(`✓ 遊戲創建成功！遊戲 ID: ${gameId}`, "success");
        logEvent("遊戲", `遊戲 ${gameId} 已創建`);
        
        // 🔥 更新遊戲計數器
        await updateGameCounter();
        
        // 自動載入新遊戲
        document.getElementById('gameIdInput').value = gameId;
        await loadGame();
        
        showToast("⚠ 請記得執行挖礦命令確認交易", "warning");
        
    } catch (error) {
        console.error("創建遊戲失敗:", error);
        showToast("✗ 創建遊戲失敗: " + error.message, "error");
        logEvent("錯誤", "創建遊戲失敗: " + error.message);
    }
}

// 加入遊戲
async function joinGame() {
    if (!currentAccount) {
        showToast("請先選擇帳號", "warning");
        return;
    }
    
    if (currentGameId === null) {
        showToast("請先載入遊戲", "warning");
        return;
    }
    
    try {
        // 先獲取遊戲資訊以確認賭注
        const info = await contract.methods.getGameInfo(currentGameId).call();
        const betAmount = info.betAmount;
        
        showToast(`⏳ 正在加入遊戲 ${currentGameId}...`, "info");
        logEvent("系統", `正在加入遊戲 ${currentGameId} (賭注: ${betAmount} wei)`);
        
        await contract.methods.joinGame(currentGameId).send({
            from: currentAccount,
            value: betAmount,
            gas: 3000000
        });
        
        showToast(`✓ 已成功加入遊戲 ${currentGameId}`, "success");
        logEvent("遊戲", `已加入遊戲 ${currentGameId}`);
        
        // 重新載入遊戲狀態
        await loadGame();
        
        showToast("⚠ 請記得執行挖礦命令確認交易", "warning");
        
    } catch (error) {
        console.error("加入遊戲失敗:", error);
        showToast("✗ 加入遊戲失敗: " + error.message, "error");
        logEvent("錯誤", "加入遊戲失敗: " + error.message);
    }
}

// 載入遊戲
async function loadGame() {
    const gameId = document.getElementById('gameIdInput').value;
    
    if (!gameId && gameId !== "0") {
        showToast("請輸入遊戲 ID", "warning");
        return;
    }
    
    try {
        currentGameId = parseInt(gameId);
        
        // 🔥 檢查遊戲是否存在
        const gameCounter = await contract.methods.gameCounter().call();
        if (currentGameId >= parseInt(gameCounter)) {
            showToast(`✗ 遊戲 ${currentGameId} 不存在！目前只有 ${gameCounter} 個遊戲 (ID: 0-${gameCounter - 1})`, "error");
            logEvent("錯誤", `遊戲 ${currentGameId} 不存在，gameCounter = ${gameCounter}`);
            return;
        }
        
        showToast(`⏳ 正在載入遊戲 ${currentGameId}...`, "info");
        
        // 獲取遊戲資訊
        const info = await contract.methods.getGameInfo(currentGameId).call();
        
        console.log('Game info:', info);
        
        // 更新遊戲資訊顯示
        document.getElementById('gameState').textContent = GameState[info.state];
        document.getElementById('currentTurn').textContent = 
            info.currentPlayer == 1 ? "黑方 (先手)" : "白方 (後手)";
        document.getElementById('blackPlayer').textContent = formatAddress(info.blackPlayer);
        document.getElementById('whitePlayer').textContent = 
            info.whitePlayer === "0x0000000000000000000000000000000000000000" 
                ? "等待中..." 
                : formatAddress(info.whitePlayer);
        document.getElementById('betAmountDisplay').textContent = `${info.betAmount} wei`;
        document.getElementById('moveCount').textContent = info.moveCount;
        
        // 🔥 使用 loadBoardState 而不是直接調用 getBoard
        await loadBoardState(currentGameId);
        
        // 保存遊戲 ID 到 localStorage
        localStorage.setItem('currentGameId', currentGameId);
        
        showToast(`✓ 遊戲 ${currentGameId} 載入成功`, "success");
        logEvent("系統", `已載入遊戲 ${currentGameId}`);
        
        // 訂閱事件
        subscribeToEvents();
        
        // 載入聊天記錄
        loadChatHistory();
        
    } catch (error) {
        console.error("載入遊戲失敗:", error);
        showToast("✗ 載入遊戲失敗: " + error.message, "error");
        logEvent("錯誤", "載入遊戲失敗: " + error.message);
    }
}

// 修正 loadBoardState 函數
async function loadBoardState(gameId) {
    try {
        console.log('📥 Loading board state for game', gameId);
        
        board.clear();
        
        // 🔥 合約使用 board[row][col]，但參數名是 getPiece(_gameId, _x, _y)
        // 其中 _x 實際上是 row，_y 實際上是 col
        for (let row = 0; row < 15; row++) {
            for (let col = 0; col < 15; col++) {
                try {
                    // 呼叫合約：getPiece(gameId, row, col)
                    const cellValue = await contract.methods.getPiece(gameId, row, col).call();
                    const piece = parseInt(cellValue);
                    
                    if (piece !== 0) {
                        // 🔥 合約的 [row][col] 對應 UI 的 (col, row)
                        // 因為 UI: x=水平(col), y=垂直(row)
                        // 合約: board[_x][_y] 其中 _x=row, _y=col
                        console.log(`📍 Contract[${row}][${col}] = ${piece} -> UI(${col}, ${row})`);
                        board.placePiece(col, row, piece);
                    }
                } catch (err) {
                    console.warn(`Failed to get cell (${row}, ${col}):`, err.message);
                }
            }
        }
        
        console.log('✅ Board state loaded');
        
    } catch (error) {
        console.error('Failed to load board state:', error);
    }
}

// 棋盤點擊處理
// 棋盤點擊處理
async function handleBoardClick(x, y) {
    console.log(`🖱️ Board clicked at: x=${x}, y=${y}`);
    await makeMove(x, y);
}

// 下棋 (從棋盤點擊觸發)
async function makeMove(x, y) {
    if (!currentAccount || currentGameId === null) {
        showToast("請先載入遊戲", "warning");
        return;
    }

    try {
        // 🔥 詳細的事前檢查
        showToast(`⏳ 檢查遊戲狀態...`, "info");
        
        const gameInfo = await contract.methods.getGameInfo(currentGameId).call();
        
        const blackPlayer = gameInfo.blackPlayer.toLowerCase();
        const whitePlayer = gameInfo.whitePlayer.toLowerCase();
        const currentTurn = parseInt(gameInfo.currentPlayer);
        const gameState = parseInt(gameInfo.state);
        const myAccount = currentAccount.toLowerCase();
        
        // 檢查遊戲狀態
        const stateNames = ["等待中", "進行中", "黑方勝", "白方勝", "平局"];
        if (gameState !== 1) {
            showToast(`✗ 遊戲不在進行中！狀態: ${stateNames[gameState]}`, "error");
            return;
        }
        
        // 判斷我是哪個玩家
        let myColor;
        let myColorName;
        if (myAccount === blackPlayer) {
            myColor = 1;
            myColorName = "黑方";
        } else if (myAccount === whitePlayer) {
            myColor = 2;
            myColorName = "白方";
        } else {
            showToast(`✗ 您不是這場遊戲的玩家！`, "error");
            logEvent("錯誤", `您的帳號: ${myAccount}`);
            logEvent("錯誤", `黑方: ${blackPlayer}`);
            logEvent("錯誤", `白方: ${whitePlayer}`);
            return;
        }
        
        logEvent("遊戲", `您是 ${myColorName}，嘗試在 (${x}, ${y}) 下棋`);
        
        // 檢查是否輪到我
        if (currentTurn !== myColor) {
            const waitingFor = currentTurn === 1 ? "黑方" : "白方";
            const waitingAddress = currentTurn === 1 ? blackPlayer : whitePlayer;
            showToast(`⚠ 還沒輪到您！現在是 ${waitingFor} 的回合`, "warning");
            logEvent("遊戲", `等待 ${waitingFor} (${waitingAddress}) 下棋...`);
            return;
        }
        
        // 檢查位置是否為空
        const piece = await contract.methods.getPiece(currentGameId, x, y).call();
        if (parseInt(piece) !== 0) {
            const pieceNames = ["空", "黑子", "白子"];
            showToast(`✗ 位置 (${x}, ${y}) 已有 ${pieceNames[parseInt(piece)]}！`, "warning");
            return;
        }

        showToast(`⏳ 正在下 ${myColorName} 棋 (${x}, ${y})...`, "info");
        
        // 🔥 關鍵修改：發送到合約時交換 x 和 y
        // 因為合約的 board[_x][_y] 實際上應該理解為 board[row][col]
        console.log(`🔄 座標轉換: UI(${x}, ${y}) -> Contract(${y}, ${x})`);
        logEvent("交易", `發送 makeMove: gameId=${currentGameId}, contract_x=${y}, contract_y=${x} (UI座標: ${x},${y})`);

        const receipt = await contract.methods.makeMove(currentGameId, y, x).send({
            from: currentAccount,
            gas: 500000
        });

        console.log("Transaction receipt:", receipt);

        showToast(`✓ 下棋成功！位置: (${x}, ${y})`, "success");
        logEvent("遊戲", `成功在 (${x}, ${y}) 下 ${myColorName} 棋`);
        
        // 本地更新棋盤（立即反饋）
        board.placePiece(x, y, myColor);
        
        showToast("⚠ 請執行挖礦命令確認交易", "warning");

    } catch (error) {
        console.error("下棋失敗:", error);
        
        let errorMsg = "未知錯誤";
        if (error.message) {
            const msg = error.message.toLowerCase();
            if (msg.includes("not your turn")) {
                errorMsg = "還沒輪到您下棋！";
                errorDetails = "請等待對手完成回合";
            } else if (msg.includes("position already occupied") || msg.includes("position occupied")) {
                errorMsg = `位置 (${x}, ${y}) 已被占用！`;
                errorDetails = "請選擇其他空位";
            } else if (msg.includes("game is not in playing state")) {
                errorMsg = "遊戲不在進行中";
                errorDetails = "請檢查遊戲狀態";
            } else if (msg.includes("invalid coordinates")) {
                errorMsg = `座標 (${x}, ${y}) 無效！`;
                errorDetails = "座標範圍應為 0-14";
            } else if (msg.includes("not a player")) {
                errorMsg = "您不是這場遊戲的玩家";
                errorDetails = "請確認您加入了正確的遊戲";
            } else if (msg.includes("user denied")) {
                errorMsg = "交易被拒絕";
                errorDetails = "您取消了交易簽名";
            } else if (msg.includes("insufficient funds")) {
                errorMsg = "餘額不足";
                errorDetails = "請確保帳號有足夠的 ETH 支付 gas";
            } else if (msg.includes("reverted")) {
                errorMsg = "交易被回滾";
                errorDetails = "可能原因：不是您的回合、位置已占用、或遊戲已結束";
            } else {
                errorMsg = error.message;
            }
        }
        
        showToast(`✗ 下棋失敗: ${errorMsg}`, "error");
        if (errorDetails) {
            showToast(`💡 ${errorDetails}`, "info");
        }
        
        logEvent("錯誤", `下棋失敗: ${errorMsg}`);
        logEvent("錯誤", `座標: (${x}, ${y})`);
        logEvent("錯誤", `詳細: ${error.message}`);
        
        // 🔥 在 console 輸出完整錯誤供調試
        console.group("❌ 下棋失敗詳情");
        console.log("座標:", x, y);
        console.log("遊戲 ID:", currentGameId);
        console.log("帳號:", currentAccount);
        console.log("錯誤訊息:", error.message);
        console.log("錯誤物件:", error);
        console.groupEnd();
    }
}

// 認輸
async function surrender() {
    if (!currentAccount) {
        showToast("請先選擇帳號", "warning");
        return;
    }
    
    if (currentGameId === null) {
        showToast("請先載入遊戲", "warning");
        return;
    }
    
    if (!confirm("確定要認輸嗎？")) {
        return;
    }
    
    try {
        showToast("⏳ 正在認輸...", "info");
        logEvent("遊戲", "玩家認輸");
        
        await contract.methods.surrender(currentGameId).send({
            from: currentAccount,
            gas: 3000000
        });
        
        showToast("✓ 已認輸", "success");
        logEvent("遊戲", "認輸成功");
        
        showToast("⚠ 請記得執行挖礦命令確認交易", "warning");
        
    } catch (error) {
        console.error("認輸失敗:", error);
        showToast("✗ 認輸失敗: " + error.message, "error");
        logEvent("錯誤", "認輸失敗: " + error.message);
    }
}

// 訂閱事件
function subscribeToEvents() {
    // 清除舊訂閱
    eventSubscriptions.forEach(sub => sub.unsubscribe());
    eventSubscriptions = [];
    
    if (currentGameId === null) return;
    
    // 訂閱 MoveMade 事件
    const moveSub = contract.events.MoveMade({
        filter: { gameId: currentGameId },
        fromBlock: 'latest'
    })
    .on('data', async (event) => {
        const { x, y, piece, player } = event.returnValues;
        
        // 🔥 座標轉換
        const uiX = parseInt(y);  // 合約的 y -> UI 的 x
        const uiY = parseInt(x);  // 合約的 x -> UI 的 y
        
        console.log(`📡 MoveMade轉換: Contract(${x}, ${y}) -> UI(${uiX}, ${uiY})`);
        logEvent("移動", `玩家 ${formatAddress(player)} 下在 (${uiX}, ${uiY})`);
        
        // 重新載入遊戲狀態
        await loadGame();
    })
    .on('error', console.error);
    
    eventSubscriptions.push(moveSub);
    
    // 訂閱 GameEnded 事件
    const endSub = contract.events.GameEnded({
        filter: { gameId: currentGameId },
        fromBlock: 'latest'
    })
    .on('data', (event) => {
        const { state, winner } = event.returnValues;
        const stateNames = ["等待中", "進行中", "黑方勝", "白方勝", "平局"];
        logEvent("遊戲結束", `結果: ${stateNames[state]}, 贏家: ${formatAddress(winner)}`);
        showToast(`🎉 遊戲結束！${stateNames[state]}`, "success");
    })
    .on('error', console.error);
    
    eventSubscriptions.push(endSub);
    
    // 訂閱 PlayerJoined 事件
    const joinSub = contract.events.PlayerJoined({
        filter: { gameId: currentGameId },
        fromBlock: 'latest'
    })
    .on('data', async (event) => {
        const { player } = event.returnValues;
        logEvent("玩家加入", `玩家 ${formatAddress(player)} 已加入遊戲`);
        showToast(`✓ 玩家已加入遊戲`, "success");
        
        // 重新載入遊戲狀態
        await loadGame();
    })
    .on('error', console.error);
    
    eventSubscriptions.push(joinSub);
}

// 工具函數：格式化地址
function formatAddress(address) {
    if (!address || address === "0x0000000000000000000000000000000000000000") {
        return "-";
    }
    return `${address.substring(0, 6)}...${address.substring(38)}`;
}

// 更新連接狀態
function updateConnectionStatus(connected) {
    const statusEl = document.getElementById('connectionStatus');
    if (connected) {
        statusEl.textContent = "已連接";
        statusEl.className = "status-connected";
    } else {
        statusEl.textContent = "未連接";
        statusEl.className = "status-disconnected";
    }
}

// 顯示提示訊息
function showToast(message, type = "info") {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast toast-${type} show`;
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 5000);
}

// 記錄事件
function logEvent(category, message) {
    const eventLog = document.getElementById('eventLog');
    const time = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = 'event-entry';
    entry.innerHTML = `
        <span class="event-time">[${time}]</span>
        <span class="event-category">[${category}]</span>
        <span class="event-message">${message}</span>
    `;
    eventLog.insertBefore(entry, eventLog.firstChild);
    
    // 限制記錄數量
    while (eventLog.children.length > 50) {
        eventLog.removeChild(eventLog.lastChild);
    }
}

// 複製挖礦命令
function copyMiningCommand() {
    const command = "miner.start(1); admin.sleep(3); miner.stop()";
    navigator.clipboard.writeText(command).then(() => {
        showToast("✓ 已複製挖礦命令", "success");
    }).catch(() => {
        showToast("✗ 複製失敗", "error");
    });
}

// 監聽下棋事件
function startEventListeners() {
    console.log('Starting event listeners...');
    
    // 監聽 MoveMade 事件
    contract.events.MoveMade({
        fromBlock: 'latest'
    })
    .on('data', async (event) => {
        console.log('📡 MoveMade event:', event.returnValues);
        
        const { gameId, x, y, piece } = event.returnValues;
        
        // 🔥 關鍵：事件中的 x, y 是合約發出的 (row, col)
        // 需要轉換為 UI 的 (col, row)
        const uiX = parseInt(y);  // 合約的 y (col) -> UI 的 x
        const uiY = parseInt(x);  // 合約的 x (row) -> UI 的 y
        
        console.log(`🔄 Event轉換: Contract(x=${x}, y=${y}) -> UI(${uiX}, ${uiY})`);
        
        if (parseInt(gameId) === currentGameId) {
            board.placePiece(uiX, uiY, parseInt(piece));
            await loadGame();
        }
        
        addEventLog(`下棋: 遊戲 ${gameId}, 位置 (${uiX}, ${uiY}), ${piece === '1' ? '黑' : '白'}子`);
    })
    .on('error', console.error);
    
    // ... 其他事件監聽器保持不變
}

// 🔥 新增：恢復遊戲會話
async function restoreGameSession() {
    try {
        // 從 localStorage 恢復帳號
        const savedAccount = localStorage.getItem('currentAccount');
        if (savedAccount) {
            const accounts = await web3.eth.getAccounts();
            if (accounts.includes(savedAccount)) {
                document.getElementById('accountSelect').value = savedAccount;
                currentAccount = savedAccount;
                document.getElementById('accountAddress').textContent = 
                    `當前帳號: ${formatAddress(currentAccount)}`;
                
                // 自動檢查並載入遊戲
                await checkAndLoadCurrentGame();
            }
        }
        
        // 從 localStorage 恢復遊戲 ID
        const savedGameId = localStorage.getItem('currentGameId');
        if (savedGameId && currentAccount) {
            document.getElementById('gameIdInput').value = savedGameId;
            showToast(`✓ 已恢復上次的遊戲會話 (遊戲 ${savedGameId})`, "info");
        }
        
    } catch (error) {
        console.error('恢復會話失敗:', error);
    }
}

// 🔥 新增：檢查並載入玩家當前的遊戲
async function checkAndLoadCurrentGame() {
    if (!currentAccount) return;
    
    try {
        // 檢查玩家是否在某个遊戲中
        const gameId = await contract.methods.playerGame(currentAccount).call();
        
        if (gameId !== "0") {
            // 獲取遊戲資訊
            const gameInfo = await contract.methods.getGameInfo(gameId).call();
            const stateNames = ["等待對手", "進行中", "黑方獲勝", "白方獲勝", "平局"];
            const stateName = stateNames[parseInt(gameInfo.state)];
            
            // 如果遊戲還在進行中
            if (parseInt(gameInfo.state) === 0 || parseInt(gameInfo.state) === 1) {
                showToast(`✓ 檢測到您在遊戲 ${gameId} 中 (${stateName})`, "success");
                
                // 自動填入遊戲 ID
                document.getElementById('gameIdInput').value = gameId;
                
                // 詢問是否載入
                const shouldLoad = confirm(`您在遊戲 ${gameId} 中 (${stateName})，是否載入該遊戲？`);
                if (shouldLoad) {
                    await loadGame();
                }
            } else {
                showToast(`上一場遊戲 ${gameId} 已結束 (${stateName})`, "info");
            }
        } else {
            showToast("您當前未在任何遊戲中", "info");
        }
        
    } catch (error) {
        console.error('檢查遊戲失敗:', error);
    }
}

// 🔥 修改：改進 loadGame 函數
async function improvedLoadGame() {
    const gameId = document.getElementById('gameIdInput').value;
    
    if (!gameId && gameId !== "0") {
        showToast("請輸入遊戲 ID", "warning");
        return;
    }
    
    try {
        currentGameId = parseInt(gameId);
        
        showToast(`⏳ 正在載入遊戲 ${currentGameId}...`, "info");
        
        // 獲取遊戲資訊
        const info = await contract.methods.getGameInfo(currentGameId).call();
        
        // 更新遊戲資訊顯示
        document.getElementById('gameState').textContent = GameState[info.state];
        document.getElementById('currentTurn').textContent = 
            info.currentPlayer == 1 ? "黑方 (先手)" : "白方 (後手)";
        document.getElementById('blackPlayer').textContent = formatAddress(info.blackPlayer);
        document.getElementById('whitePlayer').textContent = 
            info.whitePlayer === "0x0000000000000000000000000000000000000000" 
                ? "等待中..." 
                : formatAddress(info.whitePlayer);
        document.getElementById('betAmountDisplay').textContent = `${info.betAmount} wei`;
        document.getElementById('moveCount').textContent = info.moveCount;
        
        // 獲取棋盤狀態
        const boardData = await contract.methods.getBoard(currentGameId).call();
        board.updateBoard(boardData);
        
        // 🔥 保存遊戲 ID 到 localStorage
        localStorage.setItem('currentGameId', currentGameId);
        
        showToast(`✓ 遊戲 ${currentGameId} 載入成功`, "success");
        logEvent("系統", `已載入遊戲 ${currentGameId}`);
        
        // 訂閱事件
        subscribeToEvents();
        
        // 載入聊天記錄
        loadChatHistory();
        
    } catch (error) {
        console.error("載入遊戲失敗:", error);
        showToast("✗ 載入遊戲失敗: " + error.message, "error");
        logEvent("錯誤", "載入遊戲失敗: " + error.message);
    }
}

// 頁面載入時初始化
window.addEventListener('load', init);
