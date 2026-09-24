# API ведомостей

API использует HTTP Basic и доступен под префиксом `/api`. Вне локальной разработки API нужно использовать только по HTTPS. Пароль пользователя в таблице `users` должен храниться в BCrypt. Учётная запись преподавателя должна быть связана с пользователем через `teachers.user_id`. Для роли `DEAN` используется ID пользователя, так как отдельной таблицы деканата нет. API сам определяет пользователя и его роль; ID актёра в запросах не передаётся.

## Посещаемость

Добавить лекцию:

`POST /api/attendance-sheets/{sheetId}/lectures`

```json
{
  "date": "2026-10-05",
  "title": "Введение"
}
```

Ответ `201 Created` содержит `id`, `date` и `title`.

Отметить студента:

`PUT /api/attendance-sheets/{sheetId}/lectures/{lectureId}/attendance/{studentId}`

```json
{
  "status": "PRESENT"
}
```

Допустимые значения `status`: `PRESENT`, `ABSENT`.

Закрыть ведомость: `POST /api/attendance-sheets/{sheetId}/close`.

Архивировать ведомость: `POST /api/attendance-sheets/{sheetId}/archive`.

## Контрольная ведомость

Внести числовой результат:

`PUT /api/control-sheets/{sheetId}/results/{studentId}/numeric`

```json
{
  "score": 88,
  "reason": "Пересдача"
}
```

Внести зачёт или незачёт:

`PUT /api/control-sheets/{sheetId}/results/{studentId}/pass-fail`

```json
{
  "result": "PASS",
  "reason": "Пересдача"
}
```

Допустимые значения `result`: `PASS`, `FAIL`.

Закрыть ведомость: `POST /api/control-sheets/{sheetId}/close`.

Переоткрыть ведомость: `POST /api/control-sheets/{sheetId}/reopen`.

Архивировать ведомость: `POST /api/control-sheets/{sheetId}/archive`.

Для переоткрытия и архивирования можно передать необязательное тело `{"reason":"Причина"}`. Операции без содержимого возвращают `204 No Content`.

## Роли и ошибки

Операции преподавателя доступны роли `TEACHER`, операции переоткрытия и архивирования доступны роли `DEAN`. Домен дополнительно проверяет, что преподаватель ведёт соответствующую часть курса.

Ошибки возвращаются в формате:

```json
{
  "timestamp": "2026-09-24T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Ведомость закрыта",
  "path": "/api/control-sheets/1/close"
}
```

Используемые статусы: `400` для некорректного запроса, `401` для отсутствующей или неверной аутентификации, `403` для недостаточных прав, `404` для отсутствующей ведомости, лекции или студента, `409` для недопустимого состояния ведомости, `422` для некорректного результата.
