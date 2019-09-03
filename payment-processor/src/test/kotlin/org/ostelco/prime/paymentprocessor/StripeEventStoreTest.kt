package org.ostelco.prime.paymentprocessor

import arrow.core.getOrElse
import com.stripe.model.Event
import com.stripe.net.ApiResource
import org.junit.Test
import org.ostelco.prime.paymentprocessor.subscribers.StripeEvent
import org.ostelco.prime.store.datastore.EntityStore
import java.time.Year
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


class StripeEventStoreTest {

    @Test
    fun `test save and fetch stripe event from datastore`() {
        val event = ApiResource.GSON.fromJson(payload, Event::class.java)
        val testData = StripeEvent(event.type,
                event.account,
                event.created,
                payload)
        val key = entityStore.add(testData)
                .mapLeft {
                    fail(it.message)
                }
                .getOrElse { null }
        assertNotNull(key)

        val fetched = entityStore.fetch(key)
                .mapLeft {
                    fail(it.message)
                }
                .getOrElse { null }
        assertNotNull(fetched)
        assertEquals(expected = testData, actual = fetched)
    }

    companion object {
        val entityStore = EntityStore(StripeEvent::class)
        val payload = """
              {
                "id": "charge.captured_00000000000000",
                "object": "event",
                "account": "null",
                "api_version": "2018-08-23",
                "created": 1326853478,
                "data": {
                    "object": {
                        "id": "ch_00000000000000",
                        "object": "charge",
                        "amount": 100,
                        "amount_refunded": 0,
                        "application": null,
                        "application_fee": null,
                        "application_fee_amount": null,
                        "alternate_statement_descriptors": null,
                        "balance_transaction": "txn_00000000000000",
                        "captured": true,
                        "created": 1554102110,
                        "currency": "nok",
                        "customer": null,
                        "description": "My First Test Charge (created for API docs)",
                        "destination": null,
                        "dispute": null,
                        "failure_code": null,
                        "failure_message": null,
                        "fraud_details": {
                        "user_report": null,
                        "stripe_report": null
                    },
                    "invoice": null,
                    "level3": null,
                    "livemode": false,
                    "metadata": {},
                    "on_behalf_of": null,
                    "order": null,
                    "outcome": null,
                    "paid": true,
                    "receipt_email": null,
                    "receipt_number": null,
                    "receipt_url": "https://pay.stripe.com/receipts/acct_1D3iWUIsscchPQo7/ch_1EKK9SIsE3jhPQo7aaavbFUx/rcpt_Enw1fqDWFhlAT3aZmk8E0G5kds7AKGr",
                    "refunded": false,
                    "refunds": {
                        "object": "list",
                        "data": [],
                        "has_more": false,
                        "total_count": 0,
                        "url": "/v1/charges/ch_1EKK9SIsE3jhPuua34nvbFUx/refunds",
                        "count": null,
                        "request_options": null,
                        "request_params": null
                    },
                    "review": null,
                    "shipping": null,
                    "source": {
                        "address_city": null,
                        "address_country": null,
                        "address_line1": null,
                        "address_line1_check": null,
                        "address_line2": null,
                        "address_state": null,
                        "address_zip": null,
                        "address_zip_check": null,
                        "available_payout_methods": null,
                        "brand": "Visa",
                        "country": "US",
                        "currency": null,
                        "cvc_check": "pass",
                        "default_for_currency": null,
                        "dynamic_last4": null,
                        "exp_month": 12,
                        "exp_year": ${Year.now().value + 1},
                        "fingerprint": "0iYTxuneb6pNlFWV",
                        "funding": "credit",
                        "last4": "4242",
                        "name": null,
                        "recipient": null,
                        "status": null,
                        "three_d_secure": null,
                        "tokenization_method": null,
                        "deleted": null,
                        "description": null,
                        "iin": null,
                        "issuer": null,
                        "type": null,
                        "id": "card_00000000000000",
                        "object": "card",
                        "account": null,
                        "customer": "cus_00000000000000",
                        "metadata": {}
                    },
                    "source_transfer": null,
                    "statement_descriptor": null,
                    "status": "succeeded",
                    "transfer": null,
                    "transfer_data": null,
                    "transfer_group": null,
                    "authorization_code": null,
                    "card": null,
                    "disputed": null,
                    "statement_description": null
                },
                "previous_attributes": null
            },
            "livemode": false,
            "pending_webhooks": 1,
            "request": null,
            "type": "charge.captured",
            "user_id": null
        }
        """.trimIndent()
    }
}