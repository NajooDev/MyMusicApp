package com.example.mymusic;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerControlView;

import com.example.mymusic.Sqlite.Sqlite;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Đây là một class đã extends BottomSheetDialogFragment
 */
@SuppressLint("ClickableViewAccessibility")
@UnstableApi
public class DetailMusic extends BottomSheetDialogFragment {
    // 1. Khai báo biến giữ listener bên ngoài
    private DialogInterface.OnDismissListener externalDismissListener;

    //Các biến khai báo còn lại
    private PlayerControlView playerControlView;

    private ArrayAdapter<String> adapter;

    private ArrayList<String> arrayList;

    private ListView musicList;

    private ImageButton playButton, pauseButton, favoriteButton, btnCollapse;

    private ExoPlayer player;

    Player.Listener myExoPlayerListener;

    ArrayList<String> favoriteList = new ArrayList<>();

    Sqlite sqlite;

    //
    BottomSheetBehavior<View> behavior;

    private String location;


    // Dùng method này để truyền dữ liệu từ Activity nếu cần (gọi mỗi lần tạo mới)
    public static DetailMusic newInstance() {
        DetailMusic fragment = new DetailMusic();
//        Bundle args = new Bundle();
//        args.putString("location", location); // Lấy dữ liệu từ arguments : getArguments().getString("title")
//        fragment.setArguments(args);
        return fragment;
    }

