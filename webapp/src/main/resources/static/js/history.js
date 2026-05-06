/**
 * history.js — localStorage-backed query history (max 20 entries).
 * Exposed as window.SearchHistory for use by both home.js and results.js.
 */
(function () {
    const STORAGE_KEY = 'comp4321_search_history';
    const MAX_ENTRIES = 20;

    function load() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
        } catch (_) {
            return [];
        }
    }

    function save(entries) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
    }

    const SearchHistory = {
        /** Add a query (de-dupes and keeps most recent at front). */
        push(query) {
            if (!query || !query.trim()) return;
            let entries = load().filter(e => e.query !== query.trim());
            entries.unshift({ query: query.trim(), ts: Date.now() });
            if (entries.length > MAX_ENTRIES) entries = entries.slice(0, MAX_ENTRIES);
            save(entries);
        },

        getAll() {
            return load();
        },

        remove(query) {
            const target = (query || '').trim();
            if (!target) return;
            save(load().filter(entry => entry.query !== target));
        },

        clear() {
            localStorage.removeItem(STORAGE_KEY);
        }
    };

    window.SearchHistory = SearchHistory;
})();
