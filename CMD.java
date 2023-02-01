package OHT;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import GUI.LogWindow;
import GUI.MainUI;
import javafx.application.Platform;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.paint.Color;
import socket.Client;
import socket.ConsoleRecv;
import socket.Server;
import tool.Toolbox;

public class CMD {
	public boolean ArriveFlag = false;
	// 보낼 데이터 만들기

	private static Vehicle OHT;

	public CMD(Vehicle OHT) {
		this.OHT = OHT;
	}

	/** 차량이 이동 메소드로 Dijkstra와 go, gomore로 구성됨 **/
	public void Move(Vehicle OHT, int endNodeID) { // Socket socket
		// String cmdType = null;
		String text = "";
		OHT.endNode = Location.fineNode(endNodeID);
		// 목적지 입력전 초기상태일 떄와 도착했을때
		if (OHT.currNodeID != endNodeID && endNodeID != 0) {
			// Go & ShortestPath
			if (!OHT.startMoveFlag && Check.checkGo(OHT, endNodeID)) { // 6m이하인것들은 계산안하고 바로 고 할수있5 만들기
				new ShortestPath(OHT, endNodeID, "Move");
				Go(OHT, endNodeID);
				OHT.startMoveFlag = true; // totalPathNode로 바꾸기

				// if (!Dijkstra2.isUsing) { //동시에 사용되는것을 방지(임시용)
				// Dijkstra2.isUsing = true;
				// System.out.println("OHT["+ OHT.ID +"] Dijkstra 시작");

				// System.out.println("OHT["+ OHT.ID +"] Dijkstra 완료");
				// Dijkstra2.isUsing = false;
				// }
			}
			// GoMore 실행
			else { // go하고 바로 gomore 계산하는거 방지 할려고 else써서 따로 따로 진행
					// 정지 상태에서 목적 지점이랑 거리가 6만 이상이면 6만거리차이 만큼 나는 노드로 go 후 최종목적지까지 gomore,6만 거리만큼 차이
					// 안나면 그냥 go
					// 목표지점과의 거리가 6만 이상이면 gomore
					// 목적지 도착 후 위치조정시 M상태이기떄문에 그떄 고려해서 size로 조건 확인
				if (OHT.goPathNode.size() > 0 && Check.checkGoMore(OHT, endNodeID)) { // load&unload중에는 이동 방지
					GoMore(OHT);
					// targetNode는 GoMore에서 갱신
					// 새로 추가된 노드가 중복인지 아닌지 검사
					Location lastNode = OHT.goPathNode.lastElement();
					// if(OHT.targetNode != null) {
					if (!OHT.targetNode.equals(lastNode)) { // path에서 마지막 노드랑 다를 경우 새노드 추가
						text = "OHT[" + OHT.ID + "]의 goPathNode 마지막노드랑 targetNode[" + OHT.targetNode.id
								+ "]랑 중복아님 통과, goPathNode에 추가";
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);
						OHT.goPathNode.add(OHT.targetNode);

						text = Toolbox.printNodeVector(OHT.goPathNode);
						text = "OHT[" + OHT.ID + "] goPath : " + text;
						MainUI.controller.append(text, "status", OHT);
					} else { // if (OHT.waitFlag == false){ //OHT.waitFlag = true 상태인데 앞 차량이 자리 비켜줘서 갈 수 있으면
								// 중복이여도 통과 (75줄)
						text = "OHT[" + OHT.ID + "]의 goPathNode 마지막노드랑 targetNode[" + OHT.targetNode.id + "]랑 중복";
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);

						text = "OHT[" + OHT.ID + "]의 송신취소예정 cmd : " + OHT.currCmd;
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);
						OHT.currCmd = ""; // 중복되는 경우 삭제하고 다시 구함
					}
					// }

				}

			}
			// Go or GoMore 메세지 준비완료

			// totalPathNode로 안전거리 체크
			// Layout.getDistToDestN(OHT, targetNode, OHT.totalPathNode)

			// 소켓에 넣어 보내기 전에 최종 검토 cmd가 비었는지, 이전 명령이랑 중복인지, 타겟노드 비었는지
			if (OHT.currCmd != null && !OHT.currCmd.equals("") && !OHT.currCmd.equals(OHT.befCmd)
					&& OHT.targetNode != null) { // 중복검사
				// ---------최종으로 다른 차량있는지 체크, OHT.waitFlag 값도 결정-------------------
				// 통과
				if (AntiCollision.checkPathNode3(OHT)) { // OHT.targetNode != OHT.destNode
					// targetNode까지 모든 노드의 setVehicle 값에 OHT 입력
					for (int i = 0; i < OHT.goPathNode.size(); i++) {
						Location location = OHT.goPathNode.elementAt(i);
						location.setVehicle = OHT;
						location.changeColor();
					}

					text = "OHT[" + OHT.ID + "]: go targetNode : " + OHT.targetNode.id;
					MainUI.controller.append(text, "status", OHT);
					// LogWindow.append(text, "checkPath", OHT.id);

					text = "OHT[" + OHT.ID + "]: beforsSendCMD통과";
					MainUI.controller.append(text, "status", OHT);
					// LogWindow.append(text, "checkPath", OHT.id);

					OHT.currCmd = OHT.currCmd + OHT.targetNode.id; // + (char) 13;
					OHT.befCmd = OHT.currCmd;

					// Server.sendData(socket, OHT.currCmd); //클라이언트때문에 잠시 주석처리
					Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0에서 부터 시작하니까
					client.send(OHT.currCmd);
					DataBase.scheduleLog(OHT.ID, OHT.currCmd);

					text = "OHT[" + OHT.ID + "]: " + OHT.currCmd + " 송신";
					MainUI.controller.append(text, "status", OHT);
				}
				// 실패
				else {
					text = "AntiCollision 때문에 OHT[" + OHT.ID + "]: beforsSendCMD 통과실패 ";
					// text += "AntiCollision.checkPathNode : " + AntiCollision.checkPathNode(OHT) +
					// "\n";
					if (OHT.targetNode != null)
						text += "OHT.targetNode : " + OHT.targetNode.id;
					else
						text += "OHT.targetNode : null";
					if (OHT.destNode != null)
						text += " OHT.destNode : " + OHT.destNode.id;
					else
						text += " OHT.destNode : null";

					MainUI.controller.append(text, "status", OHT);
					// LogWindow.append(text, "checkPath", OHT.id);

					OHT.currCmd = ""; // 초기화
					OHT.goPathNode.remove(OHT.targetNode); // 추가 되었던 destNode를 다시 삭제 , 리스폰X 일경우에는 clear로 초기화됨
					OHT.targetNode = null;
				}

				text = "OHT[" + OHT.ID + "]의 현재 경로 : ";
				text = text + AntiCollision.printNodeVector(OHT.goPathNode);
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
			}
			text = "";
			text = "waitFlag : " + String.valueOf(OHT.waitFlag);
			String nameMethod = Thread.currentThread().getStackTrace()[1].getMethodName();
			// SaveData.termsLog(OHT, text);
			DataBase.allStatus(OHT, nameMethod + text);
			MainUI.controller.append(text, "status", OHT);

			// 이전 작업이 GoMore이고 다른 차량에 의해 멈춤상태(arrived)이면 OHT.startMoveFlag를 초기화해서 다시 경로계산한다
			if (OHT.waitFlag && (OHT.currState.equals("A") || OHT.currState.equals("I"))) {
				OHT.startMoveFlag = false;

				System.out.println("OHT 상태 A 또는 I, startMoveFlag : True -> False");
				text = OHT.ID + "번 차량이 멈췄다가 새 경로 계산 후 이동합니다, waitFlag = " + OHT.waitFlag;
				System.out.println(text);
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
			}

		}

		else {// 도착하면 초기화
				// 이거 두개 없애면 schedule() 무한이 돌릴 수 있고, 있으면 한번 돌고 초기화되서 아래 작업들 연속적으로 하는거 막아줌
				// Server.work = "";
				// Server.cmdType = "";

			ConsoleRecv.CmmDestNode = 0;

			if (OHT.destNode != null) {
				text = OHT.destNode.id + "Node에 도착";
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
			}
			if (OHT.startMoveFlag == true) {
				OHT.startMoveFlag = false;
				System.out.println(text + "OHT 목적지에 도착, startMoveFlag : True -> False");
				System.out.println("OHT.currNodeID : " + OHT.currNodeID + "endNodeID : " + endNodeID);
				OHT.TaskCNT++;
			}

			text = "OHT[" + OHT.ID + "]" + "의 Task count : " + OHT.TaskCNT;
			MainUI.controller.append(text, "status", OHT);
			OHT.goPathNode.clear();
			OHT.totalPathNode.clear();
			text = Toolbox.printNodeVector(OHT.totalPathNode);
			LogWindow.append(text, "totalPath", OHT.ID);
			text = Toolbox.printNodeVector(OHT.goPathNode);
			LogWindow.append(text, "goPath", OHT.ID);
			// OHT.endNode = null;

			if (OCS.cmdType.equals("M")) {
				OCS.cmdType = "Fin";
			}
		}
	}

	public void Go(Vehicle OHT, int end) {
		String text = "";
		if (OHT.totalDist > 60000) { // 아직 출발안해서 남은 거리는 알수없음
			OHT.targetNode = OHT.firstNode;
			text = "totalDist : " + OHT.totalDist + "가 기준거리보다 길다";
		} else {// 짧은 거리라면 그냥 go로 한번에 이동
			OHT.targetNode = Location.fineNode(end);
			text = "totalDist : " + OHT.totalDist + "가 기준거리보다 짧다";
		}
		MainUI.controller.append(text, "status", OHT);

		text = "OHT[" + OHT.ID + "]: go targetNode : " + OHT.targetNode.id;
		MainUI.controller.append(text, "status", OHT);
		// LogWindow.append(text, "checkPath", OHT.id);

		// 시뮬레이션 용 -> 흠 beforSendCMD랑 어떻게 되는지 생각해봐야할듯
		if (OCS.simulationFlag) {
			/*
			 * OHT.currCmd = "G/" + OHT.ID + "/" + OHT.targetNode.id; for (int i = 0; i
			 * <OHT.totalPathNode.size(); i++) { //모든 경로도 같이 보내기 OHT.currCmd = OHT.currCmd +
			 * "/" + OHT.totalPathNode.elementAt(i).id; } OHT.currCmd = OHT.currCmd +
			 * (char)13; //마지막 캐리지리턴
			 */
			OHT.currCmd = "G/" + OHT.ID + "/";
		}
		// 실제현장 용
		else
			OHT.currCmd = "G/" + OHT.ID + "/"; // + OHT.destNodeID + (char) 13;

	}

	public void GoMore(Vehicle OHT) {
		String text = "";
		for (int i = 0; i < OHT.totalPathNode.size(); i++) {
			if (OHT.destNode.id != OHT.totalPathNode.lastElement().id) { // 최종 목적지에서 다가나는걸 방지 (n이 마지막인데 n번째에서 계산되면 n+1로
																			// 가야되서)
				int nextNodeIndex = OHT.totalPathNode.indexOf(OHT.destNode) + 1;
				OHT.targetNode = OHT.totalPathNode.elementAt(nextNodeIndex); // 다음인덱스 노드저장
				OHT.currCmd = "GM/" + OHT.ID + "/"; // + OHT.destNodeID + (char) 13;

				text = "OHT[" + OHT.ID + "].waitflag : " + OHT.waitFlag + ", GoMore 메소드 동작";
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
				break;
			}
		}
	}

	// !!!일단 차량들의 명령동작이 혼선이 되는걸 막기위해 CMD 객체 선언시 차량 개수에 맞게 cmd[i]를 선언해놔서 Load()에 굳이
	// OHT를 파라미터를 넣어줘야할 지는 나중에 디버깅하면서 판단 -> 생성자때 받은 OHT객체 사용에 문제가 없으면 패스
	public void Load(Vehicle OHT, int loadNodeID, String carrierID, int portnum) {
		String text = "";
		if (Check.checkLoad(OHT, loadNodeID)) {
			try {
				text = "OHT[" + OHT.ID + "] loading 전 일정 시간 0.5초 Delay";
				MainUI.controller.append(text, "status", OHT);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				OHT.currCmd = "L/" + OHT.ID + "/" + carrierID + "/" + portnum; // + (char) 13;
				Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0에서 부터 시작하니까
				client.send(OHT.currCmd);

				// Server.dataOutputStream.write(OHT.currCmd.getBytes());
				OHT.loadID = carrierID;

				text = "Foup[" + OHT.loadID + "]을 놓기 위해 Loading 시작";
				MainUI.controller.append(text, "status", OHT);

				DataBase.commLog(OHT.ID, ">" + OHT.currCmd);
				DataBase.scheduleLog(OHT.ID, OHT.currCmd);
				OHT.loadID = "Empty";
				OHT.TaskCNT++;
				text = "OHT[" + OHT.ID + "]" + "의 Task count : " + OHT.TaskCNT;
				MainUI.controller.append(text, "status", OHT);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void Unload(Vehicle OHT, int unloadNodeID, String carrierID, int portnum) {
		String text = "";
		if (Check.checkUnload(OHT, unloadNodeID)) {
			try {
				text = "OHT[" + OHT.ID + "] unloading 전 일정 시간 0.5초 Delay";
				MainUI.controller.append(text, "status", OHT);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				OHT.currCmd = "U/" + OHT.ID + "/" + carrierID + "/" + portnum; // + (char) 13;

				Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0에서 부터 시작하니까
				client.send(OHT.currCmd);

				// Server.dataOutputStream.write(OHT.currCmd.getBytes());
				OHT.loadID = carrierID;

				text = "Foup[" + OHT.loadID + "] 집기위해 Unloading 시작";
				MainUI.controller.append(text, "status", OHT);

				DataBase.commLog(OHT.ID, ">" + OHT.currCmd);
				DataBase.scheduleLog(OHT.ID, OHT.currCmd);
				OHT.TaskCNT++;
				text = "OHT[" + OHT.ID + "]" + "의 Task count : " + OHT.TaskCNT;
				MainUI.controller.append(text, "status", OHT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// # 21.04.28 : 문제있어서 잠시 Schedule10 -> Schedule1 원복
	// # 21.05.01 : 가상OHT 3대(1,5,6) + 실제OHT(2,3,4) 3대를 위해 이동 경로 수정
	// # 21.05.06 : OHT.loadExist 체크가 없으면 도착하고 계속 연달아서 다음 작업 명령을 내려버림 꼭 필요!
	public void Schedule1(Vehicle OHT) {
		switch (OHT.ID) {
		case 1:
			if (OHT.TaskCNT == 0)
				Move(OHT, 404); // 109 801
			else if (OHT.TaskCNT == 1)
				Move(OHT, 801); // 609 210
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 2:
//			if (OHT.TaskCNT == 0)
//				Move(OHT, 906); //104 801 404
//			else if (OHT.TaskCNT == 1)
//				Move(OHT, 407); //161 210 409
//			else if (OHT.TaskCNT >= 2)
//				OHT.TaskCNT = 0;

			if (OHT.TaskCNT == 0)
				Move(OHT, 906);
			else if (OHT.TaskCNT == 1 && OHT.loadExist == 0)
				Unload(OHT, 906, "A", 1);
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1) // UF 조건 걸어놓으면 M상태일때 안먹혀버림
				Move(OHT, 407);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 906);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
				Load(OHT, 906, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0) // UF 조건 걸어놓으면 M상태일때 안먹혀버림
				Move(OHT, 407);
			else if (OHT.TaskCNT >= 6)
				OHT.TaskCNT = 0;
			break;

		case 3:
//			if (OHT.TaskCNT == 0)
//				Move(OHT, 903);
//			else if (OHT.TaskCNT == 1)
//				Move(OHT, 202);
//			else if (OHT.TaskCNT >= 2)
//				OHT.TaskCNT = 0;

			if (OHT.TaskCNT == 0)
				Move(OHT, 903);
			else if (OHT.TaskCNT == 1 && OHT.loadExist == 0)
				Unload(OHT, 903, "A", 1);
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1) // UF 조건 걸어놓으면 M상태일때 안먹혀버림
				Move(OHT, 202);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 903);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
				Load(OHT, 903, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0) // UF 조건 걸어놓으면 M상태일때 안먹혀버림
				Move(OHT, 202);
			else if (OHT.TaskCNT >= 6)
				OHT.TaskCNT = 0;
			break;

		case 4:
			if (OHT.TaskCNT == 0)
				Move(OHT, 905);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 108);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 5:
			if (OHT.TaskCNT == 0)
				Move(OHT, 152); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 161);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 6:
			if (OHT.TaskCNT == 0)
				Move(OHT, 303); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 104);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 7:
			if (OHT.TaskCNT == 0)
				Move(OHT, 308); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 202);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 8:
			if (OHT.TaskCNT == 0)
				Move(OHT, 313); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 407);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 9:
			if (OHT.TaskCNT == 0)
				Move(OHT, 158); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 407);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 10:
			if (OHT.TaskCNT == 0)
				Move(OHT, 163); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 409);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		default:
			break;
		}
	}

	// 시뮬레이션에서 load, unload 작업
	public void Schedule2(Vehicle OHT) {
		switch (OHT.ID) {
		case 2:
			if (OHT.TaskCNT == 0)
				Move(OHT, 116); // 104 801 404
			else if (OHT.TaskCNT == 1)
				Move(OHT, 613); // 161 210 409
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 3:
			if (OHT.TaskCNT == 0)
				Move(OHT, 210);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 801);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 4:
			if (OHT.TaskCNT == 0)
				Move(OHT, 404);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 409);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 5:
			if (OHT.TaskCNT == 0)
				Move(OHT, 102); // 203 //104
			else if (OHT.TaskCNT == 1)
				Move(OHT, 602);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		default:
			break;
		}

	}

	// # 1호기 : 604 <-> 906, 2호기 : 903 <-> 905, 3호기 : 807 <-> 904, 4호기 : 404 <-> 907,
	public void Schedule3(Vehicle OHT) {
		switch (OHT.ID) {
		case 1:
			/*
			 * if (OHT.TaskCNT == 0) Move(OHT, 404); //109 801 else if (OHT.TaskCNT == 1)
			 * Move(OHT, 801); //609 210 else if (OHT.TaskCNT >= 2) OHT.TaskCNT = 0; break;
			 */

			if (OHT.TaskCNT == 0)
				Move(OHT, 904);
			else if (OHT.TaskCNT == 1 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 904, "A", 1);
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
				Move(OHT, 158);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 1) // # Load
				Load(OHT, 158, "A", 2);
			else if (OHT.TaskCNT == 6 && OHT.loadExist == 0)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 7 && OHT.loadExist == 0)
				Move(OHT, 158);
			else if (OHT.TaskCNT == 8 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 158, "A", 2);
			else if (OHT.TaskCNT == 9 && OHT.loadExist == 1)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 10 && OHT.loadExist == 1)
				Move(OHT, 904);
			else if (OHT.TaskCNT == 11 && OHT.loadExist == 1) // # Load
				Load(OHT, 904, "A", 1);
			else if (OHT.TaskCNT >= 12)
				OHT.TaskCNT = 1;
			break;

		case 2:
			if (OHT.TaskCNT == 0)
				Move(OHT, 604);
			else if (OHT.TaskCNT == 1 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 604, "A", 1);
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 906);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1) // # Load
				Load(OHT, 906, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 6 && OHT.loadExist == 0)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 7 && OHT.loadExist == 0)
				Move(OHT, 906);
			else if (OHT.TaskCNT == 8 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 906, "A", 1);
			else if (OHT.TaskCNT == 9 && OHT.loadExist == 1)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 10 && OHT.loadExist == 1)
				Move(OHT, 604);
			else if (OHT.TaskCNT == 11 && OHT.loadExist == 1) // # Load
				Load(OHT, 604, "A", 1);
			else if (OHT.TaskCNT >= 12)
				OHT.TaskCNT = 1;
			break;

		case 3:
			if (OHT.TaskCNT == 0)
				Move(OHT, 903);
			else if (OHT.TaskCNT == 1 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 903, "A", 1);
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 807);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1) // # Load
				Load(OHT, 807, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 6 && OHT.loadExist == 0)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 7 && OHT.loadExist == 0)
				Move(OHT, 807);
			else if (OHT.TaskCNT == 8 && OHT.loadExist == 0) // # Unload
				Unload(OHT, 807, "A", 1);
			else if (OHT.TaskCNT == 9 && OHT.loadExist == 1)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 10 && OHT.loadExist == 1)
				Move(OHT, 903);
			else if (OHT.TaskCNT == 11 && OHT.loadExist == 1) // # Load
				Load(OHT, 903, "A", 1);
			else if (OHT.TaskCNT >= 12)
				OHT.TaskCNT = 1;
			break;

		case 4:
			if (OHT.TaskCNT == 0)
				Move(OHT, 404);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 408);
			else if (OHT.TaskCNT == 2)
				Move(OHT, 117);
			else if (OHT.TaskCNT == 3)
				Move(OHT, 203);
			else if (OHT.TaskCNT == 4)
				Move(OHT, 209);
			else if (OHT.TaskCNT == 5)
				Move(OHT, 615);
			else if (OHT.TaskCNT == 6)
				Move(OHT, 410);
			else if (OHT.TaskCNT >= 7)
				OHT.TaskCNT = 0;
			break;

