package com.hwzy.app.ui.screens.discover

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.abs
import java.util.Locale

private const val TAG = "SensorScreen"
private const val SMOOTHING_FACTOR = 0.3f // 平滑因子，值越小越平滑
private const val MIN_CHANGE_THRESHOLD = 0.01f // 最小变化阈值

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    
    // 权限状态
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasBluetoothPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasBluetoothPermission = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
    }
    
    // 请求权限
    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (!hasLocationPermission) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (true && !hasBluetoothPermission) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    // 获取所有已知类型的传感器
    val sensors = remember {
        sensorManager.getSensorList(Sensor.TYPE_ALL)
            .filter { sensor -> getSensorTypeName(sensor.type) != "未知传感器" }
    }
    
    // 传感器数据状态
    val sensorData = remember { mutableStateMapOf<Int, FloatArray>() }
    val smoothedData = remember { mutableStateMapOf<Int, FloatArray>() }
    
    // 设备信息状态
    val deviceInfo = remember { mutableStateOf(DeviceInfo()) }
    
    // 位置信息状态
    var locationInfo by remember { mutableStateOf(LocationInfo()) }
    
    // 位置监听器
    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationInfo = LocationInfo(
                    latitude = String.format(Locale.getDefault(), "%.6f", location.latitude),
                    longitude = String.format(Locale.getDefault(), "%.6f", location.longitude),
                    altitude = String.format(Locale.getDefault(), "%.1f 米", location.altitude),
                    accuracy = String.format(Locale.getDefault(), "%.1f 米", location.accuracy),
                    provider = location.provider.toString(),
                    speed = String.format(Locale.getDefault(), "%.1f 米/秒", location.speed),
                    bearing = String.format(Locale.getDefault(), "%.1f°", location.bearing),
                    lastUpdateTime = String.format(Locale.getDefault(), "%tF %tT", location.time, location.time)
                )
            }
            
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
    }
    
    // 请求位置更新
    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasLocationPermission) {
            try {
                // 优先使用GPS定位
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L, // 最小时间间隔：1秒
                        1f,    // 最小距离变化：1米
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
                
                // 如果GPS不可用，使用网络定位
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        1f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
                
                // 获取最后一次已知位置
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                lastKnownLocation?.let {
                    locationInfo = LocationInfo(
                        latitude = String.format(Locale.getDefault(), "%.6f", it.latitude),
                        longitude = String.format(Locale.getDefault(), "%.6f", it.longitude),
                        altitude = String.format(Locale.getDefault(), "%.1f 米", it.altitude),
                        accuracy = String.format(Locale.getDefault(), "%.1f 米", it.accuracy),
                        provider = it.provider.toString(),
                        speed = String.format(Locale.getDefault(), "%.1f 米/秒", it.speed),
                        bearing = String.format(Locale.getDefault(), "%.1f°", it.bearing),
                        lastUpdateTime = String.format(Locale.getDefault(), "%tF %tT", it.time, it.time)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "获取位置信息失败")
            }
        }
    }
    
    // 更新设备信息
    LaunchedEffect(Unit) {
        while (true) {
            deviceInfo.value = collectDeviceInfo(context)
            delay(1000) // 每秒更新一次
        }
    }
    
    // 传感器监听器
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentValues = event.values.clone()
                val previousValues = smoothedData[event.sensor.type] ?: currentValues
                
                // 应用平滑处理
                val smoothedValues = FloatArray(currentValues.size) { i ->
                    val current = currentValues[i]
                    val previous = previousValues[i]
                    val diff = current - previous
                    
                    // 如果变化太小，保持原值
                    if (abs(diff) < MIN_CHANGE_THRESHOLD) {
                        previous
                    } else {
                        // 使用指数平滑
                        previous + (diff * SMOOTHING_FACTOR)
                    }
                }
                
                sensorData[event.sensor.type] = currentValues
                smoothedData[event.sensor.type] = smoothedValues
            }
            
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                Timber.tag(TAG).d("传感器 ${sensor.name} 精度变化: $accuracy")
            }
        }
    }
    
    // 注册传感器监听
    DisposableEffect(Unit) {
        sensors.forEach { sensor ->
            sensorManager.registerListener(
                sensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL // 使用正常更新频率
            )
        }
        
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
    
    // 按类型对传感器进行分组
    remember(sensors) {
        sensors.groupBy { getSensorCategory(it.type) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("设备信息") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RectangleShape
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            item {
                InfoCard(
                    title = "基本信息",
                    items = listOf(
                        "设备型号" to deviceInfo.value.deviceModel,
                        "Android 版本" to deviceInfo.value.androidVersion,
                        "系统版本" to deviceInfo.value.systemVersion,
                        "CPU 架构" to deviceInfo.value.cpuArchitecture,
                        "内核版本" to deviceInfo.value.kernelVersion
                    )
                )
            }
            
            // 存储信息
            item {
                InfoCard(
                    title = "存储信息",
                    items = listOf(
                        "内部存储总空间" to deviceInfo.value.internalStorageTotal,
                        "内部存储可用空间" to deviceInfo.value.internalStorageAvailable,
                        "外部存储总空间" to deviceInfo.value.externalStorageTotal,
                        "外部存储可用空间" to deviceInfo.value.externalStorageAvailable
                    )
                )
            }
            
            // 内存信息
            item {
                InfoCard(
                    title = "内存信息",
                    items = listOf(
                        "总内存" to deviceInfo.value.totalMemory,
                        "可用内存" to deviceInfo.value.availableMemory,
                        "内存使用率" to deviceInfo.value.memoryUsage
                    )
                )
            }
            
            // 网络信息
            item {
                InfoCard(
                    title = "网络信息",
                    items = listOf(
                        "网络类型" to deviceInfo.value.networkType,
                        "WiFi 信号强度" to deviceInfo.value.wifiSignalStrength,
                        "移动网络类型" to deviceInfo.value.mobileNetworkType,
                        "移动网络信号强度" to deviceInfo.value.mobileSignalStrength
                    )
                )
            }
            
            // 电池信息
            item {
                InfoCard(
                    title = "电池信息",
                    items = listOf(
                        "电池电量" to deviceInfo.value.batteryLevel,
                        "充电状态" to deviceInfo.value.batteryStatus,
                        "电池温度" to deviceInfo.value.batteryTemperature,
                        "电池电压" to deviceInfo.value.batteryVoltage
                    )
                )
            }

            // 位置信息
            item {
                InfoCard(
                    title = "位置信息",
                    items = listOf(
                        "纬度" to locationInfo.latitude,
                        "经度" to locationInfo.longitude,
                        "海拔" to locationInfo.altitude,
                        "精度" to locationInfo.accuracy,
                        "位置提供者" to locationInfo.provider,
                        "速度" to locationInfo.speed,
                        "方向" to locationInfo.bearing,
                        "最后更新时间" to locationInfo.lastUpdateTime
                    )
                )
            }
            
            // 蓝牙信息
            item {
                InfoCard(
                    title = "蓝牙信息",
                    items = listOf(
                        "蓝牙状态" to deviceInfo.value.bluetoothInfo.isEnabled,
                        "设备名称" to deviceInfo.value.bluetoothInfo.name,
                        "连接状态" to deviceInfo.value.bluetoothInfo.state,
                        "已配对设备" to deviceInfo.value.bluetoothInfo.connectedDevices,
                        "支持的配置文件" to deviceInfo.value.bluetoothInfo.supportedProfiles
                    )
                )
            }

            // 传感器信息
            item {
                InfoCard(
                    title = "传感器信息",
                    items = deviceInfo.value.sensors
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            items.forEach { (label, value) ->
                Text(
                    text = "$label: $value",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

private fun getSensorCategory(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR,
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "运动传感器"
        
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_PROXIMITY,
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_RELATIVE_HUMIDITY -> "环境传感器"
        
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "位置传感器"
        
        Sensor.TYPE_HEART_RATE,
        Sensor.TYPE_HEART_BEAT,
        Sensor.TYPE_STEP_COUNTER,
        Sensor.TYPE_STEP_DETECTOR -> "健康传感器"
        
        else -> "其他传感器"
    }
}

private fun getSensorTypeName(type: Int): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "加速度计"
        Sensor.TYPE_GRAVITY -> "重力传感器"
        Sensor.TYPE_GYROSCOPE -> "陀螺仪"
        Sensor.TYPE_LINEAR_ACCELERATION -> "线性加速度计"
        Sensor.TYPE_ROTATION_VECTOR -> "旋转向量"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "游戏旋转向量"
        Sensor.TYPE_LIGHT -> "光线传感器"
        Sensor.TYPE_PROXIMITY -> "距离传感器"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "温度传感器"
        Sensor.TYPE_PRESSURE -> "压力传感器"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "湿度传感器"
        Sensor.TYPE_MAGNETIC_FIELD -> "磁场传感器"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "未校准磁场传感器"
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "地磁旋转向量"
        Sensor.TYPE_HEART_RATE -> "心率传感器"
        Sensor.TYPE_HEART_BEAT -> "心跳传感器"
        Sensor.TYPE_STEP_COUNTER -> "计步器"
        Sensor.TYPE_STEP_DETECTOR -> "步数检测器"
        else -> "其他传感器"
    }
}

private data class DeviceInfo(
    val deviceModel: String = "未知",
    val androidVersion: String = "未知",
    val systemVersion: String = "未知",
    val cpuArchitecture: String = "未知",
    val kernelVersion: String = "未知",
    val internalStorageTotal: String = "未知",
    val internalStorageAvailable: String = "未知",
    val externalStorageTotal: String = "未知",
    val externalStorageAvailable: String = "未知",
    val totalMemory: String = "未知",
    val availableMemory: String = "未知",
    val memoryUsage: String = "未知",
    val networkType: String = "未知",
    val wifiSignalStrength: String = "未知",
    val mobileNetworkType: String = "未知",
    val mobileSignalStrength: String = "未知",
    val batteryLevel: String = "未知",
    val batteryStatus: String = "未知",
    val batteryTemperature: String = "未知",
    val batteryVoltage: String = "未知",
    val sensors: List<Pair<String, String>> = emptyList(),
    val locationInfo: LocationInfo = LocationInfo(),
    val bluetoothInfo: BluetoothInfo = BluetoothInfo()
)

private data class LocationInfo(
    val latitude: String = "未知",
    val longitude: String = "未知",
    val altitude: String = "未知",
    val accuracy: String = "未知",
    val provider: String = "未知",
    val speed: String = "未知",
    val bearing: String = "未知",
    val lastUpdateTime: String = "未知"
)

private data class BluetoothInfo(
    val isEnabled: String = "未知",
    val name: String = "未知",
    val state: String = "未知",
    val connectedDevices: String = "未知",
    val supportedProfiles: String = "未知"
)

private fun collectDeviceInfo(context: Context): DeviceInfo {
    context.packageManager
    DecimalFormat("#.##")
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // 检查权限
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasBluetoothPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
    
    // 基本信息
    val deviceModel = Build.MODEL
    val androidVersion = "Android ${Build.VERSION.RELEASE}"
    val systemVersion = Build.DISPLAY
    val cpuArchitecture = System.getProperty("os.arch") ?: "未知"
    val kernelVersion = System.getProperty("os.version") ?: "未知"
    
    // 存储信息
    val internalStorage = StatFs(Environment.getDataDirectory().path)
    val externalStorage = StatFs(Environment.getExternalStorageDirectory().path)
    
    val internalStorageTotal = formatSize(internalStorage.totalBytes.toDouble())
    val internalStorageAvailable = formatSize(internalStorage.availableBytes.toDouble())
    val externalStorageTotal = formatSize(externalStorage.totalBytes.toDouble())
    val externalStorageAvailable = formatSize(externalStorage.availableBytes.toDouble())
    
    // 内存信息
    val runtime = Runtime.getRuntime()
    val totalMemory = formatSize(runtime.totalMemory().toDouble())
    val availableMemory = formatSize(runtime.freeMemory().toDouble())
    val memoryUsage = String.format(Locale.getDefault(), "%.2f%%", (1 - runtime.freeMemory().toDouble() / runtime.totalMemory()) * 100)
    
    // 网络信息
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    
    val networkType = when {
        capabilities == null -> "无网络"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
        else -> "其他"
    }
    
    // 电池信息
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    val batteryLevel = "${batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
    val batteryStatus = when (batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)) {
        android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
        android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
        android.os.BatteryManager.BATTERY_STATUS_FULL -> "已充满"
        android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
        else -> "未知"
    }
    
    // 使用 Intent 获取电池温度
    val batteryTemperature = try {
        val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val temperature = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        "${temperature / 10.0}°C"
    } catch (_: Exception) {
        "未知"
    }
    
    // 使用 Intent 获取电池电压
    val batteryVoltage = try {
        val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val voltage = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        "${voltage / 1000.0}V"
    } catch (_: Exception) {
        "未知"
    }
    
    // 传感器信息
    val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        .map { sensor ->
            sensor.name to when (sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> "加速度计"
                Sensor.TYPE_GYROSCOPE -> "陀螺仪"
                Sensor.TYPE_LIGHT -> "光线传感器"
                Sensor.TYPE_PROXIMITY -> "距离传感器"
                Sensor.TYPE_MAGNETIC_FIELD -> "磁场传感器"
                Sensor.TYPE_PRESSURE -> "压力传感器"
                Sensor.TYPE_AMBIENT_TEMPERATURE -> "环境温度传感器"
                Sensor.TYPE_GRAVITY -> "重力传感器"
                Sensor.TYPE_LINEAR_ACCELERATION -> "线性加速度计"
                Sensor.TYPE_ROTATION_VECTOR -> "旋转向量"
                Sensor.TYPE_RELATIVE_HUMIDITY -> "湿度传感器"
                Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "未校准磁场传感器"
                Sensor.TYPE_GAME_ROTATION_VECTOR -> "游戏旋转向量"
                Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "未校准陀螺仪"
                Sensor.TYPE_SIGNIFICANT_MOTION -> "显著运动传感器"
                Sensor.TYPE_STEP_DETECTOR -> "步数检测器"
                Sensor.TYPE_STEP_COUNTER -> "计步器"
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "地磁旋转向量"
                Sensor.TYPE_HEART_RATE -> "心率传感器"
                else -> "其他传感器"
            }
        }
    
    // 蓝牙信息
    val bluetoothInfo = if (hasBluetoothPermission) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        bluetoothAdapter?.let {
            val profiles = mutableListOf<String>()
            if (it.isEnabled) {
                // 检查基本配置文件
                if (it.getProfileConnectionState(BluetoothProfile.A2DP) != BluetoothAdapter.STATE_DISCONNECTED) {
                    profiles.add("A2DP")
                }
                if (it.getProfileConnectionState(BluetoothProfile.HEADSET) != BluetoothAdapter.STATE_DISCONNECTED) {
                    profiles.add("HSP")
                }
                if (it.getProfileConnectionState(BluetoothProfile.GATT) != BluetoothAdapter.STATE_DISCONNECTED) {
                    profiles.add("GATT")
                }
                if (it.getProfileConnectionState(BluetoothProfile.GATT_SERVER) != BluetoothAdapter.STATE_DISCONNECTED) {
                    profiles.add("GATT Server")
                }
                
                // 检查高级配置文件（需要版本检查）
                if (it.getProfileConnectionState(BluetoothProfile.LE_AUDIO) != BluetoothAdapter.STATE_DISCONNECTED) {
                    profiles.add("LE Audio")
                }
            }
            
            BluetoothInfo(
                isEnabled = if (it.isEnabled) "已启用" else "已禁用",
                name = it.name ?: "未知",
                state = when (it.state) {
                    BluetoothAdapter.STATE_OFF -> "已关闭"
                    BluetoothAdapter.STATE_TURNING_OFF -> "正在关闭"
                    BluetoothAdapter.STATE_ON -> "已开启"
                    BluetoothAdapter.STATE_TURNING_ON -> "正在开启"
                    else -> "未知"
                },
                connectedDevices = "${it.bondedDevices.size} 个已配对设备",
                supportedProfiles = if (profiles.isEmpty()) "无" else profiles.joinToString(", ")
            )
        } ?: BluetoothInfo()
    } else {
        BluetoothInfo()
    }

    return DeviceInfo(
        deviceModel = deviceModel,
        androidVersion = androidVersion,
        systemVersion = systemVersion,
        cpuArchitecture = cpuArchitecture,
        kernelVersion = kernelVersion,
        internalStorageTotal = internalStorageTotal,
        internalStorageAvailable = internalStorageAvailable,
        externalStorageTotal = externalStorageTotal,
        externalStorageAvailable = externalStorageAvailable,
        totalMemory = totalMemory,
        availableMemory = availableMemory,
        memoryUsage = memoryUsage,
        networkType = networkType,
        batteryLevel = batteryLevel,
        batteryStatus = batteryStatus,
        batteryTemperature = batteryTemperature,
        batteryVoltage = batteryVoltage,
        sensors = sensors,
        locationInfo = LocationInfo(),
        bluetoothInfo = bluetoothInfo
    )
}

private fun formatSize(size: Double): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = size
    var unitIndex = 0
    
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    
    return String.format(Locale.getDefault(), "%.2f %s", value, units[unitIndex])
} 