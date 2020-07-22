package moka.net.a4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import java.util.Arrays;

public class TeamDetailActivity extends AppCompatActivity {
    private Button submitBtn;
    private String s;
    private EditText cityEt, nameEt, mvpEt, idEt;
    private Spinner sportSpinner;
    private String sportEt;
    private LinearLayout mainLayout;
    private LinearLayout btnLayout;
    private Button updateBtn, exitBtn, deleteBtn, uploadImgBtn;
    private ImageView imageView;

    public void onBackPressed() {
        startActivity(new Intent(TeamDetailActivity.this, MainActivity.class));
        finishAffinity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_detail);

        init();

        sportSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                sportEt = sportSpinner.getItemAtPosition(position).toString();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DatabaseHelper db = new DatabaseHelper(getApplicationContext());

                Bitmap converetedImage = getResizedBitmap(((BitmapDrawable) imageView.getDrawable()).getBitmap(), 300);

                Team team = new Team(cityEt.getText().toString(), nameEt.getText().toString(), sportEt, mvpEt.getText().toString(), converetedImage);

                if (team.getCity().length() == 0 || team.getName().length() == 0) {
                    Toast.makeText(getApplicationContext(), "City and Name are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.insertTeam(team);

                String str = "";
                nameEt.setText(str);
                cityEt.setText(str);

                sportSpinner.setVisibility(View.VISIBLE);
                sportSpinner.setSelection(0);

                mvpEt.setText(str);
                imageView.setImageResource(R.drawable.img_not_found);
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(TeamDetailActivity.this, MainActivity.class));
                finishAffinity();
            }
        });
        
        Intent intent = getIntent();
        Team team = (Team) intent.getSerializableExtra("team");
        String action = intent.getStringExtra("action");
        idEt.setText(intent.getStringExtra("id"));
        cityEt.setText(team.getCity());
        nameEt.setText(team.getName());
        boolean flag=false;

        for(int i=0; i<sportSpinner.getCount() && !flag; i++){
            if(sportSpinner.getItemAtPosition(i).equals(team.getSport()))
            {
                sportSpinner.setSelection(i);
                flag=true;
            }
        }
        mvpEt.setText(team.getMvp());

        if (action.equals("add")) {
            deleteBtn.setVisibility(View.GONE);
            updateBtn.setVisibility(View.GONE);
            submitBtn.setVisibility(View.VISIBLE);
            mainLayout.setBackgroundColor(getResources().getColor(R.color.holo_orange_light));

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_not_found);
            imageView.setImageBitmap(bitmap);
        }
        
        if (action.equals("up")) {
            deleteBtn.setVisibility(View.VISIBLE);
            updateBtn.setVisibility(View.VISIBLE);
            submitBtn.setVisibility(View.GONE);
            mainLayout.setBackgroundColor(getResources().getColor(R.color.holo_red_light));
            imageView.setImageBitmap(team.getImage());
        }

        uploadImgBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 100);
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DatabaseHelper db = new DatabaseHelper(getApplicationContext());

                String name = nameEt.getText().toString();
                String city = cityEt.getText().toString();
                String mvp = mvpEt.getText().toString();
                String sport = sportSpinner.getSelectedItem().toString();
                Bitmap converetedImage = getResizedBitmap(((BitmapDrawable) imageView.getDrawable()).getBitmap(), 300);

                if (city.length() > 0) {
                    db.deleteTeam(idEt.getText().toString());
                    db.insertTeam(new Team(city, name, sport, mvp, converetedImage));
                    startActivity(new Intent(TeamDetailActivity.this, MainActivity.class));
                }
            }
        });
        
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatabaseHelper(getApplicationContext()).deleteTeam(idEt.getText().toString());
                startActivity(new Intent(TeamDetailActivity.this, MainActivity.class));
            }
        });
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length <= 0 || grantResults[0] != 0) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            String[] filePathColumn = {"_data"};
            Cursor cursor = getContentResolver().query(data.getData(), filePathColumn, null, null, null);
            cursor.moveToFirst();
            s = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();
            ((ImageView) findViewById(R.id.imgUpload)).setImageBitmap(BitmapFactory.decodeFile(s));
        } catch (Exception e) {
        }
    }

    private void init(){
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);

        idEt = findViewById(R.id.IdEt);
        cityEt = findViewById(R.id.cityEt);
        nameEt = findViewById(R.id.nameEt);
        sportSpinner = findViewById(R.id.sportSpinner);
        sportSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, Arrays.asList(new String[]{" ", "Baseball", "Basketball", "Football", "Hockey"})));
        mvpEt = findViewById(R.id.mvpEt);
        imageView = findViewById(R.id.imgUpload);
        imageView.setImageResource(R.drawable.img_not_found);

        updateBtn = findViewById(R.id.updateDBtn);
        exitBtn = findViewById(R.id.exitDBtn);
        deleteBtn = findViewById(R.id.deleteDBtn);
        submitBtn = findViewById(R.id.submitDBtn);
        uploadImgBtn = findViewById(R.id.uploadImgBtn);
        mainLayout = findViewById(R.id.teamDetailMainLL);
        btnLayout = findViewById(R.id.buttonsLL);
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int height;
        int width;
        float bitmapRatio = ((float) image.getWidth()) / ((float) image.getHeight());
        if (bitmapRatio > 1.0f) {
            width = maxSize;
            height = (int) (((float) width) / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (((float) height) * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}
