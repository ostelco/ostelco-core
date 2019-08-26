package org.ostelco.simcards.admin

import org.jdbi.v3.sqlobject.statement.SqlUpdate

/**
 * Clear tables.  This library shouldn't be part of normal
 * running code, should be part of the test harness.
 */
interface ClearTablesForTestingDB {

    @SqlUpdate("TRUNCATE sim_import_batches")
    fun truncateImportBatchesTable()

    @SqlUpdate("TRUNCATE sim_entries")
    fun truncateSimEntryTable()

    @SqlUpdate("TRUNCATE hlr_adapters")
    fun truncateHlrAdapterTable()

    @SqlUpdate("TRUNCATE profile_vendor_adapters")
    fun truncateProfileVendorAdapterTable()

    @SqlUpdate("TRUNCATE sim_vendors_permitted_hlrs")
    fun truncateSimVendorsPermittedTable()
}

class ClearTablesForTestingDAO(private val db: ClearTablesForTestingDB) {

    fun clearTables() {
        db.truncateImportBatchesTable()
        db.truncateSimEntryTable()
        db.truncateHlrAdapterTable()
        db.truncateProfileVendorAdapterTable()
        db.truncateSimVendorsPermittedTable()
    }
}