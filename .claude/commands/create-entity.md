---
description: 새로운 JPA Entity와 Repository 생성
argument-hint: <EntityName> [fields...]
---

새로운 JPA Entity와 Repository를 생성합니다.

**Entity 이름:** $1

**요구사항:**
1. `src/main/java/com/concrete/buildup/domain/` 패키지에 Entity 클래스 생성
   - `@Entity`, `@Table` 어노테이션 사용
   - Lombok `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` 사용
   - ID는 `@GeneratedValue(strategy = GenerationType.IDENTITY)` 사용
   - 생성일시/수정일시 필드 포함 (`@CreatedDate`, `@LastModifiedDate`)

2. `src/main/java/com/concrete/buildup/repository/` 패키지에 Repository 인터페이스 생성
   - `JpaRepository<Entity, Long>` 상속

**추가 필드:** $ARGUMENTS

모든 필드는 적절한 Java 타입으로 선언하고, JPA 어노테이션을 추가해주세요.