# API Rest com Spring Security + Microsserviços com RabbitMQ

Projeto acadêmico desenvolvido para a disciplina **Web 3 — IFSP**  
Curso de Análise e Desenvolvimento de Sistemas · CP3025861  
Tag de entrega: `entrega1`

---

## Visão Geral

Este repositório contém três microsserviços Spring Boot independentes que juntos formam um sistema completo de autenticação e comunicação assíncrona via mensageria:

| Serviço | Porta | Função |
|---------|-------|--------|
| `user-service` | 8081 | Autenticação e autorização com JWT + Spring Security |
| `ms-user` | 8081 | Cadastro de usuários com publicação de evento no RabbitMQ |
| `ms-email` | 8082 | Consumidor RabbitMQ que envia e-mail e persiste o registro |

> `user-service` e `ms-user` usam a mesma porta — não devem rodar simultaneamente.

---

## Arquitetura

### user-service (Etapa 1)

```
Cliente (Postman)
     │
     ├── POST /users          → cria usuário com senha BCrypt e role
     ├── POST /users/login    → autentica e retorna token JWT
     └── GET  /users/test/*   → rota protegida por role
              │
         [Spring Security]
         UserAuthenticationFilter → JwtTokenService → UserDetailsServiceImpl
              │
         [MySQL] banco: ms_user
         tabelas: users, roles, users_roles
```

### ms-user + ms-email (Etapa 2)

```
Cliente
  │
  POST /users { name, email }
  │
ms-user (8081) ──────────────────────────────► RabbitMQ
  │  salva em ms_user/TB_USERS                 fila: default.email
  │                                                │
  │                                         ms-email (8082)
  │                                           │  consome a mensagem
  │                                           │  envia e-mail via Gmail SMTP
  │                                           └─► salva em ms_email/TB_EMAILS
  │                                               status: SENT | ERROR
```

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java | 17 |
| Maven | 3.8 |
| MySQL | 8.0 (XAMPP ou standalone) |
| RabbitMQ | CloudAMQP (já configurado) |
| Postman | qualquer versão |

---

## Configuração inicial

### 1. Garantir que o XAMPP MySQL está rodando

Abra o painel XAMPP e clique **Start** na linha do MySQL.  
A senha do root no XAMPP é **vazia** por padrão.

### 2. Criar os bancos de dados

Abra o phpMyAdmin (`http://localhost/phpmyadmin`) ou execute via terminal:

```sql
CREATE DATABASE ms_user;
CREATE DATABASE ms_email;
```

---

## Como executar

Abra um terminal separado para cada serviço. Navegue até a pasta do projeto antes de rodar.

### user-service

```powershell
cd "H:\...\API-Rest-com-Spring-Security\user-service"
mvn spring-boot:run
```

Aguarde a mensagem:
```
Started UserServiceApplication in X seconds
```

### ms-user (apenas Etapa 2 — não rodar junto com user-service)

```powershell
cd "H:\...\API-Rest-com-Spring-Security\ms-user"
mvn spring-boot:run
```

### ms-email

```powershell
cd "H:\...\API-Rest-com-Spring-Security\ms-email"
mvn spring-boot:run
```

---

## user-service — Documentação completa

### Tecnologias utilizadas

- Spring Boot 3.2.4
- Spring Security 6 (stateless, sem sessão)
- JWT — biblioteca `jjwt 0.11.5`
- Spring Data JPA + Hibernate
- Spring AMQP (reservado para Etapa 2)
- MySQL via HikariCP
- BCrypt para hash de senha

### Modelo de dados

**Tabela `users`**

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | BIGINT (PK) | Identificador auto-incremento |
| `email` | VARCHAR (UNIQUE) | E-mail do usuário |
| `password` | VARCHAR | Senha criptografada com BCrypt |

**Tabela `roles`**

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | BIGINT (PK) | Identificador |
| `name` | ENUM | `ROLE_ADMINISTRATOR` ou `ROLE_CUSTOMER` |

