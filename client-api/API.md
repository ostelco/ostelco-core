# Rest API for the "project pi" client

This document describes the API between the client and the backend.

<!--ts-->
   * [Introduction](#introduction)
   * [Data model](#data-model)
   * [Common for all API methods](#common-for-all-api-methods)
      * [Preferred language](#preferred-language)
         * [Language indication sent from client](#language-indication-sent-from-client)
         * [Format multi-language text strings sent in reponses to client](#format-multi-language-text-strings-sent-in-reponses-to-client)
      * [HTTP status codes and error reporting](#http-status-codes-and-error-reporting)
         * [If a request gives no error](#if-a-request-gives-no-error)
         * [If a request results in an error (Bad Request or Forbidden)](#if-a-request-results-in-an-error-bad-request-or-forbidden)
         * [Unknown path (Not Found)](#unknown-path-not-found)
         * [Server side errors](#server-side-errors)
   * [The API](#the-api)
      * [Sign up and authentication](#sign-up-and-authentication)
         * [Register personal information](#register-personal-information)
         * [Authenticate using verification code](#authenticate-using-verification-code)
         * [Refreshing the access token](#refreshing-the-access-token)
      * [Sign in](#sign-in)
      * [User profile](#user-profile)
         * [Fetch profile](#fetch-profile)
         * [Update profile](#update-profile)
      * [Subscriptions](#subscriptions)
         * [Get subscription status](#get-subscription-status)
      * [Offers](#offers)
         * [Get list of new offers](#get-list-of-new-offers)
         * [Accept or reject an offer](#accept-or-reject-an-offer)
         * [Undo a previously accepted offer](#undo-a-previously-accepted-offer)
         * [Dismiss an offer](#dismiss-an-offer)
      * [Consents](#consents)
         * [Get list of consents](#get-list-of-consents)
         * [Set or update consents](#set-or-update-consents)
      * [Analytics](#analytics)
         * [Report an analytics event](#report-an-analytics-event)
   * [Appendix](#appendix)

<!-- Added by: kmm, at: 2018-04-26T08:39+02:00 -->

<!--te-->

## Introduction

The API described is based on a simplified data model, suitable for handling the "100 users" test case. The
model and the corresponding API will have to be reworked in order to handle 100+ users.

Furthermore:

 - The API is a REST API.
 - Assumes that some OAuth2 or similar based service is used for authentication.
 - All client interactions goes through the backend, including handling of authentication, payment etc.
 - Subscriptions as such has already been activated through the CRM system including registration of email
   address etc.

## Data model

Figure describing the data model:

![Data model](images/user-subscription-data-model.svg)

The model assumes that:

 1. A user has only one subscription, which is then associated with only one handset/SIM.
 2. Offers are given to this subscription.

This is a simplified model. In reality a user might have multiple subscriptions and one subscription might be
"managed" by an user different from the user using the subscription, etc.

For cases where a subscription (user) has been given multiple offers and accepted them, the offers are "consumed"
in sequence. The ordering can typically be by the offers "expire" date. That is the offer that expires first,
is used up first and then the next etc.

## Common for all API methods
### Preferred language

 1. The preferred language is indicated in every request from client.
 2. The client can indicate request for one or more languages.
 3. Text strings in responses to the client should be in all languages indicated in the request. This
    will allow the client to switch between languages without connecting to backend.
 4. If a language that is not supported is indicated in a request, it should be ignored.
 5. Text strings in the default language should always be included in a response.

#### Language indication sent from client

 1. As a query paramter in the URI.

```
    /long/url/to/somewhere?lang=no,en
```

 2. Using the "Accept-Language" HTTP header.

```
    Accept-Language: no, en-gb;q=0.8, en;q=0.7
    Accept-Language: *
```

 3. With no `lang` query parameter in URI or `Accept-Language` HTTP header the default language should be used.
 4. If none of the requested languages are supported, fall back to the default language.
 5. The `lang` query parameter values has priority over the `Accept-Language` HTTP header.

#### Format multi-language text strings sent in reponses to client

    "message": [{
                 "lang": "en",           // ISO 639-1
                 "text": "an error"
              },
              {
                 "lang": "no",
                 "text": "en feil"
              }]

The `en` (english) language is the default language and is always included. Sections for additional languages
are added according to the language specification included in the request if available.

### HTTP status codes and error reporting

The API uses the following HTTP status codes.

code | meaning
-----|--------------------
 200 | OK
 201 | Created
 400 | Bad Request
 401 | Unauthorized
 403 | Forbidden
 404 | Not Found
 500 | Internal Server Error
 503 | Service Unavailable

In addition a service specific error code in included in the document describing the error in the error repsonse.

#### If a request gives no error

    -> <some HTTP request>
    <- 200 OK
    <- 201 Created

#### If a request results in an error (Bad Request or Forbidden)

    -> GET /somewhere/out/there
    <- 400 Bad Request
       {
         "code": "<error-code>",              // Service specific error code
         "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
         "message": [{
                      "lang": "en",           // ISO 639-1
                      "text": "an error"
                   },
                   {
                      "lang": "no",
                      "text": "en feil"
                   }]
       }

For cases where the request contains a list of updates and one or more of them are incorrect, a list
with error messages are returned. Each error message in the list then includes an index value pointing
to the request that caused an error.

    -> POST /somewhere/out/there
       { ...
       }
    <- 400 Bad Request
       [{
         "index": <n>,                        // Index of the list element that caused the error
         "code": "<error-code>",              // Service specific error code
         "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
         "message": [{
                      "lang": "en",           // ISO 639-1
                      "text": "an error"
                   },
                   {
                      "lang": "no",
                      "text": "en feil"
                   }]
        },
        {
         "index": <n>,                        // Index of the list element that caused the error
         "code": "<error-code>",              // Service specific error code
         "error_id": "<error-id>",            // id tracing the API call (for lookup in logs)
         "message": [ ...
                   }]
       }]

Note that an error text is provided with each error report. This will make it possible to report different
types of error messages depending on the type of error.

Entries that are not referenced to in the error response with an index value, have been processed without
error.

#### Unknown path (Not Found)

    -> GET /somewhere/out/there
    <- 404 Not Found
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Can not find requested address"
                    },
                    {
                       "lang": "no",
                       "text": "Kan ikke finne adressen"
                    }]
       }

#### Server side errors

On errors in the backend.

    -> GET /somewhere/out/there
    <- 500 Unknown Error
       {
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Something went wrong"
                    },
                    {
                       "lang": "no",
                       "text": "Noe gikk galt"
                    }]
       }

When the service is down for maintenance or similar.

    -> GET /somewhere/out/there
    <- 503 Service Unavailable
       {
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Service is down, try again later"
                    },
                    {
                       "lang": "no",
                       "text": "Tjenesten er nede, prøv igjen senere"
                    }]
       }

## The API

### Sign up and authentication


![Signin flow sequence diagram](/diagrams/signin-flow.svg)


The first time the users starts the client, a _sign up_ procedure should be done. As part of the sign up, the
user registers the following information:

 - Name
 - Email address (previously registered as part of the subscription activation)

An email with a verification code is then sent to the registered email address. The verification code is then
entered into the client and the sign up procedure has been completed.

#### Register personal information

    -> POST /register
       {
         "name": "<name>",
         "email": "<email address>"
       }
    <- 201 Created

Provided that the given email address is know, an email with a verification code is sent to the registred
email address.

On unknown email address a HTTP `403` status code is returned.

    -> POST /register
       {
         "name": "<name>",
         "email": "me@illegal-address.com"
       }
    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Unknown email address"
                    },
                    {
                       "lang": "no",
                       "text": "Ukjent email addresse"
                    }]
       }

#### Authenticate using verification code

The verification code, sent to the subscriber by email as part of the [sign up](#sign-up) process, is used
to authenticate with the service.

    -> POST /auth/token
       {
         "grant_type": "authorization_code",
         "code": "<code>",
         "email": "<email address>"
       }
    <- 201 Created
       {
         "token_type": "bearer",
         "access_token": "<access-token>",
         "refresh_token": "<refresh-token>",
         "expires_in": <n min>
       }

The response is an OAuth2 Bearer token, including a _refresh_ token. The client should refresh the _access_
token when it has expired with the _refresh_ token at the `/register` endpoint.

Note! On successful return of an OAuth2 token, the client should first obtain the _subscriber-id_ using the
`/profile` endpoint - see the [Fetch profile](#fetch-profile) section.

On an unknown verification code a HTTP `403` status code is returned.

    -> POST /auth/token
       {
         "verification-code": "<code>",
         "email": "<email address>"
       }
    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Unknown code"
                    },
                    {
                       "lang": "no",
                       "text": "Ukjent kode"
                    }]
       }

#### Refreshing the access token

    -> POST /auth/token
       {
         "grant_type": "refresh_token",
         "refresh_token": "<refresh-token>"
       }
    <- 201 Created
       {
         "token_type": "bearer",
         "access_token": "<access-token>",
         "expires_in": <n min>
       }

The response is an OAuth2 Bearer token, but without the _refresh_ token.

HTTP `403` is returned on unknown refresh token.

    -> POST /auth/token
       {
         "grant_type": "refresh_token",
         "refresh_token": "<refresh-token>"
       }
    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Unknown token"
                    },
                    {
                       "lang": "no",
                       "text": "Ukjent token"
                    }]
       }

### Sign in

If the JWT _access token_ has expired, then the client should try to [refresh](#refreshing-the-access-token)
the token using the _expire token_. If this fails then a new [sign up](#sign-up) should be done.

### User profile
#### Fetch profile

Fetch profile content.

    -> GET /profile
    <- 200 OK
       {
         "name": "<name">,
         "email": "<email address>",
         "subscription_id": "<subscription-id>"
       }

Note that the returned profile contains the `subscription-id`.

On error a HTTP `404` status code is returned.

    <- 404 Not Found
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Can't find profile"
                    },
                    {
                       "lang": "no",
                       "text": "Fant ingen profil"
                    }]
       }

#### Update profile

One or more fields in the profile can be updated at once. The exception is the `subscription-id` which
cannot be updated.

    -> PUT /profile
       {
          "name": "new name"
       }
    <- 200 OK (no body)

On error a HTTP `400` status code is returned.

    <- 400 Bad Request
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Illegal field value"
                    },
                    {
                       "lang": "no",
                       "text": "Feil i oppgitt verdi"
                    }]
       }

### Subscriptions

 1. A user can (with the simplified data model) only have one or no _subscription_.
 2. A _subscription_ is identified with an `subscription-id`.
 3. The `subscription-id` is included in requests.
 4. If a user has a _subscription_ the user also has a handset (where the client is running).
 5. An offer might be to one _subscription_ (a particular user) or to multiple subscriptions (more than one user).

#### Get info for one subscription

    -> GET /subscription/status
    <- 200 OK
       {
         "id": "<subscription-id>",
         "remaining": <value>,                // Remaining data quota in KB (long)
         "accepted_offers": [{
                                "offer_id": "SKU_1",
                                "value": <value>,        // In KB
                                "usage": <value>,        // In KB
                                "expires": <timestamp>   // ms since epoch
                             },
                             {
                                "offer_id": "SKU_3",
                                "value": <value>,
                                "usage": <value>,
                                "expires": <timestamp>
                             }]
       }

Notes:

 - The `value` field could be qualified with `dimension` field with the values KB, MB or GB. But it seems to be easier to just
   assume that KB is the unit and let the client do the conversion as needed. (The `value` should then be a `long`). Btw., to
   convert maybe just divide by 1000, not 1024.

#### Get info for all subscriptions

    -> GET /subscriptions
    <- 200 OK
       [{
          "id": "<subscription-id>",
          ...
        },
        {
          ...
        }]

#### Create and delete subscriptions

Creation and deletion of subscriptions is handled by the CRM system (handled by customer service).

### Offers
#### Get list of new offers

    -> GET /offers/<subscription-id>
    <- 200 OK
       [{
          "id": "<offer-id>",
          "label": "A big offer",                // Name of the offer
          "price": 99.99,                        // Two-digit float
          "value": 100,                          // How much the offer tops up in KB (long)
          "expires": <timestamp>                 // ms since epoch
        },
        { ...
        }]

Notes:

 - The same list will be returned the next time, unless one or more offers has expired or been retracted or new ones has been added.
 - If an offer has been accepted then this offer will not reappear in the list the next time (is now an "accepted offer").
 - It is up to the client to ensure that previously seen and rejected offers does not reappear. This will allow the client to display previously rejected offers again.

#### Accept or reject an offer

Accepting one offer.

    -> PUT /offers/<offer-id>?accepted=[true,false]
    <- 200 OK (no body)

On error a HTTP `403` status code is returned.

    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Unknown offer, or expired offer"
                    },
                    {
                       "lang": "no",
                       "text": "Ukjent tilbud, eller tilbudet har utløpt"
                    }]
       }

#### Undo a previously accepted offer

A previously accepted offer can be undone (reverted) if:

 1. It is done within a certain time limit, f.ex. within 10 min. This should also be possible even if the the offer has started to "run".
 2. The offer has not started to "run" yet.

```
    -> PUT /offers/<offer-id>?accepted=false
    <- 200 OK (no body)
```

On error a HTTP `403` status code is returned.

    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Offer already taken into use, contact customer service"
                    },
                    {
                       "lang": "no",
                       "text": "Tilbudet er allerede tatt i bruk, kontakt kundeservice"
                    }]
       }

#### Dismiss an offer

If the user is not interested in an offer, it can be removed from the list of outstanding offers.

This event is reported back as an [analytics](#analytics) event.

### Consents

Before collecting analytics, the user should be asked whether it this is OK or not. The "consent request" should
contain information about what data that is collected and why.

A consent is valid for all "subscriptions" (devices) registered on a user.

#### Get list of consents

    -> GET /consents
    <- 200 OK
       [{
          "id": "<consent-id>",
          "description": "<some text>",
          "accepted": <bool>
        },
        {
          ...
        }]

#### Set or update consents

    -> PUT /consents/<consent-id>?accepted=[true|false]
    <- 200 OK (no body)

On error a HTTP `403` status code is returned.

    <- 403 Forbidden
       {
          "code": "<error-code>",              // Service specific error code
          "error_id": "<error-id>",            // An id tracing the API call (for lookup in logs)
          "message": [{
                       "lang": "en",           // ISO 639-1
                       "text": "Unknown consent"
                    },
                    {
                       "lang": "no",
                       "text": "Ukjent avtale"
                    }]
       }

### Analytics

Sending of analytics events depends on the [consents](#consents) set.

Analytics events will normally be implicitly reported as part of normal client/backend interaction flow. F.ex. when an offer
is [accepted](#accept an offer).

In cases where an event is not part of the normal client/backend interaction flow, an explict analytics event will be sent. F.ex. if
an offer is [dismissed](#dismiss an offer).

In both cases reporting of the event for analytics purposes is subject to the consents given by the user.

#### Report an analytics event

The API for reporting events that are not implictly given by the normal client/backend interaction flow, uses an `event-type` field
to specify which event that is reported. Only the `event-type` is common to such reports, the remaining content is dependent upon
the type of event reported.

    -> POST /analytics/<subscription-id>
       [{
          "event-type": "DELETES_AN_OFFER",
          "offer-id": <"offer-id>"
        },
        {
          "event": "....",

        }]
    <- 201 Created (no body)

The analytics `event-type` determines what kind of information that is provided with the report.

event-type | parameters
-----------|-------------
DELETES_AN_OFFER | `offer-id`
FETCHES_OFFER_LIST | (none)

## Appendix

TBD.
