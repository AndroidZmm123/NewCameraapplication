package com.example.cameraapplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    //打开相机的返回码
    private static final int CAMERA_REQUEST_CODE = 1;
    //选择图片的返回码
    private static final int IMAGE_REQUEST_CODE = 2;
    //剪切图片的返回码
    public static final int CROP_REREQUEST_CODE = 3;
    private ImageView iv;

    //相机
    public static final int REQUEST_CODE_PERMISSION_CAMERA = 100;

    public static final int REQUEST_CODE_PERMISSION_GALLERY = 101;

    //照片图片名
    private String photo_image;
    //截图图片名
    private String crop_image;

    //拍摄的图片的真实路径
    private String takePath;
    //拍摄的图片的虚拟路径
    private Uri imageUri;
    private Uri cropUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = findViewById(R.id.iv);
    }

    /**
     * 拍照
     *
     * @param view
     */

    public void onClickTakePhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(REQUEST_CODE_PERMISSION_CAMERA);
            return;
        }
        openCamera();
    }

    /**
     * 打开系统的相机的时候。我们需要传入一个uri。该uri就是拍摄的照片的地址。
     * 也就是：cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, getImageUri());
     * 这里就用到了FileProvider
     */
    private void openCamera() {
        if (isSdCardExist()) {
            Intent cameraIntent = new Intent(
                "android.media.action.IMAGE_CAPTURE");

            photo_image = new SimpleDateFormat("yyyy_MMdd_hhmmss").format(new Date()) + ".jpg";
            imageUri = getImageUri(photo_image);
            //Log.e("zmm", "图片存储的uri---------->" + imageUri);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT
                , imageUri);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "SD卡不存在", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 打开图库
     * 不需要用FileProvider
     *
     * @param view
     */
    public void onClickOpenGallery(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(REQUEST_CODE_PERMISSION_GALLERY);
            return;
        }
        openGallery();
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_REQUEST_CODE);
    }

    /**
     * @param path 原始图片的路径
     */
    public void cropPhoto(String path) {
        crop_image = new SimpleDateFormat("yyyy_MMdd_hhmmss").format(new Date()) + "_crop" +
            ".jpg";
        File cropFile = createFile(crop_image);
        File file = new File(path);


        Intent intent = new Intent("com.android.camera.action.CROP");
        //TODO:访问相册需要被限制，需要通过FileProvider创建一个content类型的Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //TODO:访问相册需要被限制，需要通过FileProvider创建一个content类型的Uri
            imageUri = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider", file);
            cropUri = Uri.fromFile(cropFile);
            //TODO:cropUri 是裁剪以后的图片保存的地方。也就是我们要写入此Uri.故不需要用FileProvider
            //cropUri = FileProvider.getUriForFile(getApplicationContext(),
            //    BuildConfig.APPLICATION_ID + ".provider", cropFile);
        } else {
            imageUri = Uri.fromFile(file);
            cropUri = Uri.fromFile(cropFile);
        }

        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("crop", "true");
        //设置宽高比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        //设置裁剪图片宽高
        intent.putExtra("outputX", 400);
        intent.putExtra("outputY", 400);
        intent.putExtra("scale", true);
        //裁剪成功以后保存的位置
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, CROP_REREQUEST_CODE);


    }


    /**
     * 获得一个uri。该uri就是将要拍摄的照片的uri
     *
     * @return
     */
    private Uri getImageUri(String name) {
        if (isSdCardExist()) {
            File file = createFile(name);
            if (file != null) {
                takePath = file.getAbsolutePath();
                Log.e("zmm", "图片的路径---》" + takePath);
                // 输出是/storage/emulated/0/Android/data/com.example.zongm.testapplication/files/2018_0713_111455.jpg
                // 根据这个path。拿到的Uri是：content://com.example.zongm.testapplication.provider/files_root/files/2018_0713_111455.jpg
                //我们可以看到真实路径：/Android/data/com.example.zongm.testapplication这一部分被files_root替代了
                //也就是我们在file_path里面写的<external-path
                //            name="files_root"
                //            path="Android/data/com.example.zongm.testapplication/" />
                //其中external-path代表的是 Environment.getExternalStorageDirectory() 也就是/storage/emulated/0
                //。。。。我说的有点乱。大家还是看那篇简书文章吧。：链接：https://www.jianshu.com/p/56b9fb319310
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //FileProvider.getUriForFile();第一个参数是context.
                    // 第二个值。比较关键、这个也就是我们在manifest里面的provider里面的
                    //android:authorities="com.example.zongm.testapplication.provider"
                    //因为我用的就是AppId.所以。这里就直接用BuildConfig.APPLICATION_ID了。
                    //如果你的android:authorities="test.provider"。那这里第二个参数就应该是test.provider
                    return FileProvider.getUriForFile(getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider", file);
                } else {
                    return Uri.fromFile(file);
                }

            }
        }
        return Uri.EMPTY;
    }

    public File createFile(String name) {
        if (isSdCardExist()) {
            File[] dirs = ContextCompat.getExternalFilesDirs(this, null);
            if (dirs != null && dirs.length > 0) {
                File dir = dirs[0];
                return new File(dir, name);
            }
        }

        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("zmm", "reque:" + requestCode + "result:" + resultCode + "data:" + (data == null));
        if (data != null) {
            Log.e("zmm", "data:value:" + data.getStringExtra("filePath"));
            Log.e("zmm", "data:value:" + data.getData());
        }

        //if(data!=null){
        //    Log.e("zmm","data:value:"+data.getData());
        //}
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE://拍照成功并且返回
                    //注意这里。可以直接用takePath。也可以直接用imageUri。
                    //因为Glide直接加载Uri。也可以加载地址。
                    //Glide.with(this)
                    //    .asBitmap()
                    //    .load(imageUri)
                    //    .into(iv);


                    //但是。这里加载的都是拍摄的原图。一般我们都会根据uri。或者path.找到文件。把bitmap取出来。然后做压缩等其他的二次处理。


                    //decodeImage(imageUri);//显示照片

                    //或者直接去裁剪

                    //这里有个坑。就是我们如果想要根据Uri---》图片的真实path.然后拿到File.一般的Uri.
                    // 例如是从图库选择照片并且回来的图片。我们拿到的Uri是这样的：
                    //content://com.android.providers.media.documents/document/image%3A732871
                    //或者这样：content://media/external/images/media/694091
                    //这样我们可以用ImageUtils.getPath()这个里面的一系列方法拿到真实路径。
                    //但是。如果是通过我们的FileProvider拿到的Uri.是这样的：
                    //content://com.example.zongm.testapplication.provider/files_root/files/2018_0713_020952.jpg
                    //这样的路径我们是用ImageUtils.getPath()这个里面的一系列方法是拿不到真实路径的。会报错：
                    //报错信息是：GetDataColumnFail java.lang.IllegalArgumentException: column '_data'does not exist
                    //我们在网上查一下。就可以知道。要想拿到FileProvider得到的Uri的真实图片路径。需要用到反射：
                    //这里大家可以去查一下：这里随便给一个博客地址。：https://blog.csdn.net/u010853225/article/details/80191880
                    // 故这里我们不能用此方法拿到真实路径 String path = ImageUtils.getPath(this, imageUri);
                    cropPhoto(takePath);

                    break;

                case IMAGE_REQUEST_CODE://选择图片成功返回
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();

                        //直接显示出来
                        //decodeImage(data.getData());
                        //或者去裁剪
                        //根据uri拿到真实路径
                        String path = ImageUtils.getPath(this, imageUri);

                        File file = new File(imageUri.getPath());

                        Log.e("zmm", "选择的图片的虚拟地址是------------>" + data.getData() + "--->" +
                            path + "--->" + file.getAbsolutePath());

                        //根据真实路径生成一个uri
                        imageUri = ImageUtils.getUri(this, path);
                        Log.e("zmm", "转换以后的路径:" + imageUri);
                        crop(imageUri, getOutCropUri());
                        //try {
                        //    file = new File(new URI(imageUri.toString()));
                        //    Log.e("zmm", "----------->" + file.getAbsolutePath());
                        //} catch (URISyntaxException e) {
                        //    e.printStackTrace();
                        //}
                        //
                        //cropPhoto1(file);
                    }
                    break;
                case CROP_REREQUEST_CODE:
                    Log.e("zmm", "裁剪以后的地址是------------>" + cropUri);


                    decodeImage(cropUri);
                    break;
            }
        }
    }

    private Uri getOutCropUri() {
        crop_image = new SimpleDateFormat("yyyy_MMdd_hhmmss").format(new Date()) + "_crop" +
            ".jpg";
        File cropFile = createFile(crop_image);
        cropUri = Uri.fromFile(cropFile);
        return cropUri;
    }

    private int aspectX = 1;
    private int aspectY = 2;

    private void crop(Uri inUri, Uri outUri) {
        if (inUri == null) {
            return;
        }


        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(inUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        intent.putExtra("noFaceDetection", true);

        if (intent.resolveActivity(getPackageManager()) != null) {
            Log.e("zmm", "安装了裁剪程序...");
            startActivityForResult(intent, CROP_REREQUEST_CODE);
        } else {
            // 没有安装所需应用
            Log.e("zmm", "没有安装所需应用...");
        }
    }

    /**
     * 根据uri拿到bitmap
     *
     * @param imageUri 这个Uri是
     */
    private void decodeImage(Uri imageUri) {
        //这样是可以正常拿到bitmap。但是我们知道。这样写。很有可能会oom
        //Bitmap bitmap = ImageUtils.decodeUriAsBitmap(this, imageUri);
        //Log.e("zmm", "初始大小-------------->" + bitmap.getByteCount());//原始大小是47235072
        //iv.setImageBitmap(bitmap);
        //所以我们一般都是把bitmap 进行一次压缩
        try {
            Bitmap bitmapFormUri = ImageUtils.getBitmapFormUri(this, imageUri);
            //Log.e("zmm", "压缩过后------------->" + bitmapFormUri
            //    .getByteCount());//压缩过后2952192
            iv.setImageBitmap(bitmapFormUri);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("zmm", "exception:" + e.getMessage());
        }

        String path = ImageUtils.getPath(this, imageUri);
        Log.e("zmm", "裁剪以后的真实地址:" + path);
        Glide.with(this).load(path).into(iv);
    }


    /**
     * 检查权限
     *
     * @param requestCode
     */
    private void checkPermission(int requestCode) {

        boolean granted = PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
            Manifest.permission_group.CAMERA);
        if (granted) {//有权限
            if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
                openCamera();//打开相机
            } else {
                openGallery();//打开图库
            }
            return;
        }
        //没有权限的要去申请权限
        //注意：如果是在Fragment中申请权限，不要使用ActivityCompat.requestPermissions,
        // 直接使用Fragment的requestPermissions方法，否则会回调到Activity的onRequestPermissionsResult
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest
                .permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            boolean flag = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    flag = false;
                    break;
                }
            }
            //权限通过以后。自动回调拍照
            if (flag) {
                if (requestCode == REQUEST_CODE_PERMISSION_CAMERA) {
                    openCamera();//打开相机
                } else {
                    openGallery();//打开图库
                }
            } else {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 检查SD卡是否存在
     */
    public boolean isSdCardExist() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

}
