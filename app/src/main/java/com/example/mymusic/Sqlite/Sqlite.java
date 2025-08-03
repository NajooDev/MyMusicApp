package com.example.mymusic.Sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Sqlite {

    SQLiteDatabase myDatabase;

    Context context;
    public Sqlite(Context context) {
        this.context = context;

        myDatabase = context.openOrCreateDatabase("qlMusicList.db", Context.MODE_PRIVATE, null);

        //xóa bảng
//        boolean result = context.deleteDatabase("qlNote.db");

        //xoa table
//        myDatabase.execSQL("DROP TABLE IF EXISTS tbnote");

        String sql1 = "CREATE TABLE IF NOT EXISTS tbmusic (" +
                "maMusic INTEGER PRIMARY KEY," +
                "name TEXT)";

        myDatabase.execSQL(sql1);
        
//        boolean result = addFavoriteMusic("test-music.mp3");
//        if(result) Toast.makeText(context, "add thanh cong", Toast.LENGTH_SHORT).show();
    }

    public boolean addFavoriteMusic(String name){
        ContentValues values= new ContentValues();

        values.put("name", name);

        int n = (int) myDatabase.insert("tbmusic", null, values);

        return n != -1;
    }

    public boolean removeFavoriteMusic(String name){

        int n = (int) myDatabase.delete("tbmusic", "name =?", new String[]{name});

        return n != -1;
    }


    public ArrayList<String> getAll(){
        ArrayList<String> result = new ArrayList<>();
        //query(table, columns(cột muốn trả về), selection(đkiện), selectionArgs, groupBy, having, orderBy)
        //dưới đây là lấy toàn bộ dữ liệu và bỏ vào con trỏ

        Cursor c = myDatabase.query("tbmusic", null, null, null, null, null, null);

        //chạy con trỏ đến dòng đầu tiên
        c.moveToNext();

        //nếu nó không phải là cái cuối cùng (sau vẫn còn) thì nó sẽ tiếp tục chạy vòng lập này
        while(!c.isAfterLast()){

            result.add(c.getString(1));

            //con trỏ chạy đến dòng tiếp theo
            c.moveToNext();
        }

        //đóng con trỏ sau khi sử dụng xong
        c.close();

        return result;
    }
}
