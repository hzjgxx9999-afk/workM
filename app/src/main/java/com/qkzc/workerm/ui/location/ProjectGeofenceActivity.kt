package com.qkzc.workerm.ui.location

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.qkzc.workerm.data.location.ProjectGeofenceSaveRequest
import com.qkzc.workerm.data.location.ProjectLocationRepository
import com.qkzc.workerm.data.privacy.AmapPrivacyInitializer
import com.qkzc.workerm.data.session.SessionStore
import com.qkzc.workerm.databinding.ActivityProjectGeofenceBinding
import com.qkzc.workerm.ui.project.ProjectDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 项目圆形电子围栏配置。保存结果由后端作为工人定位上报的唯一判断依据。 */
class ProjectGeofenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProjectGeofenceBinding
    private lateinit var mapView: MapView
    private val repository = ProjectLocationRepository()
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private var projectId = 0L
    private var accessToken = ""
    private var selectedCenter: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AmapPrivacyInitializer.initializeIfConsented(this)) {
            toast("隐私政策已更新，请重新登录并确认同意后使用地图")
            finish()
            return
        }
        binding = ActivityProjectGeofenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        binding.backButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { save() }
        binding.disableButton.setOnClickListener { disable() }
        mapView.map.setOnMapClickListener(::selectCenter)
        lifecycleScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                accessToken = session.accessToken
                projectId = intent.getLongExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, 0L)
                    .takeIf { it > 0L } ?: session.projectId.toLongOrNull() ?: error("未获取到项目")
                repository.geofence(accessToken, projectId)
            }.onSuccess { existing ->
                existing?.centerLatitude?.let { latitude ->
                    existing.centerLongitude?.let { longitude ->
                        binding.radiusInput.setText((existing.radius ?: DEFAULT_RADIUS).toString())
                        selectCenter(LatLng(latitude, longitude))
                    }
                }
            }.onFailure { toast(it.message ?: "加载围栏失败") }
        }
    }

    private fun selectCenter(point: LatLng) {
        selectedCenter = point
        val radius = binding.radiusInput.text.toString().toIntOrNull()?.coerceIn(MIN_RADIUS, MAX_RADIUS) ?: DEFAULT_RADIUS
        mapView.map.clear()
        mapView.map.addMarker(MarkerOptions().position(point).title("围栏中心"))
        mapView.map.addCircle(CircleOptions().center(point).radius(radius.toDouble()).strokeWidth(4f))
        mapView.map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 17f))
        binding.statusText.text = "已选择围栏中心，半径 $radius 米"
    }

    private fun save() {
        val center = selectedCenter ?: run { toast("请先点击地图选择围栏中心"); return }
        val radius = binding.radiusInput.text.toString().toIntOrNull()?.takeIf { it in MIN_RADIUS..MAX_RADIUS }
            ?: run { binding.radiusInput.error = "半径范围 $MIN_RADIUS-$MAX_RADIUS 米"; return }
        lifecycleScope.launch {
            runCatching {
                repository.saveGeofence(accessToken, projectId, ProjectGeofenceSaveRequest("CIRCLE", center.longitude, center.latitude, radius))
            }.onSuccess { toast("电子围栏已保存"); selectCenter(center) }
                .onFailure { toast(it.message ?: "围栏保存失败") }
        }
    }

    private fun disable() {
        lifecycleScope.launch {
            runCatching { repository.disableGeofence(accessToken, projectId) }
                .onSuccess { selectedCenter = null; mapView.map.clear(); binding.statusText.text = "未启用电子围栏"; toast("电子围栏已停用") }
                .onFailure { toast(it.message ?: "围栏停用失败") }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { mapView.onSaveInstanceState(outState); super.onSaveInstanceState(outState) }

    companion object {
        private const val DEFAULT_RADIUS = 200
        private const val MIN_RADIUS = 20
        private const val MAX_RADIUS = 5000
    }
}
