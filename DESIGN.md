# Design Document - Stock Ledger

## 1. Tables - what's stored vs. what's worked out

- **items** - `id, code, name, unit, active`. Renamed via `PATCH
  /items/{code}/rename`, which only changes future display (see rule 2).
- **warehouses** - `id, code, name, active`.
- **movements** - the only table that matters for correctness. One row
  per real event: `kind (IN/OUT/TRANSFER), item_id, item_code_snapshot,
  item_name_snapshot, item_unit_snapshot, quantity (always positive),
  warehouse_id (IN/OUT), from_warehouse_id/to_warehouse_id (TRANSFER),
  reason, occurred_at, recorded_at, recorded_by, cancels_movement_id`.

**Nothing stores "current stock" anywhere.** It's calculated on every
request by summing movement rows for that item (and warehouse, if asked)
where `occurred_at <= the moment asked about`. IN adds, OUT subtracts,
TRANSFER adds at the destination and subtracts at the source; company-wide
a TRANSFER nets to zero since nothing actually enters or leaves.

## 2. Stock on a past date

Same query as current stock, different cutoff: `occurred_at <= the date
asked for` instead of `occurred_at <= now`. It filters on `occurred_at`
(when the event happened), not `recorded_at` (when it was typed in) - a
movement backdated to last Tuesday counts toward last Tuesday's figure,
which is why both timestamps are stored separately.

## 3. Cancelling a movement, row by row

Movement #12 = "IN, 50 units, Warehouse North, last Tuesday."

1. Look up #12. If another row already has `cancels_movement_id = 12`,
   refuse.
2. Insert new row #47: `kind = OUT` (opposite of #12), same item,
   warehouse, quantity, `cancels_movement_id = 12`.
3. `#47.occurred_at` = now, the moment of cancellation, not last Tuesday.
   `#12` is never edited or deleted.
4. Both rows stay forever; history shows both, #47 pointing back at #12.
5. No special-case logic needed in stock queries - #47 is an ordinary row
   with the opposite effect, so it nets out naturally.

Cancelling a TRANSFER: the reversal is a new TRANSFER with `from`/`to`
swapped.

## 4. What breaks at 10 million movements

`SUM(...) WHERE item_id = ? AND occurred_at <= ?` stays fast with an index
on `(item_id, occurred_at)` - a range scan, not a full table scan, even at
10M rows. What actually breaks:
- **Offset pagination** on `/movements` slows as the offset grows; keyset
  pagination (`WHERE recorded_at < :lastSeen ... LIMIT :size`) would
  replace it.
- **Frequent current-stock lookups** for the same hot item recompute the
  sum every time. Fix: a materialized `stock_balances(item_id,
  warehouse_id, quantity, as_of_movement_id)` cache, updated incrementally
  per movement - `movements` stays the source of truth, cache is always
  rebuildable from it.
- Eventually, partitioning `movements` by month and archiving old
  partitions, so day-to-day queries skip cold data.

Not built for this submission - recomputing is simpler and provably
correct at this scale.

## 5. Decisions made, and where each hurts

- **Negative stock?** Refused - an OUT (or outgoing leg of a TRANSFER) is
  blocked if it would take a warehouse below zero. Matches physical
  reality; hurts if an OUT needs recording before its matching IN exists
  yet (paperwork out of order) - currently refused, IN must come first.
- **Transfer: one row or two?** One row (`from`/`to` warehouse). One real
  event = one ledger entry, cancelling stays atomic. Cost: stock queries
  need a couple of extra `CASE WHEN` branches for TRANSFER.
- **Recompute vs. running total?** Recompute (§4) - simpler, always
  correct by construction, no drift risk. Cost is a SUM per read instead
  of O(1); mitigation documented above if that stops being enough.
- **Direction: signed or always-positive + kind?** Always positive,
  `kind` decides direction - simpler to read, no sign convention to
  remember.
- **Backdated entry - does the past change?** Yes, deliberately - a
  correction entered today for last Tuesday changes what "stock on
  Wednesday" returns from now on, since the query filters on
  `occurred_at`, not `recorded_at`. An already-printed report doesn't
  rewrite itself, but a newly generated one reflects everything now known.
- **API shape** (also left to me): REST resource per entity (`/items`,
  `/warehouses`, `/movements`, `/stock/*`); `404` missing resource, `422`
  business-rule violation, `400` malformed input; consistent JSON error
  body; offset pagination for movement history.
- **Beyond the brief:** rule 3 only requires disabled *items* to refuse
  new movements. I applied the same to disabled *warehouses* - it
  shouldn't silently keep accepting stock either.

## 6. Not finished / what I'd change for real use

- No materialized balance table (§4) - first thing to add under load.
  Pagination is offset-based, not keyset-based - fine at current scale.
- No DB-level unique constraint on `cancels_movement_id` - two concurrent
  cancel requests for the same movement could both pass the "already
  cancelled?" check before either commits. Would add a unique constraint
  (excluding NULLs) before real use.
- No authentication, as specified - `recordedBy` is trusted free text.
- For production: managed MySQL, real migrations (Flyway/Liquibase)
  instead of `ddl-auto=update`, the balance table and unique constraint
  above, structured logging/metrics per endpoint.
