package com.example.android.a20april2listview;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class Details extends AppCompatActivity {
    public static String stringDataName64;
    Button addrow2Button;
    Button deleteButton;
    EditText editText0;
    EditText editText1;
    String editText2;
    EditText editText3;
    EditText editTextID;
    Button exit2Button;
    byte[] imageArray = null;
    ImageView imageView;
    String imgDecodableString;
    Spinner spinner1;
    String stringDataName;
    LinearLayout topLayout;
    Button updateButton;
    Button uploadImageButton;

    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_details);
        this.editTextID = (EditText) findViewById(R.id.textViewID);
        this.editText0 = (EditText) findViewById(R.id.textView0);
        this.editText1 = (EditText) findViewById(R.id.textView1);
        this.editText3 = (EditText) findViewById(R.id.textView3);
        this.updateButton = (Button) findViewById(R.id.buttonUpdate);
        this.addrow2Button = (Button) findViewById(R.id.buttonAddRow2);
        this.deleteButton = (Button) findViewById(R.id.buttonDelete);
        this.exit2Button = (Button) findViewById(R.id.buttonExit2);
        this.uploadImageButton = (Button) findViewById(R.id.buttonImageID);
        this.imageView = (ImageView) findViewById(R.id.imageViewID);
        this.imageView.setImageResource(0);
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 1);
        this.spinner1 = (Spinner) findViewById(R.id.textView2spinner);
        this.spinner1.setAdapter(new ArrayAdapter<>(this, 17367043, Arrays.asList(new String[]{" ", "Baseball", "Basketball", "Football", "Hockey"})));
        Intent intent = getIntent();
        this.stringDataName = intent.getStringExtra("link0");
        this.editTextID.setText(this.stringDataName);
        this.stringDataName = intent.getStringExtra("link1");
        this.editText0.setText(this.stringDataName);
        this.stringDataName = intent.getStringExtra("link2");
        this.editText1.setText(this.stringDataName);
        this.stringDataName = intent.getStringExtra("link3");
        this.spinner1.setSelection(Integer.valueOf(Integer.parseInt(this.stringDataName)).intValue());
        this.stringDataName = intent.getStringExtra("link4");
        this.editText3.setText(this.stringDataName);
        this.spinner1.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Details details = Details.this;
                StringBuilder sb = new StringBuilder();
                sb.append("");
                sb.append(position);
                details.editText2 = sb.toString();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        this.stringDataName = intent.getStringExtra("command");
        this.topLayout = (LinearLayout) findViewById(R.id.layoutTop);
        if (this.stringDataName.equals("add")) {
            this.deleteButton.setVisibility(8);
            this.updateButton.setVisibility(8);
            this.addrow2Button.setVisibility(0);
            this.topLayout.setBackgroundColor(getResources().getColor(17170456));
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 20, byteArrayOutputStream);
            this.imageArray = byteArrayOutputStream.toByteArray();
            byte[] bArr = this.imageArray;
            this.imageView.setImageBitmap(BitmapFactory.decodeByteArray(bArr, 0, bArr.length));
        }
        if (this.stringDataName.equals("ud")) {
            this.deleteButton.setVisibility(0);
            this.updateButton.setVisibility(0);
            this.addrow2Button.setVisibility(8);
            this.topLayout.setBackgroundColor(getResources().getColor(17170454));
            byte[] decodedString = Base64.decode(stringDataName64, 0);
            this.imageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
        }
        this.addrow2Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                DatabaseHandler databaseHandler2 = new DatabaseHandler(Details.this.getApplicationContext());
                String item0 = Details.this.editText0.getText().toString();
                String item1 = Details.this.editText1.getText().toString();
                String item3 = Details.this.editText3.getText().toString();
                Bitmap converetdImage = Details.this.getResizedBitmap(((BitmapDrawable) Details.this.imageView.getDrawable()).getBitmap(), 300);
                ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
                converetdImage.compress(CompressFormat.PNG, 20, byteArrayOutputStream1);
                Details.this.imageArray = byteArrayOutputStream1.toByteArray();
                String base64String = Base64.encodeToString(Details.this.imageArray, 0);
                if (item0.length() <= 0 || item1.length() <= 0) {
                    Toast.makeText(Details.this.getApplicationContext(), "City and Name are required", 0).show();
                    return;
                }
                databaseHandler2.insertItem(item0, item1, Details.this.editText2, item3, base64String);
                String str = "";
                Details.this.editText0.setText(str);
                Details.this.editText1.setText(str);
                Details.this.editText3.setText(str);
                Details.this.spinner1.setVisibility(0);
                Details.this.spinner1.setSelection(0);
                Bitmap bitmap = BitmapFactory.decodeResource(Details.this.getResources(), R.drawable.ic_launcher);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(CompressFormat.PNG, 20, byteArrayOutputStream);
                Details.this.imageArray = byteArrayOutputStream.toByteArray();
                Details.this.imageView.setImageBitmap(BitmapFactory.decodeByteArray(Details.this.imageArray, 0, Details.this.imageArray.length));
            }
        });
        this.updateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                DatabaseHandler dbHandler = new DatabaseHandler(Details.this.getApplicationContext());
                String itemID = Details.this.editTextID.getText().toString();
                String item0 = Details.this.editText0.getText().toString();
                String item1 = Details.this.editText1.getText().toString();
                String item3 = Details.this.editText3.getText().toString();
                Bitmap converetdImage = Details.this.getResizedBitmap(((BitmapDrawable) Details.this.imageView.getDrawable()).getBitmap(), 300);
                ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
                converetdImage.compress(CompressFormat.PNG, 20, byteArrayOutputStream1);
                Details.this.imageArray = byteArrayOutputStream1.toByteArray();
                String base64String = Base64.encodeToString(Details.this.imageArray, 0);
                if (item0.length() > 0) {
                    dbHandler.deleteItem(itemID);
                    dbHandler.insertItem(item0, item1, Details.this.editText2, item3, base64String);
                    Details.this.startActivity(new Intent(Details.this, MainActivity.class));
                }
            }
        });
        this.deleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new DatabaseHandler(Details.this.getApplicationContext()).deleteItem(Details.this.editTextID.getText().toString());
                Details.this.startActivity(new Intent(Details.this, MainActivity.class));
            }
        });
        this.exit2Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Details.this.startActivity(new Intent(Details.this, MainActivity.class));
                Details.this.finishAffinity();
            }
        });
        this.uploadImageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Details.this.startActivityForResult(new Intent("android.intent.action.PICK", Media.EXTERNAL_CONTENT_URI), 100);
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length <= 0 || grantResults[0] != 0) {
                Toast.makeText(this, "Permission Denied", 0).show();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            String[] filePathColumn = {"_data"};
            Cursor cursor = getContentResolver().query(data.getData(), filePathColumn, null, null, null);
            cursor.moveToFirst();
            this.imgDecodableString = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();
            ((ImageView) findViewById(R.id.imageViewID)).setImageBitmap(BitmapFactory.decodeFile(this.imgDecodableString));
        } catch (Exception e) {
        }
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
