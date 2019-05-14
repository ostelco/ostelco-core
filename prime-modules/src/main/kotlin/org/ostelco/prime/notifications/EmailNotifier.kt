package org.ostelco.prime.notifications

import arrow.core.Either

interface EmailNotifier {
    fun sendESimQrCodeEmail(email: String, name: String, qrCode: String) : Either<Unit, Unit>
}