# GitHub Actions CI/CD 설정 가이드

Build-Up 프로젝트의 GitHub Actions CI/CD 파이프라인 설정 가이드입니다.

## 📋 목차

1. [CI 파이프라인 개요](#ci-파이프라인-개요)
2. [필수 설정](#필수-설정)
3. [선택적 설정](#선택적-설정)
4. [GitHub Secrets 등록 방법](#github-secrets-등록-방법)
5. [CD 파이프라인 설정](#cd-파이프라인-설정)

---

## CI 파이프라인 개요

### 자동 실행 조건

- **Pull Request**: `main`, `develop` 브랜치로의 PR 생성 시
- **Push**: `main`, `develop` 브랜치에 직접 푸시 시

### CI 작업 흐름

```
1. 코드 체크아웃
2. Java 21 설정
3. 테스트 실행 (H2 메모리 DB 사용)
4. 애플리케이션 빌드
5. JAR 아티팩트 업로드
6. Docker 이미지 빌드 (PR인 경우)
7. Docker 컨테이너 테스트 (PR인 경우)
8. Discord 알림 (선택사항)
```

---

## 필수 설정

### ✅ 자동 제공되는 테스트 환경 변수

CI 파이프라인은 **별도의 GitHub Secrets 설정 없이도** 정상 동작합니다.

테스트 및 빌드에 필요한 환경 변수는 자동으로 제공됩니다:

```yaml
JWT_SECRET: test-secret-key-min-256-bits-long-for-hs256-algorithm-test
ENCRYPTION_KEY: 0123456789abcdef0123456789abcdef
```

> ⚠️ **주의**: 위 값들은 테스트 전용이며, **절대 운영 환경에서 사용하면 안 됩니다**.

---

## 선택적 설정

### 1. Discord 알림 (권장)

빌드 결과를 Discord로 받고 싶은 경우 설정합니다.

**설정하지 않아도 CI는 정상 동작합니다.**

#### Discord Webhook 생성

1. Discord 서버 설정 → 연동 → Webhooks
2. "새 웹훅" 클릭
3. 웹훅 이름 설정 (예: "Build-Up CI")
4. 채널 선택
5. "웹훅 URL 복사" 클릭

#### GitHub에 Webhook 등록

**Settings** → **Secrets and variables** → **Actions** → **New repository secret**

- Name: `DISCORD_WEBHOOK`
- Value: 복사한 Webhook URL

### 2. 운영 환경 배포 (CD 파이프라인 사용 시)

CD 파이프라인을 사용하여 실제 서버에 배포하려는 경우 아래 Secrets가 필요합니다.

#### 필수 Secrets

| Secret 이름 | 설명 | 예시 |
|------------|------|------|
| `JWT_SECRET` | 운영용 JWT 비밀키 | `openssl rand -hex 32` 결과 |
| `ENCRYPTION_KEY` | 운영용 암호화 키 | `openssl rand -hex 32` 결과 |
| `SPRING_DATASOURCE_URL` | 운영 DB URL | `jdbc:mysql://...` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | `buildup_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `your_password` |
| `ADMIN_USERNAME` | 관리자 계정명 | `admin` |
| `ADMIN_PASSWORD` | 관리자 비밀번호 | `your_password` |
| `AWS_S3_ENABLED` | S3 활성화 여부 | `true` |
| `AWS_S3_BUCKET` | S3 버킷 이름 | `buildup-files` |
| `AWS_REGION` | AWS 리전 | `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | AWS 비밀 키 | `...` |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 | `your_username` |
| `DOCKERHUB_TOKEN` | Docker Hub 토큰 | `dckr_pat_...` |
| `AWS_EC2_HOST` | EC2 서버 IP | `3.37.234.173` |
| `AWS_EC2_USERNAME` | SSH 사용자명 | `ubuntu` |
| `AWS_EC2_SSH_KEY` | SSH Private Key | `-----BEGIN...` |

---

## GitHub Secrets 등록 방법

### 1. Repository Settings 접속

GitHub Repository → **Settings** → **Secrets and variables** → **Actions**

### 2. New repository secret 클릭

### 3. Secret 등록

- **Name**: Secret 이름 (위 표 참고)
- **Secret**: 실제 값 입력

### 4. 반복

필요한 모든 Secret을 등록할 때까지 반복합니다.

---

## 환경 변수 생성 방법

### JWT Secret & Encryption Key 생성

```bash
# JWT Secret 생성 (32바이트)
openssl rand -hex 32

# Encryption Key 생성 (32바이트)
openssl rand -hex 32
```

### Docker Hub Token 생성

1. [Docker Hub](https://hub.docker.com/) 로그인
2. **Account Settings** → **Security** → **New Access Token**
3. Token 생성 후 복사 (재확인 불가하므로 안전하게 보관)

### AWS Credentials 확인

```bash
# AWS CLI로 확인
aws configure list

# 또는 ~/.aws/credentials 파일 확인
cat ~/.aws/credentials
```

---

## CD 파이프라인 설정

CD 파이프라인(`.github/workflows/cd.yml`)은 브랜치에 푸시될 때 자동으로 실행됩니다.

### 자동 배포 브랜치

| 브랜치 | 환경 | Docker 태그 | Spring 프로파일 |
|--------|------|-------------|-----------------|
| `main` | Production | `latest` | `prod` |
| `develop` | Staging | `develop` | `dev` |

### 배포 흐름

```
1. 코드 체크아웃
2. Java 21 설정
3. 애플리케이션 빌드
4. Docker 이미지 빌드 및 Push
5. .env 파일 생성
6. EC2 서버에 .env 파일 전송
7. EC2에서 Docker Compose로 배포
8. 헬스체크
9. Discord 알림
```

### 수동 배포

GitHub Actions 탭에서 **CD Pipeline** → **Run workflow**를 통해 수동으로 배포할 수 있습니다.

---

## 트러블슈팅

### 1. 테스트 실패: "JWT_SECRET is required"

**원인**: 환경 변수가 전달되지 않음

**해결**: CI 파일의 환경 변수 설정 확인 (자동으로 설정되어야 함)

### 2. Docker 빌드 실패

**원인**: Docker Hub 자격 증명 문제

**해결**: 
- CD 파이프라인에서만 Docker Hub 로그인 필요
- CI 파이프라인은 로컬 빌드만 수행

### 3. Discord 알림이 오지 않음

**원인**: `DISCORD_WEBHOOK` Secret 미설정

**해결**: 
- Discord Webhook URL을 GitHub Secrets에 등록
- 또는 알림 없이 CI 계속 사용 (선택사항)

### 4. 배포 실패: SSH 연결 오류

**원인**: EC2 SSH 키 또는 호스트 정보 오류

**해결**:
- `AWS_EC2_HOST`: 정확한 IP 주소 확인
- `AWS_EC2_SSH_KEY`: Private Key 전체 내용 복사 (BEGIN/END 포함)
- EC2 보안 그룹에서 GitHub Actions IP 허용 (또는 전체 허용)

---

## 참고 자료

- [GitHub Actions 공식 문서](https://docs.github.com/en/actions)
- [Docker Hub Token 생성](https://docs.docker.com/docker-hub/access-tokens/)
- [AWS IAM 사용자 생성](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html)
- [Discord Webhook 설정](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks)

---

## 요약

### ✅ CI만 사용 (PR 테스트)
- **필요한 설정**: 없음 (자동 동작)
- **선택적 설정**: `DISCORD_WEBHOOK`

### 🚀 CD까지 사용 (자동 배포)
- **필요한 설정**: 위의 모든 운영 환경 Secrets
- **추가 설정**: Docker Hub, AWS EC2, SSH 키

**처음에는 CI만 설정하고, 배포가 필요할 때 CD를 추가하는 것을 권장합니다.**