    //Tạo View cho fragment này
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail_music, container, false);

        //tao sqlite
        sqlite = new Sqlite(getContext());

        favoriteList.addAll(sqlite.getAll());
        // lấy exoplayer từ activity
        player = ((MainActivity)getActivity()).getExoPlayer();

        //gán playercontrolview
        playerControlView = view.findViewById(R.id.player_control_view);


        //MUSIC LIST
        musicList = view.findViewById(R.id.music_list);

        //arrayLIst
        arrayList = new ArrayList<>();

        //ADAPTER (Không cần viết custom Adapter nếu bạn chỉ muốn đổi màu nền (hoặc drawable) cho item đang được chọn – bạn có thể dùng thẳng ArrayAdapter với layout tự tạo và selector như sau:)
        adapter = new ArrayAdapter<>(requireContext(), R.layout.music_item, R.id.text1, arrayList);   // có thể dùng getContext() nhưng nên ensure non-null

        //set adapter vào musicList (ListView)
        musicList.setAdapter(adapter);

        //set exoplayer vào playercontrolview để điều khiển (next, prev,... cho danh sách bài hát)
        playerControlView.setPlayer(player);

        favoriteButton = playerControlView.findViewById(R.id.exo_favorite);

        // Bind thủ công play/pause
        // (vì khi dùng custom playerControlView Nút exo_play, exo_pause không tự động bind trừ khi dùng layout mặc định hoặc tự set listener thủ công.
        //Các nút next, previous, rewind, fastforward thì được PlayerControlView xử lý qua setPlayer().
        // dòng android:id="@id/exo_play" phải chuyển thành android:id="@+id/exo_play" thì mới tìm được (ý là dùng custom nên hai nút đó không tìm được, phải +id tức là tạo mới button đó rồi bind play(), pause() sau)
        playButton = playerControlView.findViewById(R.id.exo_play);
        pauseButton = playerControlView.findViewById(R.id.exo_pause);

        btnCollapse = view.findViewById(R.id.btnCollapse);
        return view;
    }

    //

    //Xử lý logic của fragment

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //di chuyển đến vị trí nhạc đang phát trong ListView
        musicList.setSelection(player.getCurrentMediaItemIndex());

        //action của nút collapse (nhấn vào thì BottomSheetDialogFragment sẽ hạ xuống và bị dismiss)
        btnCollapse.setOnClickListener(v -> {
            if (behavior != null) {
                // Nếu đang full, collapse xuống peek height (hoặc 0 để ẩn)
                if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        //set ActionListener thủ công cho play và pause button
        playButton.setOnClickListener(v -> {
            if (player != null) {
                player.play();
            }
        });

        pauseButton.setOnClickListener(v -> {
            if (player != null) {
                player.pause();
            }
        });

        favoriteButton.setOnClickListener(v -> {
            int currentIndex = player.getCurrentMediaItemIndex();
            if(favoriteButton.isSelected()){
                favoriteButton.setSelected(false);
                removeFavorite(currentIndex);

            }else{
                favoriteButton.setSelected(true);
                addFavorite(currentIndex);
            }
        });

        //action musicList (nhấn vào thì nó sẽ làm đậm background của nhạc được chọn)
        musicList.setOnItemClickListener((parent, view1, position, id) -> {

            //ExoPlayer chuyển đến vị trí bài hát đã nhấn và chạy nếu đã ready
            player.seekToDefaultPosition(position);

            // Nếu muốn tự động phát ngay khi buffering xong: player.setPlayWhenReady(true);
            // Nếu muốn giữ ở trạng thái pause, chờ người dùng nhấn nút Play: player.setPlayWhenReady(false);
            player.setPlayWhenReady(true);
        });

        //actionListener của exoplayer
        //phải làm cách này để có thể remove listener vì khi BottomSheetDialogFragment bị dismiss thì bên MainActivity không cần những phương thức này
        myExoPlayerListener = new Player.Listener() {
            // Gọi mỗi khi media item mới bắt đầu phát (player.setPlayWhenReady(true);)
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {

                int currentIndex = player.getCurrentMediaItemIndex();

                Log.d("position", currentIndex+"");
                //set android:state_activated="true" cho vị trí được nhấn để nó đổi màu
                musicList.setItemChecked(currentIndex, true);

                checkFavoriteSong(currentIndex);
            }

            // Phát hoặc tạm dừng được kích hoạt
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {

                //set lại nút để hiển thị nút pause và ẩn nút play
                if(playWhenReady) {
                    playButton.setVisibility(View.GONE); //vì hai nút này chồng lên nhau nên làm vậy để nhấn nút này thì nút kia bị ẩn)
                    pauseButton.setVisibility(View.VISIBLE);
                }else{
                    playButton.setVisibility(View.VISIBLE); //vì hai nút này chồng lên nhau nên làm vậy để nhấn nút này thì nút kia bị ẩn)
                    pauseButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                // 1) Xác định đây là lỗi IO/read
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND     // ExoPlayer 2.15+
                        || error.getCause() instanceof FileNotFoundException) {
                    // 2) Thông báo cho user
                    Toast.makeText(getContext(), "nhạc đã bị xóa", Toast.LENGTH_SHORT).show();
                    int currentIndex = player.getCurrentMediaItemIndex();

                    //xóa trên mảng
                    arrayList.remove(currentIndex);
                    adapter.notifyDataSetChanged();

                    //xóa trong exoplayer
                    player.stop();
                    player.removeMediaItem(currentIndex);
                    player.prepare();

                    //phát lại vị trí hiện tại(vị trí mới của nhạc tiếp theo vì nhạc hiện tại đã bị xóa)
                    player.seekToDefaultPosition(currentIndex);
                    player.setPlayWhenReady(true);
                } else {
                    // Các lỗi khác
                    Toast.makeText(getContext(), "lỗi khác", Toast.LENGTH_SHORT).show();
                }
            }
        };

        player.addListener(myExoPlayerListener);

        //update lại nút, danh sách nhạc và nút yêu thích của bài hiện tại(nếu có)
        updateFirstEnter();
    }



    public void checkPlaying(){
        if(player.isPlaying()) {
            playButton.setVisibility(View.GONE); //vì hai nút này chồng lên nhau nên làm vậy để nhấn nút này thì nút kia bị ẩn)
            pauseButton.setVisibility(View.VISIBLE);
        }else{
            playButton.setVisibility(View.VISIBLE); //vì hai nút này chồng lên nhau nên làm vậy để nhấn nút này thì nút kia bị ẩn)
            pauseButton.setVisibility(View.GONE);
        }
    }

    public void updateList(int currentPosition){
        //lấy từng tên trong exoplayer rồi thêm vào arrayList hiển thị ListView
        int count = player.getMediaItemCount();
        String fileName;
        for (int i = 0; i < count; i++) {
            MediaItem item = player.getMediaItemAt(i);
            Uri uri = item.localConfiguration.uri; // phiên bản Media3

            fileName = uri.getLastPathSegment();

            arrayList.add(getMusicName(fileName));
        }

        musicList.setItemChecked(currentPosition, true);

        //cập nhật lại list
        adapter.notifyDataSetChanged();
    }

    public void checkFavoriteSong(int currentPosition){

        MediaItem item = player.getMediaItemAt(currentPosition);
        Uri uri = item.localConfiguration.uri; // phiên bản Media3

        String fileName = uri.getLastPathSegment();
        if(favoriteList.contains(fileName)){
            favoriteButton.setSelected(true);
        }else{
            favoriteButton.setSelected(false);
        }
    }


    public void updateFirstEnter(){
        int currentIndex = player.getCurrentMediaItemIndex();
        //Kiểm tra nếu nhạc đang phát thì update lại nút play or pause(vì mỗi lần nhấn vào BottomSheetMusicDialogFragment là mỗi lần tạo mới mà)
        checkPlaying();

        //update lại danh sách nhạc đồng thời hiển thị nhạc đang phát hiện tại luôn
        updateList(currentIndex);

        //update lại nút favorite của bài hát hiện tại(nếu có)
        checkFavoriteSong(currentIndex);
    }


    public void addFavorite(int currentPosition){
        MediaItem item = player.getMediaItemAt(currentPosition);
        Uri uri = item.localConfiguration.uri; // phiên bản Media3

        String fileName = uri.getLastPathSegment();

        favoriteList.add(fileName);

        sqlite.addFavoriteMusic(fileName);
    }

    public void removeFavorite(int currentPosition){
        MediaItem item = player.getMediaItemAt(currentPosition);
        Uri uri = item.localConfiguration.uri; // phiên bản Media3

        String fileName = uri.getLastPathSegment();

        favoriteList.remove(fileName);

        sqlite.removeFavoriteMusic(fileName);
    }

    //phương thức bỏ .mp3 khỏi tên được lấy trong exoplayer để nhìn cho đẹp
    public String getMusicName(String name){
        return name.replace(".mp3","");
    }

    /**
     * 2. Phương thức này cho phép Activity hoặc bất cứ ai tạo fragment
     *    có thể gán một OnDismissListener riêng.
     */
    public void setExternalOnDismissListener(DialogInterface.OnDismissListener l) {
        this.externalDismissListener = l;
    }



    //Cái này dùng để thay đổi chiểu cao (dưới đây là chỉnh sao cho nhấn vào thì nó hiển thị full cả màn hình luôn)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // 2. Đặt một OnShowListener để biết khi nào dialog đã sẵn sàng.
        // setOnShowListener(...) đảm bảo bạn đang thao tác trên Dialog đã hoàn thành khâu inflate và attach.
        //sự kiện này chắc chắn chạy sau khi dialog đã được khởi tạo và trước khi nó hiển thị.
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                // 1. Đặt chiều cao match_parent
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();

                // 2. Thiết lập behavior
                behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setDraggable(false);

                behavior.setFitToContents(false);               // Cho phép mở toàn màn hình
                behavior.setExpandedOffset(0);                  // Khi expanded thì offset = 0
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            }

            // 3. Bên trong, gán OnDismissListener “nội bộ”
            d.setOnDismissListener(internal -> {
                player.removeListener(myExoPlayerListener);
                // 4. Khi dialog dismiss, gọi tiếp ra listener bên ngoài (nếu có)
                if (externalDismissListener != null) {
                    externalDismissListener.onDismiss(internal);
                }
            });
        });

        return dialog;
    }

}

