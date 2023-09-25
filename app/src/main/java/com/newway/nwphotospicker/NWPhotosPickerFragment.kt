package com.newway.nwphotospicker

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.newway.nwphotospicker.adapter.NWFolderAdapter
import com.newway.nwphotospicker.adapter.NWFolderAdapterInterface
import com.newway.nwphotospicker.adapter.NWPhotosAdapter
import com.newway.nwphotospicker.adapter.NWPhotosAdapterInterface
import com.newway.nwphotospicker.databinding.FragmentNwPhotosPickerBinding
import com.newway.nwphotospicker.extension.rotateImage
import com.newway.nwphotospicker.extension.singleClick
import com.newway.nwphotospicker.extension.slideDown
import com.newway.nwphotospicker.extension.slideUp
import com.newway.nwphotospicker.model.NWMedia
import com.newway.nwphotospicker.model.NWPhotoDirectory
import com.newway.nwphotospicker.viewmodel.NWPhotosPickerViewModel
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


interface NWPhotosPickerDialogInterface {
    fun onCameraSelected()
    fun onDismissWithImages(images: List<Uri>)
}

class NWPhotosPickerFragment : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(isShowCamera:Boolean,maxSelect:Int) : NWPhotosPickerFragment {
            val dialog = NWPhotosPickerFragment()
            dialog.isShowCamera = isShowCamera
            dialog.maxSelect = maxSelect
            return dialog
        }
    }

    private lateinit var viewModel: NWPhotosPickerViewModel
    private lateinit var binding: FragmentNwPhotosPickerBinding
    var listener: NWPhotosPickerDialogInterface? = null

    private lateinit var photosAdapter: NWPhotosAdapter
    private lateinit var folderAdapter: NWFolderAdapter

    private var isShowFolderView : Boolean = false
    private var maxSelect : Int = 1
    private var isShowCamera : Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[NWPhotosPickerViewModel::class.java]
        binding = FragmentNwPhotosPickerBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return super.onCreateDialog(savedInstanceState).apply {

            window?.setDimAmount(0.6f)

            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
                bottomSheet.setBackgroundResource(android.R.color.transparent)

                val displayMetrics = resources.displayMetrics
                val height = displayMetrics.heightPixels

                val params: ViewGroup.LayoutParams = binding.rootLayout.layoutParams
                params.height = height
                binding.rootLayout.requestLayout()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        bind()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onDismissWithImages(listOf())
    }


    // LAYOUT
    private fun setUpView(){
        binding.btnClose.singleClick {
            this.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                listener?.onDismissWithImages(listOf())
            }, 100)
        }
        binding.btnDone.isVisible = maxSelect != 1
        binding.btnDone.singleClick {
            this.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                listener?.onDismissWithImages(viewModel.getSelectedMedias())
            }, 100)
        }
        binding.btnChangeListPhoto.singleClick {
            if (isShowFolderView) {
                binding.contentFolderView.slideDown()
            } else {
                binding.contentFolderView.slideUp()
                //reset folder image
            }
            binding.arrow.rotateImage(180f)
            isShowFolderView = !isShowFolderView
        }
        setUpPhotoView()
        setUpFolderView()
    }

    private fun bind(){
        viewModel.getMedia()
        viewModel.getPhotoDirs()

        viewModel.isDataChanged.observe(viewLifecycleOwner){
            viewModel.getMedia()
        }
        viewModel.folders.observe(viewLifecycleOwner){
            if (::folderAdapter.isInitialized){
                folderAdapter.setContent(it)
            }
        }
        viewModel.medias.observe(viewLifecycleOwner){
            if (::photosAdapter.isInitialized){
                photosAdapter.setContent(it)
            }
        }
    }

    private fun setUpPhotoView() {
        binding.listImage.layoutManager = GridLayoutManager(context,3)
        photosAdapter = NWPhotosAdapter()
        photosAdapter.showCamera = true
        binding.listImage.adapter = photosAdapter
        photosAdapter.listener = object : NWPhotosAdapterInterface {
            override fun onClickPhoto(media: NWMedia) {
                if (maxSelect == 1){
                    //dismiss
                    this@NWPhotosPickerFragment.dismiss()
                    Handler(Looper.getMainLooper()).postDelayed({
                        listener?.onDismissWithImages(listOf(media.path))
                    }, 100)
                }else{
                    //dismiss when max selected
                    val selected = viewModel.getSelectedMedias()
                    if (maxSelect <= selected.size){
                        this@NWPhotosPickerFragment.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            listener?.onDismissWithImages(selected)
                        }, 100)
                    }
                }
            }

            override fun onClickCamera() {
                openCamera()
            }
        }
    }
    private fun setUpFolderView(){
        binding.listFolder.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        folderAdapter = NWFolderAdapter()
        folderAdapter.listner = object : NWFolderAdapterInterface {
            override fun onClickFolder(folder: NWPhotoDirectory) {
                if (isShowFolderView) {
                    binding.contentFolderView.slideDown()
                } else {
                    binding.contentFolderView.slideUp()
                    //reset folder image
                }
                binding.arrow.rotateImage(180f)
                isShowFolderView = !isShowFolderView
                viewModel.getMedia(folder.bucketId)
            }
        }
        binding.listFolder.adapter = folderAdapter
    }
    //FUN
    fun openCamera(){
        context?.let { ctx ->
            createImageFileInAppDir()?.let { file ->
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoURI: Uri = FileProvider.getUriForFile(
                    ctx,
                    "com.newway.nwphotospicker.provider",
                    file
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivity(cameraIntent)
            }
        }
    }

    private fun createImageFileInAppDir(): File? {
        return if (context != null) {
            val imagePath = context?.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            File(imagePath, "JPEG_${TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())}_" + ".jpg")
        }else {
            null
        }
    }
    //ACTION

}
