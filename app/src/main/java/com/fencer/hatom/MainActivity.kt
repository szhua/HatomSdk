package com.fencer.hatom

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fencer.hatomsdk.PlayConfig
import com.fencer.hatomsdk.VideoPlayFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val path  ="http://120.224.28.51:8014/lcyzt/app-AppYsq-queryVideoUrl.do?userDevBean.telphone=18363030195&userDevBean.deviceid=271233455&spInfoBean.id=46A347ED731742D88F8F1A318B4FF6AA"
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.add (R.id.main_container, VideoPlayFragment.getInstance(PlayConfig().apply {
            autoPlay=true
            url = "rtsp://221.2.205.44:554/openUrl/E58wyvm"
            hiddenController=true
            screenRatio = "h,16:10"
        }))
        beginTransaction.commit()
    }
}