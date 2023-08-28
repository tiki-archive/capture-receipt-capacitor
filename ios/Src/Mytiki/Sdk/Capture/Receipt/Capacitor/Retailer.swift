/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

import Foundation
import BlinkReceipt
import BlinkEReceipt

public class Retailer {
    
    public init(req: ReqInitialize) {
        BRScanManager.shared().licenseKey = req.licenseKey
        BRScanManager.shared().prodIntelKey = req.productKey
        BRAccountLinkingManager.shared()
    }
    public func account (retailer: RetailerCommon, username: String, password: String, dayCutoff: Int) -> BRAccountLinkingError {
        let connection = BRAccountLinkingConnection(retailer: retailer.toBRAccountLinkingRetailer()!, username: username, password: password)
        connection.configuration.dayCutoff = dayCutoff
        connection.configuration.returnLatestOrdersOnly = false
        connection.configuration.countryCode = "US"
        
        print("############Account done")
        return BRAccountLinkingManager.shared().linkRetailer(with: connection)
    }
    
    public func verifyConnection(retailer: RetailerCommon, username: String, password: String, completion: @escaping (_ success: Bool, _ erro: Error?, _ sessionId: String) -> Void) {
        
        let connection = BRAccountLinkingConnection(retailer: retailer.toBRAccountLinkingRetailer()!, username: username, password: password)
        connection.configuration.dayCutoff = 500
        let taskId = BRAccountLinkingManager.shared().verifyRetailer(with: connection, withCompletion: { error, viewController, sessionId in
            if (error == .verificationNeeded) {
              // Display UIViewController for 2FA verification, CAPTCHA or other user input
                self.VerificationNeededPresent(viewController: viewController!)

            } else if (error == .verificationCompleted) {
              // Dismiss the previously presented UIViewController
                self.VerificationNeededDismiss()
            } else {

                if (error == .noCredentials) {
                    completion(false, error as! Error, sessionId)
                  // Error: Credentials have not been provided
                } else if (error == .internal) {
                    completion(false, error as! Error, sessionId)
                  // Error: Unexpected error
                } else if (error == .parsingFail) {
                    completion(false, error as! Error, sessionId)
                  // Error: General Error
                } else if (error == .invalidCredentials) {
                    completion(false, error as! Error, sessionId)
                  // Error: Probable cause of the failure is invalid credentials
                } else if (error == .cancelled) {
                    completion(false, error as! Error, sessionId)
                  // Operation has been cancelled.
                  // Dismiss any previously presented user input UIViewController
                }
                // Verification Completed Successfully
                // Account can be linked
                print("######VerificationCompleted")
                let linkRetailer = BRAccountLinkingManager.shared().linkRetailer(with: connection)
                completion(true, nil, sessionId)
              }
            })
        }
    
    
    public func orders(retailer :BRAccountLinkingRetailer, completion: @escaping (_ success: Bool, _ retailer: BRAccountLinkingRetailer, _ remaining: Int, _ sessionId: String, _ erroCode: Error?) -> Void){

        let taskId = BRAccountLinkingManager.shared().grabNewOrders(for: retailer) { retailer, order, remaining, viewController, errorCode, sessionId in
          if (errorCode == .verificationNeeded) {
            // Display UIViewController for 2FA verification, CAPTCHA or other user input
              print("###GrabOrders verification needed")
              self.VerificationNeededPresent(viewController: viewController!)
          } else if errorCode == .invalidKey {
              print("####InvalidKey")
          }else if errorCode == .verificationCompleted {
            // Dismiss the previously presented UIViewController
              self.VerificationNeededDismiss()
          } else if errorCode == .none {
              print("###Everiting Works Fine")
            // Order may be returned here
              print(order?.description)
            if (remaining <= 0) {
              // Grab Orders Completed Successfully
              // No more orders to fetch
            }
          } else {
            // Grab Orders Failed. Check error for more info
          }
            
        }
    }
    public func VerificationNeededPresent(viewController: UIViewController){
        print("###verification needed")
        let rootVc = UIApplication.shared.windows.first?.rootViewController
        rootVc!.present(viewController, animated: true, completion: nil)
    }
    public func VerificationNeededDismiss(){
        print("###verification needed Dismiss")
        let rootVc = UIApplication.shared.windows.first?.rootViewController
        rootVc!.dismiss(animated: true)
    }

}

