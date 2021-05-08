import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
///接收flutter发送回来的信息并处理
class MethodCallHandlerImpl(context: Context, messenger: BinaryMessenger) : MethodChannel.MethodCallHandler {
    private val channelName: String = "com.bd.cheng/location"
    private var channel: MethodChannel = MethodChannel(messenger, channelName)
    private var context: Context
    private val locationManager: LocationManager

    init {
        channel.setMethodCallHandler(this)
        this.context = context
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }


    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "setSetting" -> setSetting(call, result)
            "isBackground" -> isBackground(call, result)
            "setBackground" -> setBackground(call, result)
            "fetchLocation" -> fetchLocation(call, result)
            "checkLocationService" -> checkLocationService(call, result)

        }
    }

    //定位设置
    private fun setSetting(call: MethodCall, result: MethodChannel.Result) {
        result.notImplemented()
    }

    //是否为后台运行
    private fun isBackground(call: MethodCall, result: MethodChannel.Result) {
        result.notImplemented()
    }

    //设置为后台运行
    private fun setBackground(call: MethodCall, result: MethodChannel.Result) {
        result.notImplemented()
    }

    //获取定位
    private fun fetchLocation(call: MethodCall, result: MethodChannel.Result) {
        val network = checkNetworkLocation();
        val gps = checkGpsLocation();
        if (!network) {
            print("设备不支持网络定位")
        }
        if (!gps) {
            print("设备不支持gps定位");
        }
        if (!network && !gps) {
            sendResult("1001", null, result, "")

            return
        }
        val permission = checkPermission();
        if (!permission) {
            sendResult("1002", null, result, "")
            return
        }
        ///开启协程获取定位
        GlobalScope.launch(Dispatchers.Main) {
            try {
                var bestLocation: Location? = null
                var hasSendResult = false
                if (gps) {
                    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (isBetterLocation(location, bestLocation)) {
                        bestLocation = location
                    }
                }

                if (network) {
                    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (isBetterLocation(location, bestLocation)) {
                        bestLocation = location
                    }
                }

                var gpsListener: LocationListener?
                var networkListener: LocationListener? = null
                gpsListener = object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (isBetterLocation(location, bestLocation)) {
                            bestLocation = location
                        }
                        locationManager.removeUpdates(this)
                        gpsListener = null
                        if (bestLocation != null) {
                            sendResult("1000", bestLocation, result, "")
                            hasSendResult = true
                            if (networkListener != null) {
                                locationManager.removeUpdates(networkListener)

                            }
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String?) {}
                    override fun onProviderDisabled(provider: String?) {}
                }

                networkListener = object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (isBetterLocation(location, bestLocation)) {
                            bestLocation = location
                        }
                        locationManager.removeUpdates(this)
                        networkListener = null
                        if(!gps && bestLocation != null){
                            sendResult("1000", bestLocation, result, "")
                            hasSendResult = true
                        }

                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String?) {}
                    override fun onProviderDisabled(provider: String?) {}
                }

                if (gps) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsListener, null)
                }
                if (network) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, networkListener, null)
                }
                ///延时10s
                delay( 10 * 1000)
                if (!hasSendResult) {
                    sendResult("1000", bestLocation, result, "")
                }
                if (gpsListener != null) {
                    locationManager.removeUpdates(gpsListener)
                }
                if (networkListener != null) {
                    locationManager.removeUpdates(networkListener)
                }


            } catch (e: SecurityException) {
                sendResult("1003", null, result, e.message)
            }
        }
    }

    private fun sendResult(type: String, location: Location?, result: MethodChannel.Result, message: String?) {

        when (type) {
            "1000" -> {
                val map = toMap(location);
                if (map == null) {
                    result.error("1004", "未获取到定位", "");
                } else {
                    result.success(map)
                }

            }
            "1001" -> result.error("1001", "该设备不支持定位", message)
            "1002" -> result.error("1002", "定位权限未开启", message)
            "1003" -> result.error("1003", "SecurityException", message)
        }
    }

    private fun toMap(location: Location?): Map<String, Any>? {
        if (location == null) {
            return null
        }
        val map = mutableMapOf<String, Double>()
        map["accuracy"] = location.accuracy.toDouble()
        map["latitude"] = location.latitude
        map["longitude"] = location.longitude
        map["altitude"] = location.altitude
        map["speed"] = location.speed.toDouble()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            map["speed_accuracy"] = location.speedAccuracyMetersPerSecond.toDouble()
        }

        map["heading"] = location.bearing.toDouble()
        map["time"] = location.time.toDouble()
        return map
    }

    private fun isBetterLocation(location: Location?, currentBestLocation: Location?): Boolean {
        if (location == null) {
            return false
        }
        if (currentBestLocation == null) {
            return true
        }
        //判断是新定位还是旧的定位
        val towMinutes = 1000 * 60 * 2
        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer: Boolean = timeDelta > towMinutes
        val isSignificantlyOlder: Boolean = timeDelta < -towMinutes
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }
        //判断定位精度
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isLessAccurate = accuracyDelta > 0
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200

        val isFromSameProvider = location.provider == currentBestLocation.provider
        if (isMoreAccurate) {
            return true
        } else if (isNewer && !isLessAccurate) {
            return true
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true
        }

        return false
    }


    //检查是否开启位置服务
    private fun checkLocationService(call: MethodCall, result: MethodChannel.Result) {
        val enabled = checkLocationService();
        result.success(enabled)
    }

    //检查网络定位是否可用
    private fun checkNetworkLocation(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    ///检查gps定位是否可用
    private fun checkGpsLocation(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    ///检查是否开启位置服务
    private fun checkLocationService(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                try {
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
                } catch (e: Settings.SettingNotFoundException) {
                    false
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                locationManager.isLocationEnabled
            }
            else -> {
                !Settings.Secure.getString(context.contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED).isNullOrEmpty()
            }
        }
    }

    ///检查位置权限
    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        val granted = ContextCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return granted == PermissionChecker.PERMISSION_GRANTED;
    }
}