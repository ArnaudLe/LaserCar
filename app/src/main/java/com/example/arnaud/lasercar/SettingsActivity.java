package com.example.arnaud.lasercar;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class SettingsActivity extends ActionBarActivity
{

    private ListView maListViewPerso;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Récupération de la listview créée dans le fichier activity_settings.xml
        maListViewPerso = (ListView) findViewById(R.id.listviewsettings);

        //Création de la ArrayList qui nous permettra de remplire la listView
        ArrayList<HashMap<String, String>> listItem = new ArrayList<HashMap<String, String>>();

        //On déclare la HashMap qui contiendra les informations pour un item
        HashMap<String, String> map;

        //Création d'une HashMap pour insérer les informations du premier item de notre listView
        map = new HashMap<String, String>();
        //on insère un élément titre que l'on récupérera dans le textView titre créé dans le fichier affichage_settings.xml
        map.put("titre", "À propos");
        //on insère un élément description que l'on récupérera dans le textView description créé dans le fichier affichage_settings.xml
        map.put("description", "Découvrez l'équipe de Laser Car !");
        //on insère la référence à l'image (convertit en String car normalement c'est un int) que l'on récupérera dans l'imageView créé dans le fichier affichageitem.xml
        map.put("img", String.valueOf(R.drawable.ic_launcher));
        //enfin on ajoute cette hashMap dans la arrayList
        listItem.add(map);

        //On refait la manip plusieurs fois avec des données différentes pour former les items de notre ListView
        map = new HashMap<String, String>();
        map.put("titre", "Paramètres");
        map.put("description", "Modifiez les paramètres du smartphone");
        map.put("img", String.valueOf(R.drawable.ic_launcher));
        listItem.add(map);


        //Création d'un SimpleAdapter qui se chargera de mettre les items présent dans notre list (listItem) dans la vue affichageitem
        SimpleAdapter mSchedule = new SimpleAdapter (this.getBaseContext(), listItem, R.layout.affichage_settings,
                new String[] {"img", "titre", "description"}, new int[] {R.id.img, R.id.titre, R.id.description});

        //On attribut à notre listView l'adapter que l'on vient de créer
        maListViewPerso.setAdapter(mSchedule);

        //Enfin on met un écouteur d'évènement sur notre listView
        maListViewPerso.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                // Apparition d'une boite de dialogue pour A propos
                if(position == 0)
                {
                    //on récupère la HashMap contenant les infos de notre item (titre, description, img)
                    HashMap<String, String> map = (HashMap<String, String>) maListViewPerso.getItemAtPosition(position);
                    //on créer une boite de dialogue
                    AlertDialog.Builder adb = new AlertDialog.Builder(SettingsActivity.this);
                    //on attribut un titre à notre boite de dialogue
                    adb.setTitle(map.get("titre"));
                    //on insère un message à notre boite de dialogue
                    adb.setMessage("Notre équipe est composée de six personnes : Arnaud LE, Pradhiban PHILLIX-ANTON, Bertrand RAYMAND, Xavier THOMAS, Eric TRAN et Alexandre WALLET");
                    //on indique que l'on veut le bouton ok à notre boite de dialogue
                    adb.setPositiveButton("OK", null);
                    //on affiche la boite de dialogue
                    adb.show();
                }

                // On renvoie vers la page Settings du smartphone
                if(position == 1)
                {
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), 0);
                }


            }
        });

    }
}