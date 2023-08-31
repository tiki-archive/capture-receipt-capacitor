import Foundation
import Capacitor
import BlinkReceipt

public class ReceiptCapture: NSObject {
    
    var email: Email? = nil
    var physical: Physical? = nil
    var retailer: Retailer? = nil
    
    public static var pendingScanCall: CAPPluginCall?
    
    public func initialize(_ call: CAPPluginCall) {
        let reqInit = ReqInitialize(call)
        let licenseKey = reqInit.licenseKey
        let productKey = reqInit.productKey
        let scanManager = BRScanManager.shared()
        scanManager.licenseKey = licenseKey
        scanManager.prodIntelKey = productKey
        physical = Physical()
        email = Email()
        retailer = Retailer(licenseKey, productKey)
    }
    
    public func login(_ call: CAPPluginCall) {
        let reqLogin = ReqLogin(data: call)
        guard let accountType = AccountCommon.defaults[reqLogin.source] else {
            call.reject("Invalid source: \(reqLogin.source)")
            return
        }
        let account = Account.init(accountType: accountType, user: reqLogin.username, password: reqLogin.password, isVerified: false)
        switch account.accountType.type {
        case .email :
            guard let email = email else {
                call.reject("Call the initialize method.")
                return
            }
            email.login(account, call)
            break
        case .retailer :
            guard let retailer = retailer else {
                call.reject("Call the initialize method.")
                return
            }
            retailer.login(account, call)
            break
        }
    }
    
    public func logout(_ call: CAPPluginCall) {
        let reqLogout = ReqLogin(data: call)
        guard let accountType = AccountCommon.defaults[reqLogout.source] else {
            call.reject("Invalid source: \(reqLogout.source)")
            return
        }
        
        let account = Account.init(accountType: accountType, user: reqLogout.username, password: reqLogout.password, isVerified: false)
        switch account.accountType.type {
        case .email :
            guard let retailer = email else {
                call.reject("Call plugin initialize method.")
                return
            }
            email.logout(account, call)
            break
        case .retailer :
            guard let retailer = retailer else {
                call.reject("Call plugin initialize method.")
                return
            }
            retailer.logout(account, call)
            break
        }
    }
    
    public func accounts(_ call: CAPPluginCall) {
        guard let retailer = retailer else {
            call.reject("Call plugin initialize method.")
            return
        }
        
        let data = retailer.accounts(call)
    }
    
    public func scan(_ call: CAPPluginCall) {
        guard let physical = physical else {
            call.reject("Call plugin initialize method.")
            return
        }
        ReceiptCapture.pendingScanCall = call
        physical.scan()
    }
    
}
