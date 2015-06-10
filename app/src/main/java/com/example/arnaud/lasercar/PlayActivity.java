package com.example.arnaud.lasercar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Handler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class PlayActivity extends Activity implements SensorEventListener
{
    /* ================================================================================ */
    /* =========================== DECLARATION ATTRIBUTS ============================== */
    /* ================================================================================ */
    // Attributs accéléromètre
    private Sensor accelerometer;
    private SensorManager sm;
    private float xAngle = 0;
    private float yAngle = 0;
    private float zAngle = 0;
    // Attributs vitesse
    private ImageButton ibAvancer;
    private ImageButton ibReculer;
    private TextView tvVitesse;
    private Handler handlerVitesse = new Handler();
    private boolean mAutoIncrement = false; // indique moteur état Avancer ou Reculer
    private boolean mAutoDecrement = false;
    private int mVitesse;
    private static int DELAY = 25;
    // Attributs laser
    private Handler handlerLaser = new Handler();
    private ImageButton ibLaser;
    private boolean lAutoIncrement = false; // indique laser état Tirer ou non
    private int timeLaser;
    // Attributs mise en forme des données
    private TextView tvLaser;
    // Attribus connexion RPI et envoi de données
    public Socket mySocket = null;
    public static final int SERVERPORT = 40450;
    public static final String SERVER_IP = "10.5.5.1";
    public static boolean flagPlayActivity;
    // Attributs divers
    private TextView tvTest;
    private TextView tvPseudo;
    private TextView tvScore;
    private TextView tvInfo;

    // Thread qui s'exécute en parallèle : gère la vitesse
    class RptUpdaterVitesse implements Runnable
    {
        public void run()
        {
            // Appuie sur avancer OU Relache reculer
            if((mVitesse >= 0 && mAutoIncrement) || (mVitesse < 0 && !mAutoDecrement))
            {
                if(mVitesse < 100)
                {
                    increment("vitesse");
                    handlerVitesse.postDelayed(new RptUpdaterVitesse(), DELAY);
                }
                else handlerVitesse.postDelayed(new RptUpdaterVitesse(), DELAY);
            }

            // Relache avancer OU Appuie reculer
            else if((mVitesse > 0 && !mAutoIncrement) || (mVitesse <= 0 && mAutoDecrement))
            {
                if(mVitesse > -100)
                {
                    decrement("vitesse");
                    handlerVitesse.postDelayed(new RptUpdaterVitesse(), DELAY);
                }
                else handlerVitesse.postDelayed(new RptUpdaterVitesse(), DELAY);
            }
        }
    } // Fin thread vitesse

    // Thread qui s'exécute en parallèle : gère le laser
    class RptUpdaterLaser implements Runnable
    {
        public void run()
        {
            // Appuie sur tirer
            if(lAutoIncrement)
            {
                if(timeLaser < 2000/DELAY) // Stop à 2 secondes
                {
                    increment("laser");
                    handlerLaser.postDelayed(new RptUpdaterLaser(), DELAY);
                }
                else handlerLaser.postDelayed(new RptUpdaterLaser(), DELAY);
            }

            // Relache tirer
            else
            {
                if(timeLaser > 0)  // Stop à 0 secondes
                {
                    decrement("laser");
                    handlerLaser.postDelayed(new RptUpdaterLaser(), DELAY);
                }
                else if(timeLaser > 0 && timeLaser < 100) // Exclu le cas où timeLaser == 0
                {
                    handlerLaser.postDelayed(new RptUpdaterLaser(), DELAY);
                }
            }
            sendLaser(); // envoi données laser tant que l'on reste appuyé sur le bouton laser
        }
    } // Fin thread laser


    // Redéfinition de la fonction principale
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        /* ================================================================================ */
        /* =========================== INSTANCIATION VARIABLES ============================ */
        /* ================================================================================ */
        // design
        Typeface abolition = Typeface.createFromAsset(getAssets(), "Abolition Regular.otf");
        // accéléromètre
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // vitesse
        tvVitesse = (TextView) findViewById(R.id.tv_vitesse);
        ibAvancer = (ImageButton) findViewById(R.id.ib_avancer);
        ibReculer = (ImageButton) findViewById(R.id.ib_reculer);
        // laser
        ibLaser = (ImageButton) findViewById(R.id.ib_laser);
        // envoi de données
        tvLaser = (TextView) findViewById(R.id.tv_laser);
        // autre
        tvTest = (TextView) findViewById(R.id.tv_test);
        tvPseudo = (TextView) findViewById(R.id.tv_pseudo); tvPseudo.setTypeface(abolition);
        tvScore = (TextView) findViewById(R.id.tv_score); tvScore.setTypeface(abolition);
        tvInfo = (TextView) findViewById(R.id.tv_info); tvInfo.setTypeface(abolition);


        /* ================================================================================ */
        /* ================= RECEPTION DONNEES DE L'ACTIVITE GAMESETTINGS ================= */
        /* ================================================================================ */
        Intent gameSettingsIntent = getIntent();
        // Réception du pseudo
        String data_pseudo = gameSettingsIntent.getStringExtra("message_pseudo"); // public String getStringExtra (String name)
        tvPseudo.setText(data_pseudo);
        // Réception du nombre de joueurs
        String data_player = gameSettingsIntent.getStringExtra("message_player");
        //tvTest.setText(data_player);
        // Réception du nombre de joueurs
        String data_time = gameSettingsIntent.getStringExtra("message_time");
        //tvTest.setText(data_time);

        /* ================================================================================ */
        /* =============== CONNEXION RASPBERRY ET ENVOIE DE DONNEES VITESSE =============== */
        /* ================================================================================ */
        connectrpi();
        flagPlayActivity = true;

        /* ================================================================================ */
        /* ============================ GESTION BOUTONS VITESSE =========================== */
        /* ================================================================================ */
        // Bouton Avancer
        ibAvancer.setOnLongClickListener
                (
                        new View.OnLongClickListener()
                        {
                            public boolean onLongClick(View arg0)
                            {
                                mAutoIncrement = true;
                                handlerVitesse.post(new RptUpdaterVitesse());
                                return false;
                            }
                        }
                );
        ibAvancer.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoIncrement)
                                {
                                    mAutoIncrement = false;
                                }
                                return false;
                            }
                        }
                );

        // Bouton Reculer
        ibReculer.setOnLongClickListener
                (
                        new View.OnLongClickListener()
                        {
                            public boolean onLongClick(View arg0)
                            {
                                mAutoDecrement = true;
                                handlerVitesse.post(new RptUpdaterVitesse());
                                return false;
                            }
                        }
                );
        ibReculer.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoDecrement)
                                {
                                    mAutoDecrement = false;
                                }
                                return false;
                            }
                        }
                );

        /* ================================================================================ */
        /* ============================= GESTION BOUTON LASER ============================= */
        /* ================================================================================ */
        ibLaser.setOnLongClickListener
                (
                        new View.OnLongClickListener()
                        {
                            public boolean onLongClick(View arg0)
                            {
                                lAutoIncrement = true;
                                handlerLaser.post(new RptUpdaterLaser());
                                return false;
                            }
                        }
                );
        ibLaser.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && lAutoIncrement)
                                {
                                    lAutoIncrement = false;
                                }
                                return false;
                            }
                        }
                );
    } // Fin onCreate : fonction principale

    // Redéfinition nécessaire pour pouvoir utiliser l'accéléromètre
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    /* ================================================================================ */
    /* ========= GESTION INCREMENTATION ET DECREMENTATION (VITESSE + LASER) =========== */
    /* ================================================================================ */
    // Fonctions gestion incrémentation
    public void increment(String s)
    {
        if(s.equals("vitesse"))
        {
            mVitesse++;
            tvVitesse.setText("Vitesse : " + mVitesse + "%");
        }
        else if(s.equals("laser"))
        {
            timeLaser++;
            tvLaser.setText("Laser : " + timeLaser + "s");
        }
    }
    // Fonctions gestion décrémentation
    public void decrement(String s)
    {
        if(s.equals("vitesse"))
        {
            mVitesse--;
            tvVitesse.setText("Vitesse : " + mVitesse + "%");
        }
        else if(s.equals("laser"))
        {
            timeLaser--;
            tvLaser.setText("Laser : " + timeLaser + "s");
        }
    }

    /* ================================================================================ */
    /* ============================ GESTION DE L'ACCELEROMETRE ======================== */
    /* ================================================================================ */
    // Met à jour les coordonnées XYZ uniquement sur changement sensor
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        accelerometerToDegrees(x, y, z);
    }

    // Conversion float (event) en degrés
    public void accelerometerToDegrees(float x, float y, float z)
    {
        xAngle = (float) Math.atan(x / (Math.sqrt(Math.sqrt(y) + Math.sqrt(z))));
        yAngle = (float) Math.atan(y / (Math.sqrt(x*x + z*z)));
        zAngle = (float) Math.atan(Math.sqrt(x*x) + (y*y) / z);

        xAngle *= 180.00;   yAngle *= 180.00;   zAngle *= 180.00;
        xAngle /= 3.141592; yAngle /= 3.141592; zAngle /= 3.141592;

        // setFormMotorAngle();
        // setFormLaser();
    }

    /* ================================================================================ */
    /* =================== MISE EN FORME DES DONNEES (VITESSE + LASER) ================ */
    /* ================================================================================ */
    // Obtenir adresse IP du smartphone
    public String getAdresseIP()
    {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    // Met sous la bonne forme pour envoi de données moteur + angle (vitesse)
    public String setFormMotorAngle()
    {
        int yFloor = (int) Math.floor(yAngle); // Arrondi de l'angle y
        String data = "";

        data = getAdresseIP() + "&moteur&" + mVitesse + "*" + yFloor;

        //tvTest.setText(data);
        return data;
    }

    // Met sous la bonne forme pour envoi de données laser
    public String setFormLaser()
    {
        String data = "";

        if(timeLaser > 0 && timeLaser < 2000/DELAY && lAutoIncrement) data = getAdresseIP() + "&laser&" + "ON";
        else data = getAdresseIP() + "&laser&" + "OFF"; // Laser OFF si on est à 0 ou à 2sec ou en décrémentation

        //tvTest.setText(data);
        return data;
    }

    /* ================================================================================ */
    /* ================ CONNEXION RASPBERRY ET ENVOI DE DONNEES VITESSE =============== */
    /* ================================================================================ */
    // Fonction connexion smartphone à RPI et envoi de données (mal optimisé)
    public void connectrpi() {new Thread(new ClientThread()).start();}

    // Thread qui gère la connexion et l'envoi de données
    class ClientThread implements Runnable
    {
        @Override
        public void run()
        {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                mySocket = new Socket(serverAddr, SERVERPORT);

                // Envoi de données
                while(flagPlayActivity)
                {
                    OutputStream outputStream;
                    String msg = setFormMotorAngle();
                    outputStream = mySocket.getOutputStream();
                    PrintStream printStream = new PrintStream(outputStream);
                    printStream.print(msg);
                    //printStream.close();
                    Thread.sleep(25);
                }

            } catch (IOException | InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    } // Fin ClientThread

    /* ================================================================================ */
    /* ========================== ENVOI DE DONNEES LASER ============================== */
    /* ================================================================================ */
    public void sendLaser()
    {
        try {
            String data = setFormLaser();
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(mySocket.getOutputStream())),
                    true);
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================================================================================ */
    /* ========================== GESTION BOUTON RETOUR =============================== */
    /* ================================================================================ */
    @Override
    public void onBackPressed()
    {
        new AlertDialog.Builder(this)
                .setTitle("Veuillez confirmer")
                .setMessage("Voulez-vous vraiment quitter cette page ?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        PlayActivity.super.onBackPressed();
                    }
                }).create().show();
    }
}
