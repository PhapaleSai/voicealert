package com.phapalesai.voicealert.privacy

object MultilingualBankParser {

    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val creditKeywords = listOf("credited", "received", "deposited", "जमा", "प्राप्त", "खात्यात")
    private val debitKeywords = listOf("debited", "spent", "withdrawn", "paid", "नावे पडले", "निकालने")

    fun isBankNotification(packageName: String, text: String): Boolean {
        val lower = text.lowercase()
        val bankTerms = listOf("bank", "a/c", "acct", "upi", "hdfc", "sbi", "icici", "axis", "kotak", "paytm", "gpay", "phonepe", "बँक", "खाते")
        return bankTerms.any { term -> lower.contains(term) || packageName.lowercase().contains(term) }
    }

    fun parseBankMessage(appName: String, text: String, language: String): String {
        val amountMatch = amountRegex.find(text)
        val amount = amountMatch?.groupValues?.get(1) ?: ""

        val isCredit = creditKeywords.any { text.contains(it, ignoreCase = true) }
        val isDebit = debitKeywords.any { text.contains(it, ignoreCase = true) }

        val typeText = when {
            isCredit -> when (language) {
                "hi" -> "जमा हुए"
                "mr" -> "जमा झाले"
                else -> "credited"
            }
            isDebit -> when (language) {
                "hi" -> "डेबिट हुए"
                "mr" -> "नावे पडले"
                else -> "debited"
            }
            else -> when (language) {
                "hi" -> "लेन-देन"
                "mr" -> "व्यवहार"
                else -> "alert"
            }
        }

        return if (amount.isNotEmpty()) {
            when (language) {
                "hi" -> "बैंक अलर्ट: ₹$amount $typeText।"
                "mr" -> "बँक अलर्ट: ₹$amount $typeText."
                else -> "Bank Alert: $amount rupees $typeText."
            }
        } else {
            when (language) {
                "hi" -> "बैंक अलर्ट प्राप्त हुआ।"
                "mr" -> "बँक अलर्ट प्राप्त झाला."
                else -> "Bank transaction alert received."
            }
        }
    }
}
