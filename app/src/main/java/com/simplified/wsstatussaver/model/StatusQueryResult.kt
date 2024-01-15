package com.simplified.wsstatussaver.model

import androidx.annotation.StringRes
import com.simplified.wsstatussaver.R

data class StatusQueryResult(val code: ResultCode, val statuses: List<Status> = emptyList()) {

    val isLoading: Boolean get() = code == ResultCode.Loading

    enum class ResultCode(@StringRes val titleRes: Int, @StringRes val descriptionRes: Int) {
        Idle(0, 0),
        Success(0, 0),
        Loading(R.string.loading, R.string.please_wait_a_second),
        NotInstalled(R.string.wa_is_not_installed_title, R.string.this_application_will_not_work),
        PermissionError(R.string.permissions_denied_title, R.string.could_not_load_statuses),
        NoStatuses(R.string.no_statuses_title, R.string.you_should_open_wa_and_download_some_statuses)
    }

    companion object {
        val Idle = StatusQueryResult(ResultCode.Idle)
    }
}