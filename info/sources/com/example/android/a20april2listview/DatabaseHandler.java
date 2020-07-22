package com.example.android.a20april2listview;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    DatabaseHandler(Context context) {
        super(context, "spinnerExample", null, 1);
    }

    public void onCreate(SQLiteDatabase db1) {
        db1.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY, name0 TEXT, name1 TEXT, name2 TEXT, name3 TEXT, name4 BLOB)");
    }

    public void onUpgrade(SQLiteDatabase db2, int oldVersion, int newVersion) {
        db2.execSQL("DROP TABLE IF EXISTS items");
        onCreate(db2);
    }

    /* access modifiers changed from: 0000 */
    public void deleteItem(String idTeam) {
        SQLiteDatabase db5 = getWritableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("id = '");
        sb.append(idTeam);
        sb.append("'");
        db5.delete("items", sb.toString(), null);
        db5.close();
    }

    /* access modifiers changed from: 0000 */
    public void insertItem(String itemA, String itemB, String itemC, String itemD, String itemE) {
        SQLiteDatabase db3 = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name0", itemA);
        contentValues.put("name1", itemB);
        contentValues.put("name2", itemC);
        contentValues.put("name3", itemD);
        contentValues.put("name4", itemE);
        db3.insert("items", null, contentValues);
        db3.close();
    }

    /* access modifiers changed from: 0000 */
    public List<String> getAllItems(String colI) {
        List<String> listItems2 = new ArrayList<>();
        SQLiteDatabase db4 = getReadableDatabase();
        Cursor cursor = db4.rawQuery("SELECT * FROM items", null);
        if (cursor.moveToFirst()) {
            do {
                listItems2.add(cursor.getString(cursor.getColumnIndex(colI)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db4.close();
        return listItems2;
    }
}
