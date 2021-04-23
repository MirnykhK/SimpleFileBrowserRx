package ru.adminmk.filebrowserrx.model

import android.arch.lifecycle.ViewModel
import android.os.Environment
import io.reactivex.Observable
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FileViewModel:ViewModel() {
    val ROOT_FILE= File(
        Environment.getExternalStorageDirectory().getPath())
    var currFile: File = ROOT_FILE
}

fun getFiles(file: File): List<VisualFile>{
    val fileList: MutableList<VisualFile> = ArrayList()
    val files: Array<File>? = file.listFiles()
    if(files==null){
        return ArrayList<VisualFile>()
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    for (file in files) {
        if (!file.isHidden() && file.canRead()) {

            val currFile = VisualFile(file, file.name, getStringSizeOFile(file.length()), formatter.format(
                    Date(file.lastModified())),file.lastModified(), file.isDirectory)

            fileList.add(currFile)
        }
    }

    Collections.sort(fileList)
    fileList.add(0, VisualFile(file, "<... Back ...>", "", "", null, false,true))

    return fileList
}

private fun getStringSizeOFile(size:Long):String{
    if(size <= 0) return ""
    val units = arrayListOf<String>("B", "kB", "MB", "GB", "TB" )
    val digitGroups = (Math.log10(size.toDouble())/Math.log10(1024.toDouble())).toInt()
    return DecimalFormat("#,##0.#").format(size/Math.pow(1024.toDouble(), digitGroups.toDouble())) + " " + units[digitGroups]
}

fun createFilesObservable(file: File): Observable<List<VisualFile>> {
    return Observable.create { emitter ->
        try {
            val fileList = getFiles(file)
            emitter.onNext(fileList)
            emitter.onComplete()
        } catch (e: Exception) {

            emitter.onError(e);
        }
    }
}