package nz.co.zsd.floatingoverlay.Data

import android.content.Context
import android.text.format.Formatter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Class that contains current release information. After sending a request to the github
 * API endpoint to get the latest release, instantiate a new release info object with the
 * JSON response to deserialize and extract relevant fields. Call the validation method to
 * ensure that the response from the API is valid and contains all required fields.
 */
public class ReleaseInfo: ViewModel() {
    //private val _state = MutableStateFlow(ReleaseInfoData())
    //private val state: StateFlow<ReleaseInfoData> = _state.asStateFlow()

    public val data = MutableLiveData<ReleaseInfoData>()

    public fun loadFromJSON(obj: JSONObject) {
        var name: String? = null
        var tag: String? = null
        var note: String? = null
        var publishTime: Date? = null
        var apkName: String? = null
        var apkUrl: String? = null
        var apkSize: Int? = null

        try {
            name = obj.optString("name")
            tag = obj.optString("tag_name")
            note = obj.optString("body")
        } catch (_: Exception) {
            // Ignore any deserialization exceptions
        }

        // Try to parse the published time str to a date obj
        publishTime =
            try {
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault()
                ).parse(obj.getString("published_at"))
            } catch (_: Exception) {
                // Exception occurred parsing data, set to null
                null
            }

        // Try to find and extract the release information (APK)
        try {
            val artifacts: JSONArray = obj.getJSONArray("assets")
            for (index in 0..artifacts.length()) {
                val artifact = artifacts.getJSONObject(index)
                if (artifact.getString("name").endsWith(".apk")) {
                    // Found the APK, set the release APK name and URL
                    apkName = artifact.getString("name")
                    apkUrl = artifact.getString("browser_download_url")
                    apkSize = artifact.getInt("size")
                    break
                }
            }
        } catch (_: Exception) {
            // Ignore exception, release APK info not found or failed to parse fields
        }

        // Update the release information data
        data.value = ReleaseInfoData(
            name = name,
            tag = tag,
            note = note,
            publishTime = publishTime,
            apkName = apkName,
            apkUrl = apkUrl,
            apkSize = apkSize
        )
    }

    /**
     * Check if the release information data is valid (all fields are populated)
     * @return True if all fields are populated, false otherwise
     */
    public fun isValid(): Boolean {
        return (
            (data.isInitialized) &&
            (isDataValid(data.value))
        )
    }

    /**
     * Get the version of the latest release as a string. If latest release is an RC, "(RC)" added
     * to end of returned version string.
     * @return Version of latest release in format major.minor
     */
    public fun getVersion(): String {
        return getVersion(data.value)
    }

    /**
     * Get the version of the release APK as an array of objects where the first
     * element represents the major version, second minor and third a flag that
     * indicates if this version is a release candidate (RC). This method will
     * work assuming the tag of releases always follow the format "vX.Y" or
     * "vX.Y-RC" where X is the major version and Y is the minor version number.
     * @return List of objects representing the major, minor and RC flag
     */
    public fun splitVersion(): List<Any?> {
        return splitVersion(data.value)
    }

    /**
     * Check if the latest release information version is newer than the current version of the app.
     * @param obj Release information object to check version against
     * @param context Context of the application to use to get current app version
     * @return True if data is newer than current app version, false otherwise
     */
    public fun isNewerVersion(context: Context): Boolean {
        return isNewerVersion(data.value, context)
    }

    companion object {
        /**
         * Check if a release information data object is valid (all fields are populated)
         * @return True if object is valid, false otherwise
         */
        public fun isDataValid(obj: ReleaseInfoData?) : Boolean {
            return (
                (obj?.name?.isNotEmpty() == true) &&
                (obj.tag?.isNotEmpty() == true) &&
                (obj.note?.isNotEmpty() == true) &&
                (obj.publishTime != null) &&
                (obj.apkName?.isNotEmpty() == true) &&
                (obj.apkUrl?.isNotEmpty() == true) &&
                (obj.apkSize != null && obj.apkSize!! > 0)
            )
        }

        /**
         * Get the version string of a release information object. If the release is a release candidate,
         * "RC" added to the end of the returned version string.
         * @param obj Release information object to get version string of
         * @return Version of latest release in format major.minor
         */
        public fun getVersion(obj: ReleaseInfoData?): String {
            // If object is null or invalid, return placeholder error string
            if (obj == null || !isDataValid(obj))
                return "INVALID INFO!"

            // Split version and return version string
            val ver = splitVersion(obj)
            return if (ver[2] == true)
                "${ver[0]}.${ver[1]}-RC"
            else
                "${ver[0]}.${ver[1]}"
        }

        /**
         * Split the version string of a release info data object into a list of its components.
         * First element of returned list is the major version number, second minor, and third is
         * a boolean which indicates if release is release candidate (contains "-RC" in tag). Method
         * assumes the version string (release tag) is in format "v.MAJOR.MINOR" or "v.MAJOR.MINOR-RC".
         * @param obj Release information object to get split version of
         * @return List of objects representing the major, minor and RC flag
         */
        public fun splitVersion(obj: ReleaseInfoData?): List<Any?> {
            val tok = obj?.tag?.split(".")
            val major = tok?.get(0)?.substring(1)?.toInt()
            val minor = tok?.get(1)?.split("-")?.get(0)?.toInt()
            val isRC = obj?.tag?.contains("-RC") ?: false

            // Return list describing version
            return listOf (major?.plus(1), minor, isRC)
        }

        /**
         * Check if the version of a release object is newer than the current version of the app.
         * @param obj Release information object to check version against
         * @param context Context of the application to use to get current app version
         * @return True if data is newer than current app version, false otherwise
         */
        public fun isNewerVersion(obj: ReleaseInfoData?, context: Context): Boolean {
            // If release info is not valid, cannot check so return false
            if (obj == null || !isDataValid(obj))
                return false

            // Split version string into components
            val ver = splitVersion(obj)
            val major = ver[0] as Int
            val minor = ver[1] as Int
            val isRC = ver[2] as Boolean

            // Retrieve the current installed app version
            val currentVer = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            val currentMajor = currentVer.split(".")[0].toInt()
            val currentMinor = currentVer.split(".")[1].split("-")[0].toInt()

            // Check if version is newer
            return (currentMajor < major) ||
                    (currentMajor == major && currentMinor < minor)
        }

        /**
         * Get the size of the release APK artifact formatted as a file size string.
         * @param obj Release information object to get size of
         * @param context Context of the application
         * @return Size of release APK artifact or "INVALID INFO!" on error
         */
        public fun getUpdateSize(obj: ReleaseInfoData?, context: Context): String {
            if (obj == null || !isDataValid(obj))
                return "INVALID INFO!"

            val appSize = (obj.apkSize ?: 0).toLong()
            return Formatter.formatFileSize(context, appSize)
        }
    }
}

data class ReleaseInfoData(
    // Name of the release (title)
    var name: String? = null,
    // Tag that contains APK version
    var tag: String? = null,
    // Release note text in markdown format
    var note: String? = null,
    // Date the release was published
    var publishTime: Date? = null,
    // Name of the release APK artifacts
    var apkName: String? = null,
    // URL to download the release APK artifacts
    var apkUrl: String? = null,
    // Size of the release APK artifact in bytes
    var apkSize: Int? = null
)