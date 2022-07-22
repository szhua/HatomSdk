package com.fencer.hatomsdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ToastUtils


/**

@author  SZhua
Create at  2022/7/21
Description: 播放视频的界面

 */

class  PlayConfig{
    var autoPlay :Boolean =false
    var url :String ? =""
    var hiddenController =false
    var screenRatio  =  "h,16:9"
}


class VideoPlayFragment :Fragment() {

    private lateinit var sampleControllerView: PreviewControlViewSample
    private lateinit var playWindow: PlayWindowView

    var playConfig:PlayConfig = PlayConfig()

    companion object{
        fun getInstance(playConfig: PlayConfig ):VideoPlayFragment{
            val videoPlayFragment = VideoPlayFragment()
            videoPlayFragment.playConfig =playConfig
            return videoPlayFragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video_play,container,false)
    }

    override fun onDestroy() {
        sampleControllerView.stopPreview()
        super.onDestroy()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         sampleControllerView = view.findViewById<PreviewControlViewSample>(R.id.sample_controller_view)
          playWindow =view.findViewById<PlayWindowView>(R.id.play_window)
        if (playConfig.url.isNullOrEmpty()||playConfig.url?.trim().isNullOrEmpty()){
            ToastUtils.showShort("当前视频路径未设置")
            return
        }
        if (playConfig.hiddenController){
            sampleControllerView.visibility =View.GONE
        }else{
            sampleControllerView.visibility =View.VISIBLE
        }

        val content = view.findViewById<ConstraintLayout>(R.id.content_container)

        /**
         * 设置比例；
         */
        if (playConfig.screenRatio.isNotEmpty()&& playConfig.screenRatio.startsWith("h")){
            val constraintSet = ConstraintSet()
            constraintSet.clone(content)
            constraintSet.setDimensionRatio(R.id.play_window,playConfig.screenRatio)
            constraintSet.applyTo(content)
        }

        sampleControllerView.previewUrl = playConfig.url?.trim()?:""
        sampleControllerView.setPlayWindowView(playWindow)
        if (playConfig.autoPlay){
            sampleControllerView.postDelayed({
                sampleControllerView.startPreview()
            },800)
        }
    }



}