package com.kuralpay.app.paymentdetection.parser

/**
 * Decides whether a notification's text is money coming in, and pulls out the amount and payer.
 * Written against real UPI app alerts and Indian bank credit SMS (IOB, HDFC style).
 */
object PaymentMessageParser {

    data class Result(val amount: Double, val sender: String)

    private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)

    /** Anything after these is the payer's own remark, which can say anything ("bill", "offer"). */
    private val remarkStart = Regex("""payer\s*remark|remarks?\s*[:\-]|\bnote\s*[:\-]|\bmessage\s*[:\-]""", IGNORE_CASE)

    private val creditSignals = Regex(
        """\bcredited\b|\breceived\b|\bdeposited\b|credit\s+alert|\bpaid\s+you\b|\bsent\s+you\b|\bmoney\s+received\b|\badded\s+to\s+your\b""",
        IGNORE_CASE
    )

    private val rejectSignals = Regex(
        listOf(
            // money going out ("will be sent to your bank account" is GPay Business settling a payment in, so not that)
            """\bdebited\b""", """\bdebit\s+of\b""", """\bspent\b""", """\bwithdrawn?\b""", """\bwithdrawal\b""",
            """\bpurchase\b""", """\bpaid\s+to\b""", """(?<!\bbe\s)\bsent\s+to\b""", """\btransferred\s+to\b""", """will\s+be\s+debited""",
            // reminders and bills
            """\bdue\b""", """\boverdue\b""", """\breminder\b""", """\bpay\s+now\b""", """\bpay\s+before\b""",
            """\bbill\b""", """\bemi\b""", """\boutstanding\b""", """minimum\s+amount""", """\bstatement\b""",
            """\bautopay\b""", """\bmandate\b""",
            // requests
            """\brequest(ed|s)?\b""", """\bcollect\b""",
            // offers and promotions
            """\boffers?\b""", """\bcashback\b""", """\bdiscount\b""", """%\s*off\b""", """\bflat\b""", """\bwin\b""", """\bwon\b""",
            """\brewards?\b""", """\bcoupon\b""", """\bvoucher\b""", """\bsale\b""", """\bup\s?to\b""", """\bloan\b""",
            """pre-?\s?approved""", """\beligible\b""", """\binsurance\b""", """\brecharge\b""",
            // security codes
            """\botp\b""", """one\s+time\s+password""", """do\s+not\s+share""", """verification\s+code""",
            // not completed
            """\bfailed\b""", """\bdeclined\b""", """\bunsuccessful\b""", """\breversed\b""", """\bpending\b"""
        ).joinToString("|"),
        IGNORE_CASE
    )

    private val amountPattern = Regex("""(?:₹|\brs\.?|\binr)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", IGNORE_CASE)
    private val balanceBefore = Regex("""(bal|balance|avl|available|limit)\W{0,6}$""", IGNORE_CASE)

    private val referencePattern = Regex(
        """\b(?:upi\s*ref(?:erence)?|upi|ref(?:erence)?|utr|rrn|txn\s*id|transaction\s*id)\s*(?:no\.?|number|id)?\s*[:.\-]?\s*(\d{9,18})\b""",
        IGNORE_CASE
    )

    /**
     * The UPI / bank reference number, if the message has one. Banks sometimes send the same credit
     * twice in different formats (IOB: "AX-IOBCHN" then "BV-IOBCHN"); the reference is the same.
     */
    fun reference(text: String): String? = referencePattern.find(text)?.groupValues?.get(1)

    fun parse(text: String): Result? {
        val clean = text.replace(Regex("""[⁦-⁩‎‏]"""), "").replace(Regex("""\s+"""), " ").trim()
        if (clean.isEmpty()) return null
        val core = remarkStart.find(clean)?.let { clean.substring(0, it.range.first) } ?: clean

        if (!creditSignals.containsMatchIn(core)) return null
        if (rejectSignals.containsMatchIn(core)) return null

        val amount = amountPattern.findAll(core)
            .filterNot { m -> balanceBefore.containsMatchIn(core.substring(maxOf(0, m.range.first - 14), m.range.first)) }
            .mapNotNull { it.groupValues[1].replace(",", "").trimEnd('.').toDoubleOrNull() }
            .firstOrNull { it > 0 }
            ?: return null

        return Result(amount, extractSender(core))
    }

    private val senderPatterns = listOf(
        // IOB: "from POORANI  B-poorani.b2109@oksbi(UPI ..." (banks sometimes mask '@' as '!')
        Regex("""\bfrom\s+([A-Za-z][A-Za-z0-9 .&]{1,40}?)\s*-\s*[\w.*-]+[@!][\w.]+""", IGNORE_CASE),
        // GPay style: "Arun Kumar paid you ₹500"
        Regex("""([A-Za-z][A-Za-z ]{1,40}?)\s+(?:has\s+)?(?:paid|sent)\s+you\b""", IGNORE_CASE),
        // HDFC: "from VPA *9795@ptyes"
        Regex("""\bfrom\s+vpa\s+([\w.*!-]+@[\w.]+)""", IGNORE_CASE),
        // "received ₹500 from Meena R", "from ARUN KUMAR on ..."
        Regex("""\bfrom\s+([A-Za-z][A-Za-z0-9 .]{1,40}?)(?=\s*(?:\bon\b|\bat\b|\bin\b|\bto\b|\bfor\b|\bvia\b|\bthrough\b|\bref\b|\bupi\b|\(|\.|,|-|$))""", IGNORE_CASE),
        Regex("""\bby\s+(?!rs\b|inr\b|upi\b|imps\b|neft\b)([A-Za-z][A-Za-z .]{1,40}?)(?=\s*(?:\bon\b|\bat\b|\bin\b|\bto\b|\bfor\b|\bvia\b|\bref\b|\(|\.|,|-|$))""", IGNORE_CASE)
    )

    private val notNames = setOf("your", "a/c", "ac", "account", "bank", "upi", "vpa", "you")

    private fun extractSender(text: String): String {
        for (pattern in senderPatterns) {
            val raw = pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: continue
            if (raw.lowercase() in notNames || raw.length < 2) continue
            if ('@' in raw) return raw
            return tidyName(raw)
        }
        return ""
    }

    /** "POORANI  B" -> "Poorani B"; mixed-case names are left as written. */
    private fun tidyName(raw: String): String {
        val collapsed = raw.replace(Regex("""\s+"""), " ").trim().trimEnd('.').trim()
        if (collapsed != collapsed.uppercase()) return collapsed
        return collapsed.split(" ").joinToString(" ") { w ->
            if (w.length <= 1) w else w.first() + w.drop(1).lowercase()
        }
    }
}
