package com.example.arnaud.lasercar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import android.view.ViewGroup.LayoutParams;

public class PlayActivity extends Activity implements SensorEventListener
{
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    /* ============================================================ DECLARATION ATTRIBUTS EN GLOBAL ============================================================ */
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    // Attributs réception de données GameSettings
    private String data_pseudo;
    private String data_player;
    private String data_time;
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
    private TextView tvLaser;
    private boolean lAutoIncrement = false; // indique laser état Tirer ou non
    private int timeLaser;
    // Attributs start
    private ImageButton ibStart;
    private String[] scorePlayer; // ex : scorePlayer[0] = Eric;2;0;4
    private String[] scoreDataPlayer; // ex : scoreDataPlayer[0] = Eric
    // Attributs Timer
    private TextView tvTimer;
    // Attribus connexion RPI et envoi de données
    public Socket clientSocketHost = null;
    public static final int SERVERPORT = 40450;
    public static String SERVER_IP_HOST = "10.5.5.1";
    public static boolean flagPlayActivity;
    // Attributs réception de données RPI
    private ServerSocketWrapper serverSocketWrapper;
    // Attributs divers
    //private TextView tvTest;
    private TextView tvPseudo;
    private TextView tvScore;
    private TextView tvInfo;
    private int score = 0;

    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    /* ============================================================= ON CREATE FONCTION PRINCIPALE ============================================================= */
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
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
        tvVitesse = (TextView) findViewById(R.id.tv_vitesse); tvVitesse.setTypeface(abolition);
        ibAvancer = (ImageButton) findViewById(R.id.ib_avancer);
        ibReculer = (ImageButton) findViewById(R.id.ib_reculer);
        // laser
        tvLaser = (TextView) findViewById(R.id.tv_laser); tvLaser.setTypeface(abolition);
        ibLaser = (ImageButton) findViewById(R.id.ib_laser);
        // start
        ibStart = (ImageButton) findViewById(R.id.ib_start);
        scoreDataPlayer = new String[4];
        scorePlayer = new String[8];
        // timer
        tvTimer = (TextView) findViewById(R.id.tv_timer); tvTimer.setTypeface(abolition);
        // autre
        //tvTest = (TextView) findViewById(R.id.tv_test);
        tvPseudo = (TextView) findViewById(R.id.tv_pseudo); tvPseudo.setTypeface(abolition);
        tvScore = (TextView) findViewById(R.id.tv_score); tvScore.setTypeface(abolition);
        tvInfo = (TextView) findViewById(R.id.tv_info); tvInfo.setTypeface(abolition);

        /* ================================================================================ */
        /* ================= RECEPTION DONNEES DE L'ACTIVITE GAMESETTINGS ================= */
        /* ================================================================================ */
        Intent gameSettingsIntent = getIntent();
        // Réception du pseudo
        data_pseudo = gameSettingsIntent.getStringExtra("message_pseudo"); // public String getStringExtra (String name)
        tvPseudo.setText(data_pseudo);
        // Réception du nombre de joueurs
        data_player = gameSettingsIntent.getStringExtra("message_player");
        //tvTest.setText(data_player);
        // Réception du nombre de joueurs
        data_time = gameSettingsIntent.getStringExtra("message_time");
        //tvTest.setText(data_time);

        // Initialisation du tableau des scores
        for(int i = 0 ; i < 8 ; i++){scorePlayer[i] = "-;-;-;-";}

        /* ================================================================================ */
        /* ============ CREATION DU SERVEUR ANDROID ET RECEPTION DE DONNEES =============== */
        /* ================================================================================ */
        serverSocketWrapper = new ServerSocketWrapper();
        serverSocketWrapper.startSocket();
        receiveData();

        /* ================================================================================ */
        /* ============== CONNEXION RPI + CONFIG ET ENVOIE DE DONNEES VITESSE ============= */
        /* ================================================================================ */
        connectrpi();
        flagPlayActivity = true;

        /* ================================================================================ */
        /* ============================ GESTION BOUTONS VITESSE =========================== */
        /* ================================================================================ */
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

