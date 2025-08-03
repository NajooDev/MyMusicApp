package com.example.mymusic;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.mymusic.Sqlite.Sqlite;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FavoritePlayListFragment extends Fragment {

    private ArrayAdapter<String> adapter;

    private ArrayList<String> arrayFavoriteSongName, arrayFavoriteList;

    private ListView musicList;

    private Sqlite sqlite;

    //Biến lưu tham chiếu tới Activity (sẽ implement interface)
    FavoritePlayListFragment.SecondFragmentListener activityCallback;

    //Khai báo một interface bên trong một class (nested interface)
    //=> bất cứ Activity nào implement cái interface này đều phải xử lý phương thức onButtonClick(...) bên trong class đó
    public interface SecondFragmentListener {
        public void favoriteListClick(int location, ArrayList<String> favoriteSongList);
    }

    //onAttach: được gọi khi gán Fragment vào activity (context ở đây là activity đã gán Fragment này)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Ép kiểu Activity đã gán Fragment này thành FirstFragmentListener
            //=> lúc này thì activityCallback là Activity đã gán fragment này
            activityCallback = (FavoritePlayListFragment.SecondFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " You must implement SecondFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite_play_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        //tao sqlite
        sqlite = new Sqlite(getContext());


        //MUSIC LIST
        musicList = view.findViewById(R.id.favorite_music_list);

        //arrayLIst
        arrayFavoriteSongName = new ArrayList<>();

        arrayFavoriteList = new ArrayList<>();

        initAndPlayFavoriteMp3();

        //ADAPTER (Không cần viết custom Adapter nếu bạn chỉ muốn đổi màu nền (hoặc drawable) cho item đang được chọn – bạn có thể dùng thẳng ArrayAdapter với layout tự tạo và selector như sau:)
        adapter = new ArrayAdapter<>(requireContext(), R.layout.music_item_normal, R.id.text1, arrayFavoriteSongName);   // có thể dùng getContext() nhưng nên ensure non-null

        //set adapter vào musicList (ListView)
        musicList.setAdapter(adapter);

        //action musicList
        musicList.setOnItemClickListener((parent, view1, position, id) -> {

            String s = arrayFavoriteList.get(position);
            if(checkFile(s)){
                activityCallback.favoriteListClick(position, arrayFavoriteList);
            }else{
                Toast.makeText(getContext(), "Không tìm thấy nhạc.", Toast.LENGTH_SHORT).show();
                arrayFavoriteList.clear();
                arrayFavoriteSongName.clear();

                sqlite.removeFavoriteMusic(s);
                initAndPlayFavoriteMp3();

                adapter.notifyDataSetChanged();
            }
        });
    }

    public ArrayList<String> getFavoriteListName(ArrayList<String> favoriteList){
        ArrayList<String> result = new ArrayList<>();

        for(String s: favoriteList){
            result.add(s.replace(".mp3",""));
        }

        return result;
    }

    public boolean checkFile(String fileName){
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        File mp3File = new File(musicDir, fileName);

        // Nếu file không tồn tại, in log hoặc thông báo
        // return hoặc xử lý tương ứng
        return mp3File.exists();
    }

    public void initAndPlayFavoriteMp3(){
        //bỏ danh sách có đuôi mp3 vào đây( để gửi cho mainActivity)
        arrayFavoriteList.addAll(sqlite.getAll());

        //danh sách này để hiển thị( đã bỏ đuôi mp3)
        arrayFavoriteSongName.addAll(getFavoriteListName(arrayFavoriteList));

        //sắp xếp theo thứ tự chữ cái không phân biệt in hoa
        Collections.sort(arrayFavoriteList, String.CASE_INSENSITIVE_ORDER);

        Collections.sort(arrayFavoriteSongName, String.CASE_INSENSITIVE_ORDER);
    }
}