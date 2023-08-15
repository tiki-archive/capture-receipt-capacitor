/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.sdk.capture.receipt.capacitor

import android.content.Context
import android.webkit.WebView
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.microblink.core.ScanResults
import com.microblink.linking.*
import com.mytiki.sdk.capture.receipt.capacitor.req.ReqInitialize
import com.mytiki.sdk.capture.receipt.capacitor.req.ReqRetailerAccount
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspRetailerAccount
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspRetailerOrders
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

class Retailer {
    private lateinit var client: AccountLinkingClient

    @OptIn(ExperimentalCoroutinesApi::class)
    fun initialize(
        req: ReqInitialize,
        context: Context,
        onError: (msg: String?, data: JSObject) -> Unit,
    ): CompletableDeferred<Unit> {
        val isLinkInitialized = CompletableDeferred<Unit>()
        BlinkReceiptLinkingSdk.licenseKey = req.licenseKey!!
        BlinkReceiptLinkingSdk.productIntelligenceKey = req.productKey!!
        BlinkReceiptLinkingSdk.initialize(context, OnInitialize(isLinkInitialized, onError))
        client = client(context)
        return isLinkInitialized
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun client(
        context: Context,
        dayCutoff: Int = 15,
        latestOrdersOnly: Boolean = false,
        countryCode: String = "US",
    ): AccountLinkingClient{
        val client = AccountLinkingClient(context)
        client.dayCutoff = dayCutoff
        client.latestOrdersOnly = latestOrdersOnly
        client.countryCode = countryCode

        return client
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun account(call: PluginCall){
        val req = ReqRetailerAccount(call.data)
        val account = Account(
            req.retailerId.value,
            PasswordCredentials(req.username!!, req.password!!)
        )
        client.link(account)
            .addOnSuccessListener {
                clientVerification(
                    req.retailerId,
                    {
                        val rsp = RspRetailerAccount(req.username, req.retailerId)
                        call.resolve(JSObject.fromJSONObject(rsp.toJson()))
                    },{
                        call.reject("Verification Failed")
                    }
                )
            }
            .addOnFailureListener {
                call.reject(it.message)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clientVerification(
        retailerId: RetailerEnum,
        onSuccess: () -> Unit,
        onError: (exception: AccountLinkingException) -> Unit
    ) {
        client.verify(
            retailerId.value,
            { _: Boolean, _: String ->
                onSuccess()
            },{ exception ->
                onError(exception)
            }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun orders(
        context: Context,
        call: PluginCall
    ): CompletableDeferred<Unit> {
        val req = ReqRetailerAccount(call.data)
        val account = Account(
            req.retailerId.value,
            PasswordCredentials(req.username!!, req.password!!)
        )
        val orders = CompletableDeferred<Unit>()
        clientVerification(
            req.retailerId,
            {
                client.orders(
                    req.retailerId.value,
                    { _: Int, results: ScanResults?, _: Int, _: String ->
                        if (results != null) {
                            val rsp = RspRetailerOrders(account, results)
                            call.resolve(JSObject.fromJSONObject(rsp.toJson()))
                            orders.complete(Unit)
                        }

                    },
                    { _: Int, exception: AccountLinkingException ->

                        errorHandler(context, exception, call::reject)
                    },
                )
            },{ exception: AccountLinkingException ->
                errorHandler(context, exception, call::reject)
                call.reject("Verification Failed")
                orders.completeExceptionally(exception)
            }
        )
        return orders
    }
    private fun errorHandler(
        context: Context,
        exception: AccountLinkingException,
        reject: (msg: String) -> Unit,
    ){

        if (exception.code == VERIFICATION_NEEDED) {
            //in this case, the exception.view will be != null, so you can show it in your app
            //and the user can resolve the needed verification, i.e.:
            if (exception.view != null) {
                val webView = WebView(context)
                exception.view!!.url?.let { webView.loadUrl(it) }
            } else {
                reject("Verification Needed")
            }
        }

        when (exception.code){
            INTERNAL_ERROR -> reject("Internal Error")
            INVALID_CREDENTIALS -> reject("Invalid Credentials")
            PARSING_FAILURE -> reject("Parsing Failure")
            USER_INPUT_COMPLETED -> reject("User Input Completed")
            JS_CORE_LOAD_FAILURE -> reject("JS Core Load Failure")
            JS_INVALID_DATA -> reject("JS Invalid Data")
            MISSING_CREDENTIALS -> reject("Missing Credentials")
            else -> reject("Unknown Error")
        }
    }

}
