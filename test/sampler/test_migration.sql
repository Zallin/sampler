create table if not exists attribute (
  id text primary key,
  txid bigint not null,
  ts timestamptz default current_timestamp,
  resource_type text default 'Attribute',
  status text not null,
  resource jsonb not null
);

create table if not exists patient (
  id text primary key,
  txid bigint not null,
  ts timestamptz default current_timestamp,
  resource_type text default 'Patient',
  status text not null,
  resource jsonb not null
)
