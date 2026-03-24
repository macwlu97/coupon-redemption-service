#!/bin/bash

# --- Colors for professional output ---
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# --- Configuration ---
GATEWAY_URL="http://localhost:8080/api/v1"
COUPON_CODE="SPRING2026"
POLISH_IP="89.64.12.150"
US_IP="8.8.8.8"
DB_CONTAINER="postgres"
DB_USER="maciejwnuklipinski"
TARGET_DB="usage_db" # Matches your init-db script

echo -e "${BLUE}🚀 Starting Coupon System Smoke Test...${NC}"
echo "-------------------------------------------------------"

# 1. COUPON CREATION
echo -n "1. Setup: Creating coupon '$COUPON_CODE' (Limit: 2, Country: PL)... "
CREATE_RES=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/coupons" \
     -H 'Content-Type: application/json' \
     -d "{\"code\": \"$COUPON_CODE\", \"usageLimit\": 2, \"targetCountry\": \"PL\"}")

if [[ "$CREATE_RES" == "201" || "$CREATE_RES" == "200" ]]; then
    echo -e "${GREEN}SUCCESS ($CREATE_RES)${NC}"
elif [[ "$CREATE_RES" == "409" ]]; then
    echo -e "${YELLOW}ALREADY EXISTS ($CREATE_RES)${NC}"
else
    echo -e "${RED}ERROR ($CREATE_RES)${NC}"
fi

# 2. GEOFENCING TEST (US IP should be rejected)
echo -n "2. Geofencing Test (Attempt from US IP: $US_IP)... "
RES_GEO=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $US_IP" \
     -H "User-Id: stranger_danger" \
     -H 'Content-Type: application/json' -d '{}')

if [[ "$RES_GEO" == "422" || "$RES_GEO" == "403" ]]; then
    echo -e "${GREEN}PASS (Correctly Blocked: $RES_GEO)${NC}"
else
    echo -e "${RED}FAIL (US IP was allowed! Status: $RES_GEO)${NC}"
fi

# 3. REDEMPTION #1 (User Alpha)
echo -n "3. Redemption #1 (User: Alpha, IP: PL)... "
RES1=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $POLISH_IP" \
     -H "User-Id: user_alpha" \
     -H 'Content-Type: application/json' -d '{}')

[[ "$RES1" =~ ^20[0-1]$ ]] && echo -e "${GREEN}PASS ($RES1)${NC}" || echo -e "${RED}FAIL ($RES1)${NC}"

# 4. REDEMPTION #2 (User Beta)
echo -n "4. Redemption #2 (User: Beta, IP: PL)... "
RES2=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $POLISH_IP" \
     -H "User-Id: user_beta" \
     -H 'Content-Type: application/json' -d '{}')

[[ "$RES2" =~ ^20[0-1]$ ]] && echo -e "${GREEN}PASS ($RES2)${NC}" || echo -e "${RED}FAIL ($RES2)${NC}"

# 5. USAGE LIMIT TEST (User Gamma should be rejected)
echo -n "5. Limit Test (User: Gamma - Attempt #3)... "
RES3=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/usages/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $POLISH_IP" \
     -H "User-Id: user_gamma" \
     -H 'Content-Type: application/json' -d '{}')

if [[ "$RES3" == "400" || "$RES3" == "409" || "$RES3" == "422" ]]; then
    echo -e "${GREEN}PASS (Correctly Rejected: $RES3)${NC}"
else
    echo -e "${RED}FAIL (Limit Exceeded! Status: $RES3)${NC}"
fi

# 6. DATABASE INTEGRITY CHECK (Targeting usage_db)
echo "-------------------------------------------------------"
echo -e "${BLUE}6. 📊 Database Check ($TARGET_DB.usage_history):${NC}"

# Querying the specific database created in your init script
docker exec -t $DB_CONTAINER psql -U $DB_USER -d $TARGET_DB -c "SELECT * FROM usage_history WHERE coupon_code = '$COUPON_CODE' ORDER BY redeemed_at DESC;"

echo "-------------------------------------------------------"
echo -e "${BLUE}✅ All tests completed! System ready for submission.${NC}"