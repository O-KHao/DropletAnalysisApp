package com.example.dropletanalysisapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yalantis.ucrop.UCrop;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageButton OPEN_ALBUM;
    private ImageButton OPEN_CAMERA;
    private ImageView PHOTO;
    private Button ANALYSIS;
    public static final int CODE_TAKE_PHOTO = 1;//声明一个请求码，用于拍照返回的结果
    public static final int CODE_GET_PHOTO = 2;//声明一个请求码，用于图库取照片返回的结果
    //public static final int CROP_SUCCESS_CODE = 3;
    public static final int PHONE_CODE_CUT = 4;

    private Uri imageUri;
    public static File tempFile;
    //private Uri cropImageUri;
    public Bitmap srcBitmap;
    private Bitmap grayBitmap;
    private Bitmap threBitmap;

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.i("cv", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.i("cv", "OpenCV library found inside package. Using it!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        OPEN_ALBUM = (ImageButton) findViewById(R.id.OPEN_ALBUM);
        OPEN_CAMERA = (ImageButton) findViewById(R.id.OPEN_CAMERA);
        PHOTO = (ImageView) findViewById(R.id.PHOTO);
        ANALYSIS = (Button) findViewById(R.id.ANALYSIS);
        OPEN_ALBUM.setOnClickListener(this);
        OPEN_CAMERA.setOnClickListener(this);
        ANALYSIS.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.OPEN_ALBUM:
                openAlbum();
                break;
            case R.id.OPEN_CAMERA:
                openCamera(this);
                break;
            case R.id.ANALYSIS:
                Grayscale();
                break;
        }
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     * @Discription: 用于系统处理事件返回结果
     */
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case CODE_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    uCropPicture(imageUri);
                }
                break;
            case CODE_GET_PHOTO:
                if (resultCode == RESULT_OK) {

                    if (data != null) {
                        Uri uri = data.getData();
                        imageUri = uri;
                        uCropPicture(imageUri);
                    }

                }
                break;
            case PHONE_CODE_CUT:
                if (resultCode == RESULT_OK) {
                    try {
                        if (data != null) {
                            Uri photoUri = UCrop.getOutput(data);
                            imageUri = photoUri;
                            Bitmap bitmap = BitmapFactory.decodeStream(
                                    getContentResolver().openInputStream(imageUri));
                            PHOTO.setImageBitmap(bitmap);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }


    /**
     * @param activity
     * @Author: Linzs email:linzs.online@gmail.com
     * @Date: 2019/10/24
     * @Description: 打开系统摄像头并返回图片（处理了兼容性问题）
     */
    public void openCamera(Activity activity) {
        int currentapiVersion = Build.VERSION.SDK_INT;// 获取系統版本
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);// 申请调用系统相机

        // 判断存储卡是否可以用，可用进行存储
        if (hasSdcard()) {
            //设置一个日期格式
            SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
            //图片的命名为日期时间
            String filename = timeStampFormat.format(new Date());
            tempFile = new File(Environment.getExternalStorageDirectory(),
                    filename + ".jpg");

            //判断系统版本号,兼容Android 7.0
            //如果系统版本号小于Android 7.0
            if (currentapiVersion < Build.VERSION_CODES.N) {
                // 从文件中创建uri
                imageUri = Uri.fromFile(tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            } else {
                //当系统版本号大于Android 7.0使用共享文件的形式
                ContentValues contentValues = new ContentValues(1);
                contentValues.put(MediaStore.Images.Media.DATA, tempFile.getAbsolutePath());
                //检查是否有存储权限，以免崩溃
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //申请内存写权限
                    Toast.makeText(this, "请开启存储权限", Toast.LENGTH_SHORT).show();
                    return;
                }
                imageUri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            }
        }
        // 开启一个带有返回值的Activity，请求码为CODE_TAKE_PHOTO
        activity.startActivityForResult(intent, CODE_TAKE_PHOTO);
    }

    /**
     * @return
     * @Author: Linzs email:linzs.online@gmail.com
     * @Date: 2019/10/24
     * @Description: 判断sdcard是否被挂载
     */
    public static boolean hasSdcard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * @Author: Linzs email:linzs.online@gmail.com
     * @Date: 2019/10/24
     * @Description: 从图库中提取照片
     */
    public void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        // 开启一个带有返回值的Activity，请求码为GET_PHOTO
        startActivityForResult(intent, CODE_GET_PHOTO);
    }

    /**
     * @Author: Linzs email:linzs.online@gmail.com
     * @Date: 2019/10/25
     * @Description: 调用CV库灰度化图片
     */
    public void Grayscale() {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Mat threMat = new Mat();
        srcBitmap = ((BitmapDrawable) PHOTO.getDrawable()).getBitmap();//从imgview中获得bitmap图
        grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        threBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.bitmapToMat(srcBitmap, rgbMat);//把image转化为Mat
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//灰度化
        Imgproc.adaptiveThreshold(grayMat, threMat,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,15,10);//灰度化
        Utils.matToBitmap(threMat, threBitmap);//把Mat转化为Bitmap
        imageDisplay(threBitmap);//显示出来
        rgbMat.release();
        grayMat.release();
    }

    /**
     * @Author: Linzs email:linzs.online@gmail.com
     * @Date: 2019/11/08
     * @Description: 调用外部的裁剪库裁剪图片
     */
    private void uCropPicture(Uri uri) {
        Uri sourceUri = uri;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        String imageName = simpleDateFormat.format(date);
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), imageName + ".jpeg"));
        UCrop.of(sourceUri, destinationUri).start(this, PHONE_CODE_CUT);

    }

    private void imageDisplay(Bitmap bitmap) {
        PHOTO.setImageBitmap(bitmap);//显示出来
    }

}