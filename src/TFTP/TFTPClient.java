package TFTP;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

/**
 * This class acts as a TFTP client and can download any file from TFTP server.
 * @author Binit
 *
 */
public class TFTPClient{

	public final int SIZE = 516;
	public DatagramPacket receivePacket = null;
	public DatagramPacket sendPacket = null;
	public byte[] receiveData = null;
	public byte[] sendData = null;
	public DatagramSocket socket = null;

	public static boolean connected = false;
	public static String SERVER_HOST = "";
	public static int SERVER_PORT;
	public static InetAddress SERVER_ADDRESS = null;
	public static String CURRENT_DIR = System.getProperty("user.dir");
	public static long time;

	/**
	 * Initialize the socket
	 */
	public TFTPClient(){
		try {
			socket = new DatagramSocket(2000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connect to the TFTP server
	 * @param host	server address
	 * @param port	server port
	 */
	public void connect(String host, int port){
		try{
			SERVER_HOST = host;
			SERVER_PORT = port;
			SERVER_ADDRESS = InetAddress.getByName(SERVER_HOST);
			connected = true;
			System.out.println("Connected!");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	} // connect

	/**
	 * Sends a request packet to the server to read a file
	 * @param file	file name
	 */
	public void readRequest(String file){
		try{
			String mode = "binary";
			// size of packet to be sent
			int bufferLength = (Short.SIZE/8)+file.length()+(Byte.SIZE/8)+mode.length()+(Byte.SIZE/8);

			// forming a read request packet with op code 01
			ByteBuffer b = ByteBuffer.allocate(bufferLength);
			b.putShort((short)01);
			b.put(file.getBytes());
			b.put((byte)0);
			b.put(mode.getBytes());
			b.put((byte)0);

			byte[] request = b.array();
			sendPacket = new DatagramPacket(request, request.length, SERVER_ADDRESS, SERVER_PORT);
			socket.send(sendPacket);
			System.out.println("sent RRQ <file=" + file + ", mode=binary>");

			this.getFile(file);
		} catch(IOException ex){
			ex.printStackTrace();
		}
	} // readRequest

	/**
	 * Sends acknowledgment for every packet received from the server
	 * @param receivePacket		packet received from the server
	 */
	public void sendAck(DatagramPacket receivePacket){
		try {
			// forming an ack packet with op code 04
			ByteBuffer b = ByteBuffer.allocate(4);
			b.putShort((short)04);
			b.put(receivePacket.getData()[2]);
			b.put(receivePacket.getData()[3]);

			byte[] request = b.array();
			sendPacket = new DatagramPacket(request, request.length, receivePacket.getAddress(), receivePacket.getPort());
			socket.send(sendPacket);
			System.out.println("sent ACK <block=" + receivePacket.getData()[3] + ">");
		} catch (IOException e) {
			e.printStackTrace();
		}
	} // sendAck

	/**
	 * Receives packets from the TFTP server writes to a file after sending back acknowledgment for that packet
	 * @param fileName	name of the file
	 */
	public void getFile(String fileName){
		FileOutputStream fstream = null;
		BufferedOutputStream bstream = null;

		try {
			fstream = new FileOutputStream(CURRENT_DIR+"/"+fileName);
			bstream = new BufferedOutputStream(fstream);
			long data = 0;

			// set timeout to 5 secs which will throw a timeout exception if nothing is received within 5 secs
			socket.setSoTimeout(5000);
			// receives data until a packet of size lesser than 512 bytes is received
			while(true){
				receiveData = new byte[this.SIZE];
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);

				// correct data if packet received with op code 03
				if(receivePacket.getData()[1] == 3){
					System.out.println("received DATA <block=" + receivePacket.getData()[3] + 
							", " + (receivePacket.getLength()-4) + " bytes>");

					bstream.write(receivePacket.getData(), 4, receivePacket.getLength()-4);
					bstream.flush();
					data += receivePacket.getLength()-4;

					// send ack for each packet
					this.sendAck(receivePacket);
					// stop if a packet is received of size less than 512 bytes
					if(receivePacket.getLength() < this.SIZE){
						System.out.println("Received " + data + " bytes");
						break;
					}
					// erroneous data if packet received with op code 05
				} else if(receivePacket.getData()[1] == 5){
					byte errorCode = receivePacket.getData()[3];
					String errorMsg = new String(receivePacket.getData(), 4, receivePacket.getLength()-4);
					System.out.println("Error code " + errorCode + ": " + errorMsg);
					break;
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException ex) {
			System.out.println("Timed out! Server may be down.");
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if(bstream != null)
					bstream.close();
				if(fstream != null)
					fstream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	} // getFile

	/**
	 * Prints help commands of TFTP
	 */
	public void help(){
		System.out.println("Commands may be abbreviated.  Commands are:");
		System.out.println();
		System.out.println("connect         connect to remote tftp");
		System.out.println("get             receive file");
		System.out.println("quit            exit tftp");
		System.out.println("?               print help information");
	} // help

	/**
	 * @param args	command line arguments. (Ignored)
	 */
	public static void main(String[] args) {

		Scanner sc = null;
		TFTPClient tftp = new TFTPClient();
		String file = new String();
		String command = new String();

		try{
			sc = new Scanner(System.in);
			//continue until a quit command is executed
			while(true){
				System.out.print("tftp> ");
				command = sc.nextLine();
				command = command.replaceAll("\\s+", " ");

				// connect to TFTP server
				if(command.contains("connect")){

					String[] conn = command.split("\\s");
					if(conn.length < 2)
						System.out.println("server not specified");
					else if(conn.length < 3)
						System.out.println("port number not specified");
					else if(!"connect".equalsIgnoreCase(conn[0].trim()))
						System.out.println("command not found");
					else if(!"glados.cs.rit.edu".equalsIgnoreCase(conn[1].trim()))
						System.out.println("invalid server");
					else if(!"69".equalsIgnoreCase(conn[2].trim()))
						System.out.println("invalid port number");
					else
						tftp.connect(conn[1], Integer.parseInt(conn[2]));

					// get file from TFTP server
				} else if(command.contains("get")){
					if(!connected){
						System.out.println("Not connected");
						continue;
					}

					file = command.substring(3).trim();
					if("".equals(file) || file == null){
						System.out.println("no file specified");
						continue;
					}

					tftp.readRequest(file);
					// quit TFTP
				} else if("quit".equalsIgnoreCase(command.trim())){
					connected = false;
					break;
					// help
				} else if(command.contains("?")){
					tftp.help();
				} else{
					System.out.println("?Invalid command");
				}
			}
		} catch(Exception ex){
			ex.printStackTrace();
		} finally{
			if(sc != null)
				sc.close();
		}
	} // main
} // TFTPClient class
