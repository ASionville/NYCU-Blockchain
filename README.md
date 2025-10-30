# NYCU-Blockchain
A simple blockchain implementation for the course "Blockchain and Smart Contract", based on the implementation of Wei-Yang Chiu.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running a Node](#running-a-node)
- [Usage](#usage)
  - [Console Commands](#console-commands)
  - [Creating Transactions](#creating-transactions)
  - [Mining](#mining)
- [Network Architecture](#network-architecture)
  - [Node Discovery](#node-discovery)
  - [P2P Communication](#p2p-communication)
  - [Consensus Mechanism](#consensus-mechanism)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Overview

This project implements a decentralized blockchain network where multiple nodes can:
- Mine new blocks with proof-of-work
- Create and verify transactions
- Manage cryptographic wallets
- Automatically discover and connect to peers
- Synchronize blockchain state across the network
- Minimal user interface via console-like commands

*Developed for the "Blockchain and Smart Contract" course at NYCU (National Yang Ming Chiao Tung University).*

## Features

- **Peer-to-Peer Network**: Automatic node discovery via UDP broadcast and network scanning
- **Proof-of-Work Mining**: Adjustable difficulty with nonce generation
- **Wallet Management**: RSA-based cryptographic wallets with key pair generation
- **Transaction System**: Create, sign, and verify transactions between wallets
- **Merkle Trees**: Efficient transaction verification using Merkle tree structures
- **Network Synchronization**: Automatic blockchain cloning from existing nodes
- **Graceful Shutdown**: Proper peer notification on node disconnect
- **Interactive Console**: Command-line interface for blockchain operations
- **Balance Tracking**: Query balances for any wallet across the network
- **Multi-Wallet Support**: List all wallets known to the network

## Architecture

### Core Components

```
p2pblockchain/
├── config/          # Configuration classes
├── main/            # Application entry point
├── types/           # Core blockchain types
│   ├── Block        # Individual block structure
│   ├── Blockchain   # Main blockchain logic
│   ├── Transaction  # Transaction handling
│   ├── Wallet       # Cryptographic wallet
│   ├── P2PNode      # Peer node representation
│   └── MerkleTree   # Merkle tree implementation
└── utils/           # Utility classes
    ├── HashUtils    # Cryptographic hashing
    ├── SecurityUtils # Signature verification
    └── Logger       # Logging system
```

### Network Protocol

The nodes communicate using TCP sockets with the following message types:
- `GET_BLOCKCHAIN`: Request full blockchain from peer
- `NEW_BLOCK`: Broadcast newly mined block
- `NEW_TRANSACTION`: Propagate transaction to network
- `JOIN_NETWORK`: Announce new node joining
- `LEAVE_NETWORK`: Announce node departure
- `GET_BALANCE`: Query wallet balance
- `GET_LOCAL_WALLETS`: Request list of wallets from peer

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Network connectivity (for P2P features)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/ASionville/NYCU-Blockchain.git
cd P2PBlockchain
```

2. Compile the project:
```bash
javac -d bin src/p2pblockchain/**/*.java
```

### Running a Node

1. Start the first node:
```bash
java -cp bin p2pblockchain.main.startBlockchain
```

2. Enter wallet name and port when prompted:
```
Enter wallet name (default: Aubin): Alice
Enter network port (default: 8300): 8300
```

3. Start additional nodes on different ports:
```bash
java -cp bin p2pblockchain.main.startBlockchain
```
```
Enter wallet name (default: Aubin): Bob
Enter network port (default: 8300): 8301
```

The new node will automatically discover and connect to existing nodes.

#### LAN Setup

When using multiple machines on a LAN, ensure:
- All machines are on the same subnet
- Firewall allows traffic on the chosen ports (default 8000-9000)

Start only one node per machine.

#### Localhost Setup

You can also run multiple nodes on the same machine using `localhost` and different ports.


## Usage

### Console Commands

Once a node is running, use these commands in the console:

- `help` - Display all available commands
- `balance <wallet_name>` - Check balance of any wallet on the network
- `mybalance` - Check your own wallet balance
- `send <recipient_wallet> <amount>` - Send coins to another wallet
- `listpeers` - Show all connected peer nodes
- `listwallets` - List all wallets known to the network with their balances
- `start` - Resume mining (if stopped)
- `stop` - Pause mining
- `quit` - Gracefully shutdown the node

### Creating Transactions

```
> send {Bob address} 10
Transaction created and broadcast to network
```

The transaction will be:
1. Signed with your private key
2. Broadcast to all connected peers
3. Included in the next mined block
4. Verified by the network

### Mining

Mining runs automatically in the background. The system:
- Collects pending transactions
- Creates a new block
- Finds a valid nonce (proof-of-work)
- Broadcasts the block to peers
- Adjusts difficulty based on mining time

## Network Architecture

### Node Discovery

Two discovery methods are supported:

1. **UDP Broadcast** (default):
   - Sends broadcast messages on port 8299
   - Peers respond with their connection info
   - Fast discovery on local networks

2. **Network Scanning** (fallback):
   - Scans IP ranges for active nodes
   - Tries multiple ports per host
   - Slower but more reliable

3. **Localhost Mode** (fallback):
   - Scans `localhost` for nodes on specified port range
   - Useful for testing multiple nodes on one machine

### P2P Communication

- Each node runs a TCP server
- Peers connect bidirectionally
- Messages use JSON format
- Automatic reconnection on failures

### Consensus Mechanism

- **Longest Chain Rule**: Nodes accept the longest valid chain
- **Proof-of-Work**: SHA3-256 based mining with adjustable difficulty
- **Transaction Validation**: ECDSA signatures verified before inclusion

## Project Structure

```
src/p2pblockchain/
├── config/
│   ├── BlockchainConfig.java     # Blockchain parameters
│   ├── NetworkConfig.java        # Network settings
│   └── SecurityConfig.java       # Security configurations
├── main/
│   └── startBlockchain.java      # Main application entry
├── types/
│   ├── Block.java                # Block data structure
│   ├── Blockchain.java           # Blockchain management
│   ├── MerkleTree.java           # Merkle tree for transactions
│   ├── MessageType.java          # Network message types
│   ├── P2PNode.java              # Peer node representation
│   ├── Transaction.java          # Transaction handling
│   └── Wallet.java               # Wallet management
└── utils/
    ├── Base64Utils.java          # Base64 encoding/decoding
    ├── Converter.java            # Data type conversions
    ├── FilesUtils.java           # File I/O operations
    ├── HashUtils.java            # Cryptographic hashing
    ├── JsonArray.java            # JSON array utilities
    ├── JsonObject.java           # JSON object utilities
    ├── Logger.java               # Logging system
    ├── NonceGenerator.java       # Nonce generation for mining
    ├── SecurityUtils.java        # Cryptographic operations
    └── TimeUtils.java            # Timestamp utilities
```

## Configuration

Key configuration files:

### BlockchainConfig.java
- Mining difficulty
- Block reward
- Genesis block parameters

### NetworkConfig.java
- Default port: `8300`
- Broadcast port: `8299`
- Discovery timeout: `3000ms`
- Port scan range: `8000-9000`

### SecurityConfig.java
- ECDSA key size: `256 bits`
- Hash algorithm: `SHA3-256`
- Signature algorithm: `SHA256withECDSA`

### Verbosity Level
To be adjusted in the `BlockchainConfig.java` file:
- `0`: No logging at all
- `1`: Errors only
- `2`: Warnings and errors
- `3`: Info, warnings, and errors
- `4`: Debug, info, warnings, and errors (very verbose)

## Security

- **ECDSA Encryption**: 256-bit key pairs for wallet security
- **Digital Signatures**: All transactions signed with private keys
- **SHA3-256 Hashing**: Secure block and transaction hashing
- **Merkle Tree**: Efficient and tamper-proof transaction verification
- **Chain Validation**: Full blockchain validation on sync

## License

This project is developed for educational purposes as part of the NYCU Blockchain course.

## Acknowledgments

- NYCU (National Yang Ming Chiao Tung University)
- Course: "Blockchain and Smart Contract"
- Instructor: Wei-Yang Chiu
- Academic Year: 2025

---

**Note**: This is an educational project and should not be used in production environments.