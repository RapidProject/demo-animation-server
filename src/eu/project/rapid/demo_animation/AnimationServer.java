/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *******************************************************************************/

package eu.project.rapid.demo_animation;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import eu.project.rapid.common.RapidMessages;
import marvin.gui.MarvinImagePanel;
import marvin.image.MarvinImage;

/**
 * A simple Java program, which listens for commands from several clients and updates the scenario
 * based on the commands. Used to show the interaction between the different entities in real-time.
 * 
 * @author sokol
 *
 */
public class AnimationServer {
  private static final String TAG = "AnimationServer";
  private ServerSocket serverSocket;
  private static final int port = 6666;

  private static BlockingQueue<String> commandQueue;

  private static ImageVisualizer imgVisualizer;

  private static final String LABEL_EXECUTION = "Execution: ";
  private static final String LABEL_DURATION = "Duration: ";
  // private static final String LABEL_ENERGY = "Energy: ";
  private static final String LABEL_DS_STATUS = "DS status: ";
  private static final String LABEL_VMM_STATUS = "VMM status: ";
  private static final String LABEL_VM_STATUS = "VM status: ";
  private static final String LABEL_SLAM_STATUS = "SLAM status: ";
  private static final String LABEL_COMM_TYPE = "Communication type: ";

  private static JLabel labelExecution = new JLabel(LABEL_EXECUTION);
  private static JLabel labelDuration = new JLabel(LABEL_DURATION);
  // private static JLabel label_energy = new JLabel(LABEL_ENERGY);
  private static JLabel labelDsStatus = new JLabel(LABEL_DS_STATUS);
  private static JLabel labelVmmStatus = new JLabel(LABEL_VMM_STATUS);
  private static JLabel labelVmStatus = new JLabel(LABEL_VM_STATUS);
  private static JLabel labelSlamStatus = new JLabel(LABEL_SLAM_STATUS);
  private static JLabel labelCommType = new JLabel(LABEL_COMM_TYPE);

  private boolean executing = false;
  private long startTime = 0;
  private double totalTime = 0;

