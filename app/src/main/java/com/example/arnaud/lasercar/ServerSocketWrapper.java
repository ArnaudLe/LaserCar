package com.example.arnaud.lasercar;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketWrapper
{
    /* ================================================================================ */
    /* =========================== DECLARATION ATTRIBUTS ============================== */
    /* ================================================================================ */
    // Attributs réception de données RPI
    private String command = "";
    private boolean flagReceiveData = false;
    public volatile boolean isRunning = true;

    private ServerSocket serverSocket = null;
    private Thread serverSocketThread;

    // Méthode pour démarer le thread de socket
    public void startSocket()
    {
        serverSocketThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Socket socket = null;
                try {
                    serverSocket = new ServerSocket(40450);
                    Log.d("MyTag", "Création du serveur");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (!Thread.currentThread().isInterrupted() && isRunning)
                {
                    try {
                        socket = serverSocket.accept();
                        Log.d("MyTag", "serverSocket.accept()");

                        while (PlayActivity.flagPlayActivity)
                        {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                            byte[] buffer = new byte[1024];

                            int bytesRead;
                            InputStream inputStream = socket.getInputStream();

                            //notice: inputStream.read() will block if no data return
                            while ((bytesRead = inputStream.read(buffer)) != -1)
                            {
                                byteArrayOutputStream.write(buffer, 0, bytesRead);
                                command = byteArrayOutputStream.toString("UTF-8");
                                byteArrayOutputStream.reset();
                            }
                        }
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

    public void stopSocket() throws IOException
    {
        isRunning  = false;
        serverSocket.close();
    }

    // Accesseurs et mutateurs
    public String getData(){return command;}
    public void setData(String s){this.command = s;}
    public boolean getIsRunning(){return isRunning;}

} // Fin class ServerSocketWrapper
