/**
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.sdk.capture.receipt.capacitor

import com.microblink.core.InitializeCallback
import kotlinx.coroutines.CompletableDeferred

/**
 * Callback class used for handling initialization events in the Receipt Capture Capacitor plugin.
 *
 * @param completable A [CompletableDeferred] instance to indicate completion of the initialization.
 * @param onError A lambda function to handle errors during initialization.
 */
class OnInitialize(
    private val completable: CompletableDeferred<Unit>,
    private val onError: (msg: String?) -> Unit,
) : InitializeCallback {

    /**
     * Called when the initialization process is successfully completed.
     */
    override fun onComplete() {
        completable.complete(Unit)
    }

    /**
     * Called when an exception occurs during initialization.
     *
     * @param throwable The exception that occurred during initialization.
     */
    override fun onException(throwable: Throwable) {
        onError(throwable.message)
        completable.complete(Unit)
    }
}
