package org.ostelco.simcards.es2plus

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonSchema(val schemaKey: String)


///
///   The fields that all requests needs to have in their headers
///   (for reasons that are unclear to me)
///

@JsonInclude(Include.NON_NULL)
data class ES2RequestHeader(
        @JsonProperty("functionRequesterIdentifier") val functionRequesterIdentifier: String,
        @JsonProperty("functionCallIdentifier") val functionCallIdentifier: String
)

///
///   The fields all responses needs to have in their headers
///   (also unknown to me :)
///

@JsonInclude(Include.NON_NULL)
data class ES2ResponseHeader(
        @JsonProperty("functionExecutionStatus") val functionExecutionStatus: FunctionExecutionStatus = FunctionExecutionStatus())

@JsonInclude(Include.NON_NULL)
enum class FunctionExecutionStatusType {
    @JsonProperty("Executed-Success")
    ExecutedSuccess,
    @JsonProperty("Executed-WithWarning")
    ExecutedWithWarning,
    @JsonProperty("Failed")
    Failed,
    @JsonProperty("Expired")
    Expired
}

@JsonInclude(Include.NON_NULL)
data class FunctionExecutionStatus(
        @JsonProperty("status") val status: FunctionExecutionStatusType = FunctionExecutionStatusType.ExecutedSuccess,
        @JsonProperty("statusCodeData") val statusCodeData: StatusCodeData? = StatusCodeData(subjectCode =  "huh?", reasonCode =  "What?"))

@JsonInclude(Include.NON_NULL)
data class StatusCodeData(
        @JsonProperty("subjectCode") var subjectCode: String,
        @JsonProperty("reasonCode") var reasonCode: String,
        @JsonProperty("subjectIdentifier") var subjectIdentifier: String?,
        @JsonProperty("message") var message: String?)

///
///  The DownloadOrder function
///

@JsonSchema("ES2+DownloadOrder-def")
@JsonInclude(Include.NON_NULL)
data class Es2PlusDownloadOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String?,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("profileType") val profileType: String?
)

@JsonSchema("ES2+DownloadOrder-response")
@JsonInclude(Include.NON_NULL)
data class Es2DownloadOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader(),
        @JsonProperty("iccid") val iccid: String
)

///
/// The ConfirmOrder function
///

@JsonSchema("ES2+ConfirmOrder-def")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2ConfirmOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("confirmationCode") val confirmationCode: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?,
        @JsonProperty("releaseFlag") val releaseFlag: Boolean
)

@JsonInclude(Include.NON_NULL)
@JsonSchema("ES2+ConfirmOrder-response")
data class Es2ConfirmOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader(),
        @JsonProperty("eid") val eid: String,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("smdsAddress") val smdsAddress: String?
)

///
///  The CancelOrder function
///

@JsonInclude(Include.NON_NULL)
@JsonSchema("ES2+CancelOrder-def")
data class Es2CancelOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String,
        @JsonProperty("iccid") val iccid: String?,
        @JsonProperty("matchingId") val matchingId: String?,
        @JsonProperty("finalProfileStatusIndicator") val finalProfileStatusIndicator: String?
)

@JsonSchema("ES2+CancelOrder-response")
data class Es2CancelOrderResponse(@JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader())

///
///  The ReleaseProfile function
///

@JsonInclude(Include.NON_NULL)
@JsonSchema("ES2+ReleaseProfile-def")
data class Es2ReleaseProfile(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccid") val iccid: String
)

@JsonInclude(Include.NON_NULL)
@JsonSchema("ES2+ReleaseProfile-response")
data class Es2ReleaseProfileResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader())


///
///  The The HandleDownloadProgressInfo function
///

@JsonSchema("ES2+HandleDownloadProgressInfo-def")
@JsonInclude(Include.NON_NULL)
data class Es2HandleDownloadProgressInfo(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("profileType") val profileType: String? = null,
        @JsonProperty("timestamp") val timestamp: String? = null,
        @JsonProperty("notificationPointId") val notificationPointId: String? = null,
        @JsonProperty("notificationPointStatus") val notificationPointStatus: ES2NotificationPointStatus? = null,
        @JsonProperty("resultData") val resultData: ES2StatusCodeData? = null,
        @JsonProperty("imei") val imei : String? = null
)

@JsonInclude(Include.NON_NULL)
data class ES2NotificationPointStatus(
        @JsonProperty("status") val status: String, // "Executed-Success, Executed-WithWarning, Failed or
        @JsonProperty("statusCodeData") val statusCodeData: ES2StatusCodeData?
)

@JsonInclude(Include.NON_NULL)
data class ES2StatusCodeData(
        @JsonProperty("subjectCode") val subjectCode: String, // "Executed-Success, Executed-WithWarning, Failed or
        @JsonProperty("reasonCode") val statusCodeData: String,
        @JsonProperty("subjectIdentifier") val subjectIdentifier: String?,
        @JsonProperty("message") val message: String?
)

@JsonSchema("ES2+HandleDownloadProgressInfo-response")
data class Es2HandleDownloadProgressInfoResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader())


///
///    Convenience functions to generate headers
///


fun newErrorHeader(e: SmDpPlusException): ES2ResponseHeader {
    return ES2ResponseHeader(
            functionExecutionStatus =
            FunctionExecutionStatus(
                    status = FunctionExecutionStatusType.Failed,
                    statusCodeData = e.statusCodeData))
}

fun eS2SuccessResponseHeader() =
        ES2ResponseHeader(functionExecutionStatus =
        FunctionExecutionStatus(status = FunctionExecutionStatusType.ExecutedSuccess))