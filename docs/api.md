# REST API

Базовый префикс: `/api`. Тела запросов и ответов в формате JSON. Вне локальной разработки API доступен только по HTTPS.

## Аутентификация и авторизация

- API использует серверную HTTP-сессию. Пароль пользователя в таблице `users` хранится в **BCrypt**.
- Учётная запись преподавателя связана с профилем через `teachers.user_id`, студента через `students.user_id`.
- Для роли `DEAN` используется ID пользователя, отдельной таблицы деканата нет.
- Управление учётными записями доступно только роли `ADMIN` через `/api/admin/users`.
- API **сам определяет пользователя и его роль**. ID актёра в запросах не передаётся.
- Сессии хранятся в PostgreSQL и завершаются после 5 минут бездействия. `POST /api/auth/logout` отзывает текущую сессию.
- Запросы с изменением данных требуют CSRF-токен.
- Операции преподавателя доступны роли `TEACHER`. Переоткрытие и архивирование доступны роли `DEAN`. Домен дополнительно проверяет, что преподаватель ведёт соответствующий курс.
- Справочники (`/api/groups`, `/api/students`, `/api/teachers`, `/api/disciplines`, `/api/semesters`) доступны только роли `DEAN`. Студент и преподаватель получают 403.

### Войти и управлять сессией

Перед входом вызови `GET /api/auth/csrf`. Ответ содержит токен и имя заголовка, а браузер получает cookie `XSRF-TOKEN`:

```json
{"headerName":"X-XSRF-TOKEN","token":"..."}
```

Отправь `POST /api/auth/login` с этим CSRF-заголовком:

```json
{"username":"demo-seminar","password":"demo1234"}
```

При успехе создаётся серверная сессия и возвращается безопасная информация о пользователе:

```json
{"username":"demo-seminar","role":"TEACHER"}
```

Браузер отправляет cookie сессии со следующими запросами. После входа получи новый CSRF-токен. `GET /api/auth/me` возвращает текущего пользователя, `POST /api/auth/logout` отзывает его сессию и возвращает `204 No Content`.

Неактивная в течение 5 минут сессия истекает автоматически.

### Управление учётными записями

Операции доступны только пользователю с ролью `ADMIN`. `GET /api/admin/users`
возвращает список аккаунтов с идентификатором, логином, ролью и признаком
активности. Пароль и его хэш в ответ не включаются.

```json
[
  {"id": 1, "username": "demo-user", "role": "STUDENT", "enabled": true}
]
```

`PUT /api/admin/users/{userId}/role` изменяет роль выбранного аккаунта:

```json
{"role":"TEACHER"}
```

Допустимые роли: `ADMIN`, `DEAN`, `TEACHER`, `STUDENT`. Для назначения роли
`TEACHER` или `STUDENT` у аккаунта уже должен быть связанный профиль. При
фактическом изменении роли активные сессии этого пользователя отзываются, и ему
нужно войти снова. Роль последнего активного администратора изменить нельзя.
Успешное изменение возвращает обновлённую учётную запись; отсутствующий аккаунт
даёт `404`, а несвязанный профиль или попытка изменить роль последнего
администратора дают `409`.

### Локальные данные для демонстрации

Для запуска и локальных демоданных см. [инструкцию по локальной разработке](development.md). Dev-профиль добавляет демоданные в схему `public`; тестовые пользователи: `demo-dean`, `demo-seminar`, `demo-lecture`, `demo-student`, пароль для каждого `demo1234`.

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

`groupId` может быть `null`. Если указан несуществующий id, API возвращает 400. В ответе группа кратко: `{ "id", "name" }`.

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

Номер зачётки уникален, содержит только заглавные латинские буквы и цифры, от 6 до 10 символов.
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
    "startSemesterName": "Осень 2026",
    "endSemesterName": "Весна 2027",
    "courseType": "SEMINAR",
    "status": "IN_PROGRESS",
    "rating": 90.0,
    "courseworkScore": null,
    "courseworkGrade": null,
    "controlResult": null
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
  "startSemesterName": "Осень 2026",
  "endSemesterName": "Весна 2027",
  "courseType": "SEMINAR",
  "status": "IN_PROGRESS",
  "rating": 90.0,
  "courseworkScore": null,
  "courseworkGrade": null,
  "controlResult": {
    "controlForm": "EXAM",
    "sheetStatus": "CLOSED",
    "numericScore": 88,
    "passFail": null,
    "finalScore": 89.0,
    "finalGrade": 5
  },
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

Для курса типа `COURSEWORK` личный балл и вычисленная оценка возвращаются в
`courseworkScore` и `courseworkGrade`; рейтинг и список работ семинарного курса
для него отсутствуют. Если для семинарного курса есть зачётная ведомость,
`controlResult` содержит результат контроля и итог по дисциплине. Пока ведомость
открыта, студент видит её форму и состояние, но поля результата остаются пустыми.
Для обычного зачёта заполняется `passFail`; для экзамена и дифференцированного
зачёта возвращаются балл контроля, рассчитанный итоговый балл и оценка.

## Чтение курсов и ведомостей

### Получить доступные курсы

`GET /api/courses`

- **Преподаватель** получает все курсы, на которые он назначен, включая курсовые.
- **Деканат** получает все курсы.

Курс задаётся отдельно для каждой дисциплины, группы, типа и периода. Типы: `SEMINAR`, `LECTURE`, `COURSEWORK`. Семестр начала и окончания могут различаться. У каждого курса один преподаватель; преподаватель может вести курсы разных типов. В курсе автоматически регистрируются студенты выбранной группы. Для курсового курса при создании также создаётся ведомость курсовых работ.

