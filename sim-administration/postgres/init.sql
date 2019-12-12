/**
 * SIM Manager DB schema.
 */

--
-- Create tables.
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
                          tsHlrState timestamp default now(),
                          tsSmdpPlusState timestamp default now(),
                          tsProvisionState timestamp default now(),
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

--
-- Add trigger for updating tshlrState timestamp on changes to hlrState.
--
create or replace function hlrState_changed()
returns trigger as $$
begin
  new.tsHlrState := now();
  return new;
end;
$$ language plpgsql;

create trigger trigger_hlrState
before update on sim_entries
for each row
when (old.hlrState is distinct from new.hlrState)
execute procedure hlrState_changed();

--
-- Add trigger for updating tsSmdpPlusState timestamp on changes to smdpPlusState.
--
create or replace function smdpPlusState_changed()
returns trigger as $$
begin
  new.tsSmdpPlusState := now();
  return new;
end;
$$ language plpgsql;

create trigger trigger_smdpPlusState
before update on sim_entries
for each row
when (old.smdpPlusState is distinct from new.smdpPlusState)
execute procedure smdpPlusState_changed();

--
-- Add trigger for updating tsProvisionState timestamp on chages to provisionState.
--
create or replace function provisionState_changed()
returns trigger as $$
begin
  new.tsProvisionState := now();
  return new;
end;
$$ language plpgsql;

create trigger trigger_provisionState
before update on sim_entries
for each row
when (old.provisionState is distinct from new.provisionState)
execute procedure provisionState_changed();
