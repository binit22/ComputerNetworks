import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class Message extends Thread{

	public String type;
	public ObjectOutputStream to = null;
	public ObjectInputStream from = null;
	PrintWriter pwFile = null;

	public Message(String type){
		this.type = type;
	}

	public void send(){
		Socket sender = null;
		Scanner sc = null;


		try{
			sender = new Socket("localhost", 5000);
			to = new ObjectOutputStream(sender.getOutputStream());
			PrintWriter out =
			        new PrintWriter(sender.getOutputStream(), true);
			sc = new Scanner("System.in");
			String sendMsg = "";
			while(true){
				sendMsg = sc.next();
				pwFile.println(sendMsg);
				pwFile.flush();
//				to.writeObject(sendMsg);
			}
			
		} catch(IOException ex){
			ex.printStackTrace();
		} catch(Exception ex){
			ex.printStackTrace();
		} finally{
			try {
				sender.close();
				sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String receive(){
		String message = "";
		Socket receiver = null;
		ServerSocket server = null;
		try{
			server = new ServerSocket(5000);
			
			receiver = server.accept();
			from = new ObjectInputStream(receiver.getInputStream());

			while(true){
//				message = from.readObject();
			}
		} catch(Exception ex){
			ex.printStackTrace();
		} finally{
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return message;
	}

	public void run(){
		if(this.type.equals("send")){
			this.send();

		}else if(this.type.equals("receive")){
			this.receive();
		}
	}

	public static void main(String[] args) {

		Thread send = new Message("send");
		Thread receive = new Message("receive");
		send.start();
		receive.start();

	}
}