  public AnimationServer() {

    commandQueue = new ArrayBlockingQueue<String>(1000);

    imgVisualizer = new ImageVisualizer();
    imgVisualizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


    new Thread(new CommandHandler()).start();

    try {
      serverSocket = new ServerSocket(port);
      log(TAG, "Waiting for connections on port " + port);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(new ClientHandler(clientSocket)).start();
      }

    } catch (IOException e) {
    }
  }

  private class ClientHandler implements Runnable {

    private static final String TAG = "AnimationClientHandler";
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

      try {
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String command = null;
        while ((command = in.readLine()) != null) {

          if (command.equals("PING")) {
            continue;
          } else {
            try {
              commandQueue.put(command);
              out.println("0");
              log(TAG, "Inserted command: " + command);
            } catch (InterruptedException e) {
              System.err.println("InterruptedException inserting command: " + e);
            }
          }
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        // e.printStackTrace();
      } finally {
        if (out != null)
          out.close();
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
          }
        }
        if (clientSocket != null) {
          try {
            clientSocket.close();
          } catch (IOException e) {
          }
        }
      }
    }
  }

  private class CommandHandler implements Runnable {

    private static final String TAG = "CommandHandler";

    @Override
    public void run() {

      initialize();
      boolean prev = false;

      while (true) {

        String command;
        try {
          // log(TAG, "Waiting for command...");
          command = commandQueue.take();

          log(TAG, command);

          switch (command) {
            case RapidMessages.DS_UP:
              imgVisualizer.updatePanel(Images.im_ds_up_0);
              labelDsStatus.setText(LABEL_DS_STATUS + "Up");
              break;

            // The VMM is started, registers with the DS and starts two VMs
            case RapidMessages.VMM_UP:
              imgVisualizer.updatePanel(Images.im_vmm_register_0);
              labelVmmStatus.setText(LABEL_VMM_STATUS + "Up");
              break;
            case RapidMessages.VMM_REGISTER_DS:
              imgVisualizer.updatePanel(Images.im_vmm_register_1);
              break;
            case RapidMessages.VMM_REGISTER_DS_OK:
              imgVisualizer.updatePanel(Images.im_vmm_register_2);
              break;
            case RapidMessages.VMM_START_TWO_VMS:
              imgVisualizer.updatePanel(Images.im_vmm_register_3);
              break;
            case RapidMessages.VMM_VM1_STARTED:
              imgVisualizer.updatePanel(Images.im_vmm_register_4);
              labelVmStatus.setText(LABEL_VM_STATUS + "Up");
              break;
            case RapidMessages.VMM_VM2_STARTED:
              imgVisualizer.updatePanel(Images.im_vmm_register_5);
              break;

            // The Acceleration Client (AC) on the User Device (UD) registers as a NEW device with
            // the DS and the VMM and asks for a VM
            case RapidMessages.AC_REGISTER_DS_NEW:
              prev = false;
              imgVisualizer.updatePanel(Images.getUdRegisterImage(1, prev));
              break;
            case RapidMessages.DS_FIND_AVAILABLE_VMM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(2, prev));
              break;
            case RapidMessages.AC_REGISTER_DS_NEW_OK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(3, prev));
              break;
            case RapidMessages.AC_REGISTER_VMM_NEW:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(4, prev));
              break;
            case RapidMessages.VMM_FIND_AVAILABLE_VM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(5, prev));
              break;
            case RapidMessages.AC_REGISTER_VMM_NEW_OK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(6, prev));
              break;

            // The Acceleration Client (AC) on the User Device (UD) registers as a PREV device
            // with the DS and the VMM and asks for a VM
            case RapidMessages.AC_REGISTER_DS_PREV:
              prev = true;
              imgVisualizer.updatePanel(Images.getUdRegisterImage(1, prev));
              break;
            case RapidMessages.DS_FIND_PREV_VMM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(2, prev));
              break;
            case RapidMessages.AC_REGISTER_DS_PREV_OK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(3, prev));
              break;
            case RapidMessages.AC_REGISTER_VMM_PREV:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(4, prev));
              break;
            case RapidMessages.VMM_FIND_PREV_VM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(5, prev));
              break;
            case RapidMessages.AC_REGISTER_VMM_PREV_OK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(6, prev));
              break;

            // The Acceleration Client (AC) on the User Device (UD) registers with the AS on the VM
            // The registration phase goes through, connection, RTT, bandwidth measurements, etc.
            case RapidMessages.AC_REGISTER_VM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(7, prev));
              labelCommType.setText(LABEL_COMM_TYPE + "Clear");
              labelVmStatus.setText(LABEL_VM_STATUS + "Up, Connected");
              break;
            case RapidMessages.AC_CONNECT_VM:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(8, prev));
              break;
            case RapidMessages.AC_SEND_APK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(9, prev));
              break;
            case RapidMessages.AC_RTT_MEASUREMENT:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(10, prev));
              break;
            case RapidMessages.AC_DL_MEASUREMENT:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(11, prev));
              break;
            case RapidMessages.AC_UL_MEASUREMENT:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(12, prev));
              break;
            case RapidMessages.AC_REGISTER_VM_OK:
              imgVisualizer.updatePanel(Images.getUdRegisterImage(13, prev));
              break;
            case RapidMessages.AC_DISCONNECT_VM:
              imgVisualizer.updatePanel(Images.im_start_1);
              labelVmStatus.setText(LABEL_VM_STATUS + "Up, Disconnected");
              labelDuration.setText(LABEL_DURATION + "-");
              labelCommType.setText(LABEL_COMM_TYPE + "-");
              labelExecution.setText(LABEL_EXECUTION + "-");
              break;

            // The Virus Scanning is performed locally
            case RapidMessages.AC_DECISION_LOCAL:
              startTime = System.currentTimeMillis();
              executing = true;
              new Thread() {
                public void run() {
                  while (executing) {
                    totalTime = (System.currentTimeMillis() - startTime) / 1000.0;
                    labelDuration.setText(
                        LABEL_DURATION + new DecimalFormat("#.##").format(totalTime) + "s");
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                  }
                }
              }.start();

              labelExecution.setText(LABEL_EXECUTION + "Local");
              break;
            case RapidMessages.AC_PREPARE_DATA:
              imgVisualizer.updatePanel(Images.im_virus_local_0);
              break;
            case RapidMessages.AC_EXEC_LOCAL:
              imgVisualizer.updatePanel(Images.im_virus_local_1);
              break;
            case RapidMessages.AC_FINISHED_LOCAL:
              executing = false;
              imgVisualizer.updatePanel(Images.im_virus_local_2);
              labelExecution.setText(LABEL_EXECUTION + "Local Finished!");
              break;

            // The Virus Scanning is performed remotely
            case RapidMessages.AC_DECISION_REMOTE:
              executing = true;
              startTime = System.currentTimeMillis();
              new Thread() {
                public void run() {
                  while (executing) {
                    totalTime = (System.currentTimeMillis() - startTime) / 1000.0;
                    labelDuration.setText(
                        LABEL_DURATION + new DecimalFormat("#.##").format(totalTime) + "s");
                    try {
                      Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                  }
                }
              }.start();
              labelExecution.setText(LABEL_EXECUTION + "Remote");
              break;
            case RapidMessages.AC_REMOTE_SEND_DATA:
              imgVisualizer.updatePanel(Images.im_virus_offload_1);
              break;
            case RapidMessages.AC_EXEC_REMOTE:
              imgVisualizer.updatePanel(Images.im_virus_offload_2);
              break;
            case RapidMessages.AC_RESULT_REMOTE:
              imgVisualizer.updatePanel(Images.im_virus_offload_3);
              break;
            case RapidMessages.AC_FINISHED_REMOTE:
              executing = false;
              imgVisualizer.updatePanel(Images.im_virus_offload_4);
              labelExecution.setText(LABEL_EXECUTION + "Remote Finished!");
              break;
          }

        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }


    private void initialize() {
      imgVisualizer.updatePanel(Images.im_start_0);
      labelDsStatus.setText(LABEL_DS_STATUS + "Down");
      labelVmmStatus.setText(LABEL_VMM_STATUS + "Down");
      labelVmStatus.setText(LABEL_VM_STATUS + "Down");
      labelSlamStatus.setText(LABEL_SLAM_STATUS + "TODO");

      labelExecution.setText(LABEL_EXECUTION + "-");
      labelDuration.setText(LABEL_DURATION + "-");
      labelCommType.setText(LABEL_COMM_TYPE + "-");
    }
  }


  private class ImageVisualizer extends JFrame {

    private static final long serialVersionUID = -5697368399955840121L;
    private MarvinImagePanel imagePanel;
    private JPanel infoPanel;

    public ImageVisualizer() {
      super("RAPID Demo");

      imagePanel = new MarvinImagePanel();
      imagePanel.setImage(Images.im_start_0);

      infoPanel = new JPanel();
      infoPanel.setLayout(new GridLayout(5, 2, 5, 5));
      infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 70, 5, 5));
      infoPanel.add(labelDsStatus);
      infoPanel.add(labelExecution);
      infoPanel.add(labelVmmStatus);
      infoPanel.add(labelDuration);
      infoPanel.add(labelVmStatus);
      // infoPanel.add(label_energy);
      infoPanel.add(labelCommType);
      infoPanel.add(labelSlamStatus);
      infoPanel.add(new JLabel());

      // Container
      Container con = getContentPane();
      con.setLayout(new BorderLayout());
      con.add(imagePanel, BorderLayout.NORTH);
      con.add(infoPanel, BorderLayout.SOUTH);

      setSize(700, 750);
      setResizable(false);
      setVisible(true);
    }

    public void updatePanel(MarvinImage image) {
      imagePanel.setImage(image);
    }

  }

  private void log(String tag, String msg) {
    System.out.println("[" + tag + "]: " + msg);
  }

  public static void main(String[] args) {
    new AnimationServer();
  }
}
