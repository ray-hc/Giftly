package com.rayhc.giftly;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class VideoActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PICK_FROM_GALLERY = 2;

//    //storage ref
//    private FirebaseStorage mStorage;
//    private StorageReference storageRef;

    //widgets
    private VideoView mVideoView;
    private Button mChooseButton, mSaveButton, mCancelButton, mDeleteButton;
    private MyMediaController mMediaController;

    //data from gift
    private Gift mGift;
    private Uri currentData;

    //from review
    private boolean mFromReview;
    private String mFileLabel;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //firebase stuff
//        mStorage = FirebaseStorage.getInstance();
//        storageRef = mStorage.getReference();

        //get data from gift
        Intent startIntent = getIntent();
        mGift = (Gift) startIntent.getSerializableExtra(Globals.CURR_GIFT_KEY);
        Log.d("LPC", "onCreate: saved gift: "+mGift.toString());
        mFromReview = startIntent.getBooleanExtra(Globals.FROM_REVIEW_KEY, false);
        mFileLabel = startIntent.getStringExtra(Globals.FILE_LABEL_KEY);

        //wire button and video view
        mChooseButton = (Button) findViewById(R.id.video_choose_button);
        mSaveButton = (Button) findViewById(R.id.video_save_button);
        mSaveButton.setEnabled(false);
        mDeleteButton = (Button) findViewById(R.id.video_delete_button);
        mDeleteButton.setVisibility(View.GONE);
        mCancelButton = (Button) findViewById(R.id.video_cancel_button);
        mVideoView = (VideoView) findViewById(R.id.chosen_video);

        //add a media controller
        mMediaController = new MyMediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        //wire button callbacks
        mChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChoose();
            }
        });
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave();
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDelete();
            }
        });

        //handle if from the review activity
        if(startIntent.getBooleanExtra(Globals.FROM_REVIEW_KEY, false)){
            mSaveButton.setEnabled(true);
            mDeleteButton.setVisibility(View.VISIBLE);
            mVideoView.setVideoURI(null);
            Log.d("LPC", "review uri: "+Uri.parse(mGift.getContentType().get(mFileLabel)));
            mVideoView.setVideoURI(Uri.parse(mGift.getContentType().get(mFileLabel)));
            mVideoView.start();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("SAVED_GIFT", mGift);
    }

    //*******BUTTON CALLBACKS*******//
    public void onChoose() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FROM_GALLERY);
    }

    /**
     * Update the content type of the gift with a video and its URI
     */
    public void onSave() {
        String key = "video_" + Globals.sdf.format(new Date(System.currentTimeMillis()));
        mGift.getContentType().put(key, "content://media/" + currentData.getPath());
        //delete the old file if its a replacement
        if(mFileLabel != null) mGift.getContentType().remove(mFileLabel);
        Log.d("LPC", "just video image: "+mGift.getContentType().get(key));
        Intent intent = new Intent(this, FragmentContainerActivity.class);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        startActivity(intent);
    }

    /**
     * Remove the chosen video from the gifts contents
     */
    public void onDelete(){
        Intent intent = new Intent(this, FragmentContainerActivity.class);
        mGift.getContentType().remove(mFileLabel);
        intent.putExtra(Globals.CURR_GIFT_KEY, mGift);
        startActivity(intent);
    }

    //******ON ACTIVITY RESULT******//

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if gallery pick was successful, save the URI and populate the video view
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_FROM_GALLERY && data != null) {
            mSaveButton.setEnabled(true);
            Uri selectedData = data.getData();
            currentData = selectedData;
            Log.d("LPC", "onActivityResult: current video path for vv: "+currentData.getPath());
            mVideoView.setVideoURI(currentData);
            mVideoView.start();
        }
    }

    /**
     * Custom Media Controller so it doesn't disappear
     */
    public static class MyMediaController extends MediaController {
        public MyMediaController(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyMediaController(Context context, boolean useFastForward) {
            super(context, useFastForward);
        }

        public MyMediaController(Context context) {
            super(context);
        }

        @Override
        public void show(int timeout) {
            super.show(0);
        }

    }
}
