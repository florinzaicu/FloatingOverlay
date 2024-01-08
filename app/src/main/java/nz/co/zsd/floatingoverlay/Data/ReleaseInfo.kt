package nz.co.zsd.floatingoverlay.Data

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
public class RelaseInfo: ViewModel() {
    //private val _state = MutableStateFlow(ReleaseInfoData())
    //private val state: StateFlow<ReleaseInfoData> = _state.asStateFlow()

    private val data = MutableLiveData<ReleaseInfoData>()
    
    //private val _state = MutableLiveData<ReleaseInfoData>()
    //private val data: LiveData<ReleaseInfoData> get() = _state

    /**
     * Get the current release info state
     * @return Release information state flow object that contains values
     */
    public fun get(): LiveData<ReleaseInfoData> {
        return data
    }

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
            (get().isInitialized) &&
            (get().value?.name?.isNotEmpty() == true) &&
            (get().value?.tag?.isNotEmpty() == true) &&
            (get().value?.note?.isNotEmpty() == true) &&
            (get().value?.publishTime != null) &&
            (get().value?.apkName?.isNotEmpty() == true) &&
            (get().value?.apkUrl?.isNotEmpty() == true) &&
            (get().value?.apkSize != null && get().value?.apkSize!! > 0)
        )
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
        val tok = get().value?.tag?.split(".")
        val major = tok?.get(0)?.substring(1)?.toInt()
        val minor = tok?.get(1)?.split("-")?.get(0)?.toInt()
        val isRC = get().value?.tag?.contains("-RC") ?: false

        // Return list describing version
        return listOf ( major, minor, isRC)
    }

    public fun isNewerVersion(context: Context): Boolean {
        val ver = splitVersion()
        val major = ver[0] as Int
        val minor = ver[1] as Int
        val isRC = ver[2] as Boolean

        val currentVer = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val currentMajor = currentVer.split(".")[0].toInt()
        val currentMinor = currentVer.split(".")[1].split("-")[0].toInt()

        // Check if version is newer
        return (currentMajor < major) ||
                (currentMajor == major && currentMinor < minor)
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

/*
public data class ReleaseInfo : Parcelable {
    // Name of the release (title)
    private var name: String? = null
    // Tag that contains APK version
    private var tag: String? = null
    // Release note text in markdown format
    private var note: String? = null
    // Date the release was published
    private var publishTime: Date? = null

    // Details of the release APK artifact (name, download URL, size in bytes)
    private var apkName: String? = null
    private var apkUrl: String? = null
    private var apkSize: Int? = null

    public fun loadFromJSON(obj: JSONObject) {
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
    }

    /**
     * Check if the release information is valid (all fields are populated)
     * @return True if all fields are populated, false otherwise
     */
    public fun isValid(): Boolean {
        return (
                (name?.isNotEmpty() == true) &&
                        (tag?.isNotEmpty() == true) &&
                        (note?.isNotEmpty() == true) &&
                        publishTime != null &&
                        (apkName?.isNotEmpty() == true) &&
                        (apkUrl?.isNotEmpty() == true) &&
                        (apkSize != null && apkSize!! > 0)
                )
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
        val tok = tag?.split(".")
        val major = tok?.get(0)?.substring(1)?.toInt()
        val minor = tok?.get(1)?.split("-")?.get(0)?.toInt()
        val isRC = tag?.contains("-RC") ?: false

        return listOf ( major, minor, isRC)
    }

    public fun isNewerVersion(context: Context): Boolean {
        val ver = splitVersion()
        val major = ver[0] as Int
        val minor = ver[1] as Int
        val isRC = ver[2] as Boolean

        val currentVer = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val currentMajor = currentVer.split(".")[0].toInt()
        val currentMinor = currentVer.split(".")[1].split("-")[0].toInt()
        if (
            (currentMajor < major) ||
            (currentMajor == major && currentMinor < minor)
        ) {
            Log.i("TTT", "Update avaiable")
            return true
        }

        // No Update
        return false;
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(tag)
        parcel.writeString(note)
        parcel.writeString(apkName)
        parcel.writeString(apkUrl)
        parcel.writeValue(apkSize)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReleaseInfo> {
        override fun createFromParcel(parcel: Parcel): ReleaseInfo {
            return ReleaseInfo(parcel)
        }

        override fun newArray(size: Int): Array<ReleaseInfo?> {
            return arrayOfNulls(size)
        }
    }
}
*/