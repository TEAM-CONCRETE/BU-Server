#!/bin/bash

echo "=== API 필터링 테스트 ==="
echo ""

# 현장 관리자 로그인 (apitestmgr 계정 사용)
echo "1. 현장 관리자 로그인 (apitestmgr)"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  --data @/tmp/login-apitestmgr.json)

MANAGER_TOKEN=$(echo $RESPONSE | jq -r '.data.accessToken')

if [ "$MANAGER_TOKEN" != "null" ] && [ -n "$MANAGER_TOKEN" ]; then
    echo "✅ 로그인 성공!"
    SITE_ID=$(echo $RESPONSE | jq -r '.data.siteId')
    echo "✅ 현장 ID: $SITE_ID"
else
    echo "❌ 로그인 실패!"
    echo "$RESPONSE" | jq '.'
    exit 1
fi
echo ""

# 작업일보 전체 조회
echo "2. 작업일보 전체 조회"
TOTAL_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/work-reports?page=0&size=10" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
echo "$TOTAL_RESPONSE" | jq '{
  success: .success,
  message: .message,
  totalElements: .data.totalElements,
  contentCount: (.data.content | length)
}'
echo ""

# 작업일보 2025년 11월 필터링
echo "3. 작업일보 연도/월 필터링 (2025년 11월)"
FILTERED_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/work-reports?year=2025&month=11&page=0&size=10" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
echo "$FILTERED_RESPONSE" | jq '{
  success: .success,
  message: .message,
  totalElements: .data.totalElements,
  contentCount: (.data.content | length)
}'
echo ""

# 안전교육일지 전체 조회
echo "4. 안전교육일지 전체 조회"
SAFETY_TOTAL=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-education-logs" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
echo "$SAFETY_TOTAL" | jq '{
  success: .success,
  message: .message,
  dataCount: (.data | length)
}'
echo ""

# 안전교육일지 2025년 11월 필터링
echo "5. 안전교육일지 연도/월 필터링 (2025년 11월)"
SAFETY_FILTERED=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-education-logs?year=2025&month=11" \
  -H "Authorization: Bearer $MANAGER_TOKEN")
echo "$SAFETY_FILTERED" | jq '{
  success: .success,
  message: .message,
  dataCount: (.data | length)
}'
echo ""

# 기업 관리자 로그인
echo "6. 기업 관리자 로그인 (apitestcorp)"
CORP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  --data @/tmp/login-apitestcorp.json)

CORP_TOKEN=$(echo $CORP_RESPONSE | jq -r '.data.accessToken')

if [ "$CORP_TOKEN" != "null" ] && [ -n "$CORP_TOKEN" ]; then
    echo "✅ 로그인 성공!"
else
    echo "❌ 로그인 실패!"
    echo "$CORP_RESPONSE" | jq '.'
    exit 1
fi
echo ""

# 통합 조회 API 전체
echo "7. 통합 조회 API 전체 조회"
COMBINED_TOTAL=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-work-documents?page=0&size=10" \
  -H "Authorization: Bearer $CORP_TOKEN")
echo "$COMBINED_TOTAL" | jq '{
  success: .success,
  message: .message,
  totalElements: .data.totalElements,
  contentCount: (.data.content | length)
}'
echo ""

# 통합 조회 API 2025년 11월 필터링
echo "8. 통합 조회 API 연도/월 필터링 (2025년 11월)"
COMBINED_FILTERED=$(curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-work-documents?year=2025&month=11&page=0&size=10" \
  -H "Authorization: Bearer $CORP_TOKEN")
echo "$COMBINED_FILTERED" | jq '{
  success: .success,
  message: .message,
  totalElements: .data.totalElements,
  contentCount: (.data.content | length)
}'
echo ""

echo "=== 테스트 완료 ==="
