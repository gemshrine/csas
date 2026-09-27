# REST API

Базовый префикс: `/api`. Тела запросов и ответов — JSON. Вне локальной разработки API доступен только по HTTPS.

## Аутентификация и авторизация

- API использует **HTTP Basic**.
- Пароль пользователя в таблице `users` хранится в **BCrypt**.
- Учётная запись преподавателя связана с пользователем через `teachers.user_id`.
- Для роли `DEAN` используется ID пользователя, отдельной таблицы деканата нет.
- API **сам определяет пользователя и его роль** — ID актёра в запросах не передаётся.
- Операции преподавателя доступны роли `TEACHER`, операции переоткрытия и архивирования — роли `DEAN`. Домен дополнительно проверяет, что преподаватель ведёт соответствующую часть курса.
- Справочники (`/api/groups`, `/api/students`, `/api/teachers`, `/api/disciplines`, `/api/semesters`) доступны только роли `DEAN`. Студент и преподаватель получают 403.

## Единый формат ошибки

Все маршруты `/api/**` возвращают один и тот же JSON, в том числе 401 и 403 от Spring Security:

```json
{
  "timestamp": "2026-09-24T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Ведомость закрыта",
  "path": "/api/control-sheets/1/close"
}
```

Сообщения Bean Validation собираются в `message` через `; `. Отдельного поля `fields` нет.

## Используемые HTTP-коды

| Код | Когда |
|-----|-------|
| 200 | Успешное чтение или обновление |
| 201 | Создание |
| 204 | Удаление или операция без содержимого |
| 400 | Невалидное тело, несуществующая группа у студента, некорректные даты семестра |
| 401 | Отсутствующая или неверная аутентификация |
| 403 | Недостаточно прав |
| 404 | Запись с таким id нет |
| 409 | Нарушена уникальность или нельзя удалить из-за связей; недопустимое состояние ведомости |
| 422 | Некорректный результат (для ведомостей) |

---

# Справочники

## Группы `/api/groups`

| Метод | Путь | Ответ |
|-------|------|-------|
| GET | `/api/groups` | 200, список |
| GET | `/api/groups/{id}` | 200 или 404 |
| POST | `/api/groups` | 201 |
| PUT | `/api/groups/{id}` | 200 или 404 |
| DELETE | `/api/groups/{id}` | 204, 404 или 409 |

Пример создания:

```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ИВТ-41-1",
    "yearOfAdmission": 2022,
    "specialty": "Информатика и вычислительная техника",
    "course": 4,
    "educationForm": "BUDGET",
    "studyForm": "FULL_TIME"
  }'
```

Удаление запрещено, если у группы есть студенты или курсы.

`educationForm`: `BUDGET`, `CONTRACT`.  
`studyForm`: `FULL_TIME`, `PART_TIME`, `EXTRAMURAL`.

## Студенты `/api/students`

| Метод | Путь | Ответ |
|-------|------|-------|
| GET | `/api/students` | 200, список |
| GET | `/api/students/{id}` | 200 или 404 |
| POST | `/api/students` | 201 |
| PUT | `/api/students/{id}` | 200 или 404 |
| DELETE | `/api/students/{id}` | 204, 404 или 409 |

`groupId` может быть `null`. Если указан несуществующий id — 400. В ответе группа кратко: `{ "id", "name" }`.

Пример создания:

```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Иван",
    "lastName": "Иванов",
    "patronymic": "Петрович",
    "recordBookNumber": "A123456",
    "birthDate": "2004-03-12",
    "email": "ivanov@example.com",
    "phone": "+79001234567",
    "gender": "MALE",
    "educationForm": "BUDGET",
    "studyForm": "FULL_TIME",
    "admissionDate": "2022-09-01",
    "status": "STUDYING",
    "groupId": 1
  }'
```

Номер зачётки уникален, только заглавные латинские буквы и цифры, 6–10 символов.  
Удаление запрещено, если студент зачислен на курс (`course_students`) либо есть результаты контроля или отметки посещаемости.

`gender`: `MALE`, `FEMALE`.  
`status`: `STUDYING`, `ACADEMIC_LEAVE`, `EXPELLED`.

## Преподаватели `/api/teachers`

