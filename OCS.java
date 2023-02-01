package OHT;
import OHT.Vehicle;



import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import OHT.Load.Attribute;
import socket.*;


import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import GUI.MainUI;

//1) ���α׷� ����
//2) ���� ���� 
//3) StateCheck�� �����̵� ���� üũ/����

/**
 * @date 210218 : dataInputStream.available() = 0 ��Ź��������� 
 * ä�����α׷�ó�� ���� ���� ���� ����ϵ��� ����(����� �ѽ������ �ݺ��������鼭 ������ �޴� �õ�����)
 * 1) ���α׷� ����
 * 2) ���� ���� 
 * 3) StateCheck�� �����̵� ���� üũ/����
 */
public class OCS {
	public static boolean simulationFlag = true; //true; //false
	private final static int THREAD_CNT = 15;
	public static ExecutorService threadPool;
	public static final int INPUT_VEHICLE_NUM = 13; //5; //10; //�ϴ� 5�� �����ϰ�  sc���� ���� ��ü�� null
	public static final int THREAD_NUM = INPUT_VEHICLE_NUM + 4; //StateCheck, ConsloeRecv, Server
	public static final int REMAIN_THRESHOLD = 90000;
	public static Vehicle[] OHT;
	public static CMD[] cmd;
	public static int checkConnect[];
	public static Location[] location;
	public static boolean startFlag = false;
	public static Scheduler simVehicle;
	public static Load load;
	public static Queue queue;
	public static LoadPort loadport;
	public static String path;
	public static Scanner in = new Scanner(System.in);
	public static String cmdType = ""; //#�ԷµǴ� �����
	public static String work = ""; //#�ԷµǴ� �۾�
	public static Vector<Client> clientList = new Vector<Client>();
	//public static String connectType = ""; //OCS�� ����/Ŭ���̾�Ʈ �� ��������� ���� ���� -> Static �����ø��°ͺ��� �׳� �Ķ���ͷ� ���� �Է�
	
	/** 20.8.14 : �Ķ���Ͱ� ������������ ��� ����
	 *	�������� �ּҰ� �޾Ƽ� �Ϻκ� �߶� �����ؼ� ����ϱ� ���� �ۼ�
	 */
	public static void getDataPath() {
		path = System.getProperty("user.dir") + "\\";
		
		// TODO : ACSó�� Intialization window�� ����
		System.out.println("���� ���丮 : " + path);
	}
	
	/** 20.8.15 : ���̾ƿ� �����޾ƿ���
	*/
	public static void createLayout() {
		new Layout();   //# ���� ���� ��ü�� �����(�׸�)
		new ReadData(); //# ��ü�� �� �ֱ�
		
		//# Layout, ReadData�� �����Ѵ��� ������ �� �ִ� �۾�
		Location.setFBLine(); //# ��� �յ� ���μ���
		Location.setFBNode(); //# ��� �յ� ��弳��
		Location.setType();
		
		//# zone ����
		Routing.defineZone();
	}
	
