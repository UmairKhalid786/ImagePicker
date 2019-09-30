package com.app.imagefactory;

/*
 *   Created     : 9/17/2019 , 12:16 AM
 *   Author      : Umair Khalid
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class ImageManagerDialog extends BottomSheetDialogFragment implements View.OnClickListener {

    private static final int SELECT_FROM_GALLERY = 0;
    private static final int SELECT_FROM_CAMERA = 1;
    private static final int RC_PERMISSIONS = 2;

    private View useCameraTv;
    private View useGalleryTv;
    private View cancelTv;

    private ImagePickerListener imagePickerListener;
    private int cameraRequestCode = 0;

    public void setImagePickerListener(ImagePickerListener imagePickerListener) {
        this.imagePickerListener = imagePickerListener;
    }

    private ImageManagerDialog() {

    }

    public static ImageManagerDialog newInstance() {
        return new ImageManagerDialog();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.layout_photo_bottom_sheet, container, false);
        initViews(view);
        setListeners();
        return view;

    }

    private void setListeners() {
        useGalleryTv.setOnClickListener(this);
        useCameraTv.setOnClickListener(this);
        cancelTv.setOnClickListener(this);
    }

    private void initViews(View view) {
        useCameraTv = view.findViewById(R.id.use_camera_tv);
        useGalleryTv = view.findViewById(R.id.use_gallery_tv);
        cancelTv = view.findViewById(R.id.cancel_tv);
    }

    @Override
    public void onClick(View view) {

        if (view == cancelTv) {
            if (imagePickerListener != null) {
                imagePickerListener.OnPhotoRemove();
                dismiss();
            }
        } else if (view == useCameraTv) {
            if (cameraPermissionGranted(getContext())) {
                captureImage();
            } else {
                requestCameraPermission();
            }
        } else if (view == useGalleryTv) {
            getFromGallery();
        }
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, cameraRequestCode);
    }

    private void getFromGallery() {
        if (getActivity() == null)
            return;

        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, SELECT_FROM_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == cameraRequestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            }
        }
    }


    private void requestPermission() {
        String[] permissions = {"android.permission.WRITE_EXTERNAL_STORAGE"};
        requestPermissions(permissions, RC_PERMISSIONS);
    }

    private void captureImage() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, SELECT_FROM_CAMERA);//zero can be replaced with any action code
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case SELECT_FROM_GALLERY:
                if (resultCode == RESULT_OK) {
                    if (imagePickerListener != null) {
                        try {
                            File destination = getFileFromIntent(imageReturnedIntent);
                            byte[] bytes = getByteFromIntent(imageReturnedIntent);
                            imagePickerListener.OnImageFound(destination, bytes, false);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        dismiss();
                    }
                }

                break;
            case SELECT_FROM_CAMERA:
                if (resultCode == RESULT_OK) {
                    if (imagePickerListener != null) {
                        try {
                            File destination = getFileFromIntent(imageReturnedIntent);
                            byte[] bytes = getByteFromIntent(imageReturnedIntent);
                            imagePickerListener.OnImageFound(destination, bytes, true);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        dismiss();
                    }
                }
                break;
        }
    }

    private File getFileFromIntent(Intent imageReturnedIntent) throws IOException {
        Bitmap thumbnail = getBitmapFromIntent(imageReturnedIntent);

        File destination = null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            destination = createImageFile();
            FileOutputStream fo;

            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return destination;
    }

    private Bitmap getBitmapFromIntent(Intent imageReturnedIntent) {

        Bitmap thumbnail = null;
        if (imageReturnedIntent.getData() == null) {
            thumbnail = (Bitmap) imageReturnedIntent.getExtras().get("data");
        } else {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                thumbnail = getBitmapFromUri(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return thumbnail;
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getActivity().getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }


    private byte[] getByteFromIntent(Intent imageReturnedIntent) throws IOException {
        Bitmap thumbnail = getBitmapFromIntent(imageReturnedIntent);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        return baos.toByteArray();
    }

    public interface ImagePickerListener {

        void OnPhotoRemove();

        void OnImageFound(File file, byte[] bytes, boolean fromCamera);
    }


    private File createImageFile() throws IOException {
        if (getActivity() == null)
            return null;

        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir =
                getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    public boolean cameraPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }
}