| Метод | Путь | Ответ |
|-------|------|-------|
| GET | `/api/teachers` | 200, список |
| GET | `/api/teachers/{id}` | 200 или 404 |
| POST | `/api/teachers` | 201 |
| PUT | `/api/teachers/{id}` | 200 или 404 |
| DELETE | `/api/teachers/{id}` | 204, 404 или 409 |

```bash
curl -X POST http://localhost:8080/api/teachers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Пётр",
    "lastName": "Петров",
    "patronymic": "Сергеевич",
    "department": "ИВТ",
    "position": "Доцент",
    "academicDegree": "к.т.н.",
    "email": "petrov@example.com",
    "phone": "+79007654321"
  }'
```

Удаление запрещено, если преподаватель назначен на курс (семинарная или лекционная часть).

## Дисциплины `/api/disciplines`

| Метод | Путь | Ответ |
|-------|------|-------|
| GET | `/api/disciplines` | 200, список |
| GET | `/api/disciplines/{id}` | 200 или 404 |
| POST | `/api/disciplines` | 201 |
| PUT | `/api/disciplines/{id}` | 200 или 404 |
| DELETE | `/api/disciplines/{id}` | 204, 404 или 409 |

```bash
curl -X POST http://localhost:8080/api/disciplines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Базы данных",
    "description": "Реляционные модели и SQL",
    "lectureHours": 32,
    "practiceHours": 16,
    "labHours": 16,
    "controlForm": "EXAM"
  }'
```

Название уникально. Удаление запрещено, если есть курсы.

`controlForm`: `EXAM`, `DIFF_CREDIT`, `CREDIT`.

## Семестры `/api/semesters`

| Метод | Путь | Ответ |
|-------|------|-------|
| GET | `/api/semesters` | 200, список |
| GET | `/api/semesters/{id}` | 200 или 404 |
| POST | `/api/semesters` | 201 |
| PUT | `/api/semesters/{id}` | 200 или 404 |
| DELETE | `/api/semesters/{id}` | 204, 404 или 409 |

```bash
curl -X POST http://localhost:8080/api/semesters \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Осенний семестр 2025/2026",
    "startDate": "2025-09-01",
    "endDate": "2026-01-31"
  }'
```

Название уникально. Дата начала должна быть строго раньше даты окончания. Удаление запрещено, если есть курсы.

### Пример ошибки связи

```bash
curl -X DELETE http://localhost:8080/api/groups/1
```

Если у группы есть студенты или курсы:

```json
{
  "timestamp": "2026-09-24T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Нельзя удалить группу: есть связанные студенты или курсы",
  "path": "/api/groups/1"
}
```

---

# Ведомости

## Личная успеваемость студента

Для маршрутов `/api/me/courses` требуется роль `STUDENT`. Студент получает только собственные курсы и баллы. Идентификатор берётся из профиля, связанного с текущей учётной записью.

### Получить свои курсы

`GET /api/me/courses`

```json
[
  {
    "courseId": 12,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "semesterName": "Осень 2026",
    "status": "IN_PROGRESS",
    "rating": 90.0
  }
]
```

### Получить свои оценки по курсу

`GET /api/me/courses/{courseId}`

Ответ содержит только личные оценки студента. Чужие баллы и список студентов курса не передаются. Рейтинг рассчитывается при чтении и отдельно в базе не хранится. Если студент не записан на курс, API возвращает `404`.

```json
{
  "courseId": 12,
  "disciplineName": "Базы данных",
  "groupName": "ИВТ-301",
  "semesterName": "Осень 2026",
  "status": "IN_PROGRESS",
  "rating": 90.0,
  "works": [
    {
      "number": 1,
      "title": "Лабораторная работа",
      "maxScore": 20,
      "date": "2026-10-01",
      "score": 18
    }
  ]
}
```

## Чтение курсов и ведомостей

### Получить доступные курсы

`GET /api/courses`

- **Преподаватель** получает курсы, где он назначен на семинарную или лекционную часть.
- **Деканат** получает все курсы.

Для каждого курса возвращаются ID ведомостей и признаки доступа к ним.

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

### Получить курс, работы и рейтинг

`GET /api/courses/{courseId}`

