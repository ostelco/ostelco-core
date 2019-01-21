package org.ostelco.simcards.inventory

import org.skife.jdbi.v2.sqlobject.SqlUpdate

/**
 * Clear tables.  This library shouldn't be part of normal
 * running code, should be part of the test harness.
 */
abstract class SimInventoryCreationDestructionDAO {

    fun clearTables() {
        truncateImportBatchesTable()
        truncateSimEntryTable()
        truncateHlrAdapterTable()
        truncateProfileVendorAdapterTable()
        truncateSimVendorsPermittedTable()
    }

    @SqlUpdate("TRUNCATE sim_import_batches")
    abstract fun truncateImportBatchesTable()

    @SqlUpdate("TRUNCATE sim_entries")
    abstract fun truncateSimEntryTable()

    @SqlUpdate("TRUNCATE hlr_adapters")
    abstract fun truncateHlrAdapterTable()

    @SqlUpdate("TRUNCATE profile_vendor_adapters")
    abstract fun truncateProfileVendorAdapterTable()

    @SqlUpdate("TRUNCATE sim_vendors_permitted_hlrs")
    abstract fun truncateSimVendorsPermittedTable()
}
