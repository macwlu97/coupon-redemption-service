#!/bin/bash

# --- Color Formatting ---
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# --- Configuration ---
GATEWAY_URL="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)
# Ensures length is exactly 15 chars
COUPON_CODE="SP26_$TIMESTAMP"
LOWER_CODE=$(echo "$COUPON_CODE" | tr '[:upper:]' '[:lower:]')

POLISH_IP="89.64.12.150"
US_IP="8.8.8.8"

# Database Config (matches create-databases.sh)
DB_CONTAINER="postgres"
DB_USER="maciejwnuklipinski"
DB_PASS="pswd"
USAGE_DB="usage_db"

echo -e "${BLUE}🚀 Starting Final Validated Smoke Test: $COUPON_CODE${NC}"
echo "-------------------------------------------------------"

# --- 1. ADMIN TEST ---
echo -n "1.1 Create Coupon... "
CREATE_RES=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/coupons" \
     -H 'Content-Type: application/json' \
     -d "{\"code\": \"$COUPON_CODE\", \"usageLimit\": 2, \"targetCountry\": \"PL\"}")

[[ "$CREATE_RES" == "201" ]] && echo -e "${GREEN}PASS ($CREATE_RES)${NC}" || { echo -e "${RED}FAIL ($CREATE_RES)${NC}"; exit 1; }

# --- 2. BUSINESS LOGIC TEST ---

echo -n "2.1 Redemption #1 (User Alpha - PL IP)... "
# Note: Using a shorter User-Id to avoid any potential length validation in your usage-service
RES1=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $POLISH_IP" \
     -H "User-Id: alpha_$TIMESTAMP" \
     -H 'Content-Type: application/json' -d '{}')

[[ "$RES1" == "200" ]] && echo -e "${GREEN}PASS ($RES1)${NC}" || echo -e "${RED}FAIL ($RES1)${NC}"

echo -n "2.2 Geofencing (US IP - Expecting 422)... "
RES_GEO=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $US_IP" \
     -H "User-Id: usa_$TIMESTAMP" \
     -H 'Content-Type: application/json' -d '{}')

if [[ "$RES_GEO" == "422" ]]; then
    echo -e "${GREEN}PASS ($RES_GEO)${NC}"
else
    echo -e "${RED}FAIL (Got $RES_GEO - Check GeoIpService Logs)${NC}"
    echo -e "${YELLOW}Hint: If 500, check your GlobalExceptionHandler for GeoIp failures.${NC}"
fi

echo "-------------------------------------------------------"

# --- 3. DATABASE VERIFICATION ---
echo -e "${BLUE}3. 📊 Verifying Records in '$USAGE_DB' (usage_history):${NC}"

# Capture the DB output to verify if rows exist
DB_OUTPUT=$(docker exec -t $DB_CONTAINER env PGPASSWORD=$DB_PASS psql -U $DB_USER -d $USAGE_DB \
    -t -c "SELECT count(*) FROM usage_history WHERE coupon_code = '$COUPON_CODE';")

# Clean whitespace
DB_COUNT=$(echo "$DB_OUTPUT" | tr -d '[:space:]')

if [[ "$DB_COUNT" -gt 0 ]]; then
    echo -e "${GREEN}SUCCESS: Found $DB_COUNT redemption(s) in database.${NC}"
    docker exec -t $DB_CONTAINER env PGPASSWORD=$DB_PASS psql -U $DB_USER -d $USAGE_DB \
        -c "SELECT redeemed_at, coupon_code, user_id FROM usage_history WHERE coupon_code = '$COUPON_CODE';"
else
    echo -e "${RED}CRITICAL FAIL: No records found in usage_db!${NC}"
    echo -e "${YELLOW}Reason: Step 2.1 returned 200 but didn't save. Check for @Transactional missing.${NC}"
fi

echo "-------------------------------------------------------"