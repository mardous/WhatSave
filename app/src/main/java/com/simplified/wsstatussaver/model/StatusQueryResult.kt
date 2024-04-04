package com.simplified.wsstatussaver.model

import androidx.annotation.StringRes
import com.simplified.wsstatussaver.R

data class StatusQueryResult(val code: ResultCode, val statuses: List<Status> = emptyList()) {

    val isLoading: Boolean get() = code == ResultCode.Loading

    enum class ResultCode(@StringRes val titleRes: Int = 0, @StringRes val descriptionRes: Int = 0, @StringRes val buttonTextRes: Int = 0) {
        Idle,
        Success,
        Loading(R.string.loading, R.string.please_wait_a_second),
        NotInstalled(R.string.wa_is_not_installed_title, R.string.this_application_will_not_work, R.string.close_action),
        PermissionError(R.string.could_not_load_statuses, R.string.permissions_denied_title, R.string.grant_permissions),
        NoStatuses(R.string.no_statuses_title, R.string.you_should_open_wa_and_download_some_statuses, R.string.open_wa_action),
        NoSavedStatuses(R.string.empty, R.string.there_may_be_no_saved_files)
    }

    companion object {
        val Idle = StatusQueryResult(ResultCode.Idle)
    }
}