--
-- Add timstamp column for hlrstate
--
alter table sim_entries add column tshlrstate timestamp;
alter table sim_entries alter column tshlrstate set not null default now();
create or replace function hlrstate_changed()
returns trigger as $$
begin
  if new.hlrstate != old.hlrstate then
    new.tshlrstate := now();
  end if;
return new;
end;
$$ language plpgsql;
create trigger trigger_hlrstate
before update on sim_entries
for each row
execute procedure hlrstate_changed();

--
-- Add timstamp column for smdpplusstate
--
alter table sim_entries add column tssmdpplusstate timestamp;
alter table sim_entries alter column tssmdpplusstate not null set default now();
create or replace function smdpplusstate_changed()
returns trigger as $$
begin
  if new.smdpplusstate != old.smdpplusstate then
    new.tssmdpplusstate := now();
  end if;
return new;
end;
$$ language plpgsql;
create trigger trigger_smdpplusstate
before update on sim_entries
for each row
execute procedure smdpplusstate_changed();

--
-- Add timstamp column for provisionstate
--
alter table sim_entries add column tsprovisionstate timestamp;
alter table sim_entries alter column tsprovisionstate not null set default now();
create or replace function provisionstate_changed()
returns trigger as $$
begin
  if new.provisionstate != old.provisionstate then
    new.tsprovisionstate := now();
  end if;
return new;
end;
$$ language plpgsql;
create trigger trigger_provisionstate
before update on sim_entries
for each row
execute procedure provisionstate_changed();
