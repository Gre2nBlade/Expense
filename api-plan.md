# API Plan — Expense

План REST API для проекта учёта расходов.

Базовый URL: `/api`

---

## 1. Транзакции

### Список транзакций
```
GET /api/transactions
```
Query-параметры:
- `type` — `income` | `expense`
- `category_id`
- `from`, `to` — даты (YYYY-MM-DD)
- `limit`, `offset`

### Создать
```
POST /api/transactions
```
```json
{
  "type": "expense",
  "amount": 1500.00,
  "category_id": 3,
  "date": "2026-09-15",
  "comment": "Обед"
}
```

### Получить / Обновить / Удалить
```
GET    /api/transactions/{id}
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
```

---

## 2. Категории

```
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
```

Тело при создании:
```json
{
  "name": "Продукты",
  "type": "expense",
  "color": "#4CAF50"
}
```

---

## 3. Настройки (включая налоги)

```
GET /api/settings
PUT /api/settings
```

Пример ответа / тела:
```json
{
  "tax_enabled": false,
  "tax_rate": 0.13
}
```

- `tax_enabled` — включён ли расчёт налогов
- `tax_rate` — ставка (по умолчанию 0.13)

---

## 4. Налоги (работают только если tax_enabled = true)

### Рассчитать налог за период
```
GET /api/taxes/calculate?from=2026-09-01&to=2026-09-30
```

Ответ:
```json
{
  "enabled": true,
  "period": {
    "from": "2026-09-01",
    "to": "2026-09-30"
  },
  "total_income": 85000.00,
  "tax_rate": 0.13,
  "tax_amount": 11050.00
}
```

Если налоги выключены — возвращать:
```json
{
  "enabled": false,
  "message": "Расчёт налогов отключён"
}
```

---

## 5. Отчёты

### Сводка за период
```
GET /api/reports/summary?from=2026-09-01&to=2026-09-30
```

Ответ:
```json
{
  "total_income": 85000,
  "total_expense": 42300,
  "balance": 42700,
  "by_category": [
    { "name": "Продукты", "amount": 12500 },
    { "name": "Транспорт", "amount": 4800 }
  ],
  "tax": {
    "enabled": true,
    "amount": 11050
  }
}
```

---

## 6. Служебные

```
GET /api/health     — проверка, что сервер жив
GET /               — отдаёт главную страницу (HTML)
```

---

## Общие правила

- Суммы — числа с двумя знаками после запятой
- Даты — `YYYY-MM-DD`
- Ошибки:
```json
{
  "detail": "Описание ошибки"
}
```
- Коды: 200, 201, 400, 404, 500

---