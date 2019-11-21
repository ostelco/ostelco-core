--
-- Add timstamp column for hlrstate
--
alter table sim_entries add column tshlrstate timestamp;
alter table sim_entries alter column tshlrstate set default now();
create or replace function hlrstate_changed()
returns trigger as $$
begin
  new.tshlrstate := now();
  return new;
end;
$$ language plpgsql;
create trigger trigger_hlrstate
before update on sim_entries
for each row
when (old.hlrstate is distinct from new.hlrstate)
execute procedure hlrstate_changed();

--
-- Add timstamp column for smdpplusstate
--
alter table sim_entries add column tssmdpplusstate timestamp;
alter table sim_entries alter column tssmdpplusstate set default now();
create or replace function smdpplusstate_changed()
returns trigger as $$
begin
  new.tssmdpplusstate := now();
  return new;
end;
$$ language plpgsql;
create trigger trigger_smdpplusstate
before update on sim_entries
for each row
when (old.smdpplusstate is distinct from new.smdpplusstate)
execute procedure smdpplusstate_changed();

--
-- Add timstamp column for provisionstate
--
alter table sim_entries add column tsprovisionstate timestamp;
alter table sim_entries alter column tsprovisionstate set default now();
create or replace function provisionstate_changed()
returns trigger as $$
begin
  new.tsprovisionstate := now();
  return new;
end;
$$ language plpgsql;
create trigger trigger_provisionstate
before update on sim_entries
for each row
when (old.provisionstate is distinct from new.provisionstate)
execute procedure provisionstate_changed();
