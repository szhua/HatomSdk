package com.fencer.hatomsdk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

}


class VideoPlayFragment :Fragment() {

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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sampleControllerView = view.findViewById<PreviewControlViewSample>(R.id.sample_controller_view)
        val  playWindow =view.findViewById<PlayWindowView>(R.id.play_window)
        if (playConfig.url.isNullOrEmpty()||playConfig.url?.trim().isNullOrEmpty()){
            ToastUtils.showShort("当前视频路径未设置")
            return
        }
        if (playConfig.hiddenController){
            sampleControllerView.visibility =View.GONE
        }else{
            sampleControllerView.visibility =View.VISIBLE
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