**Tabela `users_roles`** (relacionamento N:N)

| Coluna | Tipo |
|--------|------|
| `user_id` | FK → users |
| `role_id` | FK → roles |

### Endpoints

#### `POST /users` — Criar usuário

Público (sem autenticação).

**Requisição:**
```json
{
  "email": "joao@email.com",
  "password": "123456",
  "role": "ROLE_CUSTOMER"
}
```

**Respostas:**

| Status | Descrição |
|--------|-----------|
| `201 Created` | Usuário criado com sucesso |
| `400 Bad Request` | Email inválido ou campo obrigatório ausente |
| `500 Internal Server Error` | Email já cadastrado (duplicado) |

> Valores válidos para `role`: `ROLE_CUSTOMER`, `ROLE_ADMINISTRATOR`  
> Se `role` for omitido, padrão é `ROLE_CUSTOMER`

---

#### `POST /users/login` — Autenticar usuário

Público (sem autenticação).

**Requisição:**
```json
{
  "email": "joao@email.com",
  "password": "123456"
}
```

**Resposta `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSIsImlhdCI6MTc..."
}
```

**Respostas de erro:**

| Status | Descrição |
|--------|-----------|
| `403 Forbidden` | Email não encontrado ou senha incorreta |

> O token tem validade de **24 horas** (`jwt.expiration=86400000` ms).

---

#### `GET /users/test/customer` — Rota protegida (CUSTOMER)

Requer token JWT válido com role `ROLE_CUSTOMER`.

**Header obrigatório:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respostas:**

| Status | Descrição |
|--------|-----------|
| `200 OK` | `"Acesso liberado para role CUSTOMER"` |
| `403 Forbidden` | Token ausente, inválido ou role incorreta |

---

#### `GET /users/test/administrator` — Rota protegida (ADMINISTRATOR)

Mesmo comportamento da rota anterior, mas exige `ROLE_ADMINISTRATOR`.

---

### Fluxo JWT

```
1. POST /users/login  →  Spring autentica com AuthenticationManager
2. BCrypt verifica a senha armazenada no banco
3. JwtTokenService gera o token assinado com HMAC-SHA256
4. Cliente recebe o token e envia em requisições subsequentes

Em cada requisição autenticada:
5. UserAuthenticationFilter extrai o Bearer token do header
6. JwtTokenService valida a assinatura e extrai o email (subject)
7. UserDetailsServiceImpl carrega o usuário do banco
8. SecurityContext é populado → Spring autoriza o acesso
```

### Configuração (`application.properties`)

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/ms_user?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=          # vazio no XAMPP
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

jwt.secret=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
jwt.expiration=86400000

# AMQP desabilitado até Etapa 2
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
```

---

## ms-user — Documentação

### Tecnologias utilizadas

- Spring Boot 3.2.4
- Spring Data JPA + MySQL
- Spring AMQP (RabbitMQ producer)
- Jackson (serialização JSON para a fila)

### Modelo de dados

**Tabela `TB_USERS`**

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `user_id` | UUID (PK) | Identificador único |
| `name` | VARCHAR | Nome do usuário |
| `email` | VARCHAR | E-mail do usuário |

### Endpoint

#### `POST /users` — Cadastrar usuário

**Requisição:**
```json
{
  "name": "João Silva",
  "email": "joao@email.com"
}
```

**Resposta `201 Created`:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@email.com"
}
```

**O que acontece internamente:**
1. Salva o usuário no banco `ms_user`
2. Publica um `EmailDto` na fila RabbitMQ `default.email`
3. O `ms-email` consome a mensagem de forma assíncrona

### Configuração (`application.properties`)

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/ms_user?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update

