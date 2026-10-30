package com.kuralpay.app.paymentdetection.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentMessageParserTest {

    private fun accepts(text: String, amount: Double, sender: String) {
        val r = PaymentMessageParser.parse(text)
        assertNotNull("should be a payment: $text", r)
        assertEquals("amount in: $text", amount, r!!.amount, 0.001)
        assertEquals("sender in: $text", sender, r.sender)
    }

    private fun rejects(text: String) = assertNull("should be ignored: $text", PaymentMessageParser.parse(text))

    // Real credit SMS received on the owner's phone.

    @Test fun iobUpiCredit() = accepts(
        "VM-IOBCHN-S. Your a/c no. XXXXX53 is credited by Rs.500.00 on 2026-10-29 13:40:07.245, from POORANI  B-poorani.b2109@oksbi(UPI Ref no 630228390304).Payer Remark - UPI -IOB",
        500.0, "Poorani B"
    )

    @Test fun iobCreditFromCompanyWithMaskedVpa() = accepts(
        "Your a/c no. XXXXX53 is credited by Rs.3.00 on 2026-10-26 03:04:42.975, from ONE97 COMMUNICATIONS LIMITED-poweraccess.paytm3!axisbank(UPI Ref no 571632152996).Payer Remark - Pay using UPI on Oct 25 2026 -IOB",
        3.0, "One97 Communications Limited"
    )

    @Test fun hdfcCreditAlertWithVpaOnly() = accepts(
        "Credit Alert!\nRs.200.00 credited to HDFC Bank A/c XX1220 on 25-10-26 from VPA *9795@ptyes (UPI 218915787062)",
        200.0, "*9795@ptyes"
    )

    @Test fun hdfcDepositWithBalanceFooter() = accepts(
        "⁨AD-HDFCBK-S⁩ Update! INR 40,544.00 deposited in HDFC Bank A/c XX1220 on 30-OCT-26 for 012056891310.Avl bal INR 40,544.00. Cheque deposits in A/C are subject to clearing",
        40544.0, ""
    )

    // Real false alarms the old reader announced.
    @Test fun airtelRechargeReminder() = rejects(
        "Recharge Reminder Your Airtel recharge pack of Rs.349 for mobile number 9843559795 is due for recharge. Ignore if already paid. Win offers from top brands"
    )
    @Test fun referralAd() = rejects(
        "₹500 for you, up to ₹500 for them! Refer your friends to apply for superCard. You get ₹500 & they get up to ₹500, once they get the card!"
    )

    @Test fun payerRemarkSayingBillStillCounts() = accepts(
        "Your a/c no. XXXXX53 is credited by Rs.1,250.00 on 2026-10-30, from MEENA R-meena@okaxis(UPI Ref no 1).Payer Remark - electricity bill -IOB",
        1250.0, "Meena R"
    )

    @Test fun balanceIsNotTakenAsAmount() = accepts(
        "Avl Bal Rs 12,345.00. Rs 300 credited to your A/c XX12 from Priya on 30-10-26",
        300.0, "Priya"
    )

    // Payment app alerts.

    @Test fun gpayPaidYou() = accepts("₹500 received. Arun Kumar paid you ₹500", 500.0, "Arun Kumar")
    @Test fun phonepeReceivedFrom() = accepts("Received ₹1,250 from Meena R", 1250.0, "Meena R")
    @Test fun paytmReceivedFrom() = accepts("Rs.240 received from Karthik S in your Paytm Wallet", 240.0, "Karthik S")
    @Test fun noSenderGiven() = accepts("₹75 received in your account", 75.0, "")

    // GPay Business notifications (title. text), as posted on a shop's phone.

    @Test fun gpayBusinessReceived() = accepts(
        "₹20.00 received from Gunalan S at 8:17 PM. The amount will be sent to your bank account.",
        20.0, "Gunalan S"
    )

    @Test fun gpayBusinessReceivedInitials() = accepts(
        "₹39.00 received from K S at 6:14 PM. The amount will be sent to your bank account.",
        39.0, "K S"
    )

    @Test fun moneySentOut() = rejects("₹500 sent to Arun Kumar from your account")

    @Test fun gpayBusinessLoanAd() = rejects("Mr S Babu, repay just ₹2999 monthly 🤏. Loans with low EMIs now available on Google Pay for Business. 100% digital. Apply now!")
    @Test fun gpayBusinessCardOffer() = rejects("Add your Mastercard to earn ₹50 on your first 4 payments")

    // Must be ignored.

    @Test fun loginAlert() = rejects("Dear Customer, Thanks for logging in to IOB Internet Banking on 30-Oct-26 09:15:43.In case you have not logged in, pls contact 18004254445.IOB")
    @Test fun debitAlert() = rejects("Rs.500.00 debited from A/c XX1220 on 30-10-26 to VPA shop@okhdfc")
    @Test fun billReminder() = rejects("Reminder: Your electricity bill of Rs 1,250 is due on 05-Nov. Pay now to avoid late fee")
    @Test fun emiDue() = rejects("Your EMI of Rs.3,000 is due tomorrow. Ensure sufficient balance")
    @Test fun moneyRequest() = rejects("Arun Kumar has requested ₹500 from you on Google Pay")
    @Test fun flatOffer() = rejects("Flat Rs 200 off on orders above Rs 999. Shop now!")
    @Test fun cashbackOffer() = rejects("Cashback of Rs 25 credited to your wallet")
    @Test fun otp() = rejects("OTP for transaction of Rs 500 is 123456. Do not share it with anyone")
    @Test fun loanOffer() = rejects("Congratulations! You have received a pre-approved loan offer of Rs 5,00,000")
    @Test fun failedPayment() = rejects("Payment of ₹500 from Arun failed. Money will be received in 3 days if debited")
    @Test fun noAmount() = rejects("You have received a new message")
}
