package com.fencer.hatomsdk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

fun ViewModel.launchOnUI(block: suspend CoroutineScope.() -> Unit): Job {
    return viewModelScope.launch {
        block()
    }
}

suspend fun ViewModel.launchOnIO(block: suspend CoroutineScope.() -> Unit) {
    withContext(Dispatchers.IO) {
        block()
    }
}


fun ViewModel.launch(tryBlock: suspend CoroutineScope.() -> Unit) {
    launchOnUI {
        tryCatch(tryBlock, {}, {}, true)
    }
}

fun ViewModel.launchOnUITryCatch(
    tryBlock: suspend CoroutineScope.() -> Unit,
    catchBlock: suspend CoroutineScope.(Throwable) -> Unit
) {
    launchOnUI {
        tryCatch(tryBlock, catchBlock, {}, true)
    }
}

suspend fun ViewModel.launchOnIOTryCatch(
    tryBlock: suspend CoroutineScope.() -> Unit,
    catchBlock: suspend CoroutineScope.(Throwable) -> Unit
) {
    launchOnIO {
        tryCatch(tryBlock, catchBlock, {}, true)
    }
}

fun ViewModel.launchOnUITryCatch(
    tryBlock: suspend CoroutineScope.() -> Unit,
    catchBlock: suspend CoroutineScope.(Throwable) -> Unit,
    finallyBlock: suspend CoroutineScope.() -> Unit,
    handleCancellationExceptionManually: Boolean
) {
    launchOnUI {
        tryCatch(tryBlock, catchBlock, finallyBlock, handleCancellationExceptionManually)
    }
}

fun ViewModel.launchOnUITryCatch(
    tryBlock: suspend CoroutineScope.() -> Unit,
    handleCancellationExceptionManually: Boolean = false
) {
    launchOnUI {
        tryCatch(tryBlock, {}, {}, handleCancellationExceptionManually)
    }
}


private suspend fun ViewModel.tryCatch(
    tryBlock: suspend CoroutineScope.() -> Unit,
    catchBlock: suspend CoroutineScope.(Throwable) -> Unit,
    finallyBlock: suspend CoroutineScope.() -> Unit,
    handleCancellationExceptionManually: Boolean = false
) {
    coroutineScope {
        try {
            tryBlock()
        } catch (e: Throwable) {
            if (e !is CancellationException || handleCancellationExceptionManually) {
                catchBlock(e)
            } else {
                throw e
            }
        } finally {
            finallyBlock()
        }
    }
}