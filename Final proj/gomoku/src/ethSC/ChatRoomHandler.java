package ethSC;

import java.math.BigInteger;
import java.util.List;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.protocol.http.HttpService;

import ethInfo.EthBasis;
import ethSC.ChatRoom.NewMessageEventResponse;

public class ChatRoomHandler {
	private static String address = "";
	private static Web3j web3j;
	private static Credentials cr;
	private static ContractGasProvider cgp;
	private static ChatRoom chatroom;
	private static TransactionManager trm;
	
	public ChatRoomHandler(String data, boolean mode) {
		// Mode = True meaning to create a new chatroom, Mode = False meaning to load an existing chatroom.
		// If Mode = True, data will be treated as new chatroom name.
		// If Mode = False, data will be treated as address to load.
		
		web3j = Web3j.build(new HttpService(EthBasis.pipeLine));
		try {
			cr = WalletUtils.loadCredentials(EthBasis.password, EthBasis.credential);
			Admin admin = Admin.build(new HttpService(EthBasis.pipeLine));
			admin.personalUnlockAccount(cr.getAddress(), EthBasis.password, BigInteger.ZERO);
		} catch (Exception e) {
			System.err.println("Bad Wallet, Check Password or Credential File");
			e.printStackTrace();
		}
		
		cgp = new DefaultGasProvider();
		trm = new RawTransactionManager(web3j, cr, EthBasis.chainID);
		
		try {
			if (mode) {
				System.out.println("ChatRoom Create Mode");
				createChatRoom(data);
			} else {
				System.out.println("ChatRoom Loading Mode");
				loadChatRoom(data);
			}
		} catch (Exception e) {
			System.err.println("Cannot load or deploy smart contract.");
			e.printStackTrace();
		}
	}
	
	private void createChatRoom(String chatRoomName) throws Exception {
		System.out.println("Creating New Chatroom with Name " + chatRoomName);
		chatroom = ChatRoom.deploy(web3j, trm, cgp, chatRoomName).send();
		address = chatroom.getContractAddress();
		System.out.println("Chatroom created! Address : " + address);
	}
	
	private void loadChatRoom(String chatRoomAddress) throws Exception {
		chatroom = ChatRoom.load(chatRoomAddress, web3j, trm, cgp);
		address = chatRoomAddress;
	}
	
	public String getChatRoomAddress() {
		return address;
	}
	
	public String getChatRoomName() {
		try {
			return chatroom.getChatRoomName().send();
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function getChatRoomName.");
			e.printStackTrace();
			return "";
		}
		
	}
	
	public boolean changeChatRoomName(String newChatRoomName) {
		try {
			chatroom.changeChatRoomName(newChatRoomName).send();
			return true;
		} catch (Exception e) {
			System.err.println("You might not be the room owner.");
			System.err.println("Cannot invoke smart contract function changeChatRoomName.");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean isUserRegistered() {
		try {
			return chatroom.isUserRegistered().send();
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function isUserRegistered.");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean addNewUser(String newUserTitle) {
		try {
			chatroom.addNewUser(newUserTitle).send();
			return true;
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function addNewUser.");
			e.printStackTrace();
			return false;
		}
	}
	
	public String getUserTitle() {
		try {
			return chatroom.getUserTitle().send();
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function getUserTitle.");
			e.printStackTrace();
			return "";
		}
	}
	
	public boolean changeUserTitle(String newUserTitle) {
		try {
			chatroom.changeUserTitle(newUserTitle).send();
			return true;
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function changeUserTitle.");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean sendMessage(String message) {
		try {
			chatroom.sendMessage(message).send();
			return true;
		} catch (Exception e) {
			System.err.println("Cannot invoke smart contract function sendMessage.");
			e.printStackTrace();
			return false;
		}
	}
	
	public ChatRoom getChatRoomSmartContract() {
		return chatroom;
	}
	
}
