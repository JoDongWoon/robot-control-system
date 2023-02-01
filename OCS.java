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

//1) 프로그램 시작
//2) 서버 연결 
//3) StateCheck로 연결이된 차량 체크/저장

/**
 * @date 210218 : dataInputStream.available() = 0 통신문제때문에 
 * 채팅프로그램처럼 차량 마다 개별 통신하도록 수정(현재는 한스레드로 반복문돌리면서 데이터 받는 시도했음)
 * 1) 프로그램 시작
 * 2) 서버 연결 
 * 3) StateCheck로 연결이된 차량 체크/저장
 */
public class OCS {
	public static boolean simulationFlag = true; //true; //false
	private final static int THREAD_CNT = 15;
	public static ExecutorService threadPool;
	public static final int INPUT_VEHICLE_NUM = 13; //5; //10; //일단 5대 고정하고  sc없는 차량 객체는 null
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
	public static String cmdType = ""; //#입력되는 운영조건
	public static String work = ""; //#입력되는 작업
	public static Vector<Client> clientList = new Vector<Client>();
	//public static String connectType = ""; //OCS를 서버/클라이언트 중 어느것으로 쓸지 정함 -> Static 변수늘리는것보단 그냥 파라미터로 따로 입력
	
	/** 20.8.14 : 파라미터값 가져오기위해 경로 설정
	 *	실행폴더 주소값 받아서 일부분 잘라서 수정해서 사용하기 위해 작성
	 */
	public static void getDataPath() {
		path = System.getProperty("user.dir") + "\\";
		
		// TODO : ACS처럼 Intialization window에 띄우기
		System.out.println("현재 디렉토리 : " + path);
	}
	
	/** 20.8.15 : 레이아웃 정보받아오기
	*/
	public static void createLayout() {
		new Layout();   //# 값을 넣을 객체를 만들고(그릇)
		new ReadData(); //# 객체에 값 넣기
		
		//# Layout, ReadData를 선언한다음 진행할 수 있는 작업
		Location.setFBLine(); //# 노드 앞뒤 라인설정
		Location.setFBNode(); //# 노드 앞뒤 노드설정
		Location.setType();
		
		//# zone 설정
		Routing.defineZone();
	}
	
	/** 2021.4.18 : 입력된 개수와 타입에 맞춰 차량 생성
	 *  @param inputVehNum : 생성 개수 
	 *  @param type : "SamePC"/"OtherPC"/"Real"/"Mixed" 차량 생성 타입에 따라 별도의 IP주소와 Port를 가짐 
	 */
	public static void createVehicle(int inputVehNum, String type) {
		// TODO : 사용자가 처음에 생성대수 입력해서 맞게 생성되도록하기, 그다음엔 생성대수저장해놨다가 이전값대로 할껀지 물어보기
		//# 차량 갯수 맞줘 객체 생성
		
		OHT = new Vehicle[inputVehNum + 1]; //# OHT[0]은 안씀
		cmd = new CMD[inputVehNum + 1]; //# cmd[0]은 안씀
		checkConnect = new int[inputVehNum + 1]; //checkConnect[0]은 안씀
		int lastOctet = 0;
		
		//# 차량 id순서대로 필요 정보들 부여
		for (int i = 1; i <= inputVehNum; i++) {
			OHT[i] = new Vehicle();
			OHT[i].ID = i; //일단 통신안되는있는 차량들도 아이디는 부여
			cmd[i] = new CMD(OHT[i]);
			
			//# 가상 OHT와 로컬 통신
			//# OHT는 서버이므로 각 개별마다 통신에 맞는 ip와 port를 가짐
			switch (type) {
			case "SamePC": //# 같은 PC내의 가상 OHT
				OHT[i].IP = "127.0.0.1";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC1": //# 서버PC OCS <-> 회사노트북의 가상 OHT
				OHT[i].IP = "192.168.100.130";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC2": //# 그램 노트북 OCS <-> 회사인터넷 회사노트북의 가상 OHT
				OHT[i].IP = "192.168.10.194";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC3": //# 그램 노트북 OCS <-> 성환인터넷 회사노트북의 가상 OHT
				OHT[i].IP = "192.168.1.50";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "OtherPC4": //# 그램 노트북 OCS <-> 회사노트북 핫스팟 연결 후 가상 OHT
				OHT[i].IP = "192.168.137.1";
				OHT[i].port = 1000 + i;
				
				break;
				
			case "Real": //# 성환공장의 실제 OHT PC와 직접통신
				lastOctet = 100 + OHT[i].ID; //ex) 192.168.1.101 / 102 / 103 / 104 ~
				OHT[i].IP = "192.168.1." + Integer.toString(lastOctet);
				OHT[i].port = 10001;
				
				break;
				
			case "Mixed": //# 성환공장의 실제 OHT와 가상OHT를 혼합해서 통신
				switch (i) {
				//# 가상 OHT
				case 1: 
				case 5:
				case 6:
				case 7:
				case 8:
					OHT[i].IP = "127.0.0.1";
					OHT[i].port = 1000 + i;
					
					break;
				
				//# 실제 OHT
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
				System.out.println(type + " : 차량 타입이 잘못되었습니다.");
				break;
			}
		}
	}
	
	
	public static void main(String[] args) {//# OCS.java를 실행할때 뒤에 붙여주는 옵션이 존재하면 java OCS ABC IBM 등 추가 옵션 추가 가능
		
		//# addShutdownHook 스레드를 통해 OCS 비정상 종료시 메세지 출력
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("addShutdownHook 실행");
			DataBase.errorLog(OCS.OHT[1], "addShutdownHook 실행");
		}));
		
		//# 현재 프로그램 폴더 경로 얻기
		getDataPath();
		
		//# THREAD_CNT 개수 만큼 스레드 관리를 위해 threadPool생성
		threadPool = Executors.newFixedThreadPool(THREAD_CNT);
		
		createLayout();
		createVehicle(INPUT_VEHICLE_NUM, "OtherPC4"); 
		
		//# 생성된 차량 대수에 맞춰 클라이언트도 생성
		// TODO : 창띄어서 기본 Default되어있는 차량호기와 IP 주소 창띄어서 이대로 연결하면될건지 물어보는 작업진행
		for (int i = 1; i <= INPUT_VEHICLE_NUM; i++) {
			Client ohtClient = new Client(OHT[i].IP, OHT[i].port, OHT[i].ID); //# 하나씩 값넣기 			
			clientList.add(ohtClient);
		}
		//# start 메소드를 통해 UI 생성 및 실행 -> 여기서 다른 스레드들 생성/실행해야할 듯, 넘어가지 못함
		// TODO : GUI 프로그램이랑 OCS 프로그램이랑 분리작업 필요
		Application.launch(MainUI.class, args); 

		load = new Load("AtOnce"); // 서버에서 Load 객체는 Load정보들을 받기위한 준비
		queue = new Queue(); // 서버에서 Load 객체는 Load정보들을 받기위한 준비
		loadport = new LoadPort();
		
		startFlag = true;
	}
	

}
