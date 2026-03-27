# Lab 1 (JDBM) alignment note

This project currently persists the spider crawl state in `state.json` (see `src/main/java/com/comp4321/spider/store/PageStore.java`) because the **indexer is intentionally not implemented yet**.

In the COMP4321 project, JDBM is primarily used to persist the **indexer/search-engine** data structures (e.g., URL↔pageId mappings, forward/inverted indexes). Once the indexer is added, the persistence layer can be migrated from JSON to JDBM as a RecordManager with multiple named HTree/BTree structures (as introduced in Lab 1).

Practical next step (when you start the indexer):

- Add a JDBM dependency/jar and create a `RecordManager` for the DB file (e.g. `index.db`).
- Store named objects for:
  - `urlToPageId` (HTree)
  - `pageIdToUrl` (HTree or BTree)
  - `wordToWordId` (HTree)
  - index structures (BTree or HTree), depending on your design

