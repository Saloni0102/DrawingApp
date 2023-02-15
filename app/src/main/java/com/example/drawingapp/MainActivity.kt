package com.example.drawingapp

import android.app.Dialog
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? =null
    private var mImageCurrentColorButton: ImageButton? = null

    val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode== RESULT_OK && result.data != null){
                val imageBg: ImageView = findViewById(R.id.iv_bg)

                imageBg.setImageURI(result.data?.data)

            }

        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach{

                val permissionName=it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted, now you can read the storage files",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                }
                else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity,
                            "Permission Denied !",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView=findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageCurrentColorButton = linearLayoutColors[1] as ImageButton
        mImageCurrentColorButton!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )

        val btn : ImageButton = findViewById(R.id.ib_brush)
        btn.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val gallery : ImageButton = findViewById(R.id.ib_gallery)
        gallery.setOnClickListener {
            requestStoragePermission()
        }

        val undoB : ImageButton = findViewById(R.id.ib_undo)
        undoB.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val saveB : ImageButton = findViewById(R.id.ib_save)
        saveB.setOnClickListener {
            if(isReadAllowed()){
                lifecycleScope.launch(){
                    val flDrawingView: FrameLayout = findViewById(R.id.frame_container)
                    val myBitmap: Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        //for custom dialog
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        val smallB = brushDialog.findViewById<ImageButton>(R.id.ib_small)
        smallB.setOnClickListener{
            drawingView?.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }
        val mediumB = brushDialog.findViewById<ImageButton>(R.id.ib_medium)
        mediumB.setOnClickListener{
            drawingView?.setSizeForBrush(15.toFloat())
            brushDialog.dismiss()
        }
        val largeB = brushDialog.findViewById<ImageButton>(R.id.ib_large)
        largeB.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View){
        //Toast.makeText(this,"Paint Clicked !", Toast.LENGTH_SHORT).show()
        if(view !== mImageCurrentColorButton){
            val imageButton= view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            mImageCurrentColorButton!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )

            mImageCurrentColorButton?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageCurrentColorButton = view
        }

    }

    private  fun isReadAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE,)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){

                showRationaleDialog("kids drawing app", "Kids Drawing App" +
                "needs to access your Internal Storage")
            }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    }

    fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap):String{
        var result=""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                    + File.separator + "Kids_Drawing_App" + System.currentTimeMillis())

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread(){
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File Saved Successfully",
                                Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(this@MainActivity,"Something Went Wrong :(",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
                catch (e: java.lang.Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
    private fun showRationaleDialog(
        title:String,
        message: String,
    ){
        val builder: AlertDialog.Builder=AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

}