spring.rabbitmq.addresses=amqps://pweydunl:gY0MMtXVP1sD9qu1MdWFcwtmp35tiEj0@fuji.lmq.cloudamqp.com/pweydunl
broker.queue.email.name=default.email
```

---

## ms-email — Documentação

### Tecnologias utilizadas

- Spring Boot 3.2.4
- Spring Data JPA + MySQL
- Spring AMQP (RabbitMQ consumer)
- Spring Mail (JavaMailSender / Gmail SMTP)
- Jackson

### Modelo de dados

**Tabela `TB_EMAILS`**

| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `email_id` | UUID (PK) | Identificador único |
| `user_id` | UUID | ID do usuário que originou o e-mail |
| `email_from` | VARCHAR | Remetente (conta Gmail configurada) |
| `email_to` | VARCHAR | Destinatário |
| `subject` | VARCHAR | Assunto |
| `text` | TEXT | Corpo do e-mail |
| `send_date_email` | DATETIME | Data e hora do envio |
| `status_email` | ENUM | `SENT` (sucesso) ou `ERROR` (falha no envio) |

### Funcionamento

O `EmailConsumer` escuta a fila `default.email`. Ao receber uma mensagem:

1. Converte o `EmailRecordDto` para um `EmailModel`
2. Chama `EmailService.sendEmail()`
3. `EmailService` tenta enviar via JavaMailSender (Gmail SMTP)
4. Define `statusEmail = SENT` (sucesso) ou `ERROR` (exceção)
5. Persiste o registro no banco `ms_email`

### Configuração (`application.properties`)

```properties
server.port=8082
spring.datasource.url=jdbc:mysql://localhost:3306/ms_email?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update

spring.rabbitmq.addresses=amqps://pweydunl:gY0MMtXVP1sD9qu1MdWFcwtmp35tiEj0@fuji.lmq.cloudamqp.com/pweydunl
broker.queue.email.name=default.email

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=SEU_EMAIL@gmail.com
spring.mail.password=SUA_SENHA_DE_APP_16_CARACTERES
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Como gerar senha de app do Gmail

1. Acesse sua **Conta Google → Segurança**
2. Ative **Verificação em 2 etapas** (obrigatório)
3. Em **Senhas de app**, gere uma senha para "Outro aplicativo"
4. Use essa senha de 16 caracteres no campo `spring.mail.password`

---

## Testando com Postman

### Passo 1 — Criar usuário

```
POST http://localhost:8081/users
Content-Type: application/json

{
  "email": "aluno@ifsp.edu.br",
  "password": "senha123",
  "role": "ROLE_CUSTOMER"
}
```
Esperado: `201 Created`

### Passo 2 — Login

```
POST http://localhost:8081/users/login
Content-Type: application/json

{
  "email": "aluno@ifsp.edu.br",
  "password": "senha123"
}
```
Esperado: `200 OK` com `{ "token": "eyJ..." }`

### Passo 3 — Rota protegida sem token

```
GET http://localhost:8081/users/test/customer
```
Esperado: `403 Forbidden`

### Passo 4 — Rota protegida com token

```
GET http://localhost:8081/users/test/customer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```
Esperado: `200 OK`

---

## Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Access denied for user 'root'` | Senha do MySQL errada | Deixar `spring.datasource.password=` vazio (XAMPP não tem senha) |
| `Port 8081 already in use` | Outro processo usando a porta | Encerrar o processo com `taskkill /PID <pid> /F` |
| `Duplicate entry` para email | E-mail já cadastrado no banco | Usar um e-mail diferente na requisição |
| `403` no `POST /users` | Versão antiga compilada em cache | Rodar `mvn clean spring-boot:run` |
| `Unable to determine Dialect` | MySQL parado | Iniciar o MySQL no painel XAMPP |

---

## Entregas

| Tag | Conteúdo |
|-----|----------|
| `entrega1` | `user-service` JWT funcionando + estrutura base do `ms-email` |

---

## Autor

**Rafael Debroi** — CP3025861  
IFSP — Análise e Desenvolvimento de Sistemas  
Disciplina: Web 3
