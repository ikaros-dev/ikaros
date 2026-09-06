create extension if not exists pgcrypto;

create or replace function uuid_v7() returns uuid
    language sql volatile
as $$
    select (
        lpad(to_hex(floor(extract(epoch from clock_timestamp()) * 1000)::bigint), 12, '0')
        || '7'
        || substring(encode(gen_random_bytes(2), 'hex') from 1 for 3)
        || substring('89ab' from floor(random() * 4)::integer + 1 for 1)
        || substring(encode(gen_random_bytes(8), 'hex') from 1 for 15)
    )::uuid
$$;
