package chatRoomApp;

import java.util.List;
import java.util.Scanner;

import ethSC.ChatRoomHandler;

public class ChatRoomManager {
	private static Scanner sc = new Scanner(System.in);
	
	public static void callManager() {
		System.out.println("Welcome to ChatRoom Manager !");
		System.out.println("Please choose your operation");
		System.out.println("1. Create a new chatroom");
		System.out.println("2. Managing an existing chatroom");
		System.out.println("3. Quit");
		System.out.println("Your choice (1/2) ?");
		
		boolean running = true;
		int choice = 0;
		while (running) {
			System.out.print("> ");
			choice = Integer.parseInt(sc.nextLine());
			if (choice == 1) {
				createChatroom();
			} else if (choice == 2) {
				managingChatroom();
			} else {
				running = false;
				System.out.println("Quit ChatRoomManager");
			}
		}
	}
	
	private static void createChatroom() {
		String userTitle = "";
		String chatRoomName = "";
		System.out.println("Good. What will be your nick name in this chatroom?");
		System.out.print("> ");
		userTitle = sc.nextLine();
		System.out.println("What will be the name of your chatroom?");
		System.out.print("> ");
		chatRoomName = sc.nextLine();
		System.out.println("Deploying Chatroom...");
		ChatRoomHandler newChatRoomHandler = new ChatRoomHandler(chatRoomName, true);
		System.out.println("Chatroom Deployed");
		System.out.println("Register your user title...");
		newChatRoomHandler.addNewUser(userTitle);
		System.out.println("User title registered");
		System.out.println("Saving Chatroom Addresses");
		ChatRoomFileIO chrmio = new ChatRoomFileIO();
		chrmio.addChatRoom(newChatRoomHandler.getChatRoomAddress());
		System.out.println("Operation Complete.");
		return;
	}
	
	public static void managingChatroom() {
		System.out.println("Which Chatroom You would like to manage?");
		ChatRoomFileIO chrmio = new ChatRoomFileIO();
		List<String> chatroomList = chrmio.getAllChatRooms();
		for (int i = 0 ; i < chatroomList.size(); i++) {
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
			System.out.println("Chatroom Added. Please re-enter.");
			return;
		} else if (roomChoice >= 0 && roomChoice < chatroomList.size()) {
			ChatRoomHandler chatRoomHandler = new ChatRoomHandler(chatroomList.get(roomChoice), false);
			System.out.println("You are managing " + chatRoomHandler.getChatRoomName());
			System.out.println("What would you like to do?");
			System.out.println("1. Change Chatroom Name");
			System.out.println("2. Quit");
			System.out.println("Your choice (1/2) ?");
			boolean running = true;
			int opChoice = 0;
			while (running) {
				System.out.print("> ");
				opChoice = Integer.parseInt(sc.nextLine());
				if (opChoice == 1) {
					System.out.println("Please enter the new Chatroom Name");
					System.out.print("> ");
					chatRoomHandler.changeChatRoomName(sc.nextLine());
					System.out.println("Name Change Complete");
					return;
				} else {
					running = false;
					System.out.println("Quit Chatroom Managing Operation");
				}
			}
			return;
		} else {
			System.err.println("Bad Choice");
			return;
		}
		
	}

}
