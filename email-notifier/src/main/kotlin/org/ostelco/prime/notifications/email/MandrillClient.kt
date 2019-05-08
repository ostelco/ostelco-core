package org.ostelco.prime.notifications.email

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.EmailNotifier
import org.ostelco.prime.notifications.email.ConfigRegistry.config
import org.ostelco.prime.notifications.email.Registry.httpClient
import java.io.ByteArrayOutputStream
import java.util.*

class MandrillClient : EmailNotifier by MandrillClientSingleton

object MandrillClientSingleton : EmailNotifier {

    private val logger by getLogger()

    private const val API_URI = "/messages/send-template.json"

    private val qrCodeWriter = QRCodeWriter()
    private val base64Encoder = Base64.getEncoder()
    private val messageTemplate = this::class.java.getResource(API_URI).readText()

    override fun sendESimQrCodeEmail(email: String, name: String, qrCode: String): Either<Unit, Unit> {

        val outputStream = ByteArrayOutputStream()
        val bitMatrix = qrCodeWriter.encode(qrCode, BarcodeFormat.QR_CODE, 600, 600)
        MatrixToImageWriter.writeToStream(bitMatrix, "png", outputStream)
        val base64EncodedQRCode = base64Encoder.encodeToString(outputStream.toByteArray())

        val reqBody = messageTemplate.setVariableData(mapOf(
                "API_KEY" to config.mandrillApiKey,
                "RECEIVER_EMAIL" to email,
                "RECEIVER_NAME" to name,
                "QR_CODE" to base64EncodedQRCode
        ))

        val httpPost = HttpPost("https://mandrillapp.com/api/1.0$API_URI")

        httpPost.entity = EntityBuilder.create()
                .setText(reqBody)
                .setContentType(ContentType.APPLICATION_JSON)
                .build()

        val response = httpClient.execute(httpPost)

        return if (response.statusLine.statusCode != 200) {
            logger.error("Failed to send email")
            logger.error(EntityUtils.toString(response.entity))
            Unit.left()
        } else {
            Unit.right()
        }
    }

    private fun String.setVariableData(variableDataMap: Map<String, String>): String {
        var result = this
        variableDataMap.forEach { (variable, value) ->
            result = result.replace(oldValue = "$$variable$", newValue = value, ignoreCase = false)
        }
        return result
    }
}