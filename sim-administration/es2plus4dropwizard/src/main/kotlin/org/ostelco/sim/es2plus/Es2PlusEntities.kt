package org.ostelco.sim.es2plus

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.ostelco.jsonschema.JsonSchema
import org.ostelco.sim.es2plus.ES2PlusClient.Companion.getNowAsDatetime


///
///   The fields that all requests needs to have in their headers
///   (for reasons that are unclear to me)
///

/**
 * ES2+ protocol header.  The functionRequesterIdentifier is an ID identifying the
 * caller of the service.  The ID is part of the contract between the service provider
 * and the service user.  The functionCallIdentifier is an unique ID that is used to
 * trace the function invocation across server and client.   In this implementation
 * it is implemented as an UUID randomUUID string.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ES2RequestHeader(
        @JsonProperty("functionRequesterIdentifier") val functionRequesterIdentifier: String,
        @JsonProperty("functionCallIdentifier") val functionCallIdentifier: String = ES2PlusClient.newRandomFunctionCallIdentifier()
)

///
///   The fields all responses needs to have in their headers
///   (also unknown to me :)
///

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ES2ResponseHeader(
        @JsonProperty("functionExecutionStatus") val functionExecutionStatus: FunctionExecutionStatus = FunctionExecutionStatus())

@JsonInclude(JsonInclude.Include.NON_NULL)
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FunctionExecutionStatus(
        @JsonProperty("status") val status: FunctionExecutionStatusType = FunctionExecutionStatusType.ExecutedSuccess,
        @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("statusCodeData") val statusCodeData: StatusCodeData? = null)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StatusCodeData(
        @JsonProperty("subjectCode") var subjectCode: String,
        @JsonProperty("reasonCode") var reasonCode: String,
        @JsonProperty("subjectIdentifier") var subjectIdentifier: String? = null,
        @JsonProperty("message") var message: String? = null)

///
///  The DownloadOrder function
///

@JsonSchema("ES2+DownloadOrder-def")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2PlusDownloadOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("profileType") val profileType: String? = null
)

sealed class EsResponse(val myHeader: ES2ResponseHeader)

@JsonSchema("ES2+DownloadOrder-response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2DownloadOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader(),
        @JsonProperty("iccid") val iccid: String? = null
): EsResponse(header)


///
///  The CancelOrder function
///


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2PlusCancelOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("finalProfileStatusIndicator") val finalProfileStatusIndicator: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2PlusCancelOrderResponse(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("finalProfileStatusIndicator") val finalProfileStatusIndicator: String? = null
)

///
///   The ProfileStatus function
///


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2PlusProfileStatus(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccidList") val iccidList: List<IccidListEntry> = listOf()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IccidListEntry(
        @JsonProperty("iccid") val iccid: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2ProfileStatusCommand(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccidList") val iccidList: List<IccidListEntry> =  listOf())


@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2ProfileStatusResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader(),
        @JsonProperty("profileStatusList") val profileStatusList: List<ProfileStatus>? = listOf(),
        @JsonProperty("completionTimestamp") val completionTimestamp: String? = getNowAsDatetime()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileStatus(
        @JsonProperty("status_last_update_timestamp") val lastUpdateTimestamp:String? = null,
        @JsonProperty("profileStatusList") val profileStatusList: List<ProfileStatus>? = listOf(),
        @JsonProperty("acToken") val acToken: String? = null,
        @JsonProperty("state") val state: String? = null,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("iccid") val iccid: String? = null,
        @JsonProperty("lockFlag") val lockFlag: Boolean? = null
)


///
/// The ConfirmOrder function
///

@JsonSchema("ES2+ConfirmOrder-def")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2ConfirmOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("iccid") val iccid: String,
        @JsonProperty("matchingId") val matchingId: String? = null,
        @JsonProperty("confirmationCode") val confirmationCode: String? = null,
        @JsonProperty("smdpAddress") val smdpAddress: String? = null,
        @JsonProperty("releaseFlag") val releaseFlag: Boolean = true
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSchema("ES2+ConfirmOrder-response")
data class Es2ConfirmOrderResponse(
        @JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader(),
        @JsonProperty("eid") val eid: String? = null,
        @JsonProperty("matchingId") val matchingId: String? = null,
        @JsonProperty("smdpAddress") val smdsAddress: String? = null
)

///
///  The CancelOrder function
///

@JsonInclude(JsonInclude.Include.NON_NULL)
// XXX CXHeck @JsonSchema("ES2+CancelOrder-def")
data class Es2CancelOrder(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("eid") val eid: String?=null,
        @JsonProperty("profileStatusList") val profileStatusList: String? = null,
        @JsonProperty("matchingId") val matchingId: String? = null,
        @JsonProperty("iccid") val iccid: String?=null,
        @JsonProperty("finalProfileStatusIndicator") val finalProfileStatusIndicator: String? = null
)

@JsonSchema("ES2+HeaderOnly-response")
data class HeaderOnlyResponse(@JsonProperty("header") val header: ES2ResponseHeader = eS2SuccessResponseHeader())


///
///  The ReleaseProfile function
///

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSchema("ES2+ReleaseProfile-def")
data class Es2ReleaseProfile(
        @JsonProperty("header") val header: ES2RequestHeader,
        @JsonProperty("iccid") val iccid: String
)


///
///  The The HandleDownloadProgressInfo function
///


@JsonSchema("ES2+HandleDownloadProgressInfo-def")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Es2HandleDownloadProgressInfo(
        val header: ES2RequestHeader,
        val eid: String? = null,
        val iccid: String,
        val profileType: String,
        val timestamp: String,
        val notificationPointId: Int,
        val notificationPointStatus: ES2NotificationPointStatus,
        val resultData: String? = null,
        val tac: String? = null,
        val imei: String? = null,
        // This field is added to ensure that the function signature of the primary and the actual
        // constructors are not confused by the JVM.  It is ignored by all business logic.
        private val ignoreThisField : String? = null) {


    // If the stored ICCID contains a trailing "F", which it may because some  vendors insist
    // on contaminating their ICCID  values in this way, then we simply rewrite the value
    // before storing it in the data object.
    // Note that this is a bad practice, it's probably much better to rewrite the
    //     input field before it hits the data class, but I don't know how to do that
    //     so this kludge is used instead.   The good thing about the current fix is that
    //     it preserves external interfaces so the damage is contained within this
    //     class.

    @JsonCreator
    constructor (@JsonProperty("header")  header: ES2RequestHeader,
                 @JsonProperty("eid")  eid: String? = null,
                 @JsonProperty("iccid")  iccid: String,
                 @JsonProperty("profileType")  profileType: String,
                 @JsonProperty("timestamp")  timestamp: String = getNowAsDatetime(),
                 @JsonProperty("tac")  tac: String? = null,
                 @JsonProperty("notificationPointId")  notificationPointId: Int,
                 @JsonProperty("notificationPointStatus")  notificationPointStatus: ES2NotificationPointStatus,
                 @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("resultData")  resultData: String? = null,
                 @JsonProperty("imei")  imei: String? = null) : this(
            header = header,
            eid = eid,
            iccid = if (!iccid.endsWith("F")) {  // Rewrite input value if necessary
                iccid
            } else {
                iccid.dropLast(1)
            },
            tac = tac,
            profileType = profileType,
            timestamp = timestamp,
            notificationPointId = notificationPointId,
            notificationPointStatus = notificationPointStatus,
            resultData = resultData,
            imei = imei,
            ignoreThisField = null  //Field is always ignored, but necessary to avoid recursion
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ES2NotificationPointStatus(
        @JsonProperty("status") val status: FunctionExecutionStatusType = FunctionExecutionStatusType.ExecutedSuccess,
        @JsonInclude(JsonInclude.Include.NON_NULL)  @JsonProperty("statusCodeData") val statusCodeData: ES2StatusCodeData? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ES2StatusCodeData(
        @JsonProperty("subjectCode") val subjectCode: String, // "Executed-Success, Executed-WithWarning, Failed or
        @JsonInclude(JsonInclude.Include.NON_NULL)  @JsonProperty("reasonCode") val statusCodeData: String,
        @JsonProperty("subjectIdentifier") val subjectIdentifier: String? = null,
        @JsonProperty("message") val message: String? = null
)

///
///    Convenience functions to generate headers
///

fun newErrorHeader(exception: SmDpPlusException): ES2ResponseHeader {
    return ES2ResponseHeader(
            functionExecutionStatus =
            FunctionExecutionStatus(
                    status = FunctionExecutionStatusType.Failed,
                    statusCodeData = exception.statusCodeData))
}

fun eS2SuccessResponseHeader() =
        ES2ResponseHeader(functionExecutionStatus =
        FunctionExecutionStatus(status = FunctionExecutionStatusType.ExecutedSuccess))