/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.sdk.capture.receipt.capacitor

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.microblink.core.ScanResults
import com.microblink.linking.*
import com.mytiki.sdk.capture.receipt.capacitor.req.ReqInitialize
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspAccountList
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspRetailerOrders
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async


class Retailer {
    @OptIn(ExperimentalCoroutinesApi::class)
    private lateinit var client: AccountLinkingClient

    @OptIn(ExperimentalCoroutinesApi::class)
    fun initialize(
        req: ReqInitialize,
        context: Context,
        onError: (msg: String?, data: JSObject) -> Unit,
    ): CompletableDeferred<Unit> {
        val isLinkInitialized = CompletableDeferred<Unit>()
        BlinkReceiptLinkingSdk.licenseKey = req.licenseKey
        BlinkReceiptLinkingSdk.productIntelligenceKey = req.productKey
        BlinkReceiptLinkingSdk.initialize(context, OnInitialize(isLinkInitialized, onError))
        client = client(context)
        return isLinkInitialized
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun login( call: PluginCall, account: Account, context: Context ) {
        val mbAccount = com.microblink.linking.Account(
            RetailerEnum.fromString(account.accountType.source).toInt(),
            PasswordCredentials(account.username, account.password!!)
        )
        client.link(mbAccount)
            .addOnSuccessListener {
                if (it) {
                    verify(mbAccount, true, context, call)
                } else {
                    call.reject("login failed")
                }
            }
            .addOnFailureListener {
                call.reject(it.message)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun remove(call: PluginCall, account: Account){
        client.accounts().addOnSuccessListener { accounts ->
            val mbAccount = accounts?.firstOrNull {
                it.retailerId == RetailerEnum.fromString(account.accountType.source).toInt()
            }
            if (mbAccount != null) {
                client.unlink(mbAccount).addOnSuccessListener {
                    call.resolve()
                }.addOnFailureListener {
                    call.reject(it.message)
                }
            } else {
                call.reject("Account not found")
            }
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun flush(call: PluginCall){
        client.resetHistory().addOnSuccessListener {
            call.resolve()
        }.addOnFailureListener {
            call.reject(it.message)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun orders( call: PluginCall ) {
        MainScope().async {
            val accounts = accounts().await()
            accounts.forEach {
                if(it.isVerified!!) {
                    val retailer = RetailerEnum.fromString(it.accountType.source).toInt()
                    val username = it.username
                    val ordersSuccessCallback =
                        { a: Int, results: ScanResults?, b: Int, c: String ->
                            if (results != null) {
                                val rsp = RspRetailerOrders(
                                    RetailerEnum.fromInt(retailer).toString(),
                                    username, results
                                )
                                Log.e("TIKI", rsp.toJson().toString())
                                call.resolve(JSObject(rsp.toJson().toString()))
                            } else {
                                Log.e("TIKI", "NO RESULT")
                                call.reject("no result")
                            }
                        }
                    val ordersFailureCallback = { _: Int, exception: AccountLinkingException ->
                        Log.e("TIKI", exception.message ?: "exception wihtout message")
                        call.reject(exception.message)
                    }
                    client.orders(
                        retailer,
                        ordersSuccessCallback,
                        ordersFailureCallback,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun accounts(): CompletableDeferred<List<Account>> {
        val accounts = CompletableDeferred<List<Account>>()
        client.accounts()
            .addOnSuccessListener { mbAccounts ->
                MainScope().async {
                    if (mbAccounts != null) {
                        val accountList = mbAccounts.map{ mbAccount ->
                            val account = Account.fromMbLinking(mbAccount)
                            account.isVerified = verify(mbAccount).await()
                            account
                        }
                        accounts.complete(accountList)
                    } else {
                        accounts.complete(mutableListOf())
                    }
                }
            }
            .addOnFailureListener {
                accounts.completeExceptionally(it)
            }
        return accounts
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun verify( mbAccount: com.microblink.linking.Account, showDialog: Boolean = false, context: Context? = null, call: PluginCall? = null ): CompletableDeferred<Boolean>{
        val verifyCompletable = CompletableDeferred<Boolean>()
        val account = Account.fromMbLinking(mbAccount)
        client.verify(
            RetailerEnum.fromString(account.accountType.source).value,
            success = { isVerified: Boolean, _: String ->
                if(isVerified){
                    account.isVerified = true
                    call?.resolve(account.toRsp())
                    verifyCompletable.complete(true)
                }else {
                    client.unlink(mbAccount)
                    call?.reject("login failed")
                    verifyCompletable.complete(false)
                }
            },
            failure = { exception ->
                if(call == null){
                    verifyCompletable.complete(false)
                }else if (exception.code == VERIFICATION_NEEDED && exception.view != null && context != null) {
                    exception.view!!.isFocusableInTouchMode = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        exception.view!!.focusable = View.FOCUSABLE
                    }
                    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                    builder.setTitle("Verify your account")
                    builder.setView(exception.view)
                    val dialog: AlertDialog = builder.create()
                    dialog.show()

                }else{
                    when (exception.code){
                        INTERNAL_ERROR -> call.reject("Login failed: Internal Error")
                        INVALID_CREDENTIALS -> call.reject("Login failed: Invalid Credentials")
                        PARSING_FAILURE -> call.reject("Login failed: Parsing Failure")
                        USER_INPUT_COMPLETED -> call.reject("Login failed: User Input Completed")
                        JS_CORE_LOAD_FAILURE -> call.reject("Login failed: JS Core Load Failure")
                        JS_INVALID_DATA -> call.reject("Login failed: JS Invalid Data")
                        MISSING_CREDENTIALS -> call.reject("Login failed: Missing Credentials")
                        else -> call.reject("Login failed: Unknown Error")
                    }
                    client.unlink(mbAccount)
                }
            }
        )
        return verifyCompletable
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun client(
        context: Context,
        dayCutoff: Int = 500,
        latestOrdersOnly: Boolean = false,
        countryCode: String = "US",
    ): AccountLinkingClient{
        val client = AccountLinkingClient(context)
        client.dayCutoff = dayCutoff
        client.latestOrdersOnly = latestOrdersOnly
        client.countryCode = countryCode

        return client
    }
}
