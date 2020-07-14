package com.example.hiddencameracapture;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by IT02106 on 17/05/2018.
 */

public class CameraManager implements  Camera.PictureCallback, Camera.ErrorCallback, Camera.PreviewCallback,Camera.AutoFocusCallback{
    private static CameraManager mManager;
    private Context mContext;
    private Camera mCamera;
    private SurfaceTexture mSurface;

    public CameraManager(Context context)
    {
        mContext = context;
    }

    public static CameraManager getInstance(Context context)
    {
        if(mManager == null )
            mManager = new CameraManager(context);
        return  mManager;
    }

    public void takePhoto()
    {
        if(isFrontCameraAvailable())
        {
            initCamera();
        }
    }

    private boolean isFrontCameraAvailable()
    {
        boolean result = false;
        if(mContext!=null && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            int numberOfCameras = Camera.getNumberOfCameras();

            for(int i = 0;i<numberOfCameras;i++)
            {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);

                if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    private void initCamera()
    {

        new AsyncTask() {



            @Override
            protected Object doInBackground(Object[] objects) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                return null;
            }

            @Override
            protected void onPostExecute(Object object)
            {
                try {
                    if(mCamera!=null)
                    {

                        mSurface = new SurfaceTexture(123);
                        mCamera.setPreviewTexture(mSurface);

                        Camera.Parameters params = mCamera.getParameters();
                        int angle = 270;//getCameraRotationAngle(Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
                        params.setRotation(angle);


                        if(autoFocusSupported(mCamera))
                        {
                            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                        }
                        else
                        {
                            Log.w("asdaxxx","Autofocus is not supported");
                        }

                        mCamera.setParameters(params);
                        mCamera.setPreviewCallback(CameraManager.this);
                        mCamera.setErrorCallback(CameraManager.this);
                        mCamera.startPreview();
                        muteSound();




                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    releaseCamera();
                }

            }




        }.execute();
    }

    private boolean autoFocusSupported(Camera camera)
    {
        if(camera != null)
        {
            Camera.Parameters parames = camera.getParameters();
            List focusModes = parames.getSupportedFocusModes();

            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            {
                return true;
            }



        }

        return false;
    }

    private void muteSound()
    {
        if(mContext != null)
        {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                mgr.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE,0);
            } else
            {
                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        }
    }

    private void releaseCamera()
    {
        if(mCamera != null)
        {
            mCamera.release();
            mSurface.release();
            mCamera = null;
            mSurface = null;
        }

        unmuteSound();
    }

    private void unmuteSound()
    {
        if(mContext != null)
        {
            AudioManager mgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                mgr.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE,0);
            }
            else
            {
                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            }
        }
    }


    @Override
    public void onError(int error, Camera camera) {

        switch (error) {
            case Camera.CAMERA_ERROR_SERVER_DIED:
                Log.e(TAG, "Camera error: Media server died");
                break;
            case Camera.CAMERA_ERROR_UNKNOWN:
                Log.e(TAG, "Camera error: Unknown");
                break;
            case Camera.CAMERA_ERROR_EVICTED:
                Log.e(TAG, "Camera error: Camera was disconnected due to use by higher priority user");
                break;
            default:
                Log.e(TAG, "Camera error: no such error id (" + error + ")");
                break;
        }
    }


    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        try
        {
            if(autoFocusSupported(camera))
            {
                // mCamera.autoFocus(this);
                camera.setPreviewCallback(null);
                camera.takePicture(null,null,this);
            }
            else
            {

                camera.setPreviewCallback(null);
                camera.takePicture(null,null,this);
            }
        } catch (Exception e) {

            Log.e(TAG, "Camera error while taking picture");
            e.printStackTrace();
            releaseCamera();
        }
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        if(camera != null)
        {
            try
            {
                camera.takePicture(null,null,this);
                mCamera.autoFocus(null);

            }catch (Exception e)
            {

                e.printStackTrace();
                releaseCamera();
            }
        }


    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {

        savePicture(bytes);
        releaseCamera();
    }

    private String savePicture(byte[] bytes)
    {
        String filepath = null;

        try
        {
            File pictureFileDir = getDir();
            if(bytes == null)
            {
                Toast.makeText(mContext, "cant save image", Toast.LENGTH_LONG).show();
                Log.e("asdaxxx","Can't save image - no data");
                return null;
            }

            if(!pictureFileDir.exists() && !pictureFileDir.mkdirs())
            {
                Toast.makeText(mContext, "Can't create directory to save image", Toast.LENGTH_LONG).show();
                Log.e("asdaxxx","Can't create directory to save image.");
                return null;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "iselfieapp_" + date + ".jpg";

            filepath = pictureFileDir.getPath() + File.separator + photoFile;
            Log.d("asdaxxx",filepath);


            File pictureFile = new File(filepath);
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(bytes);
            fos.close();



            Log.d("asdaxxx","New image was saved" + photoFile);



        } catch (Exception e)
        {
            Log.e("asdaxxx",e.toString());

            e.printStackTrace();
        }

        return filepath;
    }

    private File getDir()
    {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "iSelfie");
    }


}
//    static final int REQUEST_IMAGE_CAPTURE = 1;
//    private static final int REQUEST_TAKE_PHOTO = 1;
//    private static final int RESULT_OK = 1;
//    private static CameraManager mManager;
//    private Context mContext;
//    private Camera mCamera;
//    private SurfaceTexture mSurface;
//    public String currentPhotoPath;
//
//
//    public CameraManager(Context context) {
//        this.mContext = context;
//    }
//
//    public void dispatchTakePictureIntent() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        // Ensure that there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
//            // Create the File where the photo should go
//            File photoFile = null;
//            try {
//                photoFile = createImageFile();
//            } catch (IOException ex) {
//                // Error occurred while creating the File
//
//            }
//            // Continue only if the File was successfully created
//            if (photoFile != null) {
//                Uri photoURI = FileProvider.getUriForFile(mContext,
//                        "com.example.android.fileprovider",
//                        photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                ((Activity)mContext).startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
//            }
//        }
//        galleryAddPic();
//    }
//
//    public static boolean isIntentAvailable(Context context, String action) {
//        final PackageManager packageManager = context.getPackageManager();
//        final Intent intent = new Intent(action);
//        List<ResolveInfo> list =
//                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
//        return list.size() > 0;
//    }
//
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
//
//        // Save a file: path for use with ACTION_VIEW intents
//        currentPhotoPath = image.getAbsolutePath();
//        return image;
//    }
//
//    private void galleryAddPic() {
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        File f = new File(currentPhotoPath);
//        Uri contentUri = Uri.fromFile(f);
//        mediaScanIntent.setData(contentUri);
//        mContext.sendBroadcast(mediaScanIntent);
//    }
//
//}

