#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE coupon_db;
    CREATE DATABASE usage_db;
EOSQL