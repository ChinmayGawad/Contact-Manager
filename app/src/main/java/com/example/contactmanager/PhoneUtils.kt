package com.example.contactmanager

object PhoneUtils {

    fun cleanPhoneNumber(number: String): String {
        val digitsOnly = number.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.startsWith("91") && digitsOnly.length == 12) {
            digitsOnly.substring(2)
        } else {
            digitsOnly
        }
    }

    fun isValidPhone(number: String): Boolean {
        val cleaned = cleanPhoneNumber(number)
        return cleaned.length in 7..15
    }

    fun formatPhoneForDisplay(number: String): String {
        val cleaned = cleanPhoneNumber(number)
        return when {
            cleaned.length == 10 -> "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            cleaned.length == 7 -> "${cleaned.substring(0, 3)}-${cleaned.substring(3)}"
            else -> cleaned
        }
    }
}