//				if (OHT.TaskCNT == 0)
//					Move(OHT, 404);
//				else if (OHT.TaskCNT == 1 && OHT.loadExist == 0) //# Unload
//					Unload(Server.clientSocket, OHT, 404, "A", 1);
//				else if (OHT.TaskCNT == 2 && OHT.loadExist == 1) 
//					Move(OHT, 408);
//				else if (OHT.TaskCNT == 3 && OHT.loadExist == 1) 
//					Move(OHT, 117);
//				else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
//					Move(OHT, 203);
//				else if (OHT.TaskCNT == 5 && OHT.loadExist == 1) //# Load
//					Load(Server.clientSocket, OHT, 203, "A", 1);
//				else if (OHT.TaskCNT == 6 && OHT.loadExist == 0)
//					Move(OHT, 209);
//				else if (OHT.TaskCNT == 7 && OHT.loadExist == 0)
//					Move(OHT, 615);
//				else if (OHT.TaskCNT == 8 && OHT.loadExist == 0)
//					Move(OHT, 410);
//				else if (OHT.TaskCNT == 9 && OHT.loadExist == 0)
//					Move(OHT, 408);
//				else if (OHT.TaskCNT == 10 && OHT.loadExist == 0)
//					Move(OHT, 117);
//				else if (OHT.TaskCNT == 11 && OHT.loadExist == 0)
//					Move(OHT, 203);
//				else if (OHT.TaskCNT == 12 && OHT.loadExist == 0) //# Unload
//					Unload(Server.clientSocket, OHT, 203, "A", 1);
//				else if (OHT.TaskCNT == 13 && OHT.loadExist == 1)
//					Move(OHT, 209);
//				else if (OHT.TaskCNT == 14 && OHT.loadExist == 1)
//					Move(OHT, 615);
//				else if (OHT.TaskCNT == 15 && OHT.loadExist == 1)
//					Move(OHT, 410);
//				else if (OHT.TaskCNT == 16 && OHT.loadExist == 1)
//					Move(OHT, 404);
//				else if (OHT.TaskCNT == 17 && OHT.loadExist == 1) //# Load
//					Load(Server.clientSocket, OHT, 404, "A", 1);
//				else if (OHT.TaskCNT >= 18)
//					OHT.TaskCNT = 1;
//				break;

		case 5:
			if (OHT.TaskCNT == 0)
				Move(OHT, 702);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 103); //
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 6:
			if (OHT.TaskCNT == 0)
				Move(OHT, 613);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 152);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 7:
			if (OHT.TaskCNT == 0)
				Move(OHT, 308);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 202);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		case 8:
			if (OHT.TaskCNT == 0)
				Move(OHT, 313);
			else if (OHT.TaskCNT == 1)
				Move(OHT, 407);
			else if (OHT.TaskCNT >= 2)
				OHT.TaskCNT = 0;
			break;

		default:
			break;
		}

	}

	// 205 <-> 210 문제
	public void Schedule4(Vehicle OHT) {
		if (OHT.TaskCNT == 0)
			Move(OHT, 210);
		else if (OHT.TaskCNT == 1)
			Move(OHT, 205);
		else if (OHT.TaskCNT >= 2)
			OHT.TaskCNT = 0;
	}

	// # 21.04.27 N대의 차량이 동시에 연속주행
	public String intialSchedule(Vehicle OHT) {
		String scheduleState = "bfStart"; //
		// # 1. 차량 위치파악, Load 위치 파악
		// # 2. 이동하

		return scheduleState;
	}
}