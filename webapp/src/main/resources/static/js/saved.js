/**
 * saved.js — localStorage-backed saved result list.
 * Exposed as window.SavedResults for home.js and results.js.
 */
(function () {
    const STORAGE_KEY = 'comp4321_saved_results';
    const MAX_ENTRIES = 30;

    function load() {
        try {
            const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            return Array.isArray(parsed) ? parsed : [];
        } catch (_) {
            return [];
        }
    }

    function save(entries) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
    }

    function normalize(item) {
        return {
            title: String(item?.title || item?.url || '').trim(),
            url: String(item?.url || '').trim(),
            score: item?.score || '',
            query: String(item?.query || '').trim(),
            ts: Number(item?.ts) || Date.now()
        };
    }

    const SavedResults = {
        push(item) {
            const next = normalize(item);
            if (!next.url) return;
            let entries = load().filter(entry => entry.url !== next.url);
            entries.unshift(next);
            if (entries.length > MAX_ENTRIES) entries = entries.slice(0, MAX_ENTRIES);
            save(entries);
        },

        has(url) {
            return load().some(entry => entry.url === url);
        },

        getAll() {
            return load();
        },

        clear() {
            localStorage.removeItem(STORAGE_KEY);
        }
    };

    window.SavedResults = SavedResults;
})();
