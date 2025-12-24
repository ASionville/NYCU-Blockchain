// 合約配置
const CONTRACT_ADDRESS = "0xC1a3dCD9178952DB33934Be3143a6413FADe7200";
const RPC_URL = "http://localhost:8545";

// 帳號配置 (從您的 keystore 導入的地址)
const ACCOUNTS = {
    player1: "0x851de6089fdbdee99562dd922a0aa74147f52b70",
    player2: "0x1b325b09c712f993f49550da3f3c9c288e4adc50"
};

// 合約 ABI (從 Solidity 編譯器生成)
const CONTRACT_ABI = [
    {
        "inputs": [],
        "name": "BOARD_SIZE",
        "outputs": [{"internalType": "uint8", "name": "", "type": "uint8"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "BLACK",
        "outputs": [{"internalType": "uint8", "name": "", "type": "uint8"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "WHITE",
        "outputs": [{"internalType": "uint8", "name": "", "type": "uint8"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "EMPTY",
        "outputs": [{"internalType": "uint8", "name": "", "type": "uint8"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "gameCounter",
        "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "createGame",
        "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
        "stateMutability": "payable",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "_gameId", "type": "uint256"}],
        "name": "joinGame",
        "outputs": [],
        "stateMutability": "payable",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "uint256", "name": "_gameId", "type": "uint256"},
            {"internalType": "uint8", "name": "_x", "type": "uint8"},
            {"internalType": "uint8", "name": "_y", "type": "uint8"}
        ],
        "name": "makeMove",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "uint256", "name": "_gameId", "type": "uint256"},
            {"internalType": "string", "name": "_message", "type": "string"}
        ],
        "name": "sendChat",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "_gameId", "type": "uint256"}],
        "name": "surrender",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "_gameId", "type": "uint256"}],
        "name": "claimTimeout",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "_gameId", "type": "uint256"}],
        "name": "getGameInfo",
        "outputs": [
            {"internalType": "address", "name": "blackPlayer", "type": "address"},
            {"internalType": "address", "name": "whitePlayer", "type": "address"},
            {"internalType": "uint8", "name": "currentPlayer", "type": "uint8"},
            {"internalType": "enum Gomoku.GameState", "name": "state", "type": "uint8"},
            {"internalType": "uint256", "name": "betAmount", "type": "uint256"},
            {"internalType": "uint8", "name": "moveCount", "type": "uint8"}
        ],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "_gameId", "type": "uint256"}],
        "name": "getBoard",
        "outputs": [{"internalType": "uint8[15][15]", "name": "", "type": "uint8[15][15]"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [
            {"internalType": "uint256", "name": "_gameId", "type": "uint256"},
            {"internalType": "uint8", "name": "_x", "type": "uint8"},
            {"internalType": "uint8", "name": "_y", "type": "uint8"}
        ],
        "name": "getPiece",
        "outputs": [{"internalType": "uint8", "name": "", "type": "uint8"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "anonymous": false,
        "inputs": [
            {"indexed": true, "internalType": "uint256", "name": "gameId", "type": "uint256"},
            {"indexed": true, "internalType": "address", "name": "creator", "type": "address"},
            {"indexed": false, "internalType": "uint256", "name": "betAmount", "type": "uint256"}
        ],
        "name": "GameCreated",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {"indexed": true, "internalType": "uint256", "name": "gameId", "type": "uint256"},
            {"indexed": true, "internalType": "address", "name": "player", "type": "address"}
        ],
        "name": "PlayerJoined",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {"indexed": true, "internalType": "uint256", "name": "gameId", "type": "uint256"},
            {"indexed": true, "internalType": "address", "name": "player", "type": "address"},
            {"indexed": false, "internalType": "uint8", "name": "x", "type": "uint8"},
            {"indexed": false, "internalType": "uint8", "name": "y", "type": "uint8"},
            {"indexed": false, "internalType": "uint8", "name": "piece", "type": "uint8"}
        ],
        "name": "MoveMade",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {"indexed": true, "internalType": "uint256", "name": "gameId", "type": "uint256"},
            {"indexed": false, "internalType": "enum Gomoku.GameState", "name": "state", "type": "uint8"},
            {"indexed": false, "internalType": "address", "name": "winner", "type": "address"}
        ],
        "name": "GameEnded",
        "type": "event"
    },
    {
        "anonymous": false,
        "inputs": [
            {"indexed": true, "internalType": "uint256", "name": "gameId", "type": "uint256"},
            {"indexed": true, "internalType": "address", "name": "sender", "type": "address"},
            {"indexed": false, "internalType": "string", "name": "message", "type": "string"},
            {"indexed": false, "internalType": "uint256", "name": "moveNumber", "type": "uint256"}
        ],
        "name": "ChatSent",
        "type": "event"
    }
];

// 遊戲狀態枚舉
const GameState = {
    0: "等待對手",
    1: "進行中",
    2: "黑方獲勝",
    3: "白方獲勝",
    4: "平局"
};
