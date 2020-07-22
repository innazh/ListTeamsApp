package com.example.android.a20april2listview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button addButton;
    Button exitButton;
    LinearLayout linearLayout;
    ListView list;
    Integer zero = Integer.valueOf(0);

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_main);
        this.linearLayout = (LinearLayout) findViewById(R.id.layoutListViewID);
        DatabaseHandler databaseHandler1 = new DatabaseHandler(getApplicationContext());
        List<String> listItems0 = databaseHandler1.getAllItems("id");
        String[] listItems0a = (String[]) listItems0.toArray(new String[0]);
        String[] listItems1a = (String[]) databaseHandler1.getAllItems("name0").toArray(new String[0]);
        String[] listItems2a = (String[]) databaseHandler1.getAllItems("name1").toArray(new String[0]);
        List<String> listItems3 = databaseHandler1.getAllItems("name2");
        String[] listItems3a = (String[]) listItems3.toArray(new String[0]);
        List<String> listItems4 = databaseHandler1.getAllItems("name3");
        String[] listItems4a = (String[]) listItems4.toArray(new String[0]);
        List<String> listItems5 = databaseHandler1.getAllItems("name4");
        String[] listItems5a = (String[]) listItems5.toArray(new String[0]);
        CustomListAdapter adapter = new CustomListAdapter(this, listItems0a, listItems1a, listItems2a);
        this.list = (ListView) findViewById(R.id.androidlist);
        this.list.setAdapter(adapter);
        DatabaseHandler databaseHandler = databaseHandler1;
        AnonymousClass1 r9 = r0;
        final String[] strArr = listItems5a;
        List list2 = listItems0;
        ListView listView = this.list;
        final String[] strArr2 = listItems0a;
        CustomListAdapter customListAdapter = adapter;
        final String[] strArr3 = listItems1a;
        List list3 = listItems5;
        final String[] strArr4 = listItems2a;
        List list4 = listItems4;
        final String[] strArr5 = listItems3a;
        List list5 = listItems3;
        final String[] strArr6 = listItems4a;
        AnonymousClass1 r0 = new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Details.stringDataName64 = strArr[position];
                Intent intent = new Intent(MainActivity.this, Details.class);
                intent.putExtra("link0", strArr2[position]);
                intent.putExtra("link1", strArr3[position]);
                intent.putExtra("link2", strArr4[position]);
                intent.putExtra("link3", strArr5[position]);
                intent.putExtra("link4", strArr6[position]);
                intent.putExtra("command", "ud");
                MainActivity.this.startActivity(intent);
            }
        };
        listView.setOnItemClickListener(r9);
        this.exitButton = (Button) findViewById(R.id.buttonExit);
        this.exitButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.finishAffinity();
            }
        });
        this.addButton = (Button) findViewById(R.id.buttonAddRow);
        this.addButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Details.class);
                String str = "";
                intent.putExtra("link1", str);
                intent.putExtra("link2", str);
                intent.putExtra("link3", MainActivity.this.zero.toString());
                intent.putExtra("link4", str);
                intent.putExtra("command", "add");
                MainActivity.this.startActivity(intent);
            }
        });
    }
}
