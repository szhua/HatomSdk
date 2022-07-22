package com.fencer.hatomsdk

import androidx.annotation.Keep
import com.hikvision.hatomplayer.PlayCallback

/**
 * 播放结果，回调成功、失败和播放结束
 */
@Keep
data class PlayResult(
    /*状态枚举*/
    var status: PlayCallback.Status = PlayCallback.Status.SUCCESS,
    /*转换后的错误码*/
    var errorCode: String = "0",
    /*额外的code，可以用来辅助判断*/
    var extraCode: String = "0"
) {
    companion object {
        const val PLAYBACK_SEEK_SUCCESS = "1"
    }
}

/**
 * 播放状态
 */
@Keep
enum class PlayStatus {
    /*闲置状态,彻底释放*/
    IDLE,

    /*正在加载中状态*/
    LOADING,

    /*播放状态*/
    PLAYING,

    /*失败状态*/
    FAIL,

    /*临时停止，有可能再次播放*/
    STOP
}
