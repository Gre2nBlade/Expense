# Expense — Учёт расходов

Проект для учёта личных доходов и расходов.

## Описание

Приложение помогает обычному человеку вести учёт своих финансов:
- добавлять доходы и расходы;
- распределять их по категориям;
- смотреть статистику за период;
- (опционально) считать примерный налог с доходов.

Налоговый расчёт можно включить или выключить — это дополнительная функция.

## Стек

- **Backend:** Java 21
- **Контейнеризация:** Docker
- **Frontend:** HTML / CSS / JavaScript

## Быстрый старт

```bash
# Клонировать репозиторий
git clone https://github.com/Gre2nBlade/expense
cd expense

# Сборка Docker-образа
docker build -t expense-api .

# Запуск контейнера
docker run -d -p 8080:8080 --name expense-api expense-api

# Проверка работы API
curl http://localhost:8080/api/health
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
├── README.md
├── api-plan.md
├── git-conflict.md
├── project-notes.md
├── screenshots/
│   ├── api_request.png
│   ├── docker_logs.png
│   ├── docker_ps.png
│   └── git-network.png
├── api/
│   ├── ExpenseServer.java
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
