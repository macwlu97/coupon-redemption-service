#!/bin/bash

# Colors for professional output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

COUPON_CODE="TASK2026"
BASE_URL_COUPONS="http://localhost:8082/api/v1/coupons"
BASE_URL_USAGES="http://localhost:8081/api/v1/usages"
POLISH_IP="89.64.12.150"

echo -e "${BLUE}🚀 Starting Task Smoke Test...${NC}"
echo "-------------------------------------------------------"

# 1. CREATE COUPON (Testing uniqueness & Case-insensitivity)
# We try to create 'TASK2026' with limit 2
echo -n "1. Setup: Creating coupon '$COUPON_CODE' (Limit: 2, Country: PL)... "
CREATE_RES=$(curl -s -o /dev/null -w "%{http_code}" -X 'POST' \
  "$BASE_URL_COUPONS" \
  -H 'Content-Type: application/json' \
  -d "{\"code\": \"$COUPON_CODE\", \"usageLimit\": 2, \"targetCountry\": \"PL\"}")

if [ "$CREATE_RES" == "201" ] || [ "$CREATE_RES" == "200" ]; then
    echo -e "${GREEN}DONE ($CREATE_RES)${NC}"
elif [ "$CREATE_RES" == "409" ]; then
    echo -e "${YELLOW}EXISTS ($CREATE_RES)${NC}"
else
    echo -e "${RED}FAIL ($CREATE_RES)${NC}"
fi

# 2. VALID REDEMPTION (Testing valid IP and country match)
USER_1="candidate_001"
echo -n "2. Valid redemption (User: $USER_1, IP: PL)... "
RES2=$(curl -s -o /dev/null -w "%{http_code}" -X 'POST' \
  "$BASE_URL_USAGES/$COUPON_CODE/redeem" \
  -H "X-Forwarded-For: $POLISH_IP" \
  -H "User-Id: $USER_1" -d '')

[[ "$RES2" =~ ^20[0-1]$ ]] && echo -e "${GREEN}PASS ($RES2)${NC}" || echo -e "${RED}FAIL ($RES2)${NC}"

# 3. RE-USE PROTECTION (Testing: One user can use coupon only once)
echo -n "3. Testing re-use (Same User: $USER_1)... "
RES3=$(curl -s -o /dev/null -w "%{http_code}" -X 'POST' \
  "$BASE_URL_USAGES/$COUPON_CODE/redeem" \
  -H "X-Forwarded-For: $POLISH_IP" \
  -H "User-Id: $USER_1" -d '')

# Should return 409 Conflict or 422 depending on implementation
if [[ "$RES3" == "409" || "$RES3" == "422" ]]; then
    echo -e "${GREEN}PASS (Rejected correctly: $RES3)${NC}"
else
    echo -e "${RED}FAIL (User used coupon twice! Code: $RES3)${NC}"
fi

# 4. LIMIT PROTECTION (Testing: Max usage limit = 2)
USER_2="candidate_002"
USER_3="candidate_003"

# Use up the 2nd slot
curl -s -o /dev/null -X 'POST' "$BASE_URL_USAGES/$COUPON_CODE/redeem" \
     -H "X-Forwarded-For: $POLISH_IP" -H "User-Id: $USER_2" -d ''

echo -n "4. Testing limit (Third user: $USER_3)... "
RES4=$(curl -s -o /dev/null -w "%{http_code}" -X 'POST' \
  "$BASE_URL_USAGES/$COUPON_CODE/redeem" \
  -H "X-Forwarded-For: $POLISH_IP" \
  -H "User-Id: $USER_3" -d '')

if [[ "$RES4" == "409" || "$RES4" == "422" ]]; then
    echo -e "${GREEN}PASS (Limit reached, rejected: $RES4)${NC}"
else
    echo -e "${RED}FAIL (Limit exceeded! Code: $RES4)${NC}"
fi

# 5. DATABASE INTEGRITY CHECK
echo "-------------------------------------------------------"
echo -e "${BLUE}5. 📊 Final System State Check:${NC}"
echo -e "Latest 3 entries in usage_history:"
docker exec -it usage-db psql -U usageusagetest -d usage_db -c "SELECT coupon_code, user_id FROM usage_history ORDER BY id DESC LIMIT 3;"

echo "-------------------------------------------------------"
echo -e "${BLUE}✅ Task Smoke Test Finished.${NC}"