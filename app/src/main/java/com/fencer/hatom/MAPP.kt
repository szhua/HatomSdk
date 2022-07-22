package com.fencer.hatom

import android.app.Application
import com.fencer.hatomsdk.HatomSdk


/**

@author  SZhua
Create at  2022/7/21
Description:

 */


class MAPP : Application() {

    override fun onCreate() {
        super.onCreate()
        HatomSdk.init(this)
    }
}