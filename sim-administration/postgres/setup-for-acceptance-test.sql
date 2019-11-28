--
-- Initial data set for acceptance tests.
--

-- dao.addProfileVendorAdapter("Bar")
INSERT INTO profile_vendor_adapters(name) VALUES ('Bar');

-- dao.addHssEntry("Foo")
INSERT INTO hlr_adapters(name) VALUES ('Foo');

-- dao.permitVendorForHssByNames(profileVendor = "Bar", hssName = "Foo")
--    val profileVendorAdapter = getProfileVendorAdapterByName(profileVendor)
--    val hlrAdapter = getHssEntryByName(hssName)
--    storeSimVendorForHssPermission(profileVendorAdapter.id, hlrAdapter.id)
INSERT INTO sim_vendors_permitted_hlrs(profileVendorid, hlrId) VALUES (1, 1)
