package com.fencer.hatomsdk

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*




/**

@author  SZhua
Create at  2022/7/22
Description:

 */

fun <T> Observable<T>.transformMain() :Observable<T>{
   return   this.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}