# API Rest com Spring Security + Microsserviços com RabbitMQ

Projeto acadêmico desenvolvido para a disciplina **Web 3 — IFSP**  
Curso de Análise e Desenvolvimento de Sistemas · CP3025861  
Tag de entrega: `entrega4`

---

## Visão Geral

Este repositório contém três microsserviços Spring Boot independentes que juntos formam um sistema completo de autenticação e comunicação assíncrona via mensageria:

| Serviço | Porta | Função |
|---------|-------|--------|
| `user-service` | 8081 | Autenticação JWT + geração de OTP + publicação no RabbitMQ |
| `ms-user` | 8081 | Cadastro de usuários com publicação de evento no RabbitMQ |
| `ms-email` | 8082 | Consumidor RabbitMQ que envia e-mail e persiste o registro |

> `user-service` e `ms-user` usam a mesma porta — não devem rodar simultaneamente.

---

## Arquitetura

### user-service (Etapa 1 + Etapa 2)

```
Cliente (Postman)
     │
     ├── POST /users              → cria usuário com senha BCrypt e role
     ├── POST /users/login        → autentica e retorna token JWT
     ├── GET  /users/test/*       → rota protegida por role
     └── POST /auth/request-code → gera OTP, armazena em cache, publica no RabbitMQ
              │                          │
         [Spring Security]        [CodigoCacheService]
         UserAuthenticationFilter  ConcurrentHashMap — TTL 5 min
         → JwtTokenService                │
         → UserDetailsServiceImpl  [UserProducer]
              │                    RabbitTemplate → fila: default.email
         [MySQL] banco: ms_user           │
         tabelas: users, roles,    [ms-email] consome e envia e-mail
                  users_roles
```

### ms-user + ms-email

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

Abra um terminal separado para cada serviço.

### user-service

```powershell
cd "H:\...\API-Rest-com-Spring-Security\user-service"
mvn spring-boot:run
```

Aguarde:
```
Started UserServiceApplication in X seconds
```

### ms-user (não rodar junto com user-service)

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
- Spring AMQP (RabbitMQ producer)
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
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvQGVtYWlsLmNvbSIsImlhdCI6MTc4MDkyNzc4NiwiZXhwIjoxNzgxMDE0MTg2fQ.uQ6nS-Kwxj2nwwMSgCk4ckhnjGlZIWKMOOeHNxjYyDM"
}
```

**Respostas de erro:**

| Status | Descrição |
|--------|-----------|
| `403 Forbidden` | Email não encontrado ou senha incorreta |

> O token tem validade de **24 horas** (`jwt.expiration=86400000` ms).

---

#### `POST /auth/request-code` — Solicitar código OTP *(Etapa 2)*

Público (sem autenticação). Não retorna o código gerado na resposta.

**Requisição:**
```json
{
  "email": "joao@email.com"
}
```

**Resposta `200 OK`:** corpo vazio.

**O que acontece internamente:**
1. Busca o e-mail no banco. Se não existir, cria um usuário temporário com `ROLE_CUSTOMER` e senha aleatória.
2. Gera um código numérico aleatório de 6 dígitos.
3. Armazena o código no `CodigoCacheService` (expira em 5 minutos).
4. Publica um `EmailDto` na fila `default.email` do RabbitMQ com assunto `"Seu código de acesso"`.
5. Retorna `200 OK` — o código **não** é exposto na resposta.

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

### Cache de OTP — CodigoCacheService

Implementado com `ConcurrentHashMap` (thread-safe, sem dependência externa).

| Detalhe | Valor |
|---------|-------|
| Estrutura | `ConcurrentHashMap<String, Entry>` |
| Chave | E-mail do usuário |
| Valor | Código de 6 dígitos + timestamp de expiração |
| TTL | 5 minutos (300 segundos) |
| Limpeza | `@Scheduled(fixedRate = 60_000)` — varre entradas expiradas a cada minuto |

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

spring.rabbitmq.addresses=amqps://usuario:senha@host.rmq.cloudamqp.com/vhost
broker.queue.email.name=default.email
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

spring.rabbitmq.addresses=amqps://usuario:senha@host.rmq.cloudamqp.com/vhost
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

spring.rabbitmq.addresses=amqps://usuario:senha@host.rmq.cloudamqp.com/vhost
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

### Etapa 1 — Autenticação JWT

**Passo 1 — Criar usuário**

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

**Passo 2 — Login**

```
POST http://localhost:8081/users/login
Content-Type: application/json

{
  "email": "aluno@ifsp.edu.br",
  "password": "senha123"
}
```
Esperado: `200 OK` com `{ "token": "eyJ..." }`

**Passo 3 — Rota protegida sem token**

```
GET http://localhost:8081/users/test/customer
```
Esperado: `403 Forbidden`

**Passo 4 — Rota protegida com token**

```
GET http://localhost:8081/users/test/customer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```
Esperado: `200 OK`

