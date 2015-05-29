package com.example.arnaud.lasercar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class PlayActivity extends Activity implements SensorEventListener
{
    /* ================================================================================ */
    /* =========================== DECLARATION ATTRIBUTS ============================== */
    /* ================================================================================ */
    // Attributs accéléromètre
    private Sensor accelerometer;
    private SensorManager sm;
    private TextView tvAccelerometerX;
    private TextView tvAccelerometerY;
    private TextView tvAccelerometerZ;
    private float xAngle = 0;
    private float yAngle = 0;
    private float zAngle = 0;
    // Attributs vitesse
    private ImageButton ibAvancer;
    private ImageButton ibReculer;
    private TextView tvVitesse;
    private Handler repeatUpdateHandler = new Handler();
    private boolean mAutoIncrement = false; // indique état Avancer ou Reculer
    private boolean mAutoDecrement = false;
    private int mVitesse;
    private static int REP_DELAY = 25;
    // Attributs mise en forme des données
    private TextView tvDonnees;
    // Attribus connexion RPI et envoi de données
    public Socket mySocket = null;
    public static final int SERVERPORT = 40450;
    public static final String SERVER_IP = "10.5.5.1";
    public DataOutputStream os = null;
    public static boolean flagPlayActivity;

    // Thread qui s'exécute en parallèle : gère la vitesse
    class RptUpdater implements Runnable
    {
        public void run()
        {
            // Appuie sur avancer OU Relache reculer
            if((mVitesse >= 0 && mAutoIncrement) || (mVitesse < 0 && !mAutoDecrement))
            {
                if(mVitesse < 100)
                {
                    increment();
                    repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
                }
                else repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
            }

            // Relache avancer OU Appuie reculer
            else if((mVitesse > 0 && !mAutoIncrement) || (mVitesse <= 0 && mAutoDecrement))
            {
                if(mVitesse > -100)
                {
                    decrement();
                    repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
                }
                else repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
            }
        }
    } // Fin thread


    // Redéfinition de la fonction principale
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        /* ================================================================================ */
        /* =========================== INSTANCIATION VARIABLES ============================ */
        /* ================================================================================ */
        // accéléromètre
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        tvAccelerometerX = (TextView) findViewById(R.id.tv_accelerometerX);
        tvAccelerometerY = (TextView) findViewById(R.id.tv_accelerometerY);
        tvAccelerometerZ = (TextView) findViewById(R.id.tv_accelerometerZ);
        // vitesse
        tvVitesse = (TextView) findViewById(R.id.tv_vitesse);
        ibAvancer = (ImageButton) findViewById(R.id.ib_avancer);
        ibReculer = (ImageButton) findViewById(R.id.ib_reculer);
        // envoi de données
        tvDonnees = (TextView) findViewById(R.id.tv_donnees);

        /* ================================================================================ */
        /* =================== CONNEXION RASPBERRY et ENVOIE DE DONNEES =================== */
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
                                repeatUpdateHandler.post(new RptUpdater());
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
                                repeatUpdateHandler.post(new RptUpdater());
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
    } // Fin onCreate : fonction principale

    // Redéfinition nécessaire pour pouvoir utiliser l'accéléromètre
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    /* ================================================================================ */
    /* =========================== GESTION DE lA VITESSE ============================== */
    /* ================================================================================ */
    // Fonctions gestion affichage vitesse
    public void increment()
    {
        mVitesse++;
        tvVitesse.setText("Vitesse : " + mVitesse + "%");
    }
    public void decrement()
    {
        mVitesse--;
        tvVitesse.setText("Vitesse : " + mVitesse + "%");
    }

    /* ================================================================================ */
    /* ============================ GESTION DE l'ACCELEROMETRE ======================== */
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

        PositionAccelerometer(xAngle, yAngle, zAngle);
        // setFormMotorAngle();
    }

    // Affiche les coordonnéees XYZ de l'accéléromètre
    public void PositionAccelerometer(float x, float y, float z)
    {
        tvAccelerometerX.setText("X : " + x);
        tvAccelerometerY.setText("Y : " + y);
        tvAccelerometerZ.setText("Z : " + z);
    }

    /* ================================================================================ */
    /* ============================ MISE EN FORME DES DONNEES ========================= */
    /* ================================================================================ */
    // Obtenir adresse MAC du smartphone
    public String getAdresseMac()
    {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    // Met sous la bonne forme pour envoi de données moteur + angle
    public String setFormMotorAngle()
    {
        int yFloor = (int) Math.floor(yAngle); // Arrondi de l'angle y
        String data = "";

        data = getAdresseMac() + "&moteur&" + mVitesse + "*" + yFloor;

        //tvDonnees.setText(data);
        return data;
    }

    /* ================================================================================ */
    /* ==================== CONNEXION RASPBERRY ET ENVOI DE DONNEES =================== */
    /* ================================================================================ */
    // Fonction connexion smartphone à RPI et envoi de données (mal optimisé)
    public void connectrpi() {new Thread(new ClientThread()).start();}

    // Thread qui gère la connexion et l'envoi de données
    class ClientThread extends TimerTask implements Runnable
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
}
