/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *******************************************************************************/

package eu.project.rapid.demo_animation;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import eu.project.rapid.common.RapidConstants;
import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidMessages.AnimationMsg;
import eu.project.rapid.common.RapidUtils;
import marvin.gui.MarvinImagePanel;
import marvin.image.MarvinImage;

/**
 * A simple Java program, which listens for commands from several clients and updates the scenario
 * based on the commands. Used to show the interaction between the different entities in real-time.
 * To be used for the Rapid demos.
 * 
 * @author sokol
 *
 */
public class AnimationServer {
  private static final String TAG = "AnimationServer";
  private ServerSocket serverSocket;

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

    // Connect with the DS and register so that the other components can get the IP
    // registerWithDs();

    // Start thread that consumes commands
    new Thread(new CommandHandler()).start();

    // Start thread that connects to the primary animation server and retrieves messages from there
    // to show here.
    new Thread(new PrimaryAnimationReader()).start();

    // Then start also listening for connections from components that don't need to go through the
    // primary animation server
    try {
      serverSocket = new ServerSocket(RapidConstants.DEFAULT_SECONDARY_ANIMATION_SERVER_PORT);
      log(TAG, "Waiting for connections on port "
          + RapidConstants.DEFAULT_SECONDARY_ANIMATION_SERVER_PORT);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(new ClientHandler(clientSocket)).start();
      }
    } catch (IOException e) {
      log(TAG, "Error while waiting for connections: " + e);
    }
  }

  private void registerWithDs() {
    String myIp = RapidUtils.getVmIpLinux();
    System.err.println("Sending my ip to DS, myIp: " + myIp);

    Socket socket = null;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      System.err.println(String.format("Connecting with DS %s:%d", RapidConstants.DEFAULT_DS_IP,
          RapidConstants.DEFAULT_DS_PORT));
      socket = new Socket(RapidConstants.DEFAULT_DS_IP, RapidConstants.DEFAULT_DS_PORT);
      oos = new ObjectOutputStream(socket.getOutputStream());
      ois = new ObjectInputStream(socket.getInputStream());

      oos.writeByte(RapidMessages.DEMO_SERVER_REGISTER_DS);
      oos.writeUTF(myIp);
      oos.flush();
    } catch (UnknownHostException e) {
      System.err.println("Could not register with DS: " + e);
    } catch (IOException e) {
      System.err.println("Could not register with DS: " + e);
      e.printStackTrace();
    } finally {
      RapidUtils.closeQuietly(ois);
      RapidUtils.closeQuietly(oos);
      RapidUtils.closeQuietly(socket);
    }
  }

  /**
   * Thread that connects with the primary animation server and waits for messages.
   * 
   * @author sokol
   *
   */
  private class PrimaryAnimationReader implements Runnable {

    private Socket primaryServerSocket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void run() {
      try {
        log(TAG, "Started thread that waits for messages from the primary animation server");

        primaryServerSocket = new Socket(RapidConstants.DEFAULT_PRIMARY_ANIMATION_SERVER_IP,
            RapidConstants.DEFAULT_PRIMARY_ANIMATION_SERVER_PORT);
        out = new PrintWriter(primaryServerSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(primaryServerSocket.getInputStream()));

        out.println("GET_COMMANDS");
        out.flush();

        // Start heartbeat server
        new Thread(new PrimaryAnimationHeartbeats(primaryServerSocket, out, in)).start();

        String command = null;
        while ((command = in.readLine()) != null) {
          log(TAG, "Command from primary animation server: " + command);
          try {
            commandQueue.put(command);
          } catch (InterruptedException e) {
            log(TAG, "Could not insert command on blocking queue: " + e);
          }
        }
      } catch (IOException e) {
        log(TAG, "Could not connect with primary animation server: " + e);
      }
    }
  }

  /**
   * Thread that sends heartbeat messages every ten seconds to the primary server so to keep the
   * connection alive.
   * 
   * @author sokol
   *
   */
  private class PrimaryAnimationHeartbeats implements Runnable {

    private Socket primaryServerSocket;
    private PrintWriter out;
    private BufferedReader in;

    public PrimaryAnimationHeartbeats(Socket primaryServerSocket, PrintWriter out,
        BufferedReader in) {
      this.primaryServerSocket = primaryServerSocket;
      this.out = out;
      this.in = in;
    }

    @Override
    public void run() {
      log(TAG, "Started thread that sends heartbeat messages to primary animation server");
      while (true) {
        try {
          log(TAG, "Sending heartbeat message to primary animation server");
          Thread.sleep(10 * 1000);
          out.println("PING");
          out.flush();
        } catch (InterruptedException e) {
        }
      }
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
          log(TAG, "Waiting for commands to be inserted in the queue...");
          command = commandQueue.take();

          log(TAG, command);

          AnimationMsg enumCommand = AnimationMsg.valueOf(command);
          switch (enumCommand) {
            // The Virus Scanning is performed locally
            case AC_DECISION_LOCAL:
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

            default:
              break;

            // The Virus Scanning is performed remotely
          }

        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }


    private void initialize() {
      imgVisualizer.updatePanel(Images.getImage(1));
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
      // imagePanel.setImage(Images.im_start_0);

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
