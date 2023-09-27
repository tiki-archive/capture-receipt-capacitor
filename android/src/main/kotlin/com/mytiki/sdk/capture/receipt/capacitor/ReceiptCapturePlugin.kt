/*
 * Copyright (c) TIKI Inc.
 * MIT license.  See LICENSE file in the root directory.
 */

package com.mytiki.sdk.capture.receipt.capacitor

import android.Manifest
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.microblink.core.ScanResults
import com.mytiki.sdk.capture.receipt.capacitor.req.ReqAccount
import com.mytiki.sdk.capture.receipt.capacitor.req.ReqInitialize
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspAccount
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspReceipt
import com.mytiki.sdk.capture.receipt.capacitor.rsp.RspScan
import kotlin.properties.Delegates

/**
*
*A Capacitor plugin for receipt capture functionality.
*This plugin provides methods for initializing, logging in, logging out, fetching accounts, and capturing receipts.
*It allows you to integrate receipt capture capabilities into your Capacitor-based mobile application.
*@license MIT license. See LICENSE file in the root directory for more details.
*@author TIKI Inc.
*@version 1.0.0
 */
@CapacitorPlugin(
    name = "ReceiptCapture"
)
class ReceiptCapturePlugin : Plugin() {
    private val receiptCapture = ReceiptCapture(
        { onInitialize() },
        { onScan(it) },
        { onAccount(it) },
        { onError(it) }
    )

    companion object{
        private lateinit var instance: ReceiptCapturePlugin

        fun onInitialize() {
            instance.notifyListeners("onInitialize", JSObject())
        }
        fun onScan(scan: ScanResults? = null) {
            val data = if (scan != null) { JSObject.fromJSONObject(RspScan(scan).toJson()) } else { JSObject() }
            instance.notifyListeners("onReceipt", data)
        }

        fun onAccount(account: Account? = null) {
            val data = if (account != null) { JSObject.fromJSONObject(RspAccount(account).toJson()) } else { JSObject() }
            instance.notifyListeners("onAccount", data)
        }

        fun onError(message: String) {
            instance.notifyListeners("onError", JSObject(message))
        }
    }

    /**
     * Initializes the receipt capture functionality.
     *
     * This method initializes the receipt capture service, preparing it for use. You should call this method
     * before using any other receipt capture features.
     *
     * @param call The Capacitor plugin call instance.
     */
    @PluginMethod
    fun initialize(call: PluginCall) {
        try {
            val reqInitialize = ReqInitialize(call.data)
            receiptCapture.initialize(context, reqInitialize.licenseKey, reqInitialize.productKey)
            instance = this
        }catch (e: Exception){
            call.reject(e.message)
        }

    }

    /**
     * Logs in with the specified account.
     *
     * This method allows users to log in their email or retailer (amazon, wallmart, gmail...) account.
     * Successful login is required
     * for accessing certain features and functionalities.
     * The [JSObject] from [PluginCall.data] sent through call must have source, username and password properties
     *
     * @param call The Capacitor [PluginCall] instance.
     */
    @PluginMethod
    fun login(call: PluginCall) {
        val username = call.data.getString("username")
        val password = call.data.getString("password")
        val source = call.data.getString("source")
        if (source.isNullOrEmpty()) {
            onError("Provide source in login request")
            call.reject("Provide source in login request")
        }else if (username.isNullOrEmpty()) {
            onError("Provide username in login request")
            call.reject("Provide username in login request")
        }else if (password.isNullOrEmpty()) {
            onError("Provide password in login request")
            call.reject("Provide password in login request")
        }else {
            receiptCapture.login(
                activity,
                username,
                password,
                source
            )
            call.resolve()
        }
    }

    /**
     * Logs out of the receipt capture service.
     *
     * This method allows users to logout their email or retailer (amazon, wallmart, gmail...) account.
     * It can be used to end the session and secure user data.
     * To logout from a retailer account the [JSObject] from [PluginCall.data] sent through call must have a source propertie.
     * To logout from an email account the [JSObject] from [PluginCall.data] sent through call must have source and source, username and password properties.
     * To logout from all retailer and email accounts the [JSObject] from [PluginCall.data] sent through call must have be empty.
     *
     * @param call The Capacitor plugin call instance.
     */
    @PluginMethod
    fun logout(call: PluginCall) {
        receiptCapture.logout(activity, Account.fromReq(call.data)) {
            call.resolve()
        }
    }

    /**
     * Fetches accounts associated.
     *
     * This method retrieves a list of user email and retailer accounts associated.
     * It can be useful for user management and selection.
     *
     * @param call The Capacitor plugin call instance.
     */
    @PluginMethod
    fun accounts(call: PluginCall){
        receiptCapture.accounts(context, AccountTypeEnum.RETAILER)
        receiptCapture.accounts(context, AccountTypeEnum.EMAIL)
        call.resolve()
    }

    /**
     * Fetches all receipts on logged accounts or starts the physical receipt scan process.
     *
     * This method fetches all receipts on logged accounts or depending on the inputs starts the physical receipt scan process, launching the camera for scanning receipts.
     * It requires the camera permission to be granted. If not, it will request the permission from the user.
     * To Fetches receipts from a specific retailer or email account the [JSObject] from [PluginCall.data] sent through call must have a scanType, source, username and password properties.
     * To Fetches receipts from all email, retailer, both or to scan a physical one the [JSObject] from [PluginCall.data] sent through call must have a scanType property.
     *
     * @param call The Capacitor plugin call instance.
     */
    @PluginMethod
    fun scan(call: PluginCall){
        val dayCutOff = call.data.getInt("dayCutOff")
        receiptCapture.scan(activity, dayCutOff)
    }

}
