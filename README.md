# v-Mazz - E-Procurement Portal

Spring Boot 3.5 / Java 21. An Amazon / Flipkart style B2B portal base, with the Spring AI product catalog
(Ollama + Elasticsearch: hybrid search, alternates, BOM matching, agent) kept as it was.

## Run
1. `./mvnw clean test`, then run `ProductApplication` (the AI part needs Elasticsearch and Ollama running,
   as before - see "AI product catalog" below).
2. Swagger: http://localhost:8080/swagger-ui.html
   Log in with `POST /api/v1/auth/login`, press **Authorize**, paste the `accessToken`.
3. Or open `samples/vmazz.http` in IntelliJ and run the requests top to bottom.

## Demo logins (password `Password@123`)
| Login | Role |
|-------|------|
| admin@vmazz.com | ADMIN of "v-Mazz Demo Supplies Pvt Ltd" |
| subadmin@vmazz.com | SUB_ADMIN of the same company |
| buyer@vmazz.com | CUSTOMER |

15 demo items are created across Food, Groceries, Goods and Raw Material.
Turn the demo data off with `vmazz.seed.enabled=false`.

## What is in the base
| Area | How it works |
|------|--------------|
| **Company** | `companies` table. A company registers with its first ADMIN user (`/auth/register-company`). |
| **Users** | `users` table, login = email. ADMIN creates SUB_ADMIN users (`/admin/users`). Customers sign up themselves (`/auth/register-customer`). JWT (HS256) login. |
| **Item master** | `items` table, per company: item code, name, **category** (FOOD, GROCERIES, GOODS, RAW_MATERIAL), sub category, brand, description, **specification**, HSN, **UOM**, **unit price**, MRP, currency, tax %, **available quantity**, min order quantity, image, status. ADMIN and SUB_ADMIN maintain it. |
| **Storefront** | Public search with filters (text, category, brand, price range, in stock, supplier) and sorting. |
| **Cart** | Add / change / remove per customer. Stock, minimum quantity, whole-number rule for countable units, single currency. |
| **Checkout** | Contact + shipping (+ optional billing) address, payment method, required delivery date, buyer reference, notes -> **purchase orders**. |
| **Purchase orders** | `purchase_orders` + `purchase_order_items`. One PO per supplier company in the cart (`PO-2026-000001`). Item details, prices and the address are stored as a snapshot. Stock is deducted at checkout. |
| **Order handling** | Customer: my orders, cancel while PLACED. Company: orders received, status PLACED > CONFIRMED > PACKED > SHIPPED > DELIVERED (or CANCELLED; cancelling returns the stock). |

## Roles
| Endpoint group | Anonymous | CUSTOMER | SUB_ADMIN | ADMIN |
|----------------|:---------:|:--------:|:---------:|:-----:|
| `/auth/login`, `/auth/register-*`, `GET /catalog/**` | yes | yes | yes | yes |
| `/cart/**`, `/orders/**` | | yes | | |
| `/admin/items/**` (create, edit, stock, list) | | | yes | yes |
| `DELETE /admin/items/{id}` (deactivate) | | | | yes |
| `/admin/orders/**` | | | yes | yes |
| `/admin/users/**`, `/admin/company/**` | | | | yes |

Company users only ever see their own company's items and orders.

## Main endpoints
```
POST /api/v1/auth/register-customer | register-company | login      GET /api/v1/auth/me
GET  /api/v1/catalog/items?q=&category=&brand=&minPrice=&maxPrice=&inStockOnly=&supplierId=&sort=&page=&size=
GET  /api/v1/catalog/items/{id}      /catalog/categories      /catalog/uoms
GET  /api/v1/cart                     POST /cart/items          PUT|DELETE /cart/items/{itemId}     DELETE /cart
POST /api/v1/orders/checkout          GET /orders               GET /orders/{id}                    POST /orders/{id}/cancel
POST|GET /api/v1/admin/items          PUT /admin/items/{id}     PUT /admin/items/{id}/stock         DELETE /admin/items/{id}
POST|GET /api/v1/admin/users          PUT /admin/users/{id}     PUT /admin/users/{id}/status
GET|PUT  /api/v1/admin/company
GET  /api/v1/admin/orders?status=     GET /admin/orders/{id}    PUT /admin/orders/{id}/status
```
Errors are JSON `{timestamp, status, message}`: 400 validation, 401 not logged in / wrong password, 403 wrong role,
404 not found, 409 conflict (duplicate, not enough stock, wrong order status).

## Configuration
| Property | Default | Meaning |
|----------|---------|---------|
| `vmazz.jwt.secret` (`VMAZZ_JWT_SECRET`) | dev value | HS256 key, 32+ characters. **Change it outside local development.** |
| `vmazz.jwt.expiry-minutes` | 120 | Token lifetime |
| `vmazz.seed.enabled` / `vmazz.seed.password` | true / Password@123 | Demo data |

## AI product catalog (unchanged)
`/api/v1/products`, `/api/v1/ai/products/...` (hybrid search, RAG, alternates), `/api/v1/ai/bom/...` (BOM matching),
`/api/v1/ai/agent/chat`, `/api/v1/admin/products/reindex`. Needs Elasticsearch (`zestpart_products_v3`) and Ollama
(`llama3.2`, `nomic-embed-text`). These endpoints are still open (no login) exactly as before:
protect them later by removing the final `permitAll` rule in `vmazz/security/SecurityConfig`.

## Notes and next steps
- Data is in H2 memory (`create-drop`): users, items and orders are recreated from the demo data at every start.
  Switch to SQL Server and `ddl-auto=update` (or migrations) before real use.
- No payment gateway, e-mail / SMS, image upload, tax invoice or address book yet: natural next steps.
- Prices are per supplier item; the cart is paid in one currency.
- Suggested next steps: Angular storefront, order e-mails, invoice PDF, supplier approval workflow,
  buyer organisations with several users, AI product search on the storefront (reuse the Elasticsearch hybrid search).

## November 2026 storefront and persistence update
- The local H2 development database now uses a file database (`./data/vmazzdb`) with Hibernate `update`, so account/profile references and catalogue data survive application restarts. The legacy electronics catalogue seed uses an H2 `MERGE` keyed by MPN to avoid duplicate rows.
- User profile photos are stored in `AppUser.profileImageUrl`; product photos use the existing `Item.imageUrl` field. Files are saved under `./uploads` by default and served from `/uploads/**`.
- `POST /api/v1/profile/image` accepts multipart field `file` for the signed-in user.
- `POST /api/v1/admin/items/images` accepts multipart field `file` for supplier/admin product images. Save the returned `imageUrl` on the item using the existing create/update endpoint.
- Supported images: JPG/JPEG, PNG, WEBP, GIF, max 5 MB.
- Welcome email is attempted after customer registration, supplier registration and admin-created sub-user creation. Configure SMTP environment variables and set `VMAZZ_EMAIL_ENABLED=true`; otherwise email is intentionally disabled and account creation still succeeds.
- CORS now allows Angular at `http://localhost:4200` (and the previous React dev origin `http://localhost:5173`).

### SMTP environment variables
`VMAZZ_EMAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD`.

### Persistent storage
Set `VMAZZ_UPLOAD_DIR` to a durable directory in deployment. Back up both the database and upload directory. The default `./uploads` is local development storage, not cloud object storage.
