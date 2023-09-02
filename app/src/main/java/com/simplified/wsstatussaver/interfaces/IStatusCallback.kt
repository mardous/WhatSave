package com.simplified.wsstatussaver.interfaces

import com.simplified.wsstatussaver.model.Status

/**
 * @author Christians Martínez Alvarado (mardous)
 */
interface IStatusCallback {
    fun onPreviewStatusClick(status: Status)
    fun onSaveStatusClick(status: Status)
    fun onShareStatusClick(status: Status)
    fun onDeleteStatusClick(status: Status)
}