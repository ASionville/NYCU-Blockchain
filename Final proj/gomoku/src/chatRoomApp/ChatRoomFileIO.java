package chatRoomApp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class ChatRoomFileIO {
	private static final String chatRoomFileName = "chatrooms.dat";
	private static File chatRoomFile;
	private static ArrayList<String> chatRoomAddresses = new ArrayList<>();
	
	ChatRoomFileIO() {
		loadChatRooms();
	}
	
	public void addChatRoom(String newChatRoomAddress) {
		if (chatRoomAddresses.contains(newChatRoomAddress)) {
			System.out.println("It's already in the list");
			return;
		}
		chatRoomAddresses.addLast(newChatRoomAddress);
		updateChatRoomFile();
	}
	
	public ArrayList<String> getAllChatRooms() {
		return chatRoomAddresses;
	}
	
	public void updateChatRoomFile() {
		chatRoomFile = new File(chatRoomFileName);
		if (chatRoomFile.exists()) {
			chatRoomFile.delete();
		}
		try {
			chatRoomFile.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(chatRoomFile));
			for (String chatRoomAddress : chatRoomAddresses) {
				bw.write(chatRoomAddress + "\n");
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			System.out.println("Cannot update chatroom record file.");
			e.printStackTrace();
		}
	}
	
	private void loadChatRooms() {
		chatRoomFile = new File(chatRoomFileName);
		if (chatRoomFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(chatRoomFile));
				while (br.ready()) {
					chatRoomAddresses.addLast(br.readLine());
				}
				br.close();
			} catch (Exception e) {
				System.err.println("Cannot read chatroom record file.");
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			try {
				chatRoomFile.createNewFile();
			} catch (Exception e) {
				System.err.println("Cannot create chatroom record file.");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
