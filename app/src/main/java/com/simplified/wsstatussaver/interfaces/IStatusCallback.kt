package com.simplified.wsstatussaver.interfaces

import com.simplified.wsstatussaver.model.Status

/**
 * @author Christians Martínez Alvarado (mardous)
 */
interface IStatusCallback {
    fun previewStatusClick(status: Status)
    fun saveStatusClick(status: Status)
    fun shareStatusClick(status: Status)
    fun deleteStatusClick(status: Status)
}