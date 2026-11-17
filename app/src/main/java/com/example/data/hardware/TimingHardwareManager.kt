package com.example.data.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GpsTelemetryState(
    val isTracking: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val accuracyMeters: Float = 0f,
    val startFinishLat: Double? = null,
    val startFinishLng: Double? = null,
    val distanceToLineMeters: Float? = null,
    val lastCrossingMonoNs: Long = 0L,
    val statusMessage: String = "GPS en espera"
)

data class BleTransponderDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

data class WifiFeedState(
    val endpointUrl: String = "",
    val isConnected: Boolean = false,
    val lastPacketSummary: String = "Sin conexión API/WebSocket",
    val error: String? = null
)

data class AutomaticLapTrigger(
    val lapTimeMs: Long,
    val source: String,
    val accuracyMeters: Double
)

class TimingHardwareManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private val _gpsState = MutableStateFlow(GpsTelemetryState())
    val gpsState: StateFlow<GpsTelemetryState> = _gpsState.asStateFlow()

    private val _bleDevices = MutableStateFlow<List<BleTransponderDevice>>(emptyList())
    val bleDevices: StateFlow<List<BleTransponderDevice>> = _bleDevices.asStateFlow()

    private val _isScanningBle = MutableStateFlow(false)
    val isScanningBle: StateFlow<Boolean> = _isScanningBle.asStateFlow()

    private val _connectedBleAddress = MutableStateFlow<String?>(null)
    val connectedBleAddress: StateFlow<String?> = _connectedBleAddress.asStateFlow()

    private val _wifiFeedState = MutableStateFlow(WifiFeedState())
    val wifiFeedState: StateFlow<WifiFeedState> = _wifiFeedState.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var bleScanCallback: ScanCallback? = null
    private var activeWebSocket: WebSocket? = null
    private var wasOutsideFinishGate = true
    private var onAutomaticLapDetected: ((AutomaticLapTrigger) -> Unit)? = null

    fun setAutomaticLapListener(listener: (AutomaticLapTrigger) -> Unit) {
        onAutomaticLapDetected = listener
    }

    fun setStartFinishLineHere() {
        val current = _gpsState.value
        if (current.latitude != 0.0 || current.longitude != 0.0) {
            _gpsState.value = current.copy(
                startFinishLat = current.latitude,
                startFinishLng = current.longitude,
                lastCrossingMonoNs = SystemClock.elapsedRealtimeNanos(),
                statusMessage = "Línea de meta GPS fijada (±${"%.1f".format(current.accuracyMeters)} m)"
            )
            wasOutsideFinishGate = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startGpsTracking() {
        if (_gpsState.value.isTracking) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                processGpsLocation(loc)
            }
        }
        locationCallback = callback
        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            _gpsState.value = _gpsState.value.copy(
                isTracking = true,
                statusMessage = "GPS activo (modo alternativo — no sustituye transponder profesional)"
            )
        } catch (e: SecurityException) {
            _gpsState.value = _gpsState.value.copy(
                isTracking = false,
                statusMessage = "Permiso de ubicación requerido para GPS"
            )
        }
    }

    fun stopGpsTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        _gpsState.value = _gpsState.value.copy(
            isTracking = false,
            statusMessage = "GPS detenido"
        )
    }

    private fun processGpsLocation(loc: Location) {
        val prev = _gpsState.value
        val lineLat = prev.startFinishLat
        val lineLng = prev.startFinishLng
        var dist: Float? = null
        var updatedLastCross = prev.lastCrossingMonoNs

        if (lineLat != null && lineLng != null) {
            val results = FloatArray(1)
            Location.distanceBetween(loc.latitude, loc.longitude, lineLat, lineLng, results)
            dist = results[0]
            val gateRadius = maxOf(12f, loc.accuracy)
            val nowNs = SystemClock.elapsedRealtimeNanos()
            if (dist <= gateRadius && wasOutsideFinishGate) {
                if (prev.lastCrossingMonoNs > 0L) {
                    val lapMs = (nowNs - prev.lastCrossingMonoNs) / 1_000_000L
                    if (lapMs >= 5_000L) {
                        onAutomaticLapDetected?.invoke(
                            AutomaticLapTrigger(
                                lapTimeMs = lapMs,
                                source = "GPS",
                                accuracyMeters = loc.accuracy.toDouble()
                            )
                        )
                    }
                }
                updatedLastCross = nowNs
                wasOutsideFinishGate = false
            } else if (dist > gateRadius * 1.8f) {
                wasOutsideFinishGate = true
            }
        }

        _gpsState.value = prev.copy(
            latitude = loc.latitude,
            longitude = loc.longitude,
            speedKmh = loc.speed * 3.6f,
            accuracyMeters = loc.accuracy,
            distanceToLineMeters = dist,
            lastCrossingMonoNs = updatedLastCross
        )
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = manager?.adapter
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled || scanner == null) {
            _isScanningBle.value = false
            return
        }
        if (_isScanningBle.value) return
        _bleDevices.value = emptyList()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val address = device.address ?: return
                val name = try {
                    device.name ?: result.scanRecord?.deviceName ?: "Transponder BLE"
                } catch (_: SecurityException) {
                    "Dispositivo BLE"
                }
                val current = _bleDevices.value.toMutableList()
                val idx = current.indexOfFirst { it.address == address }
                val entry = BleTransponderDevice(name = name, address = address, rssi = result.rssi)
                if (idx >= 0) current[idx] = entry else current.add(entry)
                _bleDevices.value = current.sortedByDescending { it.rssi }
            }
        }
        bleScanCallback = callback
        try {
            scanner.startScan(callback)
            _isScanningBle.value = true
        } catch (_: SecurityException) {
            _isScanningBle.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val scanner = manager?.adapter?.bluetoothLeScanner
        try {
            bleScanCallback?.let { scanner?.stopScan(it) }
        } catch (_: SecurityException) {
        }
        bleScanCallback = null
        _isScanningBle.value = false
    }

    fun selectBleDevice(address: String) {
        _connectedBleAddress.value = address
        stopBleScan()
    }

    fun disconnectBleDevice() {
        _connectedBleAddress.value = null
    }

    fun connectWifiWebSocket(url: String) {
        disconnectWifiWebSocket()
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            _wifiFeedState.value = WifiFeedState(error = "Ingresa una URL ws:// o http:// válida")
            return
        }
        val wsUrl = when {
            trimmed.startsWith("ws://") || trimmed.startsWith("wss://") -> trimmed
            trimmed.startsWith("http://") -> trimmed.replaceFirst("http://", "ws://")
            trimmed.startsWith("https://") -> trimmed.replaceFirst("https://", "wss://")
            else -> "ws://$trimmed"
        }
        _wifiFeedState.value = WifiFeedState(
            endpointUrl = wsUrl,
            isConnected = false,
            lastPacketSummary = "Conectando a $wsUrl..."
        )
        try {
            val req = Request.Builder().url(wsUrl).build()
            activeWebSocket = okHttpClient.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _wifiFeedState.value = _wifiFeedState.value.copy(
                        isConnected = true,
                        lastPacketSummary = "WebSocket enlazado en espera de telemetría",
                        error = null
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseIncomingExternalJson(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _wifiFeedState.value = _wifiFeedState.value.copy(
                        isConnected = false,
                        error = t.localizedMessage ?: "Error de conexión Wi-Fi/API"
                    )
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _wifiFeedState.value = _wifiFeedState.value.copy(
                        isConnected = false,
                        lastPacketSummary = "Conexión cerrada ($code)"
                    )
                }
            })
        } catch (e: Exception) {
            _wifiFeedState.value = WifiFeedState(
                endpointUrl = wsUrl,
                isConnected = false,
                error = e.localizedMessage ?: "URL inválida"
            )
        }
    }

    fun pollHttpTimingEndpoint(url: String) {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            _wifiFeedState.value = _wifiFeedState.value.copy(error = "Usa http:// o https:// para consulta REST")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(trimmed).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && body.isNotBlank()) {
                        parseIncomingExternalJson(body)
                        _wifiFeedState.value = _wifiFeedState.value.copy(
                            endpointUrl = trimmed,
                            isConnected = true,
                            error = null
                        )
                    } else {
                        _wifiFeedState.value = _wifiFeedState.value.copy(
                            isConnected = false,
                            error = "HTTP ${resp.code}"
                        )
                    }
                }
            } catch (e: Exception) {
                _wifiFeedState.value = _wifiFeedState.value.copy(
                    isConnected = false,
                    error = e.localizedMessage ?: "No se pudo consultar API externa"
                )
            }
        }
    }

    private fun parseIncomingExternalJson(raw: String) {
        try {
            val json = JSONObject(raw)
            val lapMs = json.optLong("lapTimeMs", 0L)
            if (lapMs >= 5_000L) {
                onAutomaticLapDetected?.invoke(
                    AutomaticLapTrigger(
                        lapTimeMs = lapMs,
                        source = "WIFI_API",
                        accuracyMeters = 0.0
                    )
                )
                _wifiFeedState.value = _wifiFeedState.value.copy(
                    lastPacketSummary = "Recibido lapTimeMs=$lapMs ms"
                )
            }
        } catch (_: Exception) {
            _wifiFeedState.value = _wifiFeedState.value.copy(
                lastPacketSummary = "Paquete recibido (${raw.take(40)})"
            )
        }
    }

    fun disconnectWifiWebSocket() {
        activeWebSocket?.close(1000, "User disconnected")
        activeWebSocket = null
        _wifiFeedState.value = _wifiFeedState.value.copy(
            isConnected = false,
            lastPacketSummary = "Desconectado"
        )
    }

    fun release() {
        stopGpsTracking()
        stopBleScan()
        disconnectWifiWebSocket()
    }
}