	/** 2021.4.18 : �Էµ� ������ Ÿ�Կ� ���� ���� ����
	 *  @param inputVehNum : ���� ���� 
	 *  @param type : "SamePC"/"OtherPC"/"Real"/"Mixed" ���� ���� Ÿ�Կ� ���� ������ IP�ּҿ� Port�� ���� 
	 */
	public static void createVehicle(int inputVehNum, String type) {
		// TODO : ����ڰ� ó���� ������� �Է��ؼ� �°� �����ǵ����ϱ�, �״����� ������������س��ٰ� ��������� �Ҳ��� �����
		//# ���� ���� ���� ��ü ����
		
		OHT = new Vehicle[inputVehNum + 1]; //# OHT[0]�� �Ⱦ�
		cmd = new CMD[inputVehNum + 1]; //# cmd[0]�� �Ⱦ�
		checkConnect = new int[inputVehNum + 1]; //checkConnect[0]�� �Ⱦ�
		int lastOctet = 0;
		
		//# ���� id������� �ʿ� ������ �ο�
		for (int i = 1; i <= inputVehNum; i++) {
			OHT[i] = new Vehicle();
			OHT[i].ID = i; //�ϴ� ��žȵǴ��ִ� �����鵵 ���̵�� �ο�
			cmd[i] = new CMD(OHT[i]);
			
			//# ���� OHT�� ���� ���
			//# OHT�� �����̹Ƿ� �� �������� ��ſ� �´� ip�� port�� ����
			switch (type) {
			case "SamePC": //# ���� PC���� ���� OHT
				OHT[i].IP = "127.0.0.1";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC1": //# ����PC OCS <-> ȸ���Ʈ���� ���� OHT
				OHT[i].IP = "192.168.100.130";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC2": //# �׷� ��Ʈ�� OCS <-> ȸ�����ͳ� ȸ���Ʈ���� ���� OHT
				OHT[i].IP = "192.168.10.194";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC3": //# �׷� ��Ʈ�� OCS <-> ��ȯ���ͳ� ȸ���Ʈ���� ���� OHT
				OHT[i].IP = "192.168.1.50";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC4": //# �׷� ��Ʈ�� OCS <-> ȸ���Ʈ�� �ֽ��� ���� �� ���� OHT
				OHT[i].IP = "192.168.137.1";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "Real": //# ��ȯ������ ���� OHT PC�� �������
				lastOctet = 100 + OHT[i].ID; //ex) 192.168.1.101 / 102 / 103 / 104 ~
				OHT[i].IP = "192.168.1." + Integer.toString(lastOctet);
				OHT[i].port = 10001;
				
				break;
				
			case "Mixed": //# ��ȯ������ ���� OHT�� ����OHT�� ȥ���ؼ� ���
				switch (i) {
				//# ���� OHT
				case 1: 
				case 5:
				case 6:
				case 7:
				case 8:
					OHT[i].IP = "127.0.0.1";
					OHT[i].port = 1000 + i;
					
					break;
				
				//# ���� OHT
				case 2:
				case 3:
				case 4:
					lastOctet = 100 + OHT[i].ID; //ex) 192.168.1.101 / 102 / 103 / 104 ~
					OHT[i].IP = "192.168.1." + Integer.toString(lastOctet);
					OHT[i].port = 10001;
					
					break;
				default:
					break;
				}
				
				break;
			
			default:
				System.out.println(type + " : ���� Ÿ���� �߸��Ǿ����ϴ�.");
				break;
			}
		}
	}
	
	
	public static void main(String[] args) {//# OCS.java�� �����Ҷ� �ڿ� �ٿ��ִ� �ɼ��� �����ϸ� java OCS ABC IBM �� �߰� �ɼ� �߰� ����
		
		//# addShutdownHook �����带 ���� OCS ������ ����� �޼��� ���
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("addShutdownHook ����");
			DataBase.errorLog(OCS.OHT[1], "addShutdownHook ����");
		}));
		
		//# ���� ���α׷� ���� ��� ���
		getDataPath();
		
		//# THREAD_CNT ���� ��ŭ ������ ������ ���� threadPool����
		threadPool = Executors.newFixedThreadPool(THREAD_CNT);
		
		createLayout();
		createVehicle(INPUT_VEHICLE_NUM, "OtherPC4"); 
		
		//# ������ ���� ����� ���� Ŭ���̾�Ʈ�� ����
		// TODO : â�� �⺻ Default�Ǿ��ִ� ����ȣ��� IP �ּ� â�� �̴�� �����ϸ�ɰ��� ����� �۾�����
		for (int i = 1; i <= INPUT_VEHICLE_NUM; i++) {
			Client ohtClient = new Client(OHT[i].IP, OHT[i].port, OHT[i].ID); //# �ϳ��� ���ֱ� 			
			clientList.add(ohtClient);
		}
		//# start �޼ҵ带 ���� UI ���� �� ���� -> ���⼭ �ٸ� ������� ����/�����ؾ��� ��, �Ѿ�� ����
		// TODO : GUI ���α׷��̶� OCS ���α׷��̶� �и��۾� �ʿ�
		Application.launch(MainUI.class, args); 

		load = new Load("AtOnce"); // �������� Load ��ü�� Load�������� �ޱ����� �غ�
		queue = new Queue(); // �������� Load ��ü�� Load�������� �ޱ����� �غ�
		loadport = new LoadPort();
		
		startFlag = true;
	}
	

}
