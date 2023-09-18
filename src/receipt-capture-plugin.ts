/*
 * Copyright (c) TIKI Inc.
 * MIT license. See LICENSE file in root directory.
 */

import type { Account } from './account';
import type { Receipt } from './receipt';

export type ScanType = 'PHYSICAL' | 'EMAIL' | 'RETAILER' | 'ONLINE';

export interface ReqAccount {
  username: string;
  password: string;
  source: string;
}

export interface ReqInitialize {
  licenseKey: string;
  productKey: string;
  googleId: string | undefined;
}

export interface ReceiptCapturePlugin {
  /**
   * Initializes the receipt capture plugin with a license key and product intelligence key.
   * @param licenseKey - The license key for your application.
   * @param productKey - The optional product intelligence key for your application.
   * @throws Error if the initialization fails.
   */
  initialize(options: ReqInitialize): Promise<void>;

  /**
   * Login into a retailer or email account to scan for receipts.
   * @param username - the username of the account.
   * @param password - the password of the account
   * @param source - the source from that account, that can be an email service or a retailer service.
   * @returns - the Account interface with the logged in information.
   */
  login(options: ReqAccount): Promise<Account>;

  /**
   * Log out from one or all {@link Account}.
   * @param username - the username of the account that will be logged out.
   * @param password - the password of the account that will be logged out
   * @param source - the source from that account, that can be an email service or a retailer service.
   * @returns - the Account that logged out
   */
  logout(options?: ReqAccount): Promise<Account>;

  /**
   * Scan for receipts. That can be a physical one, the receipts from an email/retailer account, or all receipts.
   * @param scanType - The type of the scan.
   * @param account - The account that will be scanned for receipts.
   * @returns - The scanned Receipt and a boolean indicates the execution.
   */
  scan(_option: {
    scanType: ScanType | undefined;
    account?: Account;
  }): Promise<{ receipt: Receipt; isRunning: boolean; account?: Account }>;

  /**
   * Retrieves all saved accounts.
   * @returns - an array of Accounts.
   */
  accounts(): Promise<Account[]>;
}
