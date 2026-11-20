#!/usr/bin/env bash
# Сквозная проверка стенда через gateway: данные в трёх сервисах, Feign-вызовы, маршруты, закрытый /internal.
# Запуск: ./scripts/smoke-test.sh [адрес gateway, по умолчанию http://localhost:8081]
set -u
GW="${1:-http://localhost:8081}"
SUFFIX="$(date +%s)"
CLASS_START="$(date -d '+3 days' +%Y-%m-%dT10:00:00 2>/dev/null || date -v+3d +%Y-%m-%dT10:00:00)"
FAILED=0

# req METHOD PATH [BODY] -> тело в $BODY, код в $CODE
req() {
  local out
  if [ $# -ge 3 ]; then
    out=$(curl -s -w $'\n%{http_code}' -X "$1" "$GW$2" -H 'Content-Type: application/json' -d "$3")
  else
    out=$(curl -s -w $'\n%{http_code}' -X "$1" "$GW$2")
  fi
  CODE="${out##*$'\n'}"
  BODY="${out%$'\n'*}"
}

check() { # описание, ожидаемый код
  if [ "$CODE" = "$2" ]; then echo "  ok   $1 ($CODE)"; else echo "  FAIL $1: ожидался $2, получен $CODE"; echo "       $BODY" | head -c 300; echo; FAILED=1; fi
}

id_of() { echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }

echo "Gateway: $GW"
req GET /api/members;           check "GET /api/members (member-service)" 200
req GET /api/trainers;          check "GET /api/trainers (catalog-service)" 200
req GET /api/bookings;          check "GET /api/bookings (booking-service)" 200
req GET /internal/members/1;    check "/internal/members закрыт" 404
req GET /internal/bookings/class/1/booked-count; check "/internal/bookings закрыт" 404

echo "Подготовка данных"
req POST /api/members "{\"firstName\":\"Smoke\",\"lastName\":\"Test\",\"email\":\"smoke$SUFFIX@test.ru\",\"status\":\"ACTIVE\"}"
check "создание участника" 201; MEMBER=$(id_of)
req POST /api/plans "{\"name\":\"Smoke $SUFFIX\",\"type\":\"MONTHLY\",\"price\":1000,\"durationDays\":30}"
check "создание тарифа" 201; PLAN=$(id_of)
req POST /api/subscriptions "{\"memberId\":$MEMBER,\"planId\":$PLAN}"
check "создание абонемента" 201; SUB=$(id_of)
req POST /api/disciplines "{\"name\":\"Smoke $SUFFIX\"}"
check "создание направления" 201; DISC=$(id_of)
req POST /api/trainers "{\"firstName\":\"Smoke\",\"lastName\":\"Trainer\",\"email\":\"trainer$SUFFIX@test.ru\",\"experienceYears\":3,\"disciplineIds\":[$DISC]}"
check "создание тренера" 201; TRAINER=$(id_of)
req POST /api/classes "{\"title\":\"Smoke class\",\"trainerId\":$TRAINER,\"disciplineId\":$DISC,\"room\":\"A1\",\"startsAt\":\"$CLASS_START\",\"durationMinutes\":60,\"capacity\":1,\"level\":\"BEGINNER\"}"
check "создание занятия" 201; CLASS=$(id_of)

echo "Запись на занятие (Feign: booking → member, booking → catalog)"
req POST /api/bookings "{\"memberId\":$MEMBER,\"classId\":$CLASS}"
check "запись" 201; BOOKING=$(id_of)
req POST /api/bookings "{\"memberId\":$MEMBER,\"classId\":$CLASS}"
check "повторная запись отклонена" 409
req POST /api/bookings "{\"memberId\":999999,\"classId\":$CLASS}"
check "несуществующий участник" 404
req GET "/api/classes/$CLASS/bookings"
check "GET /api/classes/{id}/bookings (маршрут в booking-service)" 200

echo "Обратные вызовы (member/catalog → booking)"
req DELETE "/api/members/$MEMBER"
check "удаление участника с записью запрещено" 409
req PUT "/api/classes/$CLASS" "{\"title\":\"Smoke class\",\"trainerId\":$TRAINER,\"disciplineId\":$DISC,\"room\":\"A1\",\"startsAt\":\"$CLASS_START\",\"durationMinutes\":60,\"capacity\":15,\"level\":\"BEGINNER\"}"
check "увеличение вместимости разрешено" 200

echo "Очистка"
req DELETE "/api/classes/$CLASS";   check "удаление занятия" 204
req DELETE "/api/bookings/$BOOKING"; echo "  (запись могла быть удалена вместе с занятием: код $CODE)"
# У участника остался абонемент (внешний ключ в БД member-service), поэтому сначала удаляем его
req DELETE "/api/members/$MEMBER"; check "участник с абонементом не удаляется (ограничение БД)" 409
req DELETE "/api/subscriptions/$SUB"; check "удаление абонемента" 204
req DELETE "/api/members/$MEMBER";  check "удаление участника без записей и абонементов" 204

HDR=$(curl -si "$GW/api/members" | grep -i '^x-request-id' || true)
[ -n "$HDR" ] && echo "  ok   X-Request-Id: ${HDR#*: }" || { echo "  FAIL нет заголовка X-Request-Id"; FAILED=1; }

if [ "$FAILED" -eq 0 ]; then echo "ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ"; else echo "ЕСТЬ ОШИБКИ"; exit 1; fi