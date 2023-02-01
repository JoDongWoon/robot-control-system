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
	// ���� ������ �����

	private static Vehicle OHT;

	public CMD(Vehicle OHT) {
		this.OHT = OHT;
	}

	/** ������ �̵� �޼ҵ�� Dijkstra�� go, gomore�� ������ **/
	public void Move(Vehicle OHT, int endNodeID) { // Socket socket
		// String cmdType = null;
		String text = "";
		OHT.endNode = Location.fineNode(endNodeID);
		// ������ �Է��� �ʱ������ ���� ����������
		if (OHT.currNodeID != endNodeID && endNodeID != 0) {
			// Go & ShortestPath
			if (!OHT.startMoveFlag && Check.checkGo(OHT, endNodeID)) { // 6m�����ΰ͵��� �����ϰ� �ٷ� �� �Ҽ���5 �����
				new ShortestPath(OHT, endNodeID, "Move");
				Go(OHT, endNodeID);
				OHT.startMoveFlag = true; // totalPathNode�� �ٲٱ�

				// if (!Dijkstra2.isUsing) { //���ÿ� ���Ǵ°��� ����(�ӽÿ�)
				// Dijkstra2.isUsing = true;
				// System.out.println("OHT["+ OHT.ID +"] Dijkstra ����");

				// System.out.println("OHT["+ OHT.ID +"] Dijkstra �Ϸ�");
				// Dijkstra2.isUsing = false;
				// }
			}
			// GoMore ����
			else { // go�ϰ� �ٷ� gomore ����ϴ°� ���� �ҷ��� else�Ἥ ���� ���� ����
					// ���� ���¿��� ���� �����̶� �Ÿ��� 6�� �̻��̸� 6���Ÿ����� ��ŭ ���� ���� go �� �������������� gomore,6�� �Ÿ���ŭ ����
					// �ȳ��� �׳� go
					// ��ǥ�������� �Ÿ��� 6�� �̻��̸� gomore
					// ������ ���� �� ��ġ������ M�����̱⋚���� �׋� ����ؼ� size�� ���� Ȯ��
				if (OHT.goPathNode.size() > 0 && Check.checkGoMore(OHT, endNodeID)) { // load&unload�߿��� �̵� ����
					GoMore(OHT);
					// targetNode�� GoMore���� ����
					// ���� �߰��� ��尡 �ߺ����� �ƴ��� �˻�
					Location lastNode = OHT.goPathNode.lastElement();
					// if(OHT.targetNode != null) {
					if (!OHT.targetNode.equals(lastNode)) { // path���� ������ ���� �ٸ� ��� ����� �߰�
						text = "OHT[" + OHT.ID + "]�� goPathNode ���������� targetNode[" + OHT.targetNode.id
								+ "]�� �ߺ��ƴ� ���, goPathNode�� �߰�";
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);
						OHT.goPathNode.add(OHT.targetNode);

						text = Toolbox.printNodeVector(OHT.goPathNode);
						text = "OHT[" + OHT.ID + "] goPath : " + text;
						MainUI.controller.append(text, "status", OHT);
					} else { // if (OHT.waitFlag == false){ //OHT.waitFlag = true �����ε� �� ������ �ڸ� �����༭ �� �� ������
								// �ߺ��̿��� ��� (75��)
						text = "OHT[" + OHT.ID + "]�� goPathNode ���������� targetNode[" + OHT.targetNode.id + "]�� �ߺ�";
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);

						text = "OHT[" + OHT.ID + "]�� �۽���ҿ��� cmd : " + OHT.currCmd;
						MainUI.controller.append(text, "status", OHT);
						// LogWindow.append(text, "checkPath", OHT.id);
						OHT.currCmd = ""; // �ߺ��Ǵ� ��� �����ϰ� �ٽ� ����
					}
					// }

				}

			}
			// Go or GoMore �޼��� �غ�Ϸ�

			// totalPathNode�� �����Ÿ� üũ
			// Layout.getDistToDestN(OHT, targetNode, OHT.totalPathNode)

			// ���Ͽ� �־� ������ ���� ���� ���� cmd�� �������, ���� ����̶� �ߺ�����, Ÿ�ٳ�� �������
			if (OHT.currCmd != null && !OHT.currCmd.equals("") && !OHT.currCmd.equals(OHT.befCmd)
					&& OHT.targetNode != null) { // �ߺ��˻�
				// ---------�������� �ٸ� �����ִ��� üũ, OHT.waitFlag ���� ����-------------------
				// ���
				if (AntiCollision.checkPathNode3(OHT)) { // OHT.targetNode != OHT.destNode
					// targetNode���� ��� ����� setVehicle ���� OHT �Է�
					for (int i = 0; i < OHT.goPathNode.size(); i++) {
						Location location = OHT.goPathNode.elementAt(i);
						location.setVehicle = OHT;
						location.changeColor();
					}

					text = "OHT[" + OHT.ID + "]: go targetNode : " + OHT.targetNode.id;
					MainUI.controller.append(text, "status", OHT);
					// LogWindow.append(text, "checkPath", OHT.id);

					text = "OHT[" + OHT.ID + "]: beforsSendCMD���";
					MainUI.controller.append(text, "status", OHT);
					// LogWindow.append(text, "checkPath", OHT.id);

					OHT.currCmd = OHT.currCmd + OHT.targetNode.id; // + (char) 13;
					OHT.befCmd = OHT.currCmd;

					// Server.sendData(socket, OHT.currCmd); //Ŭ���̾�Ʈ������ ��� �ּ�ó��
					Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0���� ���� �����ϴϱ�
					client.send(OHT.currCmd);
					DataBase.scheduleLog(OHT.ID, OHT.currCmd);

					text = "OHT[" + OHT.ID + "]: " + OHT.currCmd + " �۽�";
					MainUI.controller.append(text, "status", OHT);
				}
				// ����
				else {
					text = "AntiCollision ������ OHT[" + OHT.ID + "]: beforsSendCMD ������� ";
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

					OHT.currCmd = ""; // �ʱ�ȭ
					OHT.goPathNode.remove(OHT.targetNode); // �߰� �Ǿ��� destNode�� �ٽ� ���� , ������X �ϰ�쿡�� clear�� �ʱ�ȭ��
					OHT.targetNode = null;
				}

				text = "OHT[" + OHT.ID + "]�� ���� ��� : ";
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

			// ���� �۾��� GoMore�̰� �ٸ� ������ ���� �������(arrived)�̸� OHT.startMoveFlag�� �ʱ�ȭ�ؼ� �ٽ� ��ΰ���Ѵ�
			if (OHT.waitFlag && (OHT.currState.equals("A") || OHT.currState.equals("I"))) {
				OHT.startMoveFlag = false;

				System.out.println("OHT ���� A �Ǵ� I, startMoveFlag : True -> False");
				text = OHT.ID + "�� ������ ����ٰ� �� ��� ��� �� �̵��մϴ�, waitFlag = " + OHT.waitFlag;
				System.out.println(text);
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
			}

		}

		else {// �����ϸ� �ʱ�ȭ
				// �̰� �ΰ� ���ָ� schedule() ������ ���� �� �ְ�, ������ �ѹ� ���� �ʱ�ȭ�Ǽ� �Ʒ� �۾��� ���������� �ϴ°� ������
				// Server.work = "";
				// Server.cmdType = "";

			ConsoleRecv.CmmDestNode = 0;

			if (OHT.destNode != null) {
				text = OHT.destNode.id + "Node�� ����";
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
			}
			if (OHT.startMoveFlag == true) {
				OHT.startMoveFlag = false;
				System.out.println(text + "OHT �������� ����, startMoveFlag : True -> False");
				System.out.println("OHT.currNodeID : " + OHT.currNodeID + "endNodeID : " + endNodeID);
				OHT.TaskCNT++;
			}

			text = "OHT[" + OHT.ID + "]" + "�� Task count : " + OHT.TaskCNT;
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
		if (OHT.totalDist > 60000) { // ���� ��߾��ؼ� ���� �Ÿ��� �˼�����
			OHT.targetNode = OHT.firstNode;
			text = "totalDist : " + OHT.totalDist + "�� ���ذŸ����� ���";
		} else {// ª�� �Ÿ���� �׳� go�� �ѹ��� �̵�
			OHT.targetNode = Location.fineNode(end);
			text = "totalDist : " + OHT.totalDist + "�� ���ذŸ����� ª��";
		}
		MainUI.controller.append(text, "status", OHT);

		text = "OHT[" + OHT.ID + "]: go targetNode : " + OHT.targetNode.id;
		MainUI.controller.append(text, "status", OHT);
		// LogWindow.append(text, "checkPath", OHT.id);

		// �ùķ��̼� �� -> �� beforSendCMD�� ��� �Ǵ��� �����غ����ҵ�
		if (OCS.simulationFlag) {
			/*
			 * OHT.currCmd = "G/" + OHT.ID + "/" + OHT.targetNode.id; for (int i = 0; i
			 * <OHT.totalPathNode.size(); i++) { //��� ��ε� ���� ������ OHT.currCmd = OHT.currCmd +
			 * "/" + OHT.totalPathNode.elementAt(i).id; } OHT.currCmd = OHT.currCmd +
			 * (char)13; //������ ĳ��������
			 */
			OHT.currCmd = "G/" + OHT.ID + "/";
		}
		// �������� ��
		else
			OHT.currCmd = "G/" + OHT.ID + "/"; // + OHT.destNodeID + (char) 13;

	}

	public void GoMore(Vehicle OHT) {
		String text = "";
		for (int i = 0; i < OHT.totalPathNode.size(); i++) {
			if (OHT.destNode.id != OHT.totalPathNode.lastElement().id) { // ���� ���������� �ٰ����°� ���� (n�� �������ε� n��°���� ���Ǹ� n+1��
																			// ���ߵǼ�)
				int nextNodeIndex = OHT.totalPathNode.indexOf(OHT.destNode) + 1;
				OHT.targetNode = OHT.totalPathNode.elementAt(nextNodeIndex); // �����ε��� �������
				OHT.currCmd = "GM/" + OHT.ID + "/"; // + OHT.destNodeID + (char) 13;

				text = "OHT[" + OHT.ID + "].waitflag : " + OHT.waitFlag + ", GoMore �޼ҵ� ����";
				MainUI.controller.append(text, "status", OHT);
				// LogWindow.append(text, "checkPath", OHT.id);
				break;
			}
		}
	}

	// !!!�ϴ� �������� ��ɵ����� ȥ���� �Ǵ°� �������� CMD ��ü ����� ���� ������ �°� cmd[i]�� �����س��� Load()�� ����
	// OHT�� �Ķ���͸� �־������ ���� ���߿� ������ϸ鼭 �Ǵ� -> �����ڶ� ���� OHT��ü ��뿡 ������ ������ �н�
	public void Load(Vehicle OHT, int loadNodeID, String carrierID, int portnum) {
		String text = "";
		if (Check.checkLoad(OHT, loadNodeID)) {
			try {
				text = "OHT[" + OHT.ID + "] loading �� ���� �ð� 0.5�� Delay";
				MainUI.controller.append(text, "status", OHT);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				OHT.currCmd = "L/" + OHT.ID + "/" + carrierID + "/" + portnum; // + (char) 13;
				Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0���� ���� �����ϴϱ�
				client.send(OHT.currCmd);

				// Server.dataOutputStream.write(OHT.currCmd.getBytes());
				OHT.loadID = carrierID;

				text = "Foup[" + OHT.loadID + "]�� ���� ���� Loading ����";
				MainUI.controller.append(text, "status", OHT);

				DataBase.commLog(OHT.ID, ">" + OHT.currCmd);
				DataBase.scheduleLog(OHT.ID, OHT.currCmd);
				OHT.loadID = "Empty";
				OHT.TaskCNT++;
				text = "OHT[" + OHT.ID + "]" + "�� Task count : " + OHT.TaskCNT;
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
				text = "OHT[" + OHT.ID + "] unloading �� ���� �ð� 0.5�� Delay";
				MainUI.controller.append(text, "status", OHT);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				OHT.currCmd = "U/" + OHT.ID + "/" + carrierID + "/" + portnum; // + (char) 13;

				Client client = OCS.clientList.elementAt(OHT.ID - 1); // 0���� ���� �����ϴϱ�
				client.send(OHT.currCmd);

				// Server.dataOutputStream.write(OHT.currCmd.getBytes());
				OHT.loadID = carrierID;

				text = "Foup[" + OHT.loadID + "] �������� Unloading ����";
				MainUI.controller.append(text, "status", OHT);

				DataBase.commLog(OHT.ID, ">" + OHT.currCmd);
				DataBase.scheduleLog(OHT.ID, OHT.currCmd);
				OHT.TaskCNT++;
				text = "OHT[" + OHT.ID + "]" + "�� Task count : " + OHT.TaskCNT;
				MainUI.controller.append(text, "status", OHT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// # 21.04.28 : �����־ ��� Schedule10 -> Schedule1 ����
	// # 21.05.01 : ����OHT 3��(1,5,6) + ����OHT(2,3,4) 3�븦 ���� �̵� ��� ����
	// # 21.05.06 : OHT.loadExist üũ�� ������ �����ϰ� ��� ���޾Ƽ� ���� �۾� ����� �������� �� �ʿ�!
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
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1) // UF ���� �ɾ������ M�����϶� �ȸ�������
				Move(OHT, 407);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 906);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
				Load(OHT, 906, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0) // UF ���� �ɾ������ M�����϶� �ȸ�������
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
			else if (OHT.TaskCNT == 2 && OHT.loadExist == 1) // UF ���� �ɾ������ M�����϶� �ȸ�������
				Move(OHT, 202);
			else if (OHT.TaskCNT == 3 && OHT.loadExist == 1)
				Move(OHT, 903);
			else if (OHT.TaskCNT == 4 && OHT.loadExist == 1)
				Load(OHT, 903, "A", 1);
			else if (OHT.TaskCNT == 5 && OHT.loadExist == 0) // UF ���� �ɾ������ M�����϶� �ȸ�������
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

	// �ùķ��̼ǿ��� load, unload �۾�
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

	// # 1ȣ�� : 604 <-> 906, 2ȣ�� : 903 <-> 905, 3ȣ�� : 807 <-> 904, 4ȣ�� : 404 <-> 907,
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

	// 205 <-> 210 ����
	public void Schedule4(Vehicle OHT) {
		if (OHT.TaskCNT == 0)
			Move(OHT, 210);
		else if (OHT.TaskCNT == 1)
			Move(OHT, 205);
		else if (OHT.TaskCNT >= 2)
			OHT.TaskCNT = 0;
	}

	// # 21.04.27 N���� ������ ���ÿ� ��������
	public String intialSchedule(Vehicle OHT) {
		String scheduleState = "bfStart"; //
		// # 1. ���� ��ġ�ľ�, Load ��ġ �ľ�
		// # 2. �̵���

		return scheduleState;
	}
}