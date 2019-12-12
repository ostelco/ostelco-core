/**
 * Script for adding timestamps to existing DB.
 *
 * For new DB, use the 'init.sql' script.
 */

--
-- Add timstamp column for hlrState.
--
alter table sim_entries add column tsHlrState timestamp;
alter table sim_entries alter column tsHlrState set default now();
update sim_entries set tsHlrState = now();
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
-- Add timstamp column for smdpPlusState.
--
alter table sim_entries add column tsSmdpPlusState timestamp;
alter table sim_entries alter column tsSmdpPlusState set default now();
update sim_entries set tsSmdpPlusState = now();
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
-- Add timstamp column for provisionState.
--
alter table sim_entries add column tsProvisionState timestamp;
alter table sim_entries alter column tsProvisionState set default now();
update sim_entries set tsProvisionState = now();
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
