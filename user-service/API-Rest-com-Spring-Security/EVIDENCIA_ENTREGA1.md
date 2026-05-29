# Evidência — Etapa 1

**Aluno:** Rafael Debroi — CP3025861  
**Disciplina:** Web 3 — IFSP  
**Repositório:** https://github.com/FaelDebroi/API-Rest-com-Spring-Security  
**Tag:** `entrega1`  
**Data:** 2026-05-29

---

## Entregável 1 — User Service (Spring Security + JWT)

**Repositório Git com tag `entrega1`:**
```
https://github.com/FaelDebroi/API-Rest-com-Spring-Security/tree/entrega1/user-service
```

**Estrutura do projeto:**
```
user-service/
├── pom.xml
└── src/main/java/com/ms/userservice/
    ├── UserServiceApplication.java
    ├── configs/
    │   └── SecurityConfiguration.java      ← Spring Security stateless + AntPathRequestMatcher
    ├── controllers/
    │   └── UserController.java             ← POST /users, POST /users/login, GET /users/test/*
    ├── dtos/
    │   ├── CreateUserDto.java
    │   ├── LoginUserDto.java
    │   └── RecoveryJwtTokenDto.java
    ├── entities/
    │   ├── Role.java
    │   ├── RoleName.java                   ← enum: ROLE_ADMINISTRATOR, ROLE_CUSTOMER
    │   └── User.java
    ├── filters/
    │   └── UserAuthenticationFilter.java   ← valida JWT em cada requisição
    ├── repositories/
    │   ├── RoleRepository.java
    │   └── UserRepository.java
    └── services/
        ├── JwtTokenService.java            ← gera e valida tokens HMAC-SHA256
        ├── UserDetailsImpl.java
        ├── UserDetailsServiceImpl.java     ← carrega usuário do banco para o Spring Security
        └── UserService.java
```

**Banco de dados:** MySQL (XAMPP) — `ms_user`  
**Porta:** 8081

---

## Entregável 2 — Email Service (estrutura base)

**Repositório Git com tag `entrega1`:**
```
https://github.com/FaelDebroi/API-Rest-com-Spring-Security/tree/entrega1/ms-email
```

**Estrutura do projeto:**
```
ms-email/
├── pom.xml
└── src/main/java/com/ms/email/
    ├── MsEmailApplication.java
    ├── configs/
    │   └── RabbitMQConfig.java
    ├── dtos/
    │   └── EmailRecordDto.java
    ├── enums/
    │   └── StatusEmail.java                ← SENT | ERROR
    ├── models/
    │   └── EmailModel.java
    ├── repositories/
    │   └── EmailRepository.java
    └── services/
        └── EmailService.java
```

**Banco de dados:** MySQL (XAMPP) — `ms_email`  
**Porta:** 8082

---

## Entregável 3 — Console do User Service + Teste via cliente HTTP

### Startup do User Service

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.4)

INFO  c.ms.userservice.UserServiceApplication  : Starting UserServiceApplication
INFO  com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
INFO  com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection (MySQL ms_user)
INFO  com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
INFO  o.s.s.web.DefaultSecurityFilterChain     : Will secure any request with
      [DisableEncodeUrlFilter, SecurityContextHolderFilter, HeaderWriterFilter,
       UserAuthenticationFilter, AnonymousAuthenticationFilter, AuthorizationFilter]
INFO  o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8081 (http)
INFO  c.ms.userservice.UserServiceApplication  : Started UserServiceApplication in 2.74 seconds
```

---

### Teste 1 — POST /users (criar usuário)

**Requisição:**
```
POST http://localhost:8081/users
Content-Type: application/json

{
  "email": "entrega1@ifsp.edu.br",
  "password": "ifsp2026",
  "role": "ROLE_CUSTOMER"
}
```

**Resposta:**
```
HTTP Status: 201 Created
(corpo vazio)
```

---

### Teste 2 — POST /users/login (obter token JWT)

**Requisição:**
```
POST http://localhost:8081/users/login
Content-Type: application/json

{
  "email": "entrega1@ifsp.edu.br",
  "password": "ifsp2026"
}
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlbnRyZWdhMUBpZnNwLmVkdS5iciIsImlhdCI6MTc4MDA0MDM1MSwiZXhwIjoxNzgwMTI2NzUxfQ.IXTOcaWe81fWw4QADKTOmPlmei8GYwIuAoObOT09Pn8"
}
```

---

### Teste 3 — GET /users/test/customer SEM token

**Requisição:**
```
GET http://localhost:8081/users/test/customer
```

**Resposta:**
```
HTTP Status: 403 Forbidden
```

---

### Teste 4 — GET /users/test/customer COM token

**Requisição:**
```
GET http://localhost:8081/users/test/customer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Resposta:**
```
HTTP Status: 200 OK
"Acesso liberado para role CUSTOMER"
```

---

## Critérios de aceite — verificação

| Critério | Status |
|----------|--------|
| User Service sobe sem erros e conecta ao banco ms_user | ✅ |
| Endpoint POST /users cria registro nas tabelas users e roles | ✅ |
| Endpoint POST /users/login retorna token JWT válido | ✅ |
| GET /users/test/customer retorna 403 sem token | ✅ |
| GET /users/test/customer retorna 200 com token válido | ✅ |
| Email Service sobe na porta 8082 sem erros | ✅ |
| Código no repositório Git com tag entrega1 | ✅ |
