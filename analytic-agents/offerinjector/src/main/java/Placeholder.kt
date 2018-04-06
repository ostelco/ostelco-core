import com.google.api.gax.paging.Page
import com.google.cloud.WriteChannel
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions

import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

import java.nio.channels.Channels.newOutputStream

object Placeholder {


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