```json
[
  {
    "courseId": 12,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "startSemesterName": "Осень 2026",
    "endSemesterName": "Весна 2027",
    "courseType": "SEMINAR",
    "courseStatus": "COMPLETED",
    "controlSheetId": 34,
    "controlSheetStatus": "OPEN",
    "attendanceSheetId": null,
    "attendanceSheetStatus": null,
    "courseworkSheetId": null,
    "courseworkSheetStatus": null,
    "canReadControlSheet": true,
    "canReadAttendanceSheet": false,
    "canReadCourseworkSheet": false
  }
]
```

### Создать и назначить курс

`POST /api/courses` доступен деканату. Все идентификаторы берутся из справочников;
студенты группы назначаются на курс автоматически. Начальный семестр не может
быть позже конечного. Для лекционного курса создаётся открытая ведомость
посещаемости. Для курсового курса создаётся открытая ведомость курсовых работ.

```json
{
  "disciplineId": 4,
  "groupId": 2,
  "startSemesterId": 3,
  "endSemesterId": 4,
  "courseType": "COURSEWORK",
  "teacherId": 7
}
```

Допустимые `courseType`: `LECTURE`, `SEMINAR`, `COURSEWORK`. Ответ `201 Created`
содержит созданный курс.

### Получить курс, работы и рейтинг

`GET /api/courses/{courseId}`

Доступно деканату и преподавателю, назначенному на этот курс. У лекционного и курсового курса список семинарских работ пустой, рейтинг для них не рассчитывается.

```json
{
  "courseId": 12,
  "disciplineName": "Базы данных",
  "groupName": "ИВТ-301",
  "startSemesterName": "Осень 2026",
  "endSemesterName": "Весна 2027",
  "courseType": "SEMINAR",
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

Все операции доступны только преподавателю семинарного курса. Идентификатор преподавателя берётся из аутентификации.

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

`POST /api/courses/{courseId}/complete` завершает курс. Семинарный курс должен
содержать хотя бы одну работу. Лекционный и курсовой курс завершаются без работ
семинарной части.

Деканат может переоткрыть завершённый курс в следующем семестре через
`POST /api/courses/{courseId}/reopen`, передав `{"semesterId": 4}`. При этом
семестр окончания курса переносится на указанный семестр, курс снова становится
доступен преподавателю для редактирования, а существующие работы и баллы
сохраняются. Переоткрыть незавершённый курс или выбрать семестр, отличный от
следующего после текущего периода, нельзя.

После завершения семинарного курса преподаватель создаёт зачётную ведомость:
`POST /api/courses/{courseId}/control-sheet`. Тело задаёт пороги итоговой шкалы,
например `{"grade5From":85,"grade4From":70,"grade3From":50}`. Форма контроля
берётся из дисциплины. Ответ `201 Created`: `{"sheetId":34}`. Повторно создать
ведомость для того же курса нельзя.

Операции изменения возвращают `204 No Content`, кроме создания работы. Для неизвестных курса или работы возвращается `404`, при нарушении доменных правил `409` или `422`, при недостатке прав `403`.

### Ведомость курсовых работ

`GET /api/coursework-sheets/{sheetId}` доступен назначенному преподавателю курса
и деканату. Для каждого студента возвращаются балл и вычисленная оценка.

```json
{
  "sheetId": 41,
  "course": {
    "id": 15,
    "disciplineName": "Базы данных",
    "groupName": "ИВТ-301",
    "startSemesterName": "Весна 2027",
    "endSemesterName": "Весна 2027",
    "courseType": "COURSEWORK",
    "status": "IN_PROGRESS"
  },
  "status": "OPEN",
  "results": [
    {"studentId": 21, "studentName": "Иванов Иван Иванович", "score": 70, "grade": "FOUR"}
  ]
}
```

Границы фиксированы: от 85 до 100 это `FIVE`, от 70 до 84 это `FOUR`, от 50 до
69 это `THREE`, от 0 до 49 это `NOT_PASSED`.

`PUT /api/coursework-sheets/{sheetId}/results/{studentId}` принимает
`{"score": 70}`. `POST /api/coursework-sheets/{sheetId}/close` закрывает ведомость,
если балл внесён каждому студенту. Деканат может переоткрыть закрытую ведомость
в следующем семестре через `POST /api/coursework-sheets/{sheetId}/reopen`, передав
`{"semesterId": 4}`, или архивировать её через
`POST /api/coursework-sheets/{sheetId}/archive`.

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
    "startSemesterName": "Осень 2026",
    "endSemesterName": "Весна 2027",
    "courseType": "SEMINAR",
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
      "passFail": null,
      "finalScore": null,
      "finalGrade": null
    }
  ]
}
```

Для закрытой ведомости в строке студента также возвращаются `finalScore` и
`finalGrade`. Они заполняются для экзамена и дифференцированного зачёта по
формуле `(рейтинг семинарного курса + балл контроля) / 2`, с округлением вниз
при переводе в оценку. Для обычного зачёта итогом остаётся `passFail`, а
числовые итоговые поля пусты. В открытой ведомости итоговые поля всегда пусты.

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
    "startSemesterName": "Осень 2026",
    "endSemesterName": "Весна 2027",
    "courseType": "LECTURE",
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
| 401 | нет | Отсутствующая или неверная аутентификация |
| 403 | нет | Недостаточно прав |
| 404 | Запись с таким id нет | Ведомость, лекция или студент не найдены |
| 409 | Нарушена уникальность, нельзя удалить из-за связей | Недопустимое состояние ведомости |
| 422 | нет | Некорректный результат |
