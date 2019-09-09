--
--    This is the schema definition that is intended to be used in the integration tests.
--    DO not use it in acceptance tests or in production.
--



create table sim_import_batches (id bigserial primary key,
                                 status text,
                                 endedAt bigint,
                                 importer text,
                                 size integer,
                                 hlrId bigserial,
                                 profileVendorId bigserial);
create table sim_entries (id bigserial primary key,
                          profileVendorId bigserial,
                          hlrId bigserial,
                          msisdn text,
                          eid text,
                          profile text,
                          hlrState text,
                          smdpPlusState text,
                          provisionState text,
                          matchingId text,
                          batch bigserial,
                          imsi varchar(16),
                          iccid varchar(22),
                          pin1 varchar(4),
                          pin2 varchar(4),
                          puk1 varchar(8),
                          puk2 varchar(8),
                          UNIQUE (imsi),
                          UNIQUE (iccid));
create table hlr_adapters (id bigserial primary key,
                           name text,
                           UNIQUE (name));
create table profile_vendor_adapters (id bigserial primary key,
                                      name text,
                                      UNIQUE (name));
create table sim_vendors_permitted_hlrs (id bigserial primary key,
                                         profileVendorId bigserial,
                                         hlrId bigserial,
                                         UNIQUE (profileVendorId, hlrId));
