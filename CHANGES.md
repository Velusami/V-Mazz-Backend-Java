# Release notes

## V1.0.0 - v-Mazz e-procurement portal base
- NEW package `com.example.catalog.vmazz`: company, users (ADMIN / SUB_ADMIN / CUSTOMER), item master with categories
  Food / Groceries / Goods / Raw Material, public storefront search, cart, checkout and purchase orders
- JWT login (Spring Security + Nimbus); only the v-Mazz endpoints are protected, existing AI endpoints stay open
- Checkout creates one purchase order per supplier with item snapshot, address, payment method and delivery date
- Stock is locked and deducted at checkout and restored on cancel
- Demo data: 1 company, admin / sub admin / buyer, 15 items (`vmazz.seed.enabled`)
- App renamed v-Mazz; Swagger has a Bearer token Authorize button; `samples/vmazz.http` for IntelliJ
- Error handling extended: 401, 403, 404, 409 (conflict, optimistic lock), invalid enum parameters

## V0.3.0 - alternates with specs + BOM matching
- Products have optional `category`, `packageType`, `specifications`; they are part of the embedding text
- Alternates filter by category (default) and optionally by package (`samePackage=true`)
- Elasticsearch index renamed to `zestpart_products_v3` (new fields) - filled by the startup reindex
- NEW BOM matching: `POST /api/v1/ai/bom/match`, `POST /api/v1/ai/bom/match/csv`, agent tool `matchBomLine`
- Seed data (`data.sql`) now has 24 products including LM358 and NE555; samples in `samples/`
- Malformed JSON now returns 400, oversized uploads 413
- `ProductRepository.findByMpnIgnoreCase` added

# Refactor notes

## Fixed
- create() no longer reports success when indexing fails (SQL commit and Elasticsearch are decoupled)
- Broken unit test replaced (ProductServiceImplTest, 6 tests)
- Empty item text ("TI products under 100") no longer crashes; IllegalArgumentException -> HTTP 400
- Elasticsearch is now populated from SQL on startup and via POST /api/v1/admin/products/reindex[?purge=true]
- Filters (manufacturer, price, currency, stock, status) are applied inside Elasticsearch (keyword AND kNN), case-insensitive
- Unhandled exceptions are now logged

## Changed
- Indexing: ProductIndexEvent -> ProductIndexer (after commit, async, 3 retries); embeddings only regenerated when name/manufacturer change
- nomic-embed-text prefixes: documents "search_document: ", queries "search_query: "  => REINDEX REQUIRED (purge=true)
- Query parsing uses Spring AI structured output (.entity) with fallback to plain hybrid search
- RAG now uses hybrid search; product text passed as template parameters (injection/brace safe)
- One agent: ProductAgentController (POST /api/v1/ai/agent/chat, /chat/stream, DELETE /memory/{id}) + ProductAgentTools + AgentConfig

## Removed
AiProductSearchService/Controller, ProductTools, ProductSearchTools, ProductToolRepository,
AiToolController, ProductAssistantController, workbook/ scratch package, target/, .idea/
Old agent URLs /api/v1/ai/tools and /api/v1/ai/products/assistant are replaced by /api/v1/ai/agent/chat.
