package com.techlad.imagepicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import butterknife.BindView
import butterknife.ButterKnife
import com.app.imagefactory.ImageManagerDialog
import java.io.File

class MainActivity : AppCompatActivity(), ImageManagerDialog.ImagePickerListener {

    var img : ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.select_image_btn)?.setOnClickListener { selectImageFromProfile() }
        img = findViewById<ImageView>(R.id.display_img);
    }

    private fun selectImageFromProfile() {
        val imageManager = ImageManagerDialog.newInstance()
        imageManager.setImagePickerListener(this)
        imageManager.show(supportFragmentManager, "image_manager")
    }

    override fun OnPhotoRemove() {

    }

    override fun OnImageFound(file: File?, bytes: ByteArray?, fromCamera: Boolean) {
        // Everything you need from a file picker
        img?.setImageURI(file?.toUri())
        Toast.makeText(this , "File address :" + (file?.path ?: "") , Toast.LENGTH_LONG ).show()
    }
}
