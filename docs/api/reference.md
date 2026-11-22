---
title: Довідник коду і SDK
owner: Yehor Ishchuk
status: published
review_date: 2027-02-19
tags: [api]
---

# Довідник коду і клієнтський SDK

## Клієнтський SDK (згенерований з контракту)

SDK не пишеться руками — його генерує OpenAPI Generator з [`openapi.yaml`](openapi.yaml):

```bash
openapi-generator-cli generate -i docs/api/openapi.yaml -g java --library native -o sdk-client \
  --additional-properties=groupId=com.kira.shop,artifactId=shop-orders-client,artifactVersion=1.0.0,\
invokerPackage=com.kira.shop.client,apiPackage=com.kira.shop.client.api,modelPackage=com.kira.shop.client.model
```

Результат: Maven-проєкт `com.kira.shop:shop-orders-client:1.0.0` (46 файлів; лог — у
[архіві ЛР11](https://github.com/YehorIshchuk/shop-quality/blob/main/docs/reports/lab11/sdk-generation.log)).
Імена класів і методів узято з `tags` та `operationId` контракту:

| Клас | Метод | Операція контракту |
|---|---|---|
| `SystemApi` | `Health getHealth()` | `GET /api/health` |
| `OrdersApi` | `List<Order> listOrders()` | `GET /api/orders` |
| `OrdersApi` | `Order getOrderById(String orderId)` | `GET /api/orders/{orderId}` |
| моделі | `Order`, `Health`, `Error` | `components.schemas` |

```java
ApiClient client = new ApiClient().setHost("localhost").setPort(8080);
Order order = new OrdersApi(client).getOrderById("ORD-1");   // 404 → ApiException з тілом Error
```

Каталог `sdk-client/` у `.gitignore`: SDK — похідний артефакт, конвеєр генерує його заново з кожною зміною контракту
і публікує як artifact збірки.

## Javadoc (статичний довідник з коментарів коду)

Варіативний пункт ЛР11 для API-проєктів: довідник з doc-коментарів рідної екосистеми Java.
Конвеєр генерує його командою `javadoc` і публікує поруч із сайтом:
**<https://yehorishchuk.github.io/shop-quality/javadoc/>**.

```bash
javadoc -d site/javadoc -sourcepath src/main/java -subpackages com.kira.shop -encoding UTF-8 -Xdoclint:none
```

## Пов'язані документи

[[sdd]] · [[coding-standards]]
