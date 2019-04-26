-- this is schema setup

create table if not exists sample (
 id text primary key,
 ts timestamptz default current_timestamp,
 resources jsonb not null,
 description text);

create table if not exists connection (
 id text primary key,
 name text unique not null,
 pghost text not null,
 pgport text not null,
 pguser text not null,
 db text not null,
 opts jsonb
);

create table if not exists client (
 id text primary key,
 key text unique not null,
 secret text not null
)
