package com.fencer.hatomsdk

import android.graphics.Rect
import android.graphics.SurfaceTexture
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.Utils
import com.hikvision.hatomplayer.DefaultHatomPlayer
import com.hikvision.hatomplayer.HatomPlayer
import com.hikvision.hatomplayer.PlayCallback
import com.hikvision.hatomplayer.PlayConfig
import com.hikvision.hatomplayer.core.*
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*


/**
 * <p> 预览ViewModel </p>
 */
class PlayViewModel : ViewModel(), PlayCallback.PlayStatusCallback, PlayCallback.VoiceTalkCallback {

    private val tasks = mutableListOf<Disposable>()

    /**
     * 播放结果
     */
    private val _previewResult = MutableLiveData<PlayResult>()
    val playResult: LiveData<PlayResult> = _previewResult

    /**
     * 对讲结果
     */
    private val _talkResult = MutableLiveData<PlayResult>()
    val talkResult: LiveData<PlayResult> = _talkResult

    /**
     * 录像结果
     */
    var recordFilePath = ""

    /**
     * 设置播放显示界面
     */
    var surfaceTexture: SurfaceTexture? = null

    /**
     * 硬解码是否开启  true-硬解码  false-软解码
     */
    var hardDecode: Boolean = false

    /**
     * 智能信息是否展示
     */
    var smartDetect: Boolean = false

    /**
     * 播放url
     */
    var playUrl = ""

    private val formatCalendar = Calendar.getInstance()

    var startTime = 0L

    var endTime = 0L

    /**
     * 转码句柄
     */
    private var formatHandle = 0L


    /*播放状态，一开始默认为空闲状态*/
    private var playStatus = PlayStatus.IDLE

    /**
     * 播放器
     */
    private val hatomPlayer: HatomPlayer by lazy {
        DefaultHatomPlayer()
    }

    private var playConfig = PlayConfig()

    /**
     * 获取播放状态
     */
    fun getPlayStatus(): PlayStatus {
        return playStatus
    }

    /**
     * 抓图
     */
    fun capture(): String {
        if (playStatus != PlayStatus.PLAYING) {
            return ""
        }
        val path = MyUtils.getCaptureImagePath(Utils.getApp())
        hatomPlayer.screenshot(path, "")
        return path
    }

    /**
     * 开启录像
     */
    fun startRecord(): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        val path = MyUtils.getLocalRecordPath(Utils.getApp())
        //todo 这里我们提供带有录像转码功能的接口:startRecordAndConvert()
        //todo 同时保留了原有接口 hatomPlayer.startRecord(path)，此接口不进行转码