Доступно деканату и преподавателю семинарской части курса. Преподаватель лекционной части не получает доступ к работам и оценкам.

```json
{
  "courseId": 12,
  "disciplineName": "Базы данных",
  "groupName": "ИВТ-301",
  "semesterName": "Осень 2026",
  "status": "IN_PROGRESS",
  "works": [
    {
      "number": 1,
      "title": "Лабораторная работа",
      "maxScore": 20,
      "date": "2026-10-01",
      "scores": [{"studentId": 21, "score": 18}]
    }
  ],
  "students": [{"studentId": 21, "name": "Иванов Иван Иванович", "rating": 90.0}]
}
```

### Управление работами и оценками

Все операции доступны только преподавателю семинарской части курса. Идентификатор преподавателя берётся из аутентификации.

`POST /api/courses/{courseId}/works`

```json
{"title": "Лабораторная работа", "maxScore": 20, "date": "2026-10-01"}
```

Ответ `201 Created`: `{"workNumber": 1}`. Номер работы назначается последовательно, а студентам курса создаются начальные оценки 0.

`PUT /api/courses/{courseId}/works/{workNumber}` принимает такое же тело и обновляет работу. Максимальный балл нельзя уменьшить ниже уже выставленной оценки.

`PUT /api/courses/{courseId}/works/{workNumber}/scores/{studentId}`

```json
{"score": 18}
```

`POST /api/courses/{courseId}/complete` завершает курс. Для завершения в курсе должна быть хотя бы одна работа.

Операции изменения возвращают `204 No Content`, кроме создания работы. Для неизвестных курса или работы возвращается `404`, при нарушении доменных правил `409` или `422`, при недостатке прав `403`.

### Получить контрольную ведомость

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

### Получить ведомость посещаемости

`GET /api/attendance-sheets/{sheetId}`

Доступ преподавателя ограничен лекционной частью его курса. Деканат может читать любую ведомость посещаемости.

Для каждой лекции возвращается отметка каждого студента. `status: null` означает, что посещение ещё не отмечено; `PRESENT` и `ABSENT` являются сохранёнными отметками.

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

### Добавить лекцию

`POST /api/attendance-sheets/{sheetId}/lectures`

```json
{
  "date": "2026-10-05",
  "title": "Введение"
}
```

Ответ `201 Created` содержит `id`, `date` и `title`.

### Отметить студента

`PUT /api/attendance-sheets/{sheetId}/lectures/{lectureId}/attendance/{studentId}`

```json
{
  "status": "PRESENT"
}
```

Допустимые значения `status`: `PRESENT`, `ABSENT`.

### Закрыть ведомость

`POST /api/attendance-sheets/{sheetId}/close`

### Архивировать ведомость

`POST /api/attendance-sheets/{sheetId}/archive`

## Контрольная ведомость

### Внести числовой результат

`PUT /api/control-sheets/{sheetId}/results/{studentId}/numeric`

```json
{
  "score": 88,
  "reason": "Пересдача"
}
```

### Внести зачёт или незачёт

`PUT /api/control-sheets/{sheetId}/results/{studentId}/pass-fail`

```json
{
  "result": "PASS",
  "reason": "Пересдача"
}
```

Допустимые значения `result`: `PASS`, `FAIL`.

### Закрыть ведомость

`POST /api/control-sheets/{sheetId}/close`

### Переоткрыть ведомость

`POST /api/control-sheets/{sheetId}/reopen`

### Архивировать ведомость

`POST /api/control-sheets/{sheetId}/archive`

Для переоткрытия и архивирования можно передать необязательное тело `{"reason":"Причина"}`. Операции без содержимого возвращают `204 No Content`.

---

# Сводная таблица кодов ошибок

| Код | Справочники | Ведомости |
|-----|-------------|-----------|
| 400 | Невалидное тело, несуществующий `groupId`, некорректные даты семестра | Некорректный запрос |
| 401 | — | Отсутствующая или неверная аутентификация |
| 403 | — | Недостаточно прав |
| 404 | Запись с таким id нет | Ведомость, лекция или студент не найдены |
| 409 | Нарушена уникальность, нельзя удалить из-за связей | Недопустимое состояние ведомости |
| 422 | — | Некорректный результат |
