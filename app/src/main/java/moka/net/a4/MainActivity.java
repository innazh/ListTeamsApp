package moka.net.a4;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button addButton;
    private Button exitButton;
    private ListView list;

    private CustomAdapter adapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.teamList);
        exitButton = findViewById(R.id.exitBtn);
        addButton = findViewById(R.id.addTeamBtn);

        DatabaseHelper db = new DatabaseHelper(getApplicationContext());

        final String[] ids = db.getAllTeams("id").toArray(new String[0]);
        final String[] cities = db.getAllTeams("city").toArray(new String[0]);
        final String[] names = db.getAllTeams("name").toArray(new String[0]);
        final String[] sports = db.getAllTeams("sport").toArray(new String[0]);
        final String[] mvps = db.getAllTeams("mvp").toArray(new String[0]);
        final List<byte[]> images = db.getAllImgs("image");

        adapter = new CustomAdapter(this, cities, names);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, TeamDetailActivity.class);

                Team team = new Team(cities[position], names[position], sports[position], mvps[position], BitmapFactory.decodeByteArray(images.get(position), 0, images.get(position).length));
                //System.out.println("AKIIII  - - -" + team.getSport() + "AKIIII  - - -" + team.getImgBytes());
                intent.putExtra("team", team);
                intent.putExtra("id", ids[position]);
                intent.putExtra("action", "up");

                startActivity(intent);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finishAffinity();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TeamDetailActivity.class);
                Team team = new Team();
                intent.putExtra("team", team);
                intent.putExtra("action", "add");

                startActivity(intent);
            }
        });
    }
}
