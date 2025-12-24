// 聊天室相關功能
let chatSubscription = null;

// 初始化聊天室
function initChat() {
    const chatInput = document.getElementById('chatInput');
    
    // 監聽 Enter 鍵
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            sendChat();
        }
    });
}

// 發送聊天訊息
async function sendChat() {
    if (!currentAccount) {
        showToast("請先選擇帳號", "warning");
        return;
    }
    
    if (currentGameId === null) {
        showToast("請先載入遊戲", "warning");
        return;
    }
    
    const chatInput = document.getElementById('chatInput');
    const message = chatInput.value.trim();
    
    if (!message) {
        showToast("請輸入訊息", "warning");
        return;
    }
    
    try {
        showToast("⏳ 正在發送訊息...", "info");
        
        await contract.methods.sendChat(currentGameId, message).send({
            from: currentAccount,
            gas: 3000000
        });
        
        // 清空輸入框
        chatInput.value = '';
        
        showToast("✓ 訊息已發送", "success");
        showToast("⚠ 請記得執行挖礦命令確認交易", "warning");
        
    } catch (error) {
        console.error("發送訊息失敗:", error);
        showToast("✗ 發送訊息失敗: " + error.message, "error");
    }
}

// 載入聊天記錄
async function loadChatHistory() {
    if (currentGameId === null) return;
    
    try {
        // 取消舊訂閱
        if (chatSubscription) {
            chatSubscription.unsubscribe();
        }
        
        const chatMessages = document.getElementById('chatMessages');
        chatMessages.innerHTML = '<div class="chat-loading">載入聊天記錄中...</div>';
        
        // 獲取歷史 ChatSent 事件
        const events = await contract.getPastEvents('ChatSent', {
            filter: { gameId: currentGameId },
            fromBlock: 0,
            toBlock: 'latest'
        });
        
        // 清空訊息區域
        chatMessages.innerHTML = '';
        
        if (events.length === 0) {
            chatMessages.innerHTML = '<div class="chat-empty">暫無聊天訊息</div>';
        } else {
            // 按時間順序顯示訊息
            events.forEach(event => {
                addChatMessage(
                    event.returnValues.sender,
                    event.returnValues.message,
                    event.returnValues.moveNumber,
                    event.blockNumber
                );
            });
        }
        
        // 訂閱新訊息
        subscribeToChatEvents();
        
    } catch (error) {
        console.error("載入聊天記錄失敗:", error);
        document.getElementById('chatMessages').innerHTML = 
            '<div class="chat-error">載入聊天記錄失敗</div>';
    }
}

// 訂閱聊天事件
function subscribeToChatEvents() {
    if (currentGameId === null) return;
    
    chatSubscription = contract.events.ChatSent({
        filter: { gameId: currentGameId },
        fromBlock: 'latest'
    })
    .on('data', (event) => {
        const { sender, message, moveNumber } = event.returnValues;
        addChatMessage(sender, message, moveNumber, event.blockNumber);
        
        // 播放提示音（可選）
        playNotificationSound();
    })
    .on('error', console.error);
}

// 添加聊天訊息到顯示區域
function addChatMessage(sender, message, moveNumber, blockNumber) {
    const chatMessages = document.getElementById('chatMessages');
    
    // 移除空訊息提示
    const emptyMsg = chatMessages.querySelector('.chat-empty, .chat-loading');
    if (emptyMsg) {
        emptyMsg.remove();
    }
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'chat-message';
    
    // 判斷是否為當前用戶的訊息
    const isCurrentUser = sender.toLowerCase() === currentAccount?.toLowerCase();
    if (isCurrentUser) {
        messageDiv.classList.add('chat-message-self');
    }
    
    const time = new Date().toLocaleTimeString();
    
    messageDiv.innerHTML = `
        <div class="chat-message-header">
            <span class="chat-sender" title="${sender}">
                ${isCurrentUser ? '我' : formatAddress(sender)}
            </span>
            <span class="chat-time">${time}</span>
            <span class="chat-move-number">移動 #${moveNumber}</span>
        </div>
        <div class="chat-message-content">${escapeHtml(message)}</div>
    `;
    
    chatMessages.appendChild(messageDiv);
    
    // 自動滾動到底部
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// 轉義 HTML 特殊字符
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 播放提示音（可選實現）
function playNotificationSound() {
    // 可以添加音效播放邏輯
    // 例如: new Audio('notification.mp3').play();
}

// 清空聊天記錄
function clearChatMessages() {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '<div class="chat-empty">暫無聊天訊息</div>';
}
