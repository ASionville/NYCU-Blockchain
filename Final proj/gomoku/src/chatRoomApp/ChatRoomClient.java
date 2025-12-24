package chatRoomApp;

import java.util.List;
import java.util.Scanner;

import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import ethSC.ChatRoomHandler;
import io.reactivex.disposables.Disposable;
import ethSC.ChatRoom;

public class ChatRoomClient {
	private static Scanner sc = new Scanner(System.in);
	
	public static void callClient() {
		System.out.println("Please choose the Chatroom you would like to join.");
		ChatRoomFileIO chrmio = new ChatRoomFileIO();
		List<String> chatroomList = chrmio.getAllChatRooms();
		for (int i = 0; i < chatroomList.size(); i++) {
			System.out.println(Integer.toString(i) + " : " + chatroomList.get(i));
		}
		System.out.println(Integer.toString(chatroomList.size()) + " : " + "It is not in my list");
		System.out.println("Your Choice (0...)");
		System.out.print("> ");
		int roomChoice = Integer.parseInt(sc.nextLine());
		if (roomChoice == chatroomList.size()) {
			System.out.println("Please Enter Chatroom Address");
			System.out.print("> ");
			chrmio.addChatRoom(sc.nextLine());
			System.out.println("Chatroom Added. Please re-enter");
			return;
		} else if (roomChoice >= 0 && roomChoice < chatroomList.size()) {
			chatRoomMenu(chatroomList.get(roomChoice));
		} else {
			System.err.println("Bad Choice");
		}
		return;
	}
	
	public static void chatRoomMenu(String chatRoomAddress) {
		ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatRoomAddress, false);
		System.out.println("Welcome to " + chatRoomHandler.getChatRoomName());
		if (!chatRoomHandler.isUserRegistered()) {
			System.out.println("You are not registered to this room yet!");
		}
		System.out.println("What would you like to do?");
		System.out.println("1. User Registration");
		System.out.println("2. Change User Title");
		System.out.println("3. Join Chatroom");
		System.out.println("4. Check Historial Messages");
		System.out.println("Your choice (1/2) ?");
		int choice = 0;
		boolean running = true;
		while (running) {
			System.out.print("> ");
			choice = Integer.parseInt(sc.nextLine());
			if (choice == 1) {
				userRegistration(chatRoomAddress);
			} else if (choice == 2) {
				userTitleChange(chatRoomAddress);
			} else if (choice == 3) {
				chatRoom(chatRoomAddress);
			} else if (choice == 4) {
				showHistorical(chatRoomAddress);
			} else {
				running = false;
				System.out.println("Quit Chatroom Managing Operation");
			}
		}
	}
	
	public static void userRegistration(String chatRoomAddress) {
		ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatRoomAddress, false);
		if (!chatRoomHandler.isUserRegistered()) {
			System.out.println("Your nickname in this chatroom?");
			System.out.print("> ");
			chatRoomHandler.addNewUser(sc.nextLine());
			System.out.println("User Registration Complete.");
			return;
		} else {
			System.out.println("You are already registered.");
			return;
		}
	}
	
	public static void userTitleChange(String chatRoomAddress) {
		ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatRoomAddress, false);
		if (chatRoomHandler.isUserRegistered()) {
			System.out.println("What would you like to change to?");
			System.out.print("> ");
			chatRoomHandler.changeUserTitle(sc.nextLine());
			System.out.println("User title change complete");
			return;
		} else {
			System.out.println("You are not registered to this chatroom yet!");
			return;
		}
	}
	
	public static void showHistorical(String chatRoomAddress) {
		ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatRoomAddress, false);
		if (chatRoomHandler.isUserRegistered()) {
			/* What about the event in the past? */
			List<ChatRoom.NewMessageEventResponse> allchats = new ArrayList<>();
			Disposable subscription = chatRoomHandler.getChatSmartContract().newMessageEventFlowable(
					DefaultBlockParameterName.EARLIEST, 
					DefaultBlockParameterName.LATEST
				).subscribe(event -> {
					allChats.addLast(event);
				});
		} else {
			System.out.println("You are not registered to this chatroom yet!");
			return;
		}
	}
	
	public static void chatRoom(String chatRoomAddress) {
		ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatRoomAddress, false);
		if (chatRoomHandler.isUserRegistered()) {
			/* How to read Event Real-Time */
			Deque<ChatRoom.NewMessageEventResponse> buffer = new ArrayDeque<>();
			Disposable subscribetion = chatRoomHandler.getChatSmartContract().newMessageEventFlowable(
					DefaultBlockParameterName.LATEST, 
					DefaultBlockParameterName.LATEST
				).subscribe(event -> {
					if (buffer.size() == 10) {
						buffer.removeFirst();
					}
					buffer.addLast(event);
					System.out.println("===chatroom===");
					for (ChatRoom.NewMessageEventResponse chatEvent : buffer) {
						System.out.println("[" + chatEvent.userTitle + "] " + chatEvent.messageContent);
					}
					System.out.print("> ");
				});
			while(true){
				System.out.print("> ");
				String message = sc.nextLine();
				if (line.equalsIgnoreCase("quit")){
					break;
				}
				chatRoomHandler.sendMessage(message);
			}
		} else {
			System.err.println("You haven't registered to this chatroom yet!");
			return;
		}
		
	}
	
	private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }
}
