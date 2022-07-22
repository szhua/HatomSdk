package com.fencer.hatomsdk

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Observer
import com.fencer.hatomsdk.window.PlayTextureView
import com.fencer.hatomsdk.window.WindowItemView
import com.hikvision.hatomplayer.PlayCallback
import com.hikvision.hatomplayer.core.CorrectType
import com.hikvision.hatomplayer.core.PlaceType
import com.hikvision.hatomplayer.core.PlaybackSpeed
import com.hikvision.hatomplayer.core.Quality
import java.text.DecimalFormat
import java.text.MessageFormat

/**
 * <p> 播放窗口View </p>
 * @author 段自航 2021/7/15 15:38
 * @version V1.0
 */

class ViewPlayWindowBinding{

    private   lateinit var root:View

    lateinit var  textureView: PlayTextureView
    lateinit var  windowBg:View
    lateinit var  loadingView : ProgressBar
    lateinit var  hintText :TextView
    lateinit var  zoomText:TextView
    lateinit var  talkHintText :TextView

    companion object{

        fun inflate(inflater: LayoutInflater,parent:ViewGroup ,attached:Boolean) :ViewPlayWindowBinding{
            val binding = ViewPlayWindowBinding()
            binding.root = inflater.inflate(R.layout.view_play_window,parent, attached)
            binding.textureView = binding.root.findViewById(R.id.textureView)
            binding.windowBg =binding.root.findViewById(R.id.windowBg)
            binding.loadingView =binding.root.findViewById(R.id.loadingView)
            binding.hintText =binding.root.findViewById(R.id.hintText)
            binding.zoomText=binding.root.findViewById(R.id.zoomText)
            binding.talkHintText = binding.root.findViewById(R.id.talkHintText)
            return  binding
        }
    }
}


class PlayWindowView : WindowItemView, TextureView.SurfaceTextureListener {

    private lateinit var viewBinding: ViewPlayWindowBinding

    /*是否正在录像*/
    var isRecording = false

    /*声音是否打开*/
    var isOpenAudio = false

    /*电子放大是否打开*/
    var isOpenZoom = false

    /*是否暂停中*/
    var isPause = false

    /*是否开启对讲*/
    var isVoiceTalking = false

    /*是否开启鱼眼模式*/
    var isOpenFishEyeMode = false

    /*上一秒总流量*/
    var lastTotalTraffic = 0L

    /*是否正在Seek*/
    var isSeeking = false

    /*viewModel这里直接new，每一个播放窗口对应一个*/
    private val viewModel by lazy {
        PlayViewModel()
    }
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    private fun initView() {
        background = null
        viewBinding = ViewPlayWindowBinding.inflate(LayoutInflater.from(context), this,true)
        setListener()
    }