        // 转码后，录像文件可以正常使用播放器播放，
        // 不进行转码，只能使用视频SDK进行播放。
        val result = hatomPlayer.startRecordAndConvert(path) ?: -1 == 0
        if (result) {
            recordFilePath = path
        }
        return result
    }

    /**
     * 停止录像
     */
    fun stopRecord() {
        if (playStatus != PlayStatus.PLAYING) {
            return
        }
        hatomPlayer.stopRecord()
    }

    /**
     * 开启预览
     */
    fun startPreview(url: String) {
        playUrl = url
        playConfig.apply {
            this.secretKey = null
            this.privateData = smartDetect
            this.hardDecode = hardDecode
            this.waterConfig = null
        }
        hatomPlayer.setSurfaceTexture(surfaceTexture)
        hatomPlayer.setPlayStatusCallback(this)
        playStatus = PlayStatus.LOADING


       val task = Observable.create<Boolean> {
            hatomPlayer.setPlayConfig(playConfig)
            hatomPlayer.setDataSource(playUrl, null)
            hatomPlayer.start()
            it.onNext(true)
        }.transformMain().subscribe({
        },{
           it.printStackTrace()
           onPlayerStatus(PlayCallback.Status.FAILED, "-1")
       })
       addTask(task)


    }

    private fun addTask(task: Disposable?) {
        task?.let {
            tasks.add(task)
        }
    }

    /**
     * 切换码流类型
     */
    fun changeStream(quality: Quality, url: String) {
        playUrl = url
        playConfig.apply {
            this.secretKey = null
            this.privateData = smartDetect
            this.hardDecode = hardDecode
            this.waterConfig = null
        }
        hatomPlayer.setSurfaceTexture(surfaceTexture)
        hatomPlayer.setPlayStatusCallback(this)
        playStatus = PlayStatus.LOADING

        val task = Observable.create<Boolean>{
            hatomPlayer.setPlayConfig(playConfig)
            hatomPlayer.setDataSource(playUrl, null)
            hatomPlayer.changeStream(quality)
            it.onNext(true)
        }.transformMain().subscribe({},{
            it.printStackTrace()
            onPlayerStatus(PlayCallback.Status.FAILED, "-1")
        })
        addTask(task)
    }

    /**
     * 停止播放
     */
    fun stopPlay() {
        if (playStatus == PlayStatus.IDLE || playStatus == PlayStatus.STOP) return
        playStatus = PlayStatus.STOP

        val task = Observable.create<Boolean>{
            hatomPlayer.stop()
            it.onNext(true)
        }.transformMain().subscribe({},{
            it.printStackTrace()
        })
        addTask(task)

    }

    /**
     * 开启对讲
     */
    fun openVoiceTalk(url: String) {
        hatomPlayer.setVoiceStatusCallback(this@PlayViewModel)
        val task = Observable.create<Boolean>{
            hatomPlayer.setPlayConfig(playConfig)
            hatomPlayer.setVoiceDataSource(url, null)
            hatomPlayer.startVoiceTalk()
            it.onNext(true)
        }.transformMain().subscribe({},{
            it.printStackTrace()
        })
        addTask(task)

    }

    /**
     * 关闭对讲
     */
    fun closeVoiceTalk() {
        val task = Observable.create<Boolean>{
            hatomPlayer.stopVoiceTalk()
            it.onNext(true)
        }.transformMain().subscribe({},{
            it.printStackTrace()
        })
        addTask(task)

    }

    /**
     * 开启回放
     */
    fun startPlayback(url: String, startTime: Long, endTime: Long) {
        playUrl = url
        this.startTime = startTime
        this.endTime = endTime
        playConfig.apply {
            this.secretKey = null
            this.privateData = smartDetect
            this.hardDecode = hardDecode
            this.waterConfig = null
        }
        val header = HashMap<String, String>()
        formatCalendar.timeInMillis = startTime
        header[HeaderParams.START_TIME] =
            CalendarUtil.calendarToyyyy_MM_dd_T_HH_mm_SSSZ(formatCalendar)
        formatCalendar.timeInMillis = endTime
        header[HeaderParams.END_TIME] =
            CalendarUtil.calendarToyyyy_MM_dd_T_HH_mm_SSSZ(formatCalendar)
        hatomPlayer.setSurfaceTexture(surfaceTexture)
        hatomPlayer.setPlayStatusCallback(this)
        playStatus = PlayStatus.LOADING

        val task = Observable.create<Boolean>{
            hatomPlayer.setPlayConfig(playConfig)
            hatomPlayer.setDataSource(playUrl, header)
            hatomPlayer.start()
            it.onNext(true)
        }.transformMain().subscribe({},{cause->
            cause.printStackTrace()
            onPlayerStatus(PlayCallback.Status.FAILED, "-1")
        })
        addTask(task)



    }

    /**
     * 获取回放的osd时间
     */
    fun getOSDTime(): Long {
        if (playStatus != PlayStatus.PLAYING) {
            return -1L
        }
        return hatomPlayer.osdTime
    }

    /**
     * 获取总流量
     */
    fun getTotalTraffic(): Long {
        if (playStatus != PlayStatus.PLAYING) {
            return 0L
        }
        return hatomPlayer.totalTraffic
    }

    /**
     * 暂停
     */
    fun pause(): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        hatomPlayer.pause()
        return true
    }

    /**
     * 恢复播放
     */
    fun resume(): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        hatomPlayer.resume()
        return true
    }

    /**
     * 设置倍速
     */
    fun setPlaybackSpeed(speed: PlaybackSpeed) {
        if (playStatus != PlayStatus.PLAYING) {
            return
        }
        hatomPlayer.playbackSpeed = speed
    }

    /**
     * 获取播放速度
     */
    fun getPlaybackSpeed(): PlaybackSpeed {
        if (playStatus != PlayStatus.PLAYING) {
            return PlaybackSpeed.NORMAL
        }
        return hatomPlayer.playbackSpeed ?: PlaybackSpeed.NORMAL
    }

    /**
     * 拖动回放
     */
    fun seekPlayback(seekTime: Long) {
        if (playStatus == PlayStatus.IDLE) {
            return
        }

        val task = Observable.create<Boolean>{
            formatCalendar.timeInMillis = seekTime
            hatomPlayer.seekPlayback(
                CalendarUtil.calendarToyyyy_MM_dd_T_HH_mm_SSSZ(
                    formatCalendar
                )
            )
            it.onNext(true)
        }.transformMain().subscribe({},{cause->
            cause.printStackTrace()
        })
        addTask(task)


    }

    /**
     * 开启电子放大
     */
    fun openDigitalZoom(original: Rect, current: Rect): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return (hatomPlayer.openDigitalZoom(original, current) ?: -1) == 0
    }

    /**
     * 关闭电子放大
     */
    fun closeDigitalZoom(): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return (hatomPlayer.closeDigitalZoom() ?: -1) == 0
    }

    /**
     * 开启或关闭鱼眼
     */
    fun setFishEyeEnable(isOpen: Boolean): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return (hatomPlayer.setFishEyeEnable(isOpen) ?: -1) == 0
    }

    /**
     * 设置鱼眼矫正模式
     * 这里的安装类型影响到可以用的鱼眼模式，顶装不支持维度拉伸，只有壁装支持
     */
    fun setFishEyeMode(
        @CorrectType correctType: Int,
        @PlaceType placeType: Int
    ): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return (hatomPlayer.setFishEyeMode(correctType, placeType) ?: -1) == 0
    }

    /**
     * 设置初始鱼眼PTZ参数
     */
    fun setOriginalPTZParam(
        originalX: Float,
        originalY: Float,
        textureViewWidth: Int,
        textureViewHeight: Int
    ) {
        if (playStatus != PlayStatus.PLAYING) {
            return
        }
        hatomPlayer.setOriginalFECParam(originalX, originalY, textureViewWidth, textureViewHeight)
    }

    /**
     * 根据初始参数处理鱼眼矫正
     */
    fun handleFishEyePTZ(
        isZoom: Boolean,
        zoom: Float,
        zoom3D: Float,
        curX: Float,
        curY: Float
    ): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return hatomPlayer.handleFishEyeCorrect(isZoom, zoom, zoom3D, curX, curY) ?: -1 == 0
    }


    /**
     * 是否开启声音
     */
    fun enableAudio(isOpen: Boolean): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        return (hatomPlayer.enableAudio(isOpen) ?: -1) == 0
    }

    /**
     * 获取码流帧率，注意，这个是码流的帧率，并不是设置后的实时帧率，
     * 码流帧率播放成功后，一般不会变
     */
    fun getFrameRate(): Int {
        if (playStatus != PlayStatus.PLAYING) {
            return -1
        }
        return hatomPlayer.getFrameRate()
    }

    /**
     * 设置期待帧率
     */
    fun setExpectedFrameRate(rate: Float): Boolean {
        if (playStatus != PlayStatus.PLAYING) {
            return false
        }
        //硬解下不生效
        if (playConfig.hardDecode) {
            return false
        }
        //这里是设置解码线程数，在解码卡顿时，可以使用此方法增加解码线程，线程数支持1-8
        //仅在软解下生效
        hatomPlayer.setDecodeThreadNum(8)
        return hatomPlayer.setExpectedFrameRate(rate) == 0
    }

    /**播放回调**********************************************/
    override fun onPlayerStatus(status: PlayCallback.Status, errorCode: String) {
        when (status) {
            PlayCallback.Status.SUCCESS -> {
                if (playStatus != PlayStatus.PLAYING) {
                    //播放成功
                    playStatus = PlayStatus.PLAYING
                    _previewResult.postValue(PlayResult(status = PlayCallback.Status.SUCCESS))
                } else {
                    //此时，应该是拖动了时间条后，再次播放成功
                    _previewResult.postValue(
                        PlayResult(
                            status = PlayCallback.Status.SUCCESS,
                            extraCode = PlayResult.PLAYBACK_SEEK_SUCCESS
                        )
                    )
                }
            }
            PlayCallback.Status.FAILED -> {
                //如果当前的状态为空闲或者停止，就不回调
                if (playStatus == PlayStatus.IDLE || playStatus == PlayStatus.STOP) {
                    return
                }
                //播放失败，先关闭播放
                stopPlay()
                playStatus = PlayStatus.FAIL
                _previewResult.postValue(
                    PlayResult(
                        status = PlayCallback.Status.FAILED,
                        errorCode = errorCode
                    )
                )
            }
            PlayCallback.Status.EXCEPTION -> {
                //发生异常，先关闭播放
                stopPlay()
                playStatus = PlayStatus.FAIL
                _previewResult.postValue(
                    PlayResult(
                        status = PlayCallback.Status.FAILED,
                        errorCode = errorCode
                    )
                )
            }
            PlayCallback.Status.FINISH -> {
                stopPlay()
                playStatus = PlayStatus.STOP
                _previewResult.postValue(
                    PlayResult(
                        status = PlayCallback.Status.FINISH
                    )
                )
            }
        }
    }

    /**对讲回调**********************************************/
    override fun onTalkStatus(status: PlayCallback.Status, errorCode: String) {
        when (status) {
            PlayCallback.Status.SUCCESS -> {
                //对讲成功
                _talkResult.postValue(PlayResult(status = PlayCallback.Status.SUCCESS))
            }
            PlayCallback.Status.FAILED -> {
                //对讲失败，先关闭对讲
                closeVoiceTalk()
                _talkResult.postValue(
                    PlayResult(
                        status = PlayCallback.Status.FAILED,
                        errorCode = errorCode
                    )
                )
            }
            PlayCallback.Status.EXCEPTION -> {
                //发生异常，先关闭对讲
                closeVoiceTalk()
                _talkResult.postValue(
                    PlayResult(
                        status = PlayCallback.Status.EXCEPTION,
                        errorCode = errorCode
                    )
                )
            }
            PlayCallback.Status.FINISH -> {
                //什么也不做，因为不会回调这个
            }
        }
    }


    override fun onCleared() {
        for (task in tasks) {
            if (!task.isDisposed){
                task.dispose()
            }
        }
        super.onCleared()
    }

}