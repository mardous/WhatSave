package com.simplified.wsstatussaver.interfaces

import com.simplified.wsstatussaver.model.Status

/**
 * @author Christians Martínez Alvarado (mardous)
 */
interface IStatusCallback {
    fun previewStatusesClick(statuses: List<Status>, startPosition: Int)
    fun saveStatusClick(status: Status)
    fun shareStatusClick(status: Status)
    fun deleteStatusClick(status: Status)
}