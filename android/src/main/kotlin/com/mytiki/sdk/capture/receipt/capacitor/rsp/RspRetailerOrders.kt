/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

package com.mytiki.sdk.capture.receipt.capacitor.rsp

import com.microblink.core.ScanResults
import org.json.JSONObject

class RspRetailerOrders(
    private val provider: String,
    private val username: String,
    private val scan: ScanResults
): Rsp {
    override fun toJson(): JSONObject =
        JSONObject()
            .put("username", username)
            .put("provider", provider)
            .put("scan",  RspScan(scan).toJson())
}
