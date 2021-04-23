package ru.adminmk.filebrowserrx

import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.reactivestreams.Subscription
import ru.adminmk.filebrowserrx.model.FileViewModel
import ru.adminmk.filebrowserrx.model.VisualFile
import ru.adminmk.filebrowserrx.model.createFilesObservable
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "MainActivity"
private const val REQUEST_PERMISSION_CODE = 1
private const val VIEW_TYPE_BACK_NAVIGATION = 1
private const val VIEW_TYPE_FILE = 0

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedDir:BehaviorSubject<File>
    private lateinit var viewModel:FileViewModel
    private var warningView:TextView?= null

    private lateinit var  subscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)).get(FileViewModel::class.java)

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter= FileAdaper(ArrayList<VisualFile>())

        warningView = findViewById<TextView>(R.id.warningView)

        initPermission()
    }

    private fun initPermission(){
        val checkResult = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if(checkResult==PackageManager.PERMISSION_DENIED){

            warningView?.let { it.visibility=View.VISIBLE }
            requestPermissions()
        }
    }

    private fun updateList(files: List<VisualFile>){
        val adapter = recyclerView.adapter as FileAdaper
        adapter.updateAdapterFiles(files)
    }

    private class FileHolder(viewItem: View):RecyclerView.ViewHolder(viewItem){
        val nameOFile= viewItem.findViewById<TextView>(R.id.nameOFileView)
        val sizeOFile= viewItem.findViewById<TextView>(R.id.sizeOFileView)
        val modifiedtext= viewItem.findViewById<TextView>(R.id.modifiedtextView)
        val image= viewItem.findViewById<ImageView>(R.id.imageView)
    }

    private inner class FileAdaper(var files: List<VisualFile>):RecyclerView.Adapter<FileHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
            lateinit var view:View

            when(viewType){
                VIEW_TYPE_FILE->{
                    view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)

                    view.setOnClickListener{val itemPosition = this@MainActivity.recyclerView.getChildLayoutPosition(view)
                        val curFile = files[itemPosition]
                        if(curFile.isDirectory){
                            curFile.file?.let {
                                selectedDir.onNext(it)
                                viewModel.currFile = it
                            }
                        }
                    }
                }
                VIEW_TYPE_BACK_NAVIGATION->{
                    view = LayoutInflater.from(parent.context).inflate(R.layout.back_layout, parent, false)

                    view.setOnClickListener{val itemPosition = this@MainActivity.recyclerView.getChildLayoutPosition(view)
                        val curFile = files[itemPosition]
                        curFile.file?.let {
                            if(curFile.file != viewModel.ROOT_FILE){
                                selectedDir.onNext(it.parentFile)
                                viewModel.currFile = it.parentFile
                            }
                        }
                    }
                }
            }

            return FileHolder(view)
        }

        override fun onBindViewHolder(holder: FileHolder, position: Int) {
            val curFile = files[position]

            if(!curFile.isBackNavigation){
                if(curFile.isDirectory){
                    holder.nameOFile.text = curFile.nameOFile
                    holder.sizeOFile.text=""
                    holder.image.setImageDrawable(getResources().getDrawable(R.drawable.ic_folder, null))
                }
                else{
                    holder.nameOFile.text = curFile.nameOFile
                    holder.image.setImageDrawable(getResources().getDrawable(R.drawable.ic_file, null))
                    holder.sizeOFile.text = curFile.sizeOFile
                }

                holder.modifiedtext.text = curFile.modifiedtext
            }
        }

        override fun getItemViewType(position: Int): Int {
            val curFile = files[position]
            if(curFile.isBackNavigation) return VIEW_TYPE_BACK_NAVIGATION

            return VIEW_TYPE_FILE
        }

        override fun getItemCount(): Int {
            return files.size
        }

        fun  updateAdapterFiles(files: List<VisualFile>){
            this.files = files
            this.notifyDataSetChanged()
        }
    }


    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode== REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            warningView?.let { it.visibility=View.GONE }
        }
    }

    private fun setRootDir(){
        selectedDir =
                BehaviorSubject.createDefault(viewModel.currFile)

        subscription =  selectedDir
                .switchMap { file ->
                    createFilesObservable(file)
                            .subscribeOn(Schedulers.io())
                }.retry(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::updateList, {e-> Toast.makeText(this, "error: $e", Toast.LENGTH_SHORT).show()})


    }

    override fun onPause() {
        super.onPause()

        if(!subscription.isDisposed){
            subscription.dispose()
        }
    }

    override fun onResume() {
        super.onResume()

        val isPermissionGranted = warningView?.visibility == View.GONE

        if(isPermissionGranted){
            setRootDir()
        }
    }
}