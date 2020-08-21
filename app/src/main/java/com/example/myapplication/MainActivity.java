package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sh1r0.caffe_android_demo.CNNListener;
import com.sh1r0.caffe_android_lib.CaffeMobile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity  {

    private static final String LOG_TAG = "test_caffe";
    private static final int REQUEST_IMAGE_PICK = 200;
    CaffeMobile caffeMobile;
    String[] IMAGENET_CLASSES;
    CNNTask cnnTask;

    Button btn;
    TextView textView;
    ImageView imageView;
    private Bitmap bmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkMyPermission();
        imageView=findViewById(R.id.imageView);
        textView=findViewById(R.id.textView);
        btn =findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,REQUEST_IMAGE_PICK);
            }
        });
        init_net();
    }
    void init_net(){
        // 1. 定义模型路径
        String modelDir = "/sdcard/caffe_mobile/bvlc_reference_caffenet";
        String modelProto = modelDir + "/deploy.prototxt";
        String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";
        // 2 加载标签列表
        AssetManager am = this.getAssets();
        try {
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 3. 创建caffe 解释器实例
        caffeMobile = new CaffeMobile();
        caffeMobile.setNumThreads(4);
        caffeMobile.loadModel(modelProto, modelBinary);
        float[] meanValues = {104, 117, 123};
        caffeMobile.setMean(meanValues);
        // 4 创建输出结果监听器
        cnnTask = new CNNTask(new CNNListener() {
            @Override
            public void onTaskCompleted(int i) {
                Toast.makeText(getApplicationContext(), IMAGENET_CLASSES[i],Toast.LENGTH_SHORT).show();
                textView.setText("鉴定结果："+IMAGENET_CLASSES[i]);
            }
        });
    }
    void run_caffe(String imgPath){

        cnnTask.execute(imgPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_IMAGE_PICK&&resultCode==RESULT_OK){
            String imgPath;

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            imgPath = cursor.getString(columnIndex);
            cursor.close();
            bmp = BitmapFactory.decodeFile(imgPath);
            imageView.setImageBitmap(bmp);
            run_caffe(imgPath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    private class CNNTask extends AsyncTask<String, Void, Integer> {
        private CNNListener listener;
        private long startTime;

        public CNNTask(CNNListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            startTime = SystemClock.uptimeMillis();
            return caffeMobile.predictImage(strings[0])[0];
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
            listener.onTaskCompleted(integer);
            super.onPostExecute(integer);
        }
    }
    private void checkMyPermission() {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
    }
}