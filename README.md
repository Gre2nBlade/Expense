# Expense — Учёт расходов

Проект для учёта личных доходов и расходов.

## Описание

Приложение помогает обычному человеку вести учёт своих финансов. Серверная часть реализована на Java и работает в связке с реляционной базой данных PostgreSQL в Docker Compose. Реализован модуль транзакций согласно плану [`api-plan.md`](api-plan.md).

## Стек

- **Backend:** Java 21 (API)
- **База данных:** PostgreSQL 16
- **Контейнеризация:** Docker, Docker Compose
- **Frontend:** HTML / CSS / JavaScript

## Быстрый старт

### Клонирование и запуск

```bash
# Клонировать репозиторий
git clone https://github.com/Gre2nBlade/expense
cd expense

# Сборка и запуск сервисов (API + PostgreSQL)
docker compose up -d --build

# Проверка статуса запущенных контейнеров
docker compose ps
```

Сервер API доступен по адресу: `http://localhost:8080`.  
База данных PostgreSQL доступна на порту: `5432`.

### Остановка сервисов

```bash
docker compose down
```

## Примеры запросов к API (CRUD транзакций)

Для удобного тестирования API подготовлена Postman-коллекция: [`postman_collection.json`](postman_collection.json).

### 1. Проверка состояния сервиса (Healthcheck)

```bash
curl http://localhost:8080/api/health
```

### 2. Создание транзакции (POST)

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "expense",
    "amount": 1500.00,
    "category_id": 3,
    "date": "2026-09-15",
    "comment": "Обед"
  }'
```

### 3. Получение всех транзакций (GET)

```bash
curl http://localhost:8080/api/transactions
```

### 4. Получение транзакции по ID (GET)

```bash
curl http://localhost:8080/api/transactions/1
```

### 5. Обновление транзакции (PUT)

```bash
curl -X PUT http://localhost:8080/api/transactions/1 \
  -H "Content-Type: application/json" \
  -d '{
    "type": "expense",
    "amount": 1650.00,
    "category_id": 3,
    "date": "2026-09-15",
    "comment": "Бизнес-ланч"
  }'
```

### 6. Удаление транзакции (DELETE)

```bash
curl -X DELETE http://localhost:8080/api/transactions/1
```

## Проверка персистентности данных

Данные PostgreSQL сохраняются в именованном томе `db_data` (`/var/lib/postgresql/data`).  
После перезапуска контейнеров данные не теряются:

```bash
# 1. Добавить транзакцию
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "income",
    "amount": 85000.00,
    "category_id": 1,
    "date": "2026-09-01",
    "comment": "Зарплата"
  }'

# 2. Перезапустить сервисы
docker compose down
docker compose up -d

# 3. Проверить, что созданная транзакция сохранилась
curl http://localhost:8080/api/transactions
```

## Основные возможности

| Функция                | Описание                                      |
|------------------------|-----------------------------------------------|
| Добавление расходов    | Сумма, категория, дата, комментарий           |
| Учёт доходов           | Зарплата, подработка и др.                    |
| Категории              | Свои категории для доходов и расходов         |
| Отчёты                 | Сводка по месяцам и категориям                |
| Налоги (опционально)   | Можно включить/выключить расчёт налога        |

## Структура проекта (примерная)

```
expense/
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── postman_collection.json
├── README.md
├── api-plan.md
├── git-conflict.md
├── project-notes.md
├── screenshots/
│   ├── api_request.png
│   ├── crud_check.png
│   ├── docker_compose_ps.png
│   ├── docker_logs.png
│   ├── docker_ps.png
│   ├── git-network.png
│   └── persistence_check.png
├── api/
│   ├── ExpenseServer.java
│   ├── lib/
│   │   └── postgresql-42.7.3.jar
│   ├── handlers/
│   │   ├── CategoryHandler.java
│   │   ├── HealthHandler.java
│   │   ├── TaxHandler.java
│   │   └── TransactionHandler.java
│   ├── models/
│   │   ├── Category.java
│   │   ├── Settings.java
│   │   └── Transaction.java
│   ├── services/
│   │   ├── TaxCalculator.java
│   │   └── TransactionService.java
│   └── storage/
│       └── Database.java
├── static/
│   ├── script.js
│   └── style.css
└── templates/
    └── index.html
```

## Автор

Gre2nBlade
