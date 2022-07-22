package com.fencer.hatomsdk

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.blankj.utilcode.util.ToastUtils

class ViewPreviewSampleBinding{

    private   lateinit var root:View

    lateinit var  startPreviewButton : AppCompatButton
    lateinit var  stopPreviewButton :AppCompatButton

    companion object{

        fun inflate(inflater: LayoutInflater, parent: ViewGroup, attached:Boolean) :ViewPreviewSampleBinding{
            val binding = ViewPreviewSampleBinding()
            binding.root = inflater.inflate(R.layout.view_preview_sample,parent, attached)
            binding.startPreviewButton = binding.root.findViewById(R.id.startPreviewButton)
            binding.stopPreviewButton =binding.root.findViewById(R.id.stopPreviewButton)
            return  binding
        }
    }
}


/**
 */
class PreviewControlViewSample : ConstraintLayout, View.OnClickListener {

    private lateinit var viewBinding: ViewPreviewSampleBinding
    var previewUrl = "--"
        set(value) {
            field =value
            stopPreview()
        }

    private var playWindowView:PlayWindowView?=null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
    }

    private fun initView() {
        viewBinding = ViewPreviewSampleBinding.inflate(LayoutInflater.from(context), this,true)
        //设置功能按钮点击监听
        viewBinding.startPreviewButton.setOnClickListener(this)
        viewBinding.stopPreviewButton.setOnClickListener(this)

    }
    fun setPlayWindowView(playWindowView: PlayWindowView){
        this.playWindowView = playWindowView
    }



    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startPreviewButton -> {
                startPreview()
            }
            R.id.stopPreviewButton -> {
                stopPreview()
            }
        }
    }



    /**
     * 设置硬解码
     */
    fun setHardDecode(isHardDecode: Boolean) {
        playWindowView?.let {
            if (it.getPlayStatus() == PlayStatus.PLAYING) {
                ToastUtils.showLong("请在播放之前设置")
                return@let
            }
            it.setHardDecodePlay(isHardDecode)
            val content = if (isHardDecode) "硬解码开" else "硬解码关"
            ToastUtils.showShort(content)
        }

    }

    /**
     * 设置智能信息
     */
    fun setSmartDetect(isSmartDetect: Boolean) {
        playWindowView?.let {
            if (it.getPlayStatus() == PlayStatus.PLAYING) {
                ToastUtils.showLong("请在播放之前设置")
                return@let
            }
            it.setSmartDetect(isSmartDetect)
            val content = if (isSmartDetect) "智能信息开" else "智能信息关"
            ToastUtils.showShort(content)
        }

    }

    /**
     * 开启预览
     */
     fun startPreview() {

        playWindowView?.let {
            if (it.getPlayStatus() == PlayStatus.PLAYING) {
                //平滑切换
                it.changeStream(url = previewUrl)
            } else {
                it.startPlay(previewUrl)
            }
            return
        }

    }

    /**
     * 关闭预览
     */
    private fun stopPreview() {
        playWindowView?.let {
            val playWindowView = it
            playWindowView.stopPlay()
            playWindowView.showPlayIdle()
        }
    }


    /**
     * 开启声音
     */
    private fun openSound() {

        playWindowView?.let {
            val playWindowView = it
            if (playWindowView.getPlayStatus() != PlayStatus.PLAYING) {
                ToastUtils.showLong("没有视频在播放")
                return@let
            }
            if (playWindowView.isPause) {
                ToastUtils.showLong("视频已暂停")
                return@let
            }
            if (!playWindowView.isOpenAudio) {
                playWindowView.executeSound()
                ToastUtils.showLong("声音已打开")
            }
        }


    }

    /**
     * 关闭声音
     */
    private fun closeSound() {
        playWindowView?.let {
            val playWindowView = it as PlayWindowView
            if (playWindowView.getPlayStatus() != PlayStatus.PLAYING) {
                ToastUtils.showLong("没有视频在播放")
                return@let
            }
            if (playWindowView.isOpenAudio) {
                playWindowView.executeSound()
                ToastUtils.showLong("声音已关闭")
            }
        }

    }


    /**
     * 抓拍
     */
    private fun capture() {

        playWindowView?.let {
            val playWindowView = it as PlayWindowView
            if (playWindowView.getPlayStatus() != PlayStatus.PLAYING) {
                ToastUtils.showLong("没有视频在播放")
                return@let
            }
            playWindowView.executeCapture()
            ToastUtils.showShort("抓图成功")
        }


    }

    /**
     * 开启录像
     */
    private fun startRecord() {
        playWindowView?.let {
            val playWindowView = it as PlayWindowView
            if (playWindowView.getPlayStatus() != PlayStatus.PLAYING) {
                ToastUtils.showLong("没有视频在播放")
                return@let
            }
            if (playWindowView.isPause) {
                ToastUtils.showLong("视频已暂停")
                return@let
            }
            if (!playWindowView.isRecording) {
                val recordPath = playWindowView.executeRecord()
                ToastUtils.showShort("开始录像")
            }
        }
    }

    /**
     * 关闭录像
     */
    private fun stopRecord() {
        playWindowView?.let {
            val playWindowView = it as PlayWindowView
            if (playWindowView.getPlayStatus() != PlayStatus.PLAYING) {
                ToastUtils.showLong("没有视频在播放")
                return@let
            }
            if (playWindowView.isRecording) {
                playWindowView.executeRecord()
                ToastUtils.showShort("关闭录像")
            }
        }
    }



}