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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class NormalPlayListFragment extends Fragment {

    private ArrayAdapter<String> adapter;

    private ArrayList<String> arraySongName , arraySongList;

    private ListView musicList;


    //Biến lưu tham chiếu tới Activity (sẽ implement interface)
    FirstFragmentListener activityCallback;

    //Khai báo một interface bên trong một class (nested interface)
    //=> bất cứ Activity nào implement cái interface này đều phải xử lý phương thức onButtonClick(...) bên trong class đó
    public interface FirstFragmentListener {
        public void normalListClick(int location, ArrayList<String> songList);
    }


    //onAttach: được gọi khi gán Fragment vào activity (context ở đây là activity đã gán Fragment này)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Ép kiểu Activity đã gán Fragment này thành FirstFragmentListener
            //=> lúc này thì activityCallback là Activity đã gán fragment này
            activityCallback = (FirstFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " You must implement FirstFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_normal_play_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        //MUSIC LIST
        musicList = view.findViewById(R.id.music_list);

        //arrayLIst
        //cái này lưu tên (không có .mp3)
        arraySongName = new ArrayList<>();

        //cái này có .mp3 để gửi cho mainActivity
        arraySongList = new ArrayList<>();

        //ADAPTER (Không cần viết custom Adapter nếu bạn chỉ muốn đổi màu nền (hoặc drawable) cho item đang được chọn – bạn có thể dùng thẳng ArrayAdapter với layout tự tạo và selector như sau:)
        adapter = new ArrayAdapter<>(requireContext(), R.layout.music_item_normal, R.id.text1, arraySongName);   // có thể dùng getContext() nhưng nên ensure non-null

        //set adapter vào musicList (ListView)
        musicList.setAdapter(adapter);

        initAndPlayExternalMp3();

        //action musicList
        musicList.setOnItemClickListener((parent, view1, position, id) -> {

            if(checkFile(arraySongList.get(position))){
                activityCallback.normalListClick(position, arraySongList);
            }else{
                Toast.makeText(getContext(), "Không tìm thấy nhạc.", Toast.LENGTH_SHORT).show();
                arraySongName.clear();
                arraySongList.clear();
                initAndPlayExternalMp3();
            }

        });
    }

    private void initAndPlayExternalMp3() {
        // Lấy đường dẫn file MP3 trên external storage ,Environment.DIRECTORY_MUSIC là nó ở thư mục Music mặc định
        File musicDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
        );
        //này là lấy toàn bộ file mp3 trong thư mục Music
        File[] mp3Files = musicDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        Log.d("number", mp3Files.length+"");

        //này là lấy toàn bộ file mp3 trong music (lấy nhiều cái nên add)
        String songName = "";
        if(mp3Files.length !=0) {
            for (File f : mp3Files) {
                songName = f.getName();

                arraySongList.add(songName);
                arraySongName.add(getMusicName(songName));
            }
        }

        //sắp xếp theo thứ tự chữ cái không phân biệt in hoa
        Collections.sort(arraySongList, String.CASE_INSENSITIVE_ORDER);

        Collections.sort(arraySongName, String.CASE_INSENSITIVE_ORDER);
        adapter.notifyDataSetChanged();
    }

    public boolean checkFile(String fileName){
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        File mp3File = new File(musicDir, fileName);

        // Nếu file không tồn tại, in log hoặc thông báo
        // return hoặc xử lý tương ứng
        return mp3File.exists();
    }
    public String getMusicName(String name){
        return name.replace(".mp3","");
    }
}