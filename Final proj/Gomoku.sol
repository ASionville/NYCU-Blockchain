// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title Gomoku Smart Contract
 * @dev Implements a blockchain-based Gomoku (five-in-a-row) game with optional wagers
 */
contract Gomoku {
    // ==================== Constants ====================
    uint8 public constant BOARD_SIZE = 15;  // Board dimension (15x15)
    uint8 public constant EMPTY = 0;        // Empty cell marker
    uint8 public constant BLACK = 1;        // Black piece
    uint8 public constant WHITE = 2;        // White piece
    
    // ==================== Game State ====================
    enum GameState {
        Waiting,    // Waiting for an opponent
        Playing,    // Game in progress
        BlackWin,   // Black wins
        WhiteWin,   // White wins
        Draw        // Board full, no winner
    }
    
    // ==================== Game Model ====================
    struct Game {
        uint256 gameId;
        address blackPlayer;    // Player using black pieces
        address whitePlayer;    // Player using white pieces
        uint8[15][15] board;    // Board state
        uint8 currentPlayer;    // Whose turn: BLACK or WHITE
        GameState state;        // Game lifecycle state
        uint256 betAmount;      // Wager per player (0 if no wager)
        uint256 lastMoveTime;   // Timestamp of the last move
        uint8 moveCount;        // Number of placed pieces
    }
    
    // ==================== State ====================
    uint256 public gameCounter;                    // Incremental game id
    mapping(uint256 => Game) public games;         // gameId -> Game
    mapping(address => uint256) public playerGame; // Player -> active gameId
    uint256 public constant TIMEOUT = 5 minutes;   // Per-move timeout
    
    // ==================== Events ====================
    event GameCreated(uint256 indexed gameId, address indexed creator, uint256 betAmount);
    event PlayerJoined(uint256 indexed gameId, address indexed player);
    event MoveMade(uint256 indexed gameId, address indexed player, uint8 x, uint8 y, uint8 piece);
    event GameEnded(uint256 indexed gameId, GameState state, address winner);
    event GameTimeout(uint256 indexed gameId, address winner);
    event ChatSent(uint256 indexed gameId, address indexed sender, string message, uint256 moveNumber);
    
    // ==================== Modifiers ====================
    modifier gameExists(uint256 _gameId) {
        require(_gameId < gameCounter, "Game does not exist");
        _;
    }
    
    modifier isPlayer(uint256 _gameId) {
        Game storage game = games[_gameId];
        require(
            msg.sender == game.blackPlayer || msg.sender == game.whitePlayer,
            "You are not a player in this game"
        );
        _;
    }
    
    modifier isCurrentPlayer(uint256 _gameId) {
        Game storage game = games[_gameId];
        require(game.state == GameState.Playing, "Game is not in playing state");
        
        if (game.currentPlayer == BLACK) {
            require(msg.sender == game.blackPlayer, "Not your turn");
        } else {
            require(msg.sender == game.whitePlayer, "Not your turn");
        }
        _;
    }
    
    // ==================== Game Creation ====================
    /**
     * @dev Create a new game; msg.value is the optional wager locked per player
     */
    function createGame() external payable returns (uint256) {
        require(playerGame[msg.sender] == 0 || 
                games[playerGame[msg.sender]].state != GameState.Playing,
                "You are already in a game");
        
        uint256 gameId = gameCounter++;
        Game storage game = games[gameId];
        
        game.gameId = gameId;
        game.blackPlayer = msg.sender;
        game.currentPlayer = BLACK;
        game.state = GameState.Waiting;
        game.betAmount = msg.value;
        game.lastMoveTime = block.timestamp;
        
        playerGame[msg.sender] = gameId;
        
        emit GameCreated(gameId, msg.sender, msg.value);
        return gameId;
    }
    
    // ==================== Join Game ====================
    /**
     * @dev Join an existing waiting game, matching the creator's wager
     */
    function joinGame(uint256 _gameId) external payable gameExists(_gameId) {
        Game storage game = games[_gameId];
        
        require(game.state == GameState.Waiting, "Game is not waiting for players");
        require(game.blackPlayer != msg.sender, "You are already in this game");
        require(msg.value == game.betAmount, "Incorrect bet amount");
        require(playerGame[msg.sender] == 0 || 
                games[playerGame[msg.sender]].state != GameState.Playing,
                "You are already in another game");
        
        game.whitePlayer = msg.sender;
        game.state = GameState.Playing;
        game.lastMoveTime = block.timestamp;
        
        playerGame[msg.sender] = _gameId;
        
        emit PlayerJoined(_gameId, msg.sender);
    }
    
    // ==================== Moves ====================
    /**
     * @dev Place a piece on the board
     * @param _gameId Game id
     * @param _x X coordinate (0-14)
     * @param _y Y coordinate (0-14)
     */
    function makeMove(uint256 _gameId, uint8 _x, uint8 _y) 
        external 
        gameExists(_gameId) 
        isCurrentPlayer(_gameId) 
    {
        Game storage game = games[_gameId];
        
        require(_x < BOARD_SIZE && _y < BOARD_SIZE, "Invalid coordinates");
        require(game.board[_x][_y] == EMPTY, "Position already occupied");
        
        // Place the piece
        game.board[_x][_y] = game.currentPlayer;
        game.moveCount++;
        game.lastMoveTime = block.timestamp;
        
        emit MoveMade(_gameId, msg.sender, _x, _y, game.currentPlayer);
        
        // Check for win
        if (checkWin(_gameId, _x, _y)) {
            if (game.currentPlayer == BLACK) {
                game.state = GameState.BlackWin;
                _distributeWinnings(_gameId, game.blackPlayer);
            } else {
                game.state = GameState.WhiteWin;
                _distributeWinnings(_gameId, game.whitePlayer);
            }
            emit GameEnded(_gameId, game.state, msg.sender);
        } 
        // Check for draw (board full)
        else if (game.moveCount >= BOARD_SIZE * BOARD_SIZE) {
            game.state = GameState.Draw;
            _distributeDraw(_gameId);
            emit GameEnded(_gameId, game.state, address(0));
        } 
        // Switch turns
        else {
            game.currentPlayer = (game.currentPlayer == BLACK) ? WHITE : BLACK;
        }
    }
    
    // ==================== Win Checking ====================
    /**
     * @dev Check if the last move wins the game
     */
    function checkWin(uint256 _gameId, uint8 _x, uint8 _y) internal view returns (bool) {
        Game storage game = games[_gameId];
        uint8 piece = game.board[_x][_y];
        
        // Check four directions: horizontal, vertical, main diagonal, anti-diagonal
        return checkDirection(_gameId, _x, _y, 1, 0, piece) ||  // Horizontal
               checkDirection(_gameId, _x, _y, 0, 1, piece) ||  // Vertical
               checkDirection(_gameId, _x, _y, 1, 1, piece) ||  // Main diagonal
               checkDirection(_gameId, _x, _y, 1, -1, piece);   // Anti-diagonal
    }
    
    /**
     * @dev Check if five in a row exists in a specific direction
     */
    function checkDirection(
        uint256 _gameId, 
        uint8 _x, 
        uint8 _y, 
        int8 _dx, 
        int8 _dy, 
        uint8 _piece
    ) internal view returns (bool) {
        Game storage game = games[_gameId];
        uint8 count = 1;  // Count includes the current piece
        
        // Forward direction
        count += countPieces(_gameId, _x, _y, _dx, _dy, _piece);
        // Reverse direction
        count += countPieces(_gameId, _x, _y, -_dx, -_dy, _piece);
        
        return count >= 5;
    }
    
    /**
     * @dev Count consecutive pieces in one direction
     */
    function countPieces(
        uint256 _gameId, 
        uint8 _x, 
        uint8 _y, 
        int8 _dx, 
        int8 _dy, 
        uint8 _piece
    ) internal view returns (uint8) {
        Game storage game = games[_gameId];
        uint8 count = 0;
        int16 x = int16(int8(_x)) + _dx;
        int16 y = int16(int8(_y)) + _dy;
        
        while (x >= 0 && x < int16(int8(BOARD_SIZE)) && 
               y >= 0 && y < int16(int8(BOARD_SIZE)) && 
               game.board[uint8(int8(x))][uint8(int8(y))] == _piece) {
            count++;
            x += _dx;
            y += _dy;
        }
        
        return count;
    }
    
    // ==================== Chat ====================
    /**
     * @dev Emit an on-chain chat message for the given game. Uses events only; no storage.
     */
    function sendChat(uint256 _gameId, string calldata _message)
        external
        gameExists(_gameId)
        isPlayer(_gameId)
    {
        Game storage game = games[_gameId];
        require(game.state == GameState.Waiting || game.state == GameState.Playing, "Game is not active");
        require(bytes(_message).length > 0, "Message is empty");

        emit ChatSent(_gameId, msg.sender, _message, game.moveCount);
    }

    // ==================== Timeout ====================
    /**
     * @dev Claim opponent timeout to win the game
     */
    function claimTimeout(uint256 _gameId) external gameExists(_gameId) isPlayer(_gameId) {
        Game storage game = games[_gameId];
        
        require(game.state == GameState.Playing, "Game is not in playing state");
        require(block.timestamp >= game.lastMoveTime + TIMEOUT, "Timeout not reached");
        
        // The player whose turn it is has timed out; opponent wins
        address winner;
        if (game.currentPlayer == BLACK) {
            game.state = GameState.WhiteWin;
            winner = game.whitePlayer;
        } else {
            game.state = GameState.BlackWin;
            winner = game.blackPlayer;
        }
        
        _distributeWinnings(_gameId, winner);
        emit GameTimeout(_gameId, winner);
        emit GameEnded(_gameId, game.state, winner);
    }
    
    // ==================== Surrender ====================
    /**
     * @dev Player resigns and grants win to the opponent
     */
    function surrender(uint256 _gameId) external gameExists(_gameId) isPlayer(_gameId) {
        Game storage game = games[_gameId];
        
        require(game.state == GameState.Playing, "Game is not in playing state");
        
        address winner;
        if (msg.sender == game.blackPlayer) {
            game.state = GameState.WhiteWin;
            winner = game.whitePlayer;
        } else {
            game.state = GameState.BlackWin;
            winner = game.blackPlayer;
        }
        
        _distributeWinnings(_gameId, winner);
        emit GameEnded(_gameId, game.state, winner);
    }
    
    // ==================== Payouts ====================
    /**
     * @dev Pay the winner the full pot
     */
    function _distributeWinnings(uint256 _gameId, address _winner) internal {
        Game storage game = games[_gameId];
        uint256 totalPot = game.betAmount * 2;
        
        if (totalPot > 0) {
            payable(_winner).transfer(totalPot);
        }
    }
    
    /**
     * @dev Split wagers back to players on draw
     */
    function _distributeDraw(uint256 _gameId) internal {
        Game storage game = games[_gameId];
        
        if (game.betAmount > 0) {
            payable(game.blackPlayer).transfer(game.betAmount);
            payable(game.whitePlayer).transfer(game.betAmount);
        }
    }
    
    // ==================== Views ====================
    /**
     * @dev Get the full board state
     */
    function getBoard(uint256 _gameId) external view gameExists(_gameId) returns (uint8[15][15] memory) {
        return games[_gameId].board;
    }
    
    /**
     * @dev Get summary info for a game
     */
    function getGameInfo(uint256 _gameId) external view gameExists(_gameId) returns (
        address blackPlayer,
        address whitePlayer,
        uint8 currentPlayer,
        GameState state,
        uint256 betAmount,
        uint8 moveCount
    ) {
        Game storage game = games[_gameId];
        return (
            game.blackPlayer,
            game.whitePlayer,
            game.currentPlayer,
            game.state,
            game.betAmount,
            game.moveCount
        );
    }
    
    /**
     * @dev Get the piece at a specific coordinate
     */
    function getPiece(uint256 _gameId, uint8 _x, uint8 _y) 
        external 
        view 
        gameExists(_gameId) 
        returns (uint8) 
    {
        require(_x < BOARD_SIZE && _y < BOARD_SIZE, "Invalid coordinates");
        return games[_gameId].board[_x][_y];
    }
}
