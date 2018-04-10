package org.ostelco.caseofficer

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions

import java.io.IOException
import java.io.PrintStream

import java.nio.channels.Channels.newOutputStream

/**
 * The intent of this program is to manage agents.  In intelligence
 * parlance the role of someone managing agents is a "case officer",
 * so we're just adopting that.
 *
 * The tasks this program will perform are:
 *
 * 1. To extract information about subscribers' behavior and demographics
 *    from whatever sources are available.
 * 2. Translate that information into consistent datasets that can
 *    be used by agents to produce offers.
 * 3. Send those datasets as .csv files to the agents via google cloud storage.
 * 4. Pick up results from the agents, and publish them to the
 *    consumption machinery.
 *
 * The tooling we will use is:
 *
 * 1. BigQuery, to pick up data.
 * 2. PubSub to listen to changes in the input bucket.
 * 3. An interface to the consumption machinery to inject
 *    offers through it.
 * 4. Dropwizard (possibly) for infrastructure, surveillance, monitoring
 *    etc.
 */
object CaseOfficer {

    // Me way want to listen to changes in buckets:
    //  -- https://cloud.google.com/storage/docs/object-change-notification
    // Listening for changes (not implemented) https://cloud.google.com/storage/docs/object-change-notification

    @JvmStatic
    fun main(argv: Array<String>) {


        // If you don't specify credentials when constructing the client, the client library will
        // look for credentials via the environment variable GOOGLE_APPLICATION_CREDENTIALS.
        val storage = StorageOptions.getDefaultInstance().service

        val bucket = "rmz-test-bucket"
        val name = "testfile-delete-me.yaml"
        val id = BlobId.of(bucket, name)
        val blobInfo = BlobInfo.newBuilder(bucket, name).build()


        try {
            storage.writer(blobInfo).use { writer ->
                val os = newOutputStream(writer)
                val ps = PrintStream(os)
                ps.println("Yabbbadabbadoo!")
            }
        } catch (e: IOException) {
            println("foo")
        }

        println("Done")
    }
}
