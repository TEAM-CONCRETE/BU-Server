#!/bin/bash

echo "=== API 테스트 시작 ==="
echo ""

# 현장 관리자 로그인
echo "1. 현장 관리자 로그인 (frontmgr)"
MANAGER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"frontmgr","password":"Admin123!@","rememberMe":false}' | jq -r '.data.accessToken')

if [ "$MANAGER_TOKEN" != "null" ] && [ -n "$MANAGER_TOKEN" ]; then
    echo "✅ 로그인 성공! Token: ${MANAGER_TOKEN:0:20}..."
else
    echo "❌ 로그인 실패!"
    exit 1
fi
echo ""

# 현장 ID 가져오기
echo "2. 현장 ID 확인"
SITE_ID=$(curl -s -X GET http://localhost:8080/api/v1/sites \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq -r '.data[0].siteId')
echo "✅ 현장 ID: $SITE_ID"
echo ""

# 작업일보 전체 조회
echo "3. 작업일보 전체 조회 (페이지네이션)"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/work-reports?page=0&size=5" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '{
    success: .success,
    message: .message,
    totalElements: .data.totalElements,
    totalPages: .data.totalPages,
    pageSize: .data.pageSize,
    contentCount: (.data.content | length)
  }'
echo ""

# 작업일보 연도/월 필터링 (2025년 1월)
echo "4. 작업일보 연도/월 필터링 (2025년 1월)"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/work-reports?year=2025&month=1&page=0&size=5" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '{
    success: .success,
    message: .message,
    totalElements: .data.totalElements,
    contentCount: (.data.content | length),
    firstItem: .data.content[0]
  }'
echo ""

# 안전교육일지 전체 조회
echo "5. 안전교육일지 전체 조회"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-education-logs" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '{
    success: .success,
    message: .message,
    dataCount: (.data | length),
    firstItem: .data[0]
  }'
echo ""

# 안전교육일지 연도/월 필터링 (2025년 1월)
echo "6. 안전교육일지 연도/월 필터링 (2025년 1월)"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-education-logs?year=2025&month=1" \
  -H "Authorization: Bearer $MANAGER_TOKEN" | jq '{
    success: .success,
    message: .message,
    dataCount: (.data | length)
  }'
echo ""

# 기업 관리자 로그인
echo "7. 기업 관리자 로그인 (frontcorp)"
CORP_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"frontcorp","password":"Admin123!@","rememberMe":false}' | jq -r '.data.accessToken')

if [ "$CORP_TOKEN" != "null" ] && [ -n "$CORP_TOKEN" ]; then
    echo "✅ 로그인 성공! Token: ${CORP_TOKEN:0:20}..."
else
    echo "❌ 로그인 실패!"
    exit 1
fi
echo ""

# 통합 조회 API 전체 조회
echo "8. 통합 조회 API 전체 조회 (기업 관리자용)"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-work-documents?page=0&size=5" \
  -H "Authorization: Bearer $CORP_TOKEN" | jq '{
    success: .success,
    message: .message,
    totalElements: .data.totalElements,
    totalPages: .data.totalPages,
    contentCount: (.data.content | length),
    firstItem: .data.content[0]
  }'
echo ""

# 통합 조회 API 연도/월 필터링 (2025년 1월)
echo "9. 통합 조회 API 연도/월 필터링 (2025년 1월)"
curl -s -X GET "http://localhost:8080/api/v1/$SITE_ID/safety-work-documents?year=2025&month=1&page=0&size=5" \
  -H "Authorization: Bearer $CORP_TOKEN" | jq '{
    success: .success,
    message: .message,
    totalElements: .data.totalElements,
    contentCount: (.data.content | length)
  }'
echo ""

echo "=== API 테스트 완료 ==="
