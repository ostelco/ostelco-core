package org.ostelco.simcards.inventory

import org.skife.jdbi.v2.sqlobject.SqlUpdate

/**
 * Create/destroy tables.  This library shouldn't be part of normal
 * running code, should be part of the test harness.
 */
abstract class SimInventoryCreationDestructionDAO {

    fun createAll() {
        createImportBatchesTable()
        createSimEntryTable()
        createHlrAdapterTable()
        createProfileVendorAdapterTable()
        createSimVendorsPermittedTable()

    }

    fun dropAll() {
        try {
            dropImportBatchesTable()
        } catch (e: Exception) {
        }
        try {
            dropSimEntryTable()
        } catch (e: Exception) {
        }
        try {
            dropHlrAdapterTable()
        } catch (e: Exception) {
        }
        try {
            dropProfileVendorAdapterTable()
        } catch (e: Exception) {
        }
        try {
            dropSimVendorsPermittedTable()
        } catch (e: Exception) {
        }
    }

    //
    // Creating and deleting tables (XXX only used for testing, and should be moved to
    // a test only DAO eventually)
    //
    @SqlUpdate("create table sim_import_batches (id integer primary key autoincrement, status text, endedAt integer, importer text, size integer, hlrId integer, profileVendorId integer)")
    abstract fun createImportBatchesTable()

    @SqlUpdate("drop table sim_import_batches")
    abstract fun dropImportBatchesTable()

    @SqlUpdate("create table sim_entries (id integer primary key autoincrement, profileVendorId integer, hlrid integer, msisdn text, eid text, hlrState text, smdpPlusState text, batch integer, imsi varchar(15), iccid varchar(22), pin1 varchar(4), pin2 varchar(4), puk1 varchar(80), puk2 varchar(80), CONSTRAINT Unique_Imsi UNIQUE (imsi), CONSTRAINT Unique_Iccid UNIQUE (iccid))")
    abstract fun createSimEntryTable()

    @SqlUpdate("drop table sim_entries")
    abstract fun dropSimEntryTable()

    @SqlUpdate("create table hlr_adapters (id integer primary key autoincrement, name text,  CONSTRAINT Unique_Name UNIQUE (name))")
    abstract fun createHlrAdapterTable()

    @SqlUpdate("drop table hlr_adapters")
    abstract fun dropHlrAdapterTable()

    @SqlUpdate("create table profile_vendor_adapters (id integer primary key autoincrement, name text,  CONSTRAINT Unique_Name UNIQUE (name))")
    abstract fun createProfileVendorAdapterTable()

    @SqlUpdate("drop table profile_vendor_adapters")
    abstract fun dropProfileVendorAdapterTable()

    @SqlUpdate("create table sim_vendors_permitted_hlrs (id integer primary key autoincrement, profileVendorId integer, hlrId integer,  CONSTRAINT Unique_pair UNIQUE (profileVendorId, hlrId))")
    abstract fun createSimVendorsPermittedTable()

    @SqlUpdate("drop  table sim_vendors_permitted_hlrs")
    abstract fun dropSimVendorsPermittedTable()
}
