package com.example.mymusic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerControlView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.util.ArrayList;

/*
    Lưu ý trước khi lập trình:
    -Trước hết, trong build.gradle module (app), bạn phải chắc chắn đang có dependency:
        implementation("androidx.media3:media3-exoplayer:1.3.1")
        implementation("androidx.media3:media3-ui:1.3.1")
    - đánh dấu là @UnstableApi (không có nghĩa là chúng không an toàn hoặc không nên sử dụng; điều này chỉ cảnh báo rằng chúng có thể thay đổi trong tương lai.)
    -Ở AndroidManiest:
        thêm: <uses-permission android:name="android.permission.INTERNET" /> => xin cho phép nếu dùng internet
        thêm: <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /> => xin cho phép nếu muốn đọc bộ nhớ ngoài
        thêm vào application : android:requestLegacyExternalStorage="true" -> một phần của việc xin phép đọc bộ nhớ (nếu không thêm sẽ lỗi)

    Quy trình gọi để khi BottomSheetDialogFragment bị kéo xuống thì nó sẽ cập nhật lại nút play/pause cho đúng với trạng thái hiện tại (isPlaying or not):
        Activity khởi tạo fragment và gán externalDismissListener.
        Fragment trong onCreateDialog() sẽ gán listener nội bộ vào BottomSheetDialog.
        Khi user swipe down hoặc gọi dismiss(), hệ thống kích hoạt listener nội bộ, và fragment tiếp tục gọi tiếp externalDismissListener.onDismiss().
        Activity nhận callback ngay lập tức, không cần phải quản lý getDialog() hay lệnh executePendingTransactions().
 */
@UnstableApi
public class MainActivity extends AppCompatActivity implements NormalPlayListFragment.FirstFragmentListener, FavoritePlayListFragment.SecondFragmentListener{

    private static final int REQUEST_READ_EXTERNAL = 1001;
    private ExoPlayer player;

    private LinearLayout layoutMiniPlayer;
    private ImageView miniCover;
    private TextView miniTitle;
    private ImageButton miniPlayPause;

    private String currentTitle, currentArtist;
    private int currentCoverRes , i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Kiểm tra quyền đọc external storage (bộ nhớ ngoài)
        //Chỉ thực hiện đoạn code bên trong {} nếu thiết bị đang chạy Android phiên bản Marshmallow (Android 6.0, API level 23) trở lên.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /*Dùng để kiểm tra xem ứng dụng đã được người dùng cấp quyền READ_EXTERNAL_STORAGE chưa.
             * Tham số đầu là Context (ở đây this thường là Activity), tham số hai là tên quyền.
             * Kết quả trả về là một trong hai giá trị:
             *  +PackageManager.PERMISSION_GRANTED (đã được cấp)
             *  +PackageManager.PERMISSION_DENIED (chưa được cấp)
             */
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //Yêu cầu cấp quyền (requestPermissions)
                /*
                 * Phương thức này sẽ bật dialog (hộp thoại) để hỏi người dùng (USER) có cho phép app sử dụng quyền này hay không.
                 * Tham số:
                 * this: Activity đang hiển thị, dùng để hiển thị dialog và nhận callback.
                 * new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}: Danh sách quyền cần yêu cầu (ở đây chỉ có 1 phần tử).
                 * REQUEST_READ_EXTERNAL: Mã số (int) bạn tự định nghĩa để phân biệt cuộc gọi cấp quyền này khi nhận kết quả.
                 */
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL
                );
            } else {
                // Nếu đã có quyền, khởi tạo ExoPlayer và phát nhạc
                initAndPlayExternalMp3();
            }
        } else {
            // Trên Android < 6.0 không cần xin runtime permission
            initAndPlayExternalMp3();
        }

        layoutMiniPlayer = findViewById(R.id.layout_mini_player);
        miniCover        = findViewById(R.id.miniCover);
        miniTitle        = findViewById(R.id.miniTitle);
        miniPlayPause    = findViewById(R.id.miniPlayPause);

        miniTitle.setSelected(true);
        //ẩn layoutMiniPlayer trước khi nhấn bài
        layoutMiniPlayer.setVisibility(View.GONE);

        // Giả sử bạn có thông tin bài hát hiện tại
//        currentTitle  = "Lac Troi";
//        currentArtist = "Sơn Tùng M-TP";
        currentCoverRes  = R.drawable.ic_album_placeholder;

