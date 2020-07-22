package moka.net.a4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
        super(context, "teams", null, 1);
    }

    public void onCreate(SQLiteDatabase db1) {
        db1.execSQL("CREATE TABLE teams (id INTEGER PRIMARY KEY, city TEXT, name TEXT, sport TEXT, mvp TEXT, image BLOB)");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS teams");
        onCreate(db);
    }

    public void deleteTeam(String id) {
        SQLiteDatabase db = getWritableDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("id = '");
        sb.append(id);
        sb.append("'");
        db.delete("teams", sb.toString(), null);
        db.close();
    }

    public void insertTeam(Team team) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("city", team.getCity());
        values.put("name", team.getName());
        values.put("sport", team.getSport());
        values.put("mvp", team.getMvp());
        values.put("image", team.getImgBytes());

        db.insert("teams", null, values);
        db.close();
    }

    public List<String> getAllTeams(String colName) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM teams", null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(cursor.getColumnIndex(colName)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return list;
    }

    public List<byte[]> getAllImgs(String colName) {
        List<byte[]> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM teams", null);

        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getBlob(cursor.getColumnIndex(colName)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return list;
    }
}
