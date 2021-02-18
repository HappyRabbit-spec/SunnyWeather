package com.sunnyweather.android.ui.weather
import android.content.Context
import com.sunnyweather.android.R

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.sunnyweather.android.databinding.ActivityWeatherBinding
import com.sunnyweather.android.databinding.ForecastItemBinding
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
import com.sunnyweather.android.logic.network.WeatherService
import com.sunnyweather.android.ui.place.PlaceFragment
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {
    lateinit var binding: ActivityWeatherBinding

    val viewModel by lazy { ViewModelProvider(this).get(WeatherViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            window.setDecorFitsSystemWindows(false)
//            window.statusBarColor = Color.TRANSPARENT
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            window.insetsController?.let {
//                it.hide(WindowInsets.Type.statusBars())
//            }
//        }
//        else{
//            val decorView = window.decorView
//            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//            window.statusBarColor = Color.TRANSPARENT
//        }

        binding = ActivityWeatherBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (viewModel.locationLng.isEmpty()){
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }

        if (viewModel.locationLat.isEmpty()){
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }

        if (viewModel.placeName.isEmpty()){
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }

        viewModel.weatherLiveData.observe(this){
            val weather = it.getOrNull()
            if (weather != null){
                showWeatherInfo(weather)
            }
            else{
                Toast.makeText(this, "无法成功获取天气信息",Toast.LENGTH_SHORT).show()
                it.exceptionOrNull()?.printStackTrace()
            }

            binding.swipeRefresh.isRefreshing = false
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500)
        refreshWeather()
        binding.swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }

        binding.nowParentLayout.navBtn.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        //主要是滑动菜单隐藏的时候要隐藏键盘
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
    }

    private fun showWeatherInfo(weather:Weather){
        binding.nowParentLayout.placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        //填充now.xml布局中数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        binding.nowParentLayout.currentTemp.text = currentTempText
        binding.nowParentLayout.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        binding.nowParentLayout.currentAQI.text = currentPM25Text
        binding.nowParentLayout.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        //填充forecast.xml布局中数据
        binding.forecastParentLayout.forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days){
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val newBinding = ForecastItemBinding.inflate(LayoutInflater.from(this),binding.forecastParentLayout.forecastLayout, false)
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            newBinding.dateInfo.text = simpleDateFormat.format(skycon.date)

            val sky = getSky(skycon.value)
            newBinding.skyIcon.setImageResource(sky.icon)
            newBinding.skyInfo.text = sky.info

            val tempText = "${temperature.min.toInt()} - ${temperature.max.toInt()} ℃"
            newBinding.temperatureInfo.text = tempText
            binding.forecastParentLayout.forecastLayout.addView(newBinding.root)
        }

        //填充lifeIndex.xml布局中数据
        val lifeIndex = daily.lifeIndex
        binding.lifeIndexParentLayout.coldRiskText.text = lifeIndex.coldRisk[0].desc
        binding.lifeIndexParentLayout.dressingText.text = lifeIndex.dressing[0].desc
        binding.lifeIndexParentLayout.ultravioletText.text = lifeIndex.ultraviolet[0].desc
        binding.lifeIndexParentLayout.carWashingText.text = lifeIndex.carWashing[0].desc

        binding.weatherLayout.visibility = View.VISIBLE
    }

    fun refreshWeather(){
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
        binding.swipeRefresh.isRefreshing = true
    }
}