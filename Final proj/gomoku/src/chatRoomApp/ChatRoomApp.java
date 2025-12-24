package chatRoomApp;

import java.io.File;
import java.util.Scanner;

import ethInfo.EthBasis;

public class ChatRoomApp {
	
	private static Scanner sc = new Scanner(System.in);
	
	public static void main(String[] args) throws Exception {
		login();
		menu();
	}
	
	public static void login() throws Exception {
		File keystoreDir = new File(EthBasis.credential);
		File[] keystores = keystoreDir.listFiles();
		if (keystores != null) {
			System.out.println("Please choose the Ethereum account you are going to use.");
			for (int i = 0; i < keystores.length; i++) {
				System.out.println(Integer.toString(i) + " : " + keystores[i].getName());
			}
			System.out.println("Your choice ? (0...)");
			System.out.print("> ");
			int choice = Integer.parseInt(sc.nextLine());
			if (choice >= 0 && choice < keystores.length) {
				EthBasis.credential = keystores[choice].getAbsolutePath();
			} else {
				System.out.println("Abort.");
				System.exit(1);
			}
			
			System.out.println("Please Enter Account Password");
			System.out.print("# ");
			EthBasis.password = sc.nextLine();
		} else {
			System.err.println("There are no Ethereum account on this machine.");
			System.exit(1);
		}
		
	}
	
	public static void menu() throws Exception {
		System.out.println("Welcome to Chatroom");
		System.out.println("What would you like to do today ?");
		System.out.println("1. Managing Chatrooms");
		System.out.println("2. Join Existing Chatroom");
		System.out.println("3. Quit");
		System.out.println("Your Choice ? (1/2)");
		
		boolean running = true;
		int choice = 0;
		while (running) {
			System.out.print("> ");
			choice = Integer.parseInt(sc.nextLine());
			if (choice == 1) {
				ChatRoomManager.callManager();
			} else if (choice == 2) {
				ChatRoomClient.callClient();
			} else {
				running = false;
				System.out.println("Quit Program");
			}
		}
	}

}
