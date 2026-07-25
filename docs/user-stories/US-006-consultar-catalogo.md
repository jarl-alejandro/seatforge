# US-006 — Consultar catálogo y detalle

**Módulo:** Events · **Prioridad:** P0

## Historia

Como comprador, quiero consultar eventos publicados y su detalle, para elegir qué entrada intentar comprar.

## Reglas

- El catálogo público solo muestra eventos `PUBLISHED` y futuros.
- Las listas son paginadas y tienen orden determinista.
- La disponibilidad mostrada puede ser un resumen, pero debe etiquetarse como aproximada.
- No se exponen entidades JPA directamente.

## Criterios de aceptación

1. Dado el catálogo, cuando se consulta sin filtros, entonces devuelve una página acotada y orden estable.
2. Dados filtros por fecha, recinto o nombre, entonces solo se devuelven coincidencias publicadas.
3. Dado un evento cancelado o borrador, entonces no aparece públicamente.
4. Dado un identificador inexistente, el detalle responde `404`.
5. Dada una página de tamaño superior al máximo, entonces se rechaza o limita de forma documentada.

## Casos de prueba

| ID | Tipo | Escenario | Resultado esperado |
|---|---|---|---|
| T01 | U | Mapear agregado a DTO | No filtra campos internos |
| T02 | I | Filtrar catálogo | Resultados correctos |
| T03 | I | Paginar con empates | Sin duplicados ni omisiones |
| T04 | P | Catálogo con 100.000 eventos | p95 y plan SQL registrados |
| T05 | S | Parámetros maliciosos | Validación; sin SQL injection |

## Dependencias

US-005.

