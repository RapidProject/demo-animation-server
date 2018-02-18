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

import eu.project.rapid.common.RapidConstants;
import eu.project.rapid.common.RapidMessages.AnimationMsg;
import marvin.gui.MarvinImagePanel;
import marvin.image.MarvinImage;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A simple Java program, which listens for commands from several clients and updates the scenario
 * based on the commands. Used to show the interaction between the different entities in real-time.
 * To be used for the Rapid demos.
 *
 * @author sokol
 */
public class SecondaryAnimationServer {
    private static final String TAG = "SecondaryAnimationServer";
    private ServerSocket serverSocket;

    private static BlockingQueue<String> commandQueue;

    private static ImageVisualizer imgVisualizer;

    private static final String LABEL_D2D_RECEIVED = "D2D Hello Rx: ";
    private static final String LABEL_D2D_SENT = "D2D Hello Tx: ";
    private static final String LABEL_EXECUTION = "Execution: ";
    private static final String LABEL_DURATION = "Duration: ";
    // private static final String LABEL_ENERGY = "Energy: ";
    private static final String LABEL_DS_STATUS = "DS status: ";
    private static final String LABEL_VMM_STATUS = "VMM status: ";
    private static final String LABEL_VM_STATUS = "VM status: ";
    private static final String LABEL_SLAM_STATUS = "SLAM status: ";
    private static final String LABEL_COMM_TYPE = "Communication type: ";

    private static JLabel labelD2dReceived = new JLabel(LABEL_D2D_RECEIVED);
    private static JLabel labelD2dSent = new JLabel(LABEL_D2D_SENT);
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
    private boolean isD2dOffloading = false;
    private int nrD2dRx = 0;
    private int nrD2dTx = 0;

    private boolean isDsUp = false;
    private boolean isVmmUp = false;
    private boolean isSlamUp = false;

    private SecondaryAnimationServer() {

        commandQueue = new ArrayBlockingQueue<>(1000);

        imgVisualizer = new ImageVisualizer();
        imgVisualizer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initialize();

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

    /**
     * Thread that connects with the primary animation server and waits for messages.
     *
     * @author sokol
     */
    private class PrimaryAnimationReader implements Runnable {

        private Socket primarySocket;
        private PrintWriter out;
        private BufferedReader in;

        @Override
        public void run() {
            try {
                log(TAG, "Started thread that waits for messages from the primary animation server");

                primarySocket = new Socket(RapidConstants.DEFAULT_PRIMARY_ANIMATION_SERVER_IP,
                        RapidConstants.DEFAULT_PRIMARY_ANIMATION_SERVER_PORT);
                out = new PrintWriter(primarySocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(primarySocket.getInputStream()));

                out.println("GET_COMMANDS");
                out.flush();

                isDsUp = in.readLine().equals(String.valueOf(true));
                isSlamUp = in.readLine().equals(String.valueOf(true));
                isVmmUp = in.readLine().equals(String.valueOf(true));

                labelDsStatus.setText(LABEL_DS_STATUS + (isDsUp ? "UP" : "Down"));
                labelSlamStatus.setText(LABEL_SLAM_STATUS + (isSlamUp ? "UP" : "Down"));
                labelVmmStatus.setText(LABEL_VMM_STATUS + (isVmmUp ? "UP" : "Down"));

                // Start heartbeat server
                new Thread(new PrimaryAnimationHeartbeats(primarySocket, out, in)).start();

                String command;
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
     */
    private class PrimaryAnimationHeartbeats implements Runnable {

        private Socket primarySocket;
        private PrintWriter out;
        private BufferedReader in;

        PrimaryAnimationHeartbeats(Socket primaryServerSocket, PrintWriter out,
                                   BufferedReader in) {
            this.primarySocket = primaryServerSocket;
            this.out = out;
            this.in = in;
        }

        @Override
        public void run() {
            log(TAG, "Started thread that sends heartbeat messages to primary animation server");
            while (true) {
                try {
                    log(TAG, "Sending heartbeat message to primary animation server");
                    Thread.sleep(30 * 1000);
                    out.println("PING");
                    out.flush();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private class ClientHandler implements Runnable {

        private static final String TAG = "AnimationClientHandler";
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String command;
                while ((command = in.readLine()) != null) {
                    if (!command.equals("PING")) {
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
                    } catch (IOException ignored) {
                    }
                }
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private class CommandHandler implements Runnable {

        private static final String TAG = "CommandHandler";

        @Override
        public void run() {

            while (true) {

                String command;
                try {
                    log(TAG, "Waiting for commands to be inserted in the queue...");
                    command = commandQueue.take();

                    log(TAG, command);

                    AnimationMsg enumCommand = AnimationMsg.valueOf(command);
                    switch (enumCommand) {
                        // Scenario 1: DS, SLAM, VMM starting up
                        case INITIAL_IMG_0:
                            imgVisualizer.updatePanel(Images.getImage(1));
                            break;
                        case DS_UP:
                            imgVisualizer.updatePanel(Images.getImage(2));
                            labelDsStatus.setText(LABEL_DS_STATUS + "UP");
                            break;
                        case SLAM_UP:
                            imgVisualizer.updatePanel(Images.getImage(3));
                            labelSlamStatus.setText(LABEL_SLAM_STATUS + "UP");
                            break;
                        case SLAM_REGISTER_DS:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(4));
                            break;
                        case VMM_UP:
                            imgVisualizer.updatePanel(Images.getImage(5));
                            labelVmmStatus.setText(LABEL_VMM_STATUS + "UP");
                            break;
                        case VMM_REGISTER_DS:
                            imgVisualizer.updatePanel(Images.getImage(6));
                            break;
                        case VMM_REGISTER_SLAM:
                            imgVisualizer.updatePanel(Images.getImage(7));
                            labelSlamStatus.setText(LABEL_SLAM_STATUS + "UP");
                            labelVmmStatus.setText(LABEL_VMM_STATUS + "UP");
                            break;
                        // Scenario 2:
                        case AC_NEW_REGISTER_DS:
                            imgVisualizer.updatePanel(Images.getImage(9));
                            break;
                        //
                        case DS_NEW_FIND_MACHINES:
                            imgVisualizer.updatePanel(Images.getImage(10));
                            break;
                        case DS_NEW_IP_LIST_AC:
                            imgVisualizer.updatePanel(Images.getImage(11));
                            break;
                        case AC_NEW_REGISTER_SLAM:
                            imgVisualizer.updatePanel(Images.getImage(12));
                            break;
                        case SLAM_NEW_VM_VMM:
                            imgVisualizer.updatePanel(Images.getImage(13));
                            break;
                        case VMM_NEW_START_VM:
                            imgVisualizer.updatePanel(Images.getImage(14));
                            break;
                        case VMM_NEW_REGISTER_AS:
                            imgVisualizer.updatePanel(Images.getImage(15));
                            break;
                        case VMM_NEW_VM_REGISTER_DS:
                            imgVisualizer.updatePanel(Images.getImage(16));
                            break;
                        case VMM_NEW_VM_IP_SLAM:
                            imgVisualizer.updatePanel(Images.getImage(17));
                            break;
                        case SLAM_NEW_VM_IP_AC:
                            imgVisualizer.updatePanel(Images.getImage(18));
                            break;
                        case AC_NEW_REGISTER_VM:
                            imgVisualizer.updatePanel(Images.getImage(19));
                            labelCommType.setText(LABEL_COMM_TYPE + "CLEAR");
                            labelVmStatus.setText(LABEL_VM_STATUS + "UP Connecting...");
                            break;
                        case AC_NEW_CONN_VM:
                            imgVisualizer.updatePanel(Images.getImage(20));
                            break;
                        case AC_NEW_APK_VM:
                            imgVisualizer.updatePanel(Images.getImage(21));
                            break;
                        case AC_NEW_RTT_VM:
                            imgVisualizer.updatePanel(Images.getImage(22));
                            break;
                        case AC_NEW_DL_RATE_VM:
                            imgVisualizer.updatePanel(Images.getImage(23));
                            break;
                        case AC_NEW_UL_RATE_VM:
                            imgVisualizer.updatePanel(Images.getImage(24));
                            break;
                        case AC_NEW_REGISTRATION_OK_VM:
                            imgVisualizer.updatePanel(Images.getImage(25));
                            String temp = labelVmStatus.getText();
                            labelVmStatus.setText(temp.substring(0, temp.lastIndexOf(" ")) + " CONNECTED");
                            labelVmStatus.setBackground(Color.GREEN);
                            break;

                        // Scenario 3:
                        case AC_PREV_REGISTRATION_OK_VM:
                            imgVisualizer.updatePanel(Images.getImage(26));
                            temp = labelVmStatus.getText();
                            labelVmStatus.setText(temp.substring(0, temp.lastIndexOf(" ")) + " CONNECTED");
                            labelVmStatus.setBackground(Color.GREEN);
                            break;
                        case AC_PREV_UL_RATE_VM:
                            imgVisualizer.updatePanel(Images.getImage(27));
                            break;
                        case AC_PREV_DL_RATE_VM:
                            imgVisualizer.updatePanel(Images.getImage(28));
                            break;
                        case AC_PREV_RTT_VM:
                            imgVisualizer.updatePanel(Images.getImage(29));
                            break;
                        case AC_PREV_APK_VM:
                            imgVisualizer.updatePanel(Images.getImage(30));
                            break;
                        //
                        // by CH
                        case AC_PREV_CONN_VM:
                            imgVisualizer.updatePanel(Images.getImage(31));
                            break;
                        case AC_PREV_REGISTER_VM:
                            imgVisualizer.updatePanel(Images.getImage(32));
                            labelCommType.setText(LABEL_COMM_TYPE + "CLEAR");
                            labelVmStatus.setText(LABEL_VM_STATUS + "UP Connecting...");
                            break;
                        case AC_REGISTER_VM_ERROR:
                            imgVisualizer.updatePanel(Images.getImage(51));
                            temp = labelVmStatus.getText();
                            labelVmStatus.setText(temp.substring(0, temp.lastIndexOf(" ")) + " NOT_CONNECTED");
                            labelVmStatus.setBackground(Color.RED);
                            break;
                        case SLAM_PREV_VM_IP_AC:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(33));
                            break;
                        case VMM_PREV_VM_IP_SLAM:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(34));
                            break;
                        case VMM_PREV_FIND_VM:
                            // Thread.sleep(1000);
                            labelVmStatus.setText(LABEL_VM_STATUS + "UP NOT_CONNECTED");
                            imgVisualizer.updatePanel(Images.getImage(35));
                            break;
                        case SLAM_PREV_VM_REQ_VMM:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(36));
                            break;
                        case AC_PREV_REGISTER_SLAM:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(37));
                            break;
                        case DS_PREV_IP_AC:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(38));
                            break;
                        case DS_PREV_FIND_MACHINE:
                            // Thread.sleep(1000);
                            imgVisualizer.updatePanel(Images.getImage(39));
                            break;
                        case AC_PREV_VM_DS:
                            imgVisualizer.updatePanel(Images.getImage(40));
                            break;
                        case AC_INITIAL_IMG:
                            imgVisualizer.updatePanel(Images.getImage(41));
                            // imgVisualizer.updatePanel(Images.getImage(51));
                            labelExecution.setText(LABEL_EXECUTION + "-");
                            labelCommType.setText(LABEL_COMM_TYPE + "-");
                            labelDuration.setText(LABEL_DURATION + "-");
                            break;
                        case AC_OFFLOADING_FINISHED:
                            executing = false;
                            imgVisualizer.updatePanel(Images.getImage(isD2dOffloading ? 52 : 42));
                            isD2dOffloading = false;
                            break;
                        case AS_RESULT_AC:
                            imgVisualizer.updatePanel(Images.getImage(isD2dOffloading ? 53 : 43));
                            break;
                        case AS_RUN_METHOD:
                            imgVisualizer.updatePanel(Images.getImage(isD2dOffloading ? 54 : 44));
                            break;
                        case AC_DECISION_OFFLOAD_AS:
                            imgVisualizer.updatePanel(Images.getImage(isD2dOffloading ? 55 : 45));

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
                                        } catch (InterruptedException ignored) {
                                        }
                                    }
                                }
                            }.start();

                            labelExecution
                                    .setText(LABEL_EXECUTION + (isD2dOffloading ? "D2D OFFLOAD" : "OFFLOAD"));
                            labelCommType.setText(LABEL_COMM_TYPE + "CLEAR");
                            break;
                        case AC_PREPARE_DATA:
                            imgVisualizer.updatePanel(Images.getImage(46));
                            break;
                        case AC_LOCAL_FINISHED:
                            executing = false;
                            imgVisualizer.updatePanel(Images.getImage(48));
                            break;
                        case AC_DECISION_LOCAL:
                            imgVisualizer.updatePanel(Images.getImage(49));
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
                                        } catch (InterruptedException ignored) {
                                        }
                                    }
                                }
                            }.start();
                            labelExecution.setText(LABEL_EXECUTION + "LOCAL");
                            break;
                        // case AC_OFFLOADING_FINISHED_D2D:
                        // isD2dOffloading = false;
                        // executing = false;
                        // imgVisualizer.updatePanel(Images.getImage(52));
                        // break;
                        // case AS_RESULT_AC_D2D:
                        // imgVisualizer.updatePanel(Images.getImage(53));
                        // break;
                        // case AS_RUN_METHOD_D2D:
                        // imgVisualizer.updatePanel(Images.getImage(54));
                        // break;
                        case AC_OFFLOAD_D2D:
                            isD2dOffloading = true;
                            // imgVisualizer.updatePanel(Images.getImage(55));
                            // labelExecution.setText(LABEL_EXECUTION + "D2D OFFLOAD");
                            // labelCommType.setText(LABEL_COMM_TYPE + "CLEAR");
                            break;
                        // case AC_PREPARE_DATA_D2D:
                        // imgVisualizer.updatePanel(Images.getImage(56));
                        // break;
                        // case D2D_INITIAL_IMG:
                        // imgVisualizer.updatePanel(Images.getImage(57));
                        // labelExecution.setText(LABEL_EXECUTION + "-");
                        // break;
                        case AC_RECEIVED_D2D:
                            // imgVisualizer.updatePanel(Images.getImage(58));
                            // labelD2dReceived.setText(LABEL_D2D_RECEIVED + (++nrD2dRx));
                            labelD2dReceived.setText(LABEL_D2D_RECEIVED + "OK");
                            break;
                        case AC_NO_MORE_D2D:
                            isD2dOffloading = false;
                            // imgVisualizer.updatePanel(Images.getImage(58));
                            // labelD2dReceived.setText(LABEL_D2D_RECEIVED + (++nrD2dRx));
                            labelD2dReceived.setText(LABEL_D2D_RECEIVED + "-");
                            break;
                        case AS_BROADCASTING_D2D:
                            // imgVisualizer.updatePanel(Images.getImage(59));
                            labelD2dSent.setText(LABEL_D2D_SENT + (++nrD2dTx));
                            break;
                        case AC_LISTENING_D2D:
                            labelD2dReceived.setText(LABEL_D2D_RECEIVED + "listening...");
                            // imgVisualizer.updatePanel(Images.getImage(60));
                            break;
                        default:
                            break;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    log(TAG, "Excepion thrown: " + e);
                }
            }
        }
    }

    private void initialize() {
        imgVisualizer.updatePanel(Images.getImage(1));
        labelD2dReceived.setText(LABEL_D2D_RECEIVED + "-");
        labelD2dSent.setText(LABEL_D2D_SENT + 0);
        labelDsStatus.setText(LABEL_DS_STATUS + "Down");
        labelVmmStatus.setText(LABEL_VMM_STATUS + "Down");
        labelVmStatus.setText(LABEL_VM_STATUS + "Unknown");
        labelSlamStatus.setText(LABEL_SLAM_STATUS + "Down");

        labelExecution.setText(LABEL_EXECUTION + "-");
        labelDuration.setText(LABEL_DURATION + "-");
        labelCommType.setText(LABEL_COMM_TYPE + "-");
    }

    private class ImageVisualizer extends JFrame {

        private static final long serialVersionUID = -5697368399955840121L;
        private MarvinImagePanel imagePanel;
        private JPanel infoPanel;

        ImageVisualizer() {
            super("RAPID Demo");

            imagePanel = new MarvinImagePanel();
            // imagePanel.setImage(Images.im_start_0);

            infoPanel = new JPanel();
            infoPanel.setLayout(new GridLayout(5, 2, 5, 5));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 70, 10, 5));
            infoPanel.add(labelD2dReceived);
            infoPanel.add(labelD2dSent);
            infoPanel.add(labelDsStatus);
            infoPanel.add(labelExecution);
            infoPanel.add(labelSlamStatus);
            infoPanel.add(labelDuration);
            infoPanel.add(labelVmmStatus);
            // infoPanel.add(label_energy);
            infoPanel.add(labelCommType);
            infoPanel.add(labelVmStatus);
            infoPanel.add(new JLabel());

            // Container
            Container con = getContentPane();
            con.setLayout(new BorderLayout());
            con.add(imagePanel, BorderLayout.NORTH);
            con.add(infoPanel, BorderLayout.SOUTH);

            setSize(730, 770);
            setResizable(false);
            setVisible(true);
        }

        void updatePanel(MarvinImage image) {
            imagePanel.setImage(image);
        }

    }

    private void log(String tag, String msg) {
        System.out.println("[" + tag + "]: " + msg);
    }

    public static void main(String[] args) {
        new SecondaryAnimationServer();
    }
}