//        miniTitle.setText(currentTitle);
        miniCover.setImageResource(currentCoverRes);

        // Khi người dùng bấm vào mini player -> mở BottomSheet
        //actionListener cho layout
        layoutMiniPlayer.setOnClickListener(v -> {

            //BottomSheetMusicDialogFragment sau khi vuốt xuống sẽ bị remove nên phải tạo lại mỗi lần nhấn
            DetailMusic sheet =
                    DetailMusic.newInstance();

            // 1. Gán listener ngay trước khi show (cái này để override lại phương thức dismiss của BottomSheetDialogFragment)
            sheet.setExternalOnDismissListener(d -> {
                // xử lý ở đây => khi BottomSheetDialogFragment bị kéo xuống(dismiss) thì bên actvity này sẽ cập nhật lại nút và tên nhạc hiện tại)
                updateButton();
                miniTitle.setText(getName());

                //Xóa hoàn toàn fragment cũ sau khi dismiss
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(sheet)
                        .commitAllowingStateLoss();
            });

            // 2. Show fragment
            sheet.show(getSupportFragmentManager(), sheet.getTag());
        });

        // Xử lý nút play/pause mini (tuỳ bạn implement media player)
        //test xem mỗi lần nhấn vào thì tittle trong BottomSheetMusicDialogFragment sẽ thay đổi theo( vì mỗi lần nhấn nó sẽ tạo một BottomSheetMusicDialogFragment mới)
        miniPlayPause.setOnClickListener(v -> {
            // TODO: play/pause music
            currentTitle = "bai moi" + i++;

            changeButton(); //thay đổi nút play/pause
        });


        //gán BottomNavigationView (thanh menu dưới cùng)
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);

        // Khi khởi tạo, hiển thị fragment mặc định
        // gán cái playlist fragment vào FrameLayout trong layout chính làm mặc định.
        // Và vì cái menu để đầu tiên trong thanh menu là playList nên mặc định của nó cũng hiện sáng ở menu PlayList trước
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new NormalPlayListFragment())
                    .commit();
        }

        //action của BottomNavigationView
        navView.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            //id của menu nào đang được chọn (hiện sáng)
            int id = item.getItemId();
            if (id == R.id.nav_playlist) {
                selected = new NormalPlayListFragment();
            }else if(id == R.id.nav_favorite){
                selected = new FavoritePlayListFragment();
            }

            //sau khi gán fragment selected rồi thì xuống dưới nó sẽ hiển thị fragment được chọn trong FrameLayout trong layout chính
            if (selected != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, selected)
                        .commit();
                return true; //trả về giá trị boolean
            }
            return false;
        });
    }

    //phương thức để lấy exoplayer cho bên BottomSheetDialogFragment dùng
    public ExoPlayer getExoPlayer() {
        return player;
    }

    public void changeButton(){
        int numberSong = player.getMediaItemCount();
        if(numberSong !=0) {
            if (player.isPlaying()) {
                player.pause();
                miniPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                player.play();
                miniPlayPause.setImageResource(R.drawable.ic_play_arrow);
            }
        }
    }

    public void updateButton(){
        miniPlayPause.setImageResource(
                player.isPlaying() ? R.drawable.ic_play_arrow : R.drawable.ic_pause
        );
    }
    /**Đoạn code này là phần xử lý kết quả sau khi bạn gọi ActivityCompat.requestPermissions(...) trong phần kiểm tra quyền phía trên
     * Khi người dùng tương tác với hộp thoại cấp quyền thì Android sẽ gọi lại onRequestPermissionsResult(...)
     * trong Activity của bạn.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 1. Kiểm tra xem đây có phải là lần gọi requestPermissions với mã REQUEST_READ_EXTERNAL hay không
        if (requestCode == REQUEST_READ_EXTERNAL) {

            // 2. grantResults[] chứa kết quả cho từng quyền trong permissions[]
            //    - Nếu length > 0 và grantResults[0] == PERMISSION_GRANTED → người dùng đã cho phép
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // 3. Nếu được cấp quyền: khởi tạo và phát file MP3 từ bộ nhớ ngoài
                initAndPlayExternalMp3();

            } else {
                // 4. Nếu người dùng từ chối cấp quyền:
                //    - Ở đây mình show một Toast để thông báo lý do cần quyền
                Toast.makeText(
                        this,
                        "Ứng dụng cần quyền đọc file để phát nhạc.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    private void initAndPlayExternalMp3() {
        // Khởi tạo ExoPlayer
        player = new ExoPlayer.Builder(this).build();

        // Chuẩn bị và bắt đầu phát
        player.prepare();
//        player.play();
    }


    public String getName(){
        MediaItem item = player.getMediaItemAt(player.getCurrentMediaItemIndex());
        Uri uri = item.localConfiguration.uri; // phiên bản Media3

        String fileName = uri.getLastPathSegment();

        return getMusicName(fileName);
    }

    public String getMusicName(String name){
        return name.replace(".mp3","");
    }

    public void changeSong(int location, ArrayList<String> songList){
        // Khi muốn thay mới hoàn toàn playlist:
//        player.stop();                          // (tùy chọn) dừng playback nếu đang chạy
        if(player.getMediaItemCount() !=0) {
            player.clearMediaItems();               // xóa hết items cũ
        }

        File musicDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
        );

        File mp3File;
        for(int i= 0; i< songList.size(); i++){
            mp3File = new File(musicDir, songList.get(i));

            if (mp3File.exists()) {
                Uri uri = Uri.fromFile(mp3File);
                player.addMediaItem(MediaItem.fromUri(uri));
            }
        }
        player.prepare();                       // chuẩn bị và bắt đầu buffering

        //ExoPlayer chuyển đến vị trí bài hát đã nhấn và chạy nếu đã ready
        player.seekToDefaultPosition(location);

        // Nếu muốn tự động phát ngay khi buffering xong: player.setPlayWhenReady(true);
        // Nếu muốn giữ ở trạng thái pause, chờ người dùng nhấn nút Play: player.setPlayWhenReady(false);
        player.setPlayWhenReady(true);

        miniPlayPause.setImageResource(R.drawable.ic_play_arrow);
        miniTitle.setText(getName());

        layoutMiniPlayer.setVisibility(View.VISIBLE);
    }
    @Override
    public void normalListClick(int location, ArrayList<String> songList) {
        changeSong(location, songList);

    }

    @Override
    public void favoriteListClick(int location, ArrayList<String> favoriteSongList) {
        changeSong(location,favoriteSongList);
    }
}