    private fun setListener() {
        setOnOpenDigitalZoomListener {
            //只有在播放的窗口才可以开启电子放大
            if (viewModel.getPlayStatus() == PlayStatus.PLAYING) {
                executeZoom()
            }
        }
        //设置鱼眼手势监听
        viewBinding.textureView.setOnFECPTZActionListener(object :
            PlayTextureView.OnFECPTZActionListener {
            override fun onFECPTZActionDown(originalX: Float, originalY: Float) {
                viewModel.setOriginalPTZParam(
                    originalX, originalY,
                    viewBinding.textureView.width, viewBinding.textureView.height
                )
            }

            override fun onFECPTZActionMove(
                isZoom: Boolean,
                zoom: Float,
                zoom3D: Float,
                curX: Float,
                curY: Float
            ) {
                viewModel.handleFishEyePTZ(isZoom, zoom, zoom3D, curX, curY)
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewBinding.textureView.surfaceTextureListener = this
        //添加监听
        viewModel.playResult.observeForever(playObserver)
        viewModel.talkResult.observeForever(talkObserver)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //去除监听
        viewBinding.textureView.surfaceTextureListener = null
        viewModel.surfaceTexture = null
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        //这里回调了窗口当前是否对用户可见，多窗口切换时，可以用来停止播放、恢复播放
        if (isVisibleToUser) {
            if (viewModel.getPlayStatus() != PlayStatus.IDLE) {
                againPlay()
            }
        } else {
            //不可见时，关闭播放
            if (viewModel.getPlayStatus() == PlayStatus.PLAYING || viewModel.getPlayStatus() == PlayStatus.LOADING) {
                //停止播放
                stopPlay()
            }
        }
    }

    /**
     * 播放结果
     */
    private val playObserver = Observer<PlayResult> {
        if (it == null) {
            return@Observer
        }
        when (it.status) {
            PlayCallback.Status.SUCCESS -> {
                if (it.extraCode == PlayResult.PLAYBACK_SEEK_SUCCESS) {
                    //这里是seek拖动成功回调
                    viewBinding.loadingView.isVisible = false
                } else {
                    showPlaySuccess()
                }
                isSeeking = false
            }
            PlayCallback.Status.FAILED -> {
                isSeeking = false
                showPlayFailed(it.errorCode)
            }
            PlayCallback.Status.EXCEPTION -> {
                isSeeking = false
                showPlayFailed(it.errorCode)
            }
            PlayCallback.Status.FINISH -> {
                isSeeking = false
                showPlayEnd()
            }
        }
    }

    fun getFrameRate(): Int {
        return viewModel.getFrameRate()
    }

    /**
     * 设置期待帧率
     */
    fun setExpectedFrameRate(rate: Float): Boolean {
        return viewModel.setExpectedFrameRate(rate)
    }

    fun showPlayIdle() {
        viewBinding.windowBg.isVisible = true
        viewBinding.hintText.isVisible = false
        viewBinding.loadingView.isVisible = false
        viewBinding.zoomText.isVisible = false
    }

    private fun showPlayLoading() {
        viewBinding.windowBg.isVisible = true
        viewBinding.hintText.isVisible = false
        viewBinding.loadingView.isVisible = true
        viewBinding.zoomText.isVisible = false
        viewBinding.talkHintText.isVisible = false
    }

    private fun showPlaySuccess() {
        viewBinding.windowBg.isVisible = false
        viewBinding.hintText.isVisible = false
        viewBinding.loadingView.isVisible = false
        viewBinding.zoomText.isVisible = false
        viewBinding.talkHintText.isVisible = false
    }

    private fun showPlayFailed(errorCode: String) {
        viewBinding.windowBg.isVisible = true
        viewBinding.hintText.isVisible = true
        viewBinding.hintText.text =
            MessageFormat.format("播放失败,错误码为：{0}", MyUtils.convertToHexString(errorCode))
        viewBinding.loadingView.isVisible = false
        viewBinding.zoomText.isVisible = false
        viewBinding.talkHintText.isVisible = false
    }

    private fun showPlayEnd() {
        viewBinding.windowBg.isVisible = true
        viewBinding.hintText.isVisible = true
        viewBinding.hintText.text = "回放结束"
        viewBinding.loadingView.isVisible = false
        viewBinding.zoomText.isVisible = false
        viewBinding.talkHintText.isVisible = false
    }

    /**
     * 对讲结果
     */
    private val talkObserver = Observer<PlayResult> {
        if (it == null) {
            return@Observer
        }
        when (it.status) {
            PlayCallback.Status.SUCCESS -> {
                //只有之前是对讲开启状态的，才可以进入对讲成功
                //因为如果之前正在开启过程中，调用关闭对讲后，会先回调成功，在被关闭，导致显示两个成功
                viewBinding.talkHintText.text = "正在对讲..."
            }
            PlayCallback.Status.FAILED -> {
                viewBinding.talkHintText.text =
                    MessageFormat.format("对讲失败，错误码：{0}", MyUtils.convertToHexString(it.errorCode))
                isVoiceTalking = false
            }
            PlayCallback.Status.EXCEPTION -> {
                viewBinding.talkHintText.text =
                    MessageFormat.format("对讲发生异常，错误码：{0}", MyUtils.convertToHexString(it.errorCode))
                isVoiceTalking = false
            }
            PlayCallback.Status.FINISH -> {
                //啥也不干，因为不会回调这个状态
            }
        }
    }

    /**
     * 是否是硬解码
     */
    fun setHardDecodePlay(isHardDecode: Boolean) {
        viewModel.hardDecode = isHardDecode
    }

    /**
     * 是否显示智能信息
     */
    fun setSmartDetect(isSmartDetect: Boolean) {
        viewModel.smartDetect = isSmartDetect
    }

    /**
     * 获取当前播放状态
     */
    fun getPlayStatus(): PlayStatus {
        return viewModel.getPlayStatus()
    }

    /**
     * 开启播放
     */
    fun startPlay(url: String, startTime: Long = 0L, endTime: Long = 0L) {
        showPlayLoading()
        if (isPreviewWindow) {
            viewModel.startPreview(url)
        } else {
            viewModel.startPlayback(url, startTime, endTime)
        }
    }

    /**
     * 码流切换
     */
    fun changeStream(url: String) {
        if (isPreviewWindow) {
            viewModel.changeStream(Quality.MAIN_STREAM_HIGH, url)
        }
    }

    /**
     * 关闭播放
     */
    fun stopPlay() {
        resetExecuteState()
        viewModel.stopPlay()
    }

    private fun againPlay() {
        if (viewModel.playUrl.isEmpty()) return
        showPlayLoading()
        if (isPreviewWindow) {
            viewModel.startPreview(viewModel.playUrl)
        } else {
            viewModel.startPlayback(viewModel.playUrl, viewModel.startTime, viewModel.endTime)
        }
    }

    /**
     * 开启对讲
     */
    fun openVoiceTalk(talkUrl: String) {
        viewBinding.talkHintText.text = "正在开启对讲"
        viewBinding.talkHintText.isVisible = true
        //设置为扬声器对讲
        val audioManage = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!audioManage.isSpeakerphoneOn) {
            audioManage.isSpeakerphoneOn = true
        }
        isVoiceTalking = true
        viewModel.openVoiceTalk(talkUrl)
    }

    /**
     * 关闭对讲
     */
    fun closeVoiceTalk() {
        hideVoiceTalkHint()
        isVoiceTalking = false
        viewModel.closeVoiceTalk()
    }

    fun hideVoiceTalkHint() {
        viewBinding.talkHintText.isVisible = false
        viewBinding.talkHintText.text = ""
    }

    /**
     * 抓拍
     */
    fun executeCapture(): String {
        return viewModel.capture()
    }

    /**
     * 执行声音改变事件
     */
    fun executeSound() {
        if (!isOpenAudio) {
            isOpenAudio = viewModel.enableAudio(true)
        } else {
            isOpenAudio = false
            viewModel.enableAudio(false)
        }
    }

    /**
     * 执行电子放大事件
     */
    fun executeZoom() {
        if (!isOpenZoom) {
            //打开电子放大
            viewBinding.textureView.setOnZoomListener { oRect, curRect ->
                viewModel.openDigitalZoom(oRect, curRect)
            }
            //设置电子放大倍率监听
            val decimalFormat = DecimalFormat("0.0")
            viewBinding.textureView.setOnZoomScaleListener { scale: Float ->
                if (scale < 1.0f && isOpenZoom) {
                    //如果已经开启了电子放大且倍率小于1就关闭电子放大
                    executeZoom()
                }
                if (scale >= 1.0f) {
                    viewBinding.zoomText.text =
                        MessageFormat.format("{0}X", decimalFormat.format(scale.toDouble()))
                }
            }
            isOpenZoom = true
            viewBinding.zoomText.isVisible = true
            viewBinding.zoomText.text = MessageFormat.format(
                "{0}X",
                decimalFormat.format(1.0)
            )
        } else {
            //关闭电子放大
            isOpenZoom = false
            viewBinding.zoomText.isVisible = false
            viewBinding.textureView.setOnZoomListener(null)
            viewBinding.textureView.setOnZoomScaleListener(null)
            viewModel.closeDigitalZoom()
        }
    }

    /**
     * 执行录像事件
     */
    fun executeRecord(): String {
        return if (!isRecording) {
            isRecording = viewModel.startRecord()
            viewModel.recordFilePath
        } else {
            isRecording = false
            viewModel.stopRecord()
            ""
        }
    }

    /**
     * 执行暂停或者恢复事件
     * 暂停时要将获取系统时间的定时器、剪辑视频功能和声音都要关闭
     */
    fun executePauseOrResume() {
        if (!isPause) {
            isPause = true
            viewModel.pause()
            if (isPause) {
                viewBinding.hintText.isVisible = true
                viewBinding.hintText.text = "暂停中..."
                //暂停时要将剪辑视频给关闭
                if (isRecording) {
                    executeRecord()
                }
                if (isOpenAudio) {
                    executeSound()
                }
            }
        } else {
            isPause = false
            viewModel.resume()
            viewBinding.hintText.isVisible = false
            viewBinding.hintText.text = ""
        }
    }

    /**
     * 设置回放倍速
     */
    fun setPlaybackSpeed(speed: PlaybackSpeed) {
        viewModel.setPlaybackSpeed(speed)
    }

    /**
     * 拖动回放
     */
    fun seekPlayback(seekTime: Long) {
        if (isRecording) {
            executeRecord()
        }
        if (isSeeking) {
            return
        }
        isSeeking = true
        viewBinding.loadingView.isVisible = true
        viewModel.setPlaybackSpeed(PlaybackSpeed.NORMAL)
        viewModel.seekPlayback(seekTime)
    }

    /**
     * 获取总流量
     */
    fun getTotalTraffic(): Long {
        return viewModel.getTotalTraffic()
    }

    /**
     * 获取OSD时间
     */
    fun getOSDTime(): Long {
        return viewModel.getOSDTime()
    }

    /**
     * 重置所有的操作状态
     */
    private fun resetExecuteState() {
        if (isOpenAudio) {
            executeSound()
        }
        if (isVoiceTalking) {
            closeVoiceTalk()
        }
        if (isRecording) {
            executeRecord()
        }
        if (isOpenZoom) {
            executeZoom()
        }
        if (isOpenFishEyeMode) {
            closeFishEyeMode()
        }
    }

    /**
     * 打开鱼眼模式
     */
    fun openFishEye(): Boolean {
        isOpenFishEyeMode = true
        viewBinding.textureView.isFECPTZMode = true
        return viewModel.setFishEyeEnable(true)
    }

    /**
     * 设置鱼眼矫正类型
     */
    fun setFishEyeMode(
        @CorrectType correctType: Int,
        @PlaceType placeType: Int = PlaceType.FEC_PLACE_CEILING
    ): Boolean {
        //todo 顶装不支持维度拉伸，只有壁装支持,这里展示所有的功能，主动修改了安装方式，实际应该根据点位的安装方式控制功能按钮是否可用
        var devicePlaceType = placeType
        if (correctType == CorrectType.FEC_CORRECT_LAT) {
            devicePlaceType = PlaceType.FEC_PLACE_WALL
        }
        return viewModel.setFishEyeMode(correctType, devicePlaceType)
    }


    /**
     * 关闭鱼眼模式
     */
    fun closeFishEyeMode() {
        viewModel.setFishEyeEnable(false)
        isOpenFishEyeMode = false
        viewBinding.textureView.isFECPTZMode = false
    }


    override fun isZoom(): Boolean {
        return isOpenZoom
    }

    override fun isOpenFishEye(): Boolean {
        return isOpenFishEyeMode
    }


    /*TextureView的监听事件***************************************start*/

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        viewModel.surfaceTexture = surface

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surface.setOnFrameAvailableListener(null)
        if (viewModel.getPlayStatus() == PlayStatus.PLAYING || viewModel.getPlayStatus() == PlayStatus.LOADING) {
            //停止播放
            stopPlay()
        }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }



    /*TextureView的监听事件***************************************end*/
    inline var View.isVisible: Boolean
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.GONE
        }

}