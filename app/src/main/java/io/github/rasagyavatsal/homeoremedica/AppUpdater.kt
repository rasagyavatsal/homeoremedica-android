package io.github.rasagyavatsal.homeoremedica

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import io.github.rasagyavatsal.homeoremedica.api.ApiException
import io.github.rasagyavatsal.homeoremedica.api.HomeoRemedicaApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Updates a build the server no longer serves: through Play's immediate update, or else its Play listing. */
class AppUpdater(private val activity: ComponentActivity) {
    // The activity has no context until it is created.
    private val manager by lazy { AppUpdateManagerFactory.create(activity) }
    private val immediate = AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)

    /** Whether the server no longer serves this build; false when it can't be asked. */
    suspend fun required(api: HomeoRemedicaApi): Boolean {
        val minimum = try {
            api.minAndroidVersionCode()
        } catch (_: ApiException) {
            return false
        }
        val installed = PackageInfoCompat.getLongVersionCode(activity.packageManager.getPackageInfo(activity.packageName, 0))
        return installed < minimum
    }

    /**
     * Starts Play's immediate update. An install Play can't update, such as one not from Play, opens the
     * app's Play listing instead when [orListing].
     */
    suspend fun start(orListing: Boolean) {
        val info = playUpdate()
        // A declined update leaves the update screen up, so its result isn't read.
        if (info != null && manager.startUpdateFlowForResult(info, activity, immediate, UPDATE_REQUEST)) return
        if (orListing) openListing()
    }

    private suspend fun playUpdate(): AppUpdateInfo? {
        val info = try {
            manager.appUpdateInfo.await()
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            return null
        }
        return when (info.updateAvailability()) {
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> info
            UpdateAvailability.UPDATE_AVAILABLE -> info.takeIf { it.isUpdateTypeAllowed(immediate) }
            else -> null
        }
    }

    private fun openListing() {
        val id = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
        }
    }

    private companion object {
        const val UPDATE_REQUEST = 1
    }
}
