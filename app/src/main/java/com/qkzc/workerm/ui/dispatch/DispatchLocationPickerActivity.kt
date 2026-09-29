package com.qkzc.workerm.ui.dispatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.qkzc.workerm.data.privacy.AmapPrivacyInitializer
import com.qkzc.workerm.databinding.ActivityDispatchLocationPickerBinding
import java.util.Locale

/** 管理端派工地图选点页；返回高德 GCJ-02 坐标，不在客户端自行做坐标转换。 */
class DispatchLocationPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDispatchLocationPickerBinding
    private lateinit var mapView: MapView
    private var selectedPoint: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AmapPrivacyInitializer.initializeIfConsented(this)) {
            Toast.makeText(this, "隐私政策已更新，请重新登录并确认同意后使用地图", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        binding = ActivityDispatchLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        binding.backButton.setOnClickListener { finish() }
        binding.confirmButton.isEnabled = false
        mapView.map.setOnMapClickListener { point -> selectPoint(point) }
        mapView.map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM))
        binding.confirmButton.setOnClickListener {
            val point = selectedPoint ?: return@setOnClickListener
            setResult(RESULT_OK, Intent().apply {
                putExtra(EXTRA_LATITUDE, point.latitude)
                putExtra(EXTRA_LONGITUDE, point.longitude)
            })
            finish()
        }
    }

    private fun selectPoint(point: LatLng) {
        selectedPoint = point
        mapView.map.clear()
        mapView.map.addMarker(MarkerOptions().position(point).title("施工位置"))
        binding.coordinateText.text = "已选：%.6f, %.6f".format(Locale.CHINA, point.latitude, point.longitude)
        binding.confirmButton.isEnabled = true
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { mapView.onSaveInstanceState(outState); super.onSaveInstanceState(outState) }

    companion object {
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        private const val DEFAULT_ZOOM = 4f
        fun intent(context: Context) = Intent(context, DispatchLocationPickerActivity::class.java)
    }
}