        // Bouton Avancer
        ibAvancer.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            @Override
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                // Appuie sur le bouton avancer
                                if(event.getAction() == MotionEvent.ACTION_DOWN)
                                {
                                    mAutoIncrement = true;
                                    handlerVitesse.post(new RptUpdaterVitesse());
                                    return false;
                                }
                                // Relâche le bouton avancer
                                else if((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoIncrement)
                                {
                                    mAutoIncrement = false;
                                }
                                return false;
                            }
                        }
                );

        // Bouton Reculer
        ibReculer.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            @Override
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                // Appuie sur le bouton reculer
                                if(event.getAction() == MotionEvent.ACTION_DOWN)
                                {
                                    mAutoDecrement = true;
                                    handlerVitesse.post(new RptUpdaterVitesse());
                                    return false;
                                }
                                // Relâche le bouton reculer
                                else if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && mAutoDecrement)
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
                }
                sendLaser(); // envoi données laser tant que l'on reste appuyé sur le bouton laser
            }
        } // Fin thread laser

        ibLaser.setOnTouchListener
                (
                        new View.OnTouchListener()
                        {
                            @Override
                            public boolean onTouch(View v, MotionEvent event)
                            {
                                // Appuie sur le bouton laser
                                if(event.getAction() == MotionEvent.ACTION_DOWN)
                                {
                                    lAutoIncrement = true;
                                    handlerLaser.post(new RptUpdaterLaser());
                                    return false;
                                }
                                // Relâche le bouton laser
                                else if((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && lAutoIncrement)
                                {
                                    lAutoIncrement = false;
                                }
                                return false;
                            }
                        }
                );

        /* ================================================================================ */
        /* ======================= GESTION BOUTON START POPUP WINDOW ====================== */
        /* ================================================================================ */
        ibStart.setOnClickListener
                (
                        new ImageButton.OnClickListener()
                        {
                            @Override
                            public void onClick(View arg0)
                            {
                                LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                                View popupView = layoutInflater.inflate(R.layout.popup_window, null);
                                final PopupWindow popupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

                                initTable(popupView, data_player, scorePlayer);

                                Button btnDismiss = (Button)popupView.findViewById(R.id.btn_dismiss);
                                btnDismiss.setOnClickListener
                                        (
                                                new Button.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(View v)
                                                    {
                                                        popupWindow.dismiss();
                                                    }
                                                }
                                        );

                                Button btnQuit = (Button)popupView.findViewById(R.id.btn_quit);
                                btnQuit.setOnClickListener
                                        (
                                                new Button.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(View v)
                                                    {
                                                        onBackPressed();
                                                    }
                                                }
                                        );

                                popupWindow.setAnimationStyle(R.style.AnimationPopup);
                                popupWindow.showAtLocation(ibStart, Gravity.CENTER, 0, 0);
                            }
                        }
                );
    } // Fin onCreate : fonction principale


    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    /* ============================================================== FONCTIONS CLIENT ANDROID ================================================================= */
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    // Redéfinition nécessaire pour pouvoir utiliser l'accéléromètre
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}

    /* ================================================================================ */
    /* ========= GESTION INCREMENTATION ET DECREMENTATION (VITESSE + LASER) =========== */
    /* ================================================================================ */
    // Fonctions gestion incrémentation
    public void increment(String s)
    {
        if(s.equals("vitesse"))
        {
            mVitesse = mVitesse + 4;
            tvVitesse.setText("Vitesse : " + mVitesse + "%");
        }
        else if(s.equals("laser"))
        {
            timeLaser++;
            tvLaser.setText("Laser : " + (int) Math.floor(timeLaser*1.25) + "%"); // Mul par 1.25 pour passage échelle 0-80 à 0-100
            if(timeLaser == 100/1.25) tvLaser.setText("Laser : 100% - SURCHAUFFE");
        }
    }
    // Fonctions gestion décrémentation
    public void decrement(String s)
    {
        if(s.equals("vitesse"))
        {
            mVitesse = mVitesse - 4;
            tvVitesse.setText("Vitesse : " + mVitesse + "%");
        }
        else if(s.equals("laser"))
        {
            timeLaser--;
            tvLaser.setText("Laser : " + (int) Math.floor(timeLaser*1.25) + "%");
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
    /* ============================ MISE EN FORME DES DONNEES ========================= */
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

    // Met sous la bonne forme pour envoi de données pour l'identification
    public String setFormProfile(){return getAdresseIP() + "&setprofile&name*" + data_pseudo + "*type*android*role*true_master*feedback*True";}

    // Met sous la bonne forme pour envoi de données pour la configuration d'une partie
    public String setFormGame(){return getAdresseIP() + "&setgame&" + data_player + "*" + data_time;}

    /* ================================================================================ */
    /* ================ CONNEXION RASPBERRY ET ENVOI DE DONNEES VITESSE =============== */
    /* ================================================================================ */
    // Fonction connexion smartphone à RPI et envoi de données (mal optimisé)
    public void connectrpi() {new Thread(new ClientThread()).start();}

    // Thread qui gère la connexion et l'envoi de données CONNEXION 1
    class ClientThread implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                // Connexion Android vers RPI
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP_HOST);
                clientSocketHost = new Socket(serverAddr, SERVERPORT);
                Log.d("MyTag", "Création du client");

                // Envoie de données Configuration Profile + Partie
                setProfile(clientSocketHost);
                Thread.sleep(2000);
                setGame();
                Thread.sleep(2000);

                // Envoi de données vitesse
                while(flagPlayActivity)
                {
                    OutputStream outputStream;
                    String msg = setFormMotorAngle();
                    outputStream = clientSocketHost.getOutputStream();
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
                    new OutputStreamWriter(clientSocketHost.getOutputStream())),
                    true);
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================================================================================ */
    /* =================== ENVOI DE DONNEES SETPROFILE + SETGAME ====================== */
    /* ================================================================================ */
    public void setProfile(Socket s)
    {
        try {
            String data = setFormProfile();
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(s.getOutputStream())),
                    true);
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setGame()
    {
        try {
            String data = setFormGame();
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(clientSocketHost.getOutputStream())),
                    true);
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================================================================================ */
    /* ========================== ENVOI DE DONNEES TIMER ============================== */
    /* ================================================================================ */
    public void sendTimer()
    {
        try {
            String data = getAdresseIP() + "&stop&" + "eric";
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(clientSocketHost.getOutputStream())),
                    true);
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    /* ============================================================== FONCTIONS SERVEUR ANDROID ================================================================ */
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */

    /* ================================================================================ */
    /* ================ CREATION SERVEUR ET RECEPTION DE DONNEES ====================== */
    /* ================================================================================ */

    public void receiveData() {new Thread(new ReceiveDataThread()).start();}

    // Thread qui gère la réception de données (Android serveur)
    class ReceiveDataThread implements Runnable
    {
        @Override
        public void run()
        {
            while(serverSocketWrapper.getIsRunning()) // récupère la donnée uniquement quand ServerSocketWrapper est actif
            {
                // On récupère la donnée
                final String data = serverSocketWrapper.getData();
                if(!data.equals("")) Log.d("MyTag", "Android reçoit " + data);

                // On traite la donnée
                if (data.contains("&"))
                {
                    // On split la string selon &
                    String[] splitData = data.split("&");
                    final String dataKey = splitData[0];
                    final String dataValue = splitData[1];

                    // Dans le cas où l'on veut afficher les données sur l'UI
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            switch (dataKey)
                            {
                                case "hitTarget":
                                    hitTarget(dataValue);
                                    break;
                                case "beTouched":
                                    beTouched(dataValue);
                                    break;
                                case "score":
                                    receiveScore(dataValue);
                                    break;
                                case "startTimer":
                                    startTimer(Integer.parseInt(dataValue));
                                    break;
                                case "fin":
                                    printEndGame(dataValue);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }

                // On réinitialise le contenu de la donnée
                serverSocketWrapper.setData(""); //

                // Ectoute toutes les 3ms
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    } // Fin ReceiveDataThread

    /* ================================================================================ */
    /* =================== PROCESS COMMAND DES DONNEES RECUES ========================= */
    /* ================================================================================ */
    public void hitTarget(String s)
    {
        tvInfo.setText("Vous avez touché " + s + " !\n +2");
        score = score + 2;
        tvScore.setText(("Score : " + score));
        ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(100);
    }
    public void beTouched(String s)
    {
        tvInfo.setText("Vous avez été touché par " + s + " !\n -1");
        score--;
        tvScore.setText(("Score : " + score));
        ((Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(400);
    }
    public void receiveScore(String s)
    {
        // Séparation par joueur
        if(s.contains("_"))
        {
            String[] splitData = s.split("_");
            for(int i = 0 ; i < Integer.parseInt(data_player) ; i++){scorePlayer[i] = splitData[i];}
        }
    }
    public void startTimer(int time)
    {
        // Création du timer
        new CountDownTimer(time*60*1000, 1000)
        {
            public void onTick(long millisUntilFinished) {tvTimer.setText("Timer : " + millisUntilFinished / 1000);}

            public void onFinish()
            {
                tvTimer.setText("TEMPS ÉCOULÉ !");
                sendTimer();
            }
        }.start();
    }
    public void printEndGame(String s)
    {
        String message = "";
        if(s.equals("egal")) message = "Il y a égalité";
        else if(s.equals("win")) message = "Vous avez gagné !";
        else message = "Vous avez perdu...";

        new AlertDialog.Builder(this)
                .setTitle("Fin de la partie")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1){}
                }).create().show();
    }


    /* ================================================================================ */
    /* ====================== GESTION DU TABLEAU DES SCORES =========================== */
    /* ================================================================================ */
    public void initTable(View popupView, String nb_player, String[] s)
    {
        TableLayout tlTable = (TableLayout) popupView.findViewById(R.id.tl_table);

        // Remplissage LIGNE 0
        TableRow tr_table_row0 = new TableRow(this);
        // Titre 0
        TextView tv_table_row0_col0 = new TextView(this);
        tv_table_row0_col0.setText("JOUEUR");
        tv_table_row0_col0.setTypeface(Typeface.DEFAULT_BOLD);
        tv_table_row0_col0.setPadding(15, 5, 15, 5);
        tv_table_row0_col0.setTextColor(Color.BLACK);
        tr_table_row0.addView(tv_table_row0_col0);
        // Titre 1
        TextView tv_table_row0_col1 = new TextView(this);
        tv_table_row0_col1.setText("A TOUCHÉ");
        tv_table_row0_col1.setTypeface(Typeface.DEFAULT_BOLD);
        tv_table_row0_col1.setPadding(15, 5, 15, 5);
        tv_table_row0_col1.setTextColor(Color.BLACK);
        tr_table_row0.addView(tv_table_row0_col1);
        // Titre 2
        TextView tv_table_row0_col2 = new TextView(this);
        tv_table_row0_col2.setText("A ÉTÉ TOUCHÉ");
        tv_table_row0_col2.setTypeface(Typeface.DEFAULT_BOLD);
        tv_table_row0_col2.setPadding(15, 5, 15, 5);
        tv_table_row0_col2.setTextColor(Color.BLACK);
        tr_table_row0.addView(tv_table_row0_col2);
        // Titre 3
        TextView tv_table_row0_col3 = new TextView(this);
        tv_table_row0_col3.setText("SCORE");
        tv_table_row0_col3.setTypeface(Typeface.DEFAULT_BOLD);
        tv_table_row0_col3.setPadding(15, 5, 15, 5);
        tv_table_row0_col3.setTextColor(Color.BLACK);
        tr_table_row0.addView(tv_table_row0_col3);
        tlTable.addView(tr_table_row0);

        // Remplissage des autres lignes
        for (int i = 0; i < Integer.parseInt(nb_player) ; i++) // boucle sur le nombre de joueurs de la partie
        {
            if(s[i].contains(";"))
            {
                String[] dataSplit = s[i].split(";");
                scoreDataPlayer[0] = dataSplit[0];
                scoreDataPlayer[1] = dataSplit[1];
                scoreDataPlayer[2] = dataSplit[2];
                scoreDataPlayer[3] = dataSplit[3];
            }

            // Colonne 0
            TableRow tr_row = new TableRow(this);
            TextView tv_col0 = new TextView(this);
            tv_col0.setText(scoreDataPlayer[0]);
            tv_col0.setTextColor(Color.BLACK);
            tv_col0.setGravity(Gravity.CENTER);
            tr_row.addView(tv_col0);
            // Colonne 1
            TextView tv_col1 = new TextView(this);
            tv_col1.setText(scoreDataPlayer[1]);
            tv_col1.setTextColor(Color.BLACK);
            tv_col1.setGravity(Gravity.CENTER);
            tr_row.addView(tv_col1);
            // Colonne 2
            TextView tv_col2 = new TextView(this);
            tv_col2.setText(scoreDataPlayer[2]);
            tv_col2.setTextColor(Color.BLACK);
            tv_col2.setGravity(Gravity.CENTER);
            tr_row.addView(tv_col2);
            // Colonne 3
            TextView tv_col3 = new TextView(this);
            tv_col3.setText(scoreDataPlayer[3]);
            tv_col3.setTextColor(Color.BLACK);
            tv_col3.setGravity(Gravity.CENTER);
            tr_row.addView(tv_col3);
            tlTable.addView(tr_row);
        }
    }

    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */
    /* ================================================================== FONCTIONS AUTRES ===================================================================== */
    /* ========================================================================================================================================================= */
    /* ========================================================================================================================================================= */

    @Override
    public void onBackPressed()
    {
        new AlertDialog.Builder(this)
                .setTitle("Veuillez confirmer")
                .setMessage("Voulez-vous vraiment quitter le jeu ?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        PlayActivity.super.onBackPressed();

                        // Arrêt de tous les thread en cours
                        Thread.currentThread().interrupt();
                    }
                }).create().show();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try {
            serverSocketWrapper.stopSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("MyTag", "serverSocketWrapper.stopSocket()");
    }
} // Fin PlayActivity

