package com.fencer.hatomsdk

import android.app.Application
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.hikvision.hatomplayer.HatomPlayerSDK


/**

@author  SZhua
Create at  2022/7/21
Description:

 */


object HatomSdk {


    fun init(context: Application){

        ToastUtils.getDefaultMaker()
            .setGravity(Gravity.CENTER, 0, 0)
            .setBgResource(R.color.black_70)
            .setTextColor(ContextCompat.getColor(context, R.color.white))
        Utils.init(context)
        HatomPlayerSDK.init(context, "", AppUtils.isAppDebug())

    }


}