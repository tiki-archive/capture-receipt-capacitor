/*
 * RspShipment Struct
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

import Foundation
import BlinkReceipt
import BlinkEReceipt
import Capacitor

/**
 Represents a response containing shipment information.

 This struct is used to convey details about a shipment, including its status and the list of products within the shipment.
 */
struct RspShipment: Rsp {
    /// The status of the shipment.
    private let status: String
    /// The list of products within the shipment.
    private let products: [RspProduct]

    /**
     Initializes an `RspShipment` struct.

     - Parameters:
        - shipment: The shipment information.
     */
    init(shipment: BRShipment) {
        status = shipment.status
        products = shipment.products.map { product in RspProduct(product: product) }
    }

    /**
     Converts the `RspShipment` struct into a dictionary suitable for use in plugin response data.

     - Returns: A dictionary containing shipment information in a format suitable for a Capacitor plugin call result.
     */
    func toPluginCallResultData() -> Capacitor.PluginCallResultData {
        var ret = JSObject()
        ret["status"] = status
        ret["products"] = JSArray(arrayLiteral: products.map { prd in prd.toPluginCallResultData() })
        return ret
    }
}

