# REST API справочников

Базовый префикс: `/api`. Тела запросов и ответов — JSON. Учётные записи (`userId`) этим API не создаются и не меняются.

Единый формат ошибки:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Ошибка валидации",
  "fields": {
    "name": "Название группы обязательно"
  }
}
```

Поле `fields` есть только при ошибке Bean Validation. Иначе `null`.

| Код | Когда |
|---|---|
| 400 | Невалидное тело, несуществующая группа у студента, даты семестра |
| 404 | Запись с таким id нет |
| 409 | Нарушена уникальность или нельзя удалить из‑за связей |
| 201 | Создание |
| 204 | Удаление |

## Группы `/api/groups`

| Метод | Путь | Ответ |
|---|---|---|
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
|---|---|---|
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
Удаление запрещено, если есть результаты контроля или отметки посещаемости.

`gender`: `MALE`, `FEMALE`.  
`status`: `STUDYING`, `ACADEMIC_LEAVE`, `EXPELLED`.

## Преподаватели `/api/teachers`

| Метод | Путь | Ответ |
|---|---|---|
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
|---|---|---|
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
|---|---|---|
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

## Пример удаления и ошибки связи

```bash
curl -X DELETE http://localhost:8080/api/groups/1
```

Если у группы есть студенты или курсы:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Нельзя удалить группу: есть связанные студенты или курсы",
  "fields": null
}
```