---

### Etapa 2 — Código OTP via RabbitMQ

**Passo 5 — Solicitar código (e-mail existente)**

```
POST http://localhost:8081/auth/request-code
Content-Type: application/json

{
  "email": "aluno@ifsp.edu.br"
}
```
Esperado: `200 OK` (corpo vazio — o código não é retornado)

**Passo 6 — Solicitar código (e-mail não cadastrado)**

```
POST http://localhost:8081/auth/request-code
Content-Type: application/json

{
  "email": "novo@email.com"
}
```
Esperado: `200 OK` — um usuário temporário é criado automaticamente.

**Verificação no CloudAMQP:**  
Acesse o painel do CloudAMQP → **RabbitMQ Manager** → aba **Queues** → fila `default.email`.  
A mensagem publicada deve aparecer com o payload JSON:
```json
{
  "emailTo": "aluno@ifsp.edu.br",
  "subject": "Seu código de acesso",
  "text": "Seu código de acesso é: 384712",
  "userId": "..."
}
```

---

## Erros comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `Access denied for user 'root'` | Senha do MySQL errada | Deixar `spring.datasource.password=` vazio (XAMPP não tem senha) |
| `Port 8081 already in use` | Outro processo usando a porta | Encerrar o processo com `taskkill /PID <pid> /F` |
| `Duplicate entry` para email | E-mail já cadastrado no banco | Usar um e-mail diferente na requisição |
| `403` no `POST /users` | Versão antiga compilada em cache | Rodar `mvn clean spring-boot:run` |
| `Unable to determine Dialect` | MySQL parado | Iniciar o MySQL no painel XAMPP |
| `Connection refused` no RabbitMQ | URI do CloudAMQP incorreta | Verificar `spring.rabbitmq.addresses` no `application.properties` |

---

## Frontend — Node.js (Etapa 3 e 4)

### Pré-requisitos adicionais

| Ferramenta | Versão mínima |
|------------|---------------|
| Node.js | 18 |
| npm | 9 |

### Instalação

```powershell
cd frontend
npm install
```

### Executar

```powershell
npm start
```

O frontend ficará disponível em **http://localhost:3000**.

### Rotas do frontend

| Rota | Método | Descrição |
|------|--------|-----------|
| `GET /` | — | Página inicial — solicitar código OTP |
| `POST /send-code` | form | Envia código OTP para o e-mail |
| `GET /verify` | — | Página de verificação do código |
| `POST /verify-code` | JSON | Verifica o código e retorna JWT |
| `GET /register` | — | Página de cadastro de nome e cargo |
| `POST /register` | JSON | Salva nome/role via `/users/update-profile` |
| `GET /dashboard` | — | Dashboard protegido |
| `GET /users/me` | proxy | Retorna perfil do usuário autenticado |
| `GET /api/protected` | proxy | Chama `/users/test/customer` no backend |

### Fluxo completo

```
1. Acesse http://localhost:3000
2. Digite seu e-mail → receba o código OTP por e-mail
3. Digite o código → JWT é gerado e armazenado no sessionStorage
4. Preencha nome e escolha cargo → perfil salvo no banco
5. Dashboard → teste endpoints protegidos e visualize seu perfil
```

---

## Etapa 4 — Endpoints adicionais no user-service

### `POST /users/update-profile` — Atualizar perfil

Requer token JWT válido (qualquer role autenticada).

**Requisição:**
```json
{
  "name": "João Silva",
  "role": "ROLE_CUSTOMER"
}
```

**Resposta `200 OK`:**
```json
{
  "email": "joao@email.com",
  "name": "João Silva",
  "roles": ["ROLE_CUSTOMER"]
}
```

---

### `GET /users/me` — Perfil do usuário autenticado

Requer token JWT válido.

**Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Resposta `200 OK`:**
```json
{
  "email": "joao@email.com",
  "name": "João Silva",
  "roles": ["ROLE_CUSTOMER"]
}
```

---

## Entregas

| Tag | Conteúdo |
|-----|----------|
| `entrega1` | `user-service` JWT funcionando + estrutura base do `ms-email` |
| `entrega2` | `user-service` com OTP, cache (`CodigoCacheService`) e producer RabbitMQ (`/auth/request-code`) |
| `entrega3` | Frontend Node.js com fluxo OTP completo (index, verify, dashboard) |
| `entrega4` | Cadastro de nome/cargo (`register.html`), `update-profile`, `GET /me`, `dashboard.html` completo, README e scripts |

---

## Autor

**Rafael Debroi** — CP3025861  
IFSP — Análise e Desenvolvimento de Sistemas  
Disciplina: Web 3
