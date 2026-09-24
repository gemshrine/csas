# API ведомостей

API использует HTTP Basic и доступен под префиксом `/api`. Вне локальной разработки API нужно использовать только по HTTPS. Пароль пользователя в таблице `users` должен храниться в BCrypt. Учётная запись преподавателя должна быть связана с пользователем через `teachers.user_id`. Для роли `DEAN` используется ID пользователя, так как отдельной таблицы деканата нет. API сам определяет пользователя и его роль; ID актёра в запросах не передаётся.

## Чтение курсов и ведомостей

Получить доступные курсы:

`GET /api/courses`

Преподаватель получает курсы, где он назначен на семинарную или лекционную часть. Деканат получает все курсы. Для каждого курса возвращаются ID ведомостей и признаки доступа к ним.

```json
[
  {
    "courseId": 12,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "semesterName": "Осень 2026",
    "courseStatus": "IN_PROGRESS",
    "controlSheetId": 34,
    "controlSheetStatus": "OPEN",
    "attendanceSheetId": 35,
    "attendanceSheetStatus": "OPEN",
    "canReadControlSheet": true,
    "canReadAttendanceSheet": false
  }
]
```

Получить контрольную ведомость:

`GET /api/control-sheets/{sheetId}`

Доступ преподавателя ограничен семинарной частью его курса. Деканат может читать любую контрольную ведомость.

```json
{
  "id": 34,
  "course": {
    "id": 12,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "semesterName": "Осень 2026",
    "status": "IN_PROGRESS"
  },
  "controlForm": "EXAM",
  "status": "OPEN",
  "gradingScale": {
    "grade5From": 90,
    "grade4From": 80,
    "grade3From": 70
  },
  "students": [
    {
      "studentId": 21,
      "studentName": "Иванов Иван Иванович",
      "numericScore": null,
      "passFail": null
    }
  ]
}
```

Получить ведомость посещаемости:

`GET /api/attendance-sheets/{sheetId}`

Доступ преподавателя ограничен лекционной частью его курса. Деканат может читать любую ведомость посещаемости. Для каждой лекции возвращается отметка каждого студента. `status: null` означает, что посещение ещё не отмечено; `PRESENT` и `ABSENT` являются сохранёнными отметками.

```json
{
  "id": 35,
  "course": {
    "id": 12,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "semesterName": "Осень 2026",
    "status": "IN_PROGRESS"
  },
  "status": "OPEN",
  "students": [
    {
      "studentId": 21,
      "studentName": "Иванов Иван Иванович"
    }
  ],
  "lectures": [
    {
      "id": 51,
      "date": "2026-10-05",
      "title": "Введение",
      "attendance": [
        {
          "studentId": 21,
          "studentName": "Иванов Иван Иванович",
          "status": null
        }
      ]
    }
  ]
}
```

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
