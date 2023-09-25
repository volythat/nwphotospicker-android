package com.newway.example

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.newway.nwphotospicker.NWPhotosPickerDialogInterface
import com.newway.nwphotospicker.NWPhotosPickerFragment
import com.newway.nwphotospicker.R
import com.newway.nwphotospicker.adapter.NWPhotosAdapterInterface
import com.newway.nwphotospicker.databinding.ActivityMainBinding
import com.newway.nwphotospicker.extension.singleClick
import com.newway.nwphotospicker.model.NWMedia

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val storagePermissions33 =
        arrayOf<String?>(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

    private val storagePermissions = arrayOf<String?>(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private var allGrantedCallback: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnTest.singleClick {
            requestPermissionsApp {
                val dialog = NWPhotosPickerFragment.newInstance(true,5)
                dialog.listener = object : NWPhotosPickerDialogInterface {
                    override fun onCameraSelected() {

                    }

                    override fun onDismissWithImages(images: List<Uri>) {
                        images.forEach {
                            Log.e("MainActivity", "onDismissWithImages: ${it.path}")
                        }
                    }

                }
                dialog.show(supportFragmentManager,"NWPhotosPickerFragment")
            }
        }
    }
    fun requestPermissionsApp(allGranted:(Boolean) -> Unit){
        this.allGrantedCallback = allGranted // Gán callback từ HomeFragment
        ActivityCompat.requestPermissions(this, permissions(), 11)
    }
    private fun permissions(): Array<String?> {
        val p: Array<String?> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storagePermissions33
        } else {
            storagePermissions
        }
        return p
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 11) {// Kiểm tra mã yêu cầu quyền
            // Xử lý kết quả cấp quyền tại đây
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                allGrantedCallback?.let { it(true) } // Gọi callback với giá trị true nếu quyền được cấp
            } else {
                allGrantedCallback?.let { it(false) } // Gọi callback với giá trị false nếu quyền bị từ chối
                Toast.makeText(this, "Loi permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
}