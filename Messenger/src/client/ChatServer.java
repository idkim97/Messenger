package client;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * A multithreaded chat room server. When a client connects the server requests a screen
 * name by sending the client the text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received. After a client submits a unique name, the server acknowledges
 * with "NAMEACCEPTED". Then all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name. The broadcast messages are prefixed
 * with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g., better
 * logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer {

	// All client names, so we can check for duplicates upon registration.
	private static Set<String> names = new HashSet<>();

	// The set of all the print writers for all the clients, used for broadcast.
	private static Set<PrintWriter> writers = new HashSet<>();
	
	// whisper ����� �����ϰ� �����ϱ� ���� hash table.
    private static Hashtable<String, PrintWriter> n =new Hashtable<String, PrintWriter>(); 
    static int userNum=0;// A variable which can save the number of current user(client). 
	public static void main(String[] args) throws Exception {
		System.out.println("The chat server is running...");
		ExecutorService pool = Executors.newFixedThreadPool(500); //maximum of client is 500
		try (ServerSocket listener = new ServerSocket(8888)) {
			while (true) {
				pool.execute(new Handler(listener.accept()));
				userNum++; // when client connect to server, userNum +1 ! 
			}
		}
	}

	/**
	 * The client handler task.
	 */
	private static class Handler implements Runnable {
		private String name;
		private Socket socket;
		private Scanner in;
		private PrintWriter out;

		/**
		 * Constructs a handler thread, squirreling away the socket. All the interesting
		 * work is done in the run method. Remember the constructor is called from the
		 * server's main method, so this has to be as short as possible.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * Services this thread's client by repeatedly requesting a screen name until a
		 * unique one has been submitted, then acknowledges the name and registers the
		 * output stream for the client in a global set, then repeatedly gets inputs and
		 * broadcasts them.
		 */
		public void run() {
			try {
				in = new Scanner(socket.getInputStream());
				out = new PrintWriter(socket.getOutputStream(), true);
				String ipaddr = socket.getInetAddress().toString(); //������ ������ Ŭ���̾�Ʈ�� �ּ� Ȯ�ο�

				// Keep requesting a name until we get a unique one.
				  while (true) {
	                    out.println("SUBMITNAME");
	                    name = in.nextLine();
	                    n.put(name,out); //�ؽ����̺� ���� �־���.
	                    writers.add(out);
	                    for (PrintWriter writer : writers) {
	                        writer.println("MESSAGE " + name + " has joined");
	                    } // Ŭ���̾�Ʈ�� ���ö����� ��ε�ĳ��Ʈ
				  
				// Now that a successful name has been chosen, add the socket's print writer
				// to the set of all writers so this client can receive broadcast messages.
				// But BEFORE THAT, let everyone else know that the new person has joined!
				out.println("NAMEACCEPTED " + name);
				System.out.println(name);
				
				for (PrintWriter writer : writers) { //���ο� ������ ���ö�, Broadcast, ���ŵ� ���� ��, ���� ���� �����ش�.
					writer.println("MESSAGE " +clock()+ name + " has joined");
					writer.println("SERVERINFO " + userNum+"��");
					writer.println("USERINFO "+ names);
				} 
				
				writers.add(out); //ó�� ������ ���ö� �ش� ������ writer Set�� �߰��ϰ�, userNum, userInfo���� �ش�.
				out.println("SERVERINFO " + userNum+"��");
				out.println("USERINFO "+ names);
				
				String tempname="";
            	PrintWriter whisper=null; //whisper ��� �߰��� ���� PW
            	
            	int index;
				// Accept messages from this client and broadcast them.
				while (true) {
					String input = in.nextLine();
				/*	if(input.startsWith("EXIT")) {
                        writers.add(out);
        					userNum--; //Decreasing number to 1.
        					names.remove(name); //names hashSet ���� �ش� name ����
        					n.remove(name,out); //n hashTable���� �ش� name ����
        					for (PrintWriter writer : writers) { //�ش� client�� �����ٴ� ��۰� �Բ� ���ŵ� ������ client�鿡�� ����
        						writer.println("MESSAGE " + clock()+ name + " has left");
        						writer.println("SERVERINFO " + userNum+"��");
        						writer.println("USERINFO "+ names);
        					}
                            socket.close();                        
                    }*/
					if(input.startsWith("/")){
							if(input.startsWith("/quit")) { //������ ���
								return;
							}
							if(input.startsWith("/w ") ||input.startsWith("/W ")) { //�ӼӸ� ���
								index = input.indexOf('w');
								if(index == 1) {
								int start = input.indexOf(" ") + 1;
								int end = input.indexOf(" ", start);
								if(end == -1)// ex) /wasdasdad �̷������� ������
								{
									out.println("MESSAGE [����] �ӼӸ� ����� \"/w [������̸�] [�� ��]\" �Դϴ�.");
								}
								else {
								tempname=input.substring(start,end);
								String msg = input.substring(end+1);
								if(!tempname.equals(name)) {// �ӼӸ��� ������ ��밡 �޶�� ���
									if(n.containsKey(tempname)) { // �޴»�� �ؽ�Ʈ����� ǥ�� 
										whisper=(PrintWriter)n.get(tempname);
										whisper.println("MESSAGE "+ clock() + "[�ӼӸ�] " +"["+  name+"]"+"�����κ��� :" + msg);
										if(n.containsKey(name)) { //�����»�� �ؽ�Ʈ����� ǥ��
											whisper=(PrintWriter)n.get(name);
											whisper.println("MESSAGE " + clock()+ "[�ӼӸ�] " +  "["+tempname+"]"+"�Կ��� :" + msg );
										}
									}
									else { // �ش����ڰ� ������
										out.println("MESSAGE "+ clock()+"[�˸�]["+tempname +"] �ش� ����ڴ� �����ϴ�.");
									}
								}else { // �ڱ��ڽſ��� �ӼӸ��� ��������
									out.println("MESSAGE "+"[����] �ڱ��ڽſ��� �ӼӸ��� ���� �� �����ϴ�.");
								}	
								}
							}
							}
							else {//   /�� �����ϴµ� �ι�° character�� 'w'�� �ƴ� ���.
									out.println("MESSAGE [����] �ӼӸ� ����� \"/w [������̸�] [�� ��]\" �Դϴ�.");
							}
						}
                    else //   '/'�� �������� �ʴ� ��� �޼����� �����Ѵ�.
                    {
                    	if(!input.startsWith("EXIT"))
                    	for (PrintWriter writer : writers) {
                    		writer.println("MESSAGE "+ clock() +"["+ name+"]" + ": " + input);
					}
                    }
				}
				  }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  finally {
				if (out != null) {
					writers.remove(out); // out
				}
				if (name != null) { //when client out server.
					System.out.println(clock()+name + " is leaving");
					userNum--; //Decreasing number to 1.
					names.remove(name); //names hashSet ���� �ش� name ����
					n.remove(name,out); //n hashTable���� �ش� name ����
					for (PrintWriter writer : writers) { //�ش� client�� �����ٴ� ��۰� �Բ� ���ŵ� ������ client�鿡�� ����
						writer.println("MESSAGE " + clock()+ name + " has left");
						writer.println("SERVERINFO " + userNum+"��");
						writer.println("USERINFO "+ names);
					}
				}
				try { socket.close(); } catch (IOException e) {}
			}
		}
	}
 /*���� �ð��� �˱����� ����*/
	static String clock() {
		Date dtime = new Date();
		String time = new SimpleDateFormat("[HH:mm:ss]").format(dtime);
		return time;
	}
}