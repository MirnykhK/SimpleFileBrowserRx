package ru.adminmk.filebrowserrx.model

import java.io.File

class VisualFile(val file: File?, val nameOFile:String, val sizeOFile:String, val modifiedtext:String, val modifiedDate:Long?, val isDirectory:Boolean, val isBackNavigation:Boolean=false):Comparable<VisualFile>{
    override fun compareTo(other: VisualFile): Int {
        if(this.isDirectory && other.isDirectory){
            return this.nameOFile.compareTo(other.nameOFile)
        }else if(!this.isDirectory && !other.isDirectory){
            return this.nameOFile.compareTo(other.nameOFile)
        }else if(this.isDirectory){
            return -1
        }

        return 1
    }
}