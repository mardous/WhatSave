package com.simplified.wsstatussaver.interfaces

import android.view.MenuItem
import com.simplified.wsstatussaver.model.Status

/**
 * @author Christians Martínez Alvarado (mardous)
 */
interface IStatusCallback {
    fun previewStatusesClick(statuses: List<Status>, startPosition: Int)
    fun multiSelectionItemClick(item: MenuItem, selection: List<Status>)
}