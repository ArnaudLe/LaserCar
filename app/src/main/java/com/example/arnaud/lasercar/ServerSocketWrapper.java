package com.example.arnaud.lasercar;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketWrapper
{
    /* ================================================================================ */
    /* =========================== DECLARATION ATTRIBUTS ============================== */
    /* ================================================================================ */
    // Attributs réception de données RPI
    public String TARGET_HIT_RECEIVE = "lol";
    public String MYSELF_HIT_RECEIVE = "Vous avez été touché";
    public String TARGET_HIT_SEND = "Ok lol";
    public String MYSELF_HIT_SEND = "Ok j'ai été touché";
    public String UNKNOWN_COMMAND_SEND = "Je ne comprends pas";

    private String command;
    private boolean flagReceiveData = false;

    private ServerSocket serverSocket = null;
    private Thread serverSocketThread;

    /**
     * Method to start thread running socket functionality
     */
    public void startSocket()
    {
        serverSocketThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Log.i("TrackingFlow", "Server socket is ready and listening...");
                Socket socket = null;
                try {
                    serverSocket = new ServerSocket(40450);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (!Thread.currentThread().isInterrupted())
                {
                    try {
                        socket = serverSocket.accept();

                        InputStream is = socket.getInputStream();
                        int lockSeconds = 5;
                        command = readMessageFromClientLockingThread(is, lockSeconds);
                        flagReceiveData = true; // On a reçu une donnée
                        String messageResponse = processCommand(command);
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                        out.println(messageResponse);
                        out.flush();
                        is.close();
                        out.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serverSocket != null)
                {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {e.printStackTrace();}
                }
            }
        });
        serverSocketThread.start();
    }

    /**
     * This method reads the data available in the client buffer or
     * waits the seconds specified in lockSeconds until there's data
     * available...
     * @param is
     * @param lockSeconds
     * @return data from client as String
     */
    private String readMessageFromClientLockingThread(InputStream is, int lockSeconds) throws IOException
    {
        lockSeconds *= 1000;//Convert to ms...
		/*
		 * This code locks the thread until there's information available in the
		 * client's output buffer OR it's been lockSeconds with no info...
		 */
        long lockThreadCheckpoint = System.currentTimeMillis();
        int availableBytes = is.available();
        while(availableBytes < 1 && (System.currentTimeMillis() < lockThreadCheckpoint + lockSeconds))
        {
            try{Thread.sleep(10);}catch(InterruptedException ie){ie.printStackTrace();}
            availableBytes = is.available();
        }

		/*
		 * Create a byte array of the size of the data available in the client buffer.
		 * As good practice, big data is supposed to be chopped in smaller parts, so for
		 * this example we will not assume any buffer size and we will, make a buffer
		 * of the size of the actual data.(Maximum socket buffer size is OS dependent).
		 */
        byte[] buffer = new byte[availableBytes];
        is.read(buffer, 0, availableBytes);
        return new String(buffer);
    }

    /**
     * This method will be used as command processing,
     * based on the command received will return a different
     * response back to the client...
     * @param command
     *
     */
    private String processCommand(String command)
    {
        if(TARGET_HIT_RECEIVE.equals(command))
        {
            return TARGET_HIT_SEND;
        }
        else
        {
            if (MYSELF_HIT_RECEIVE.equals(command))
            {
                return MYSELF_HIT_SEND;
            }
            return UNKNOWN_COMMAND_SEND;
        }
    }

    /**
     * Method to stop thread running socket functionality
     */
    public void stopSocket()
    {
        serverSocketThread.interrupt();
        if(serverSocket != null)
        {
            try {
                serverSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    // Accesseurs
    public String getData(){return command;}
    public boolean getFlagReceiveData(){return flagReceiveData;}
    public void setFlagReceiveData(boolean v) {this.flagReceiveData = v;}

} // Fin class ServerSocketWrapper
