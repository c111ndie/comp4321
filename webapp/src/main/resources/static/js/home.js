/**
 * home.js — Homepage logic.
 * - Renders recent searches inside the home search input
 */
(function () {
    const SUGGEST_DEBOUNCE_MS = 180;
    const SUGGEST_LIMIT = 8;
    const form = document.getElementById('homeSearchForm');
    const input = document.getElementById('homeQuery');
    const recentDropdown = document.getElementById('homeRecentDropdown');
    const savedToggle = document.getElementById('savedToggle');
    const savedSection = document.getElementById('savedSection');
    const savedList = document.getElementById('savedList');
    const savedCount = document.getElementById('savedCount');
    const savedEmpty = document.getElementById('savedEmpty');
    const clearSavedBtn = document.getElementById('clearSaved');
    const browseBtn = document.getElementById('homeBrowseKeywords');
    let suggestTimer = null;
    let lastSuggestQuery = '';

    initKeywordBrowser({
        browseButton: browseBtn,
        panel: document.getElementById('homeKeywordBrowser'),
        filterInput: document.getElementById('homeKeywordFilter'),
        listEl: document.getElementById('homeKeywordList'),
        selectedWrap: document.getElementById('homeKeywordSelected'),
        selectedChips: document.getElementById('homeKeywordSelectedChips'),
        clearButton: document.getElementById('homeKeywordClear'),
        queryInput: input
    });

    function renderSearchDropdown() {
        const value = input.value.trim();
        if (value) {
            queueKeywordSuggestions(value);
            return;
        }
        clearTimeout(suggestTimer);
        renderRecentSearches();
    }

    function renderRecentSearches() {
        const entries = SearchHistory.getAll();

        recentDropdown.innerHTML = '';

        if (!entries.length) {
            recentDropdown.hidden = true;
            return;
        }

        const header = document.createElement('div');
        header.className = 'recent-dropdown-header';

        const title = document.createElement('span');
        title.textContent = 'Recent searches';

        const clearAll = document.createElement('button');
        clearAll.type = 'button';
        clearAll.className = 'recent-clear-all';
        clearAll.textContent = 'Clear all';
        clearAll.addEventListener('mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
            SearchHistory.clear();
            renderRecentSearches();
        });

        header.appendChild(title);
        header.appendChild(clearAll);
        recentDropdown.appendChild(header);

        entries.forEach(entry => {
            const row = document.createElement('div');
            row.className = 'recent-item';
            row.setAttribute('role', 'button');
            row.setAttribute('tabindex', '0');
            row.addEventListener('mousedown', (e) => {
                e.preventDefault();
                input.value = entry.query;
                SearchHistory.push(entry.query);
                window.location.href = '/results.html?q=' + encodeURIComponent(entry.query);
            });
            row.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    input.value = entry.query;
                    SearchHistory.push(entry.query);
                    window.location.href = '/results.html?q=' + encodeURIComponent(entry.query);
                }
            });

            const icon = document.createElement('span');
            icon.className = 'recent-icon';
            icon.setAttribute('aria-hidden', 'true');
            icon.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="12 8 12 12 14 14" /><circle cx="12" cy="12" r="10" /></svg>';

            const text = document.createElement('span');
            text.className = 'recent-query';
            text.textContent = entry.query;

            const time = document.createElement('span');
            time.className = 'recent-time';
            time.textContent = relativeTime(entry.ts);

            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'recent-remove';
            remove.setAttribute('aria-label', 'Remove "' + entry.query + '" from recent searches');
            remove.innerHTML = '&times;';
            remove.addEventListener('mousedown', (e) => {
                e.preventDefault();
                e.stopPropagation();
                SearchHistory.remove(entry.query);
                renderRecentSearches();
            });

            row.appendChild(icon);
            row.appendChild(text);
            row.appendChild(time);
            row.appendChild(remove);
            recentDropdown.appendChild(row);
        });

        recentDropdown.hidden = false;
    }

    function queueKeywordSuggestions(value) {
        clearTimeout(suggestTimer);
        suggestTimer = setTimeout(() => {
            fetchKeywordSuggestions(value);
        }, SUGGEST_DEBOUNCE_MS);
    }

    function fetchKeywordSuggestions(value) {
        const token = latestToken(value);
        if (!token) {
            renderRecentSearches();
            return;
        }

        lastSuggestQuery = token;
        fetch('/api/suggest?q=' + encodeURIComponent(token))
            .then(response => response.ok ? response.json() : [])
            .then(payload => {
                if (lastSuggestQuery !== token) {
                    return;
                }
                renderKeywordSuggestions(normalizeSuggestions(payload).slice(0, SUGGEST_LIMIT), token);
            })
            .catch(() => {
                recentDropdown.hidden = true;
            });
    }

    function renderKeywordSuggestions(items, token) {
        recentDropdown.innerHTML = '';

        if (!items.length) {
            recentDropdown.hidden = true;
            return;
        }

        const header = document.createElement('div');
        header.className = 'recent-dropdown-header';
        header.textContent = 'Suggested keywords';
        recentDropdown.appendChild(header);

        items.forEach(item => {
            const row = document.createElement('div');
            row.className = 'recent-item suggestion-item';
            row.setAttribute('role', 'button');
            row.setAttribute('tabindex', '0');

            const selectSuggestion = () => {
                input.value = queryWithSuggestion(input.value, item);
                SearchHistory.push(input.value);
                window.location.href = '/results.html?q=' + encodeURIComponent(input.value);
            };

            row.addEventListener('mousedown', (e) => {
                e.preventDefault();
                selectSuggestion();
            });
            row.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    selectSuggestion();
                }
            });

            const icon = document.createElement('span');
            icon.className = 'recent-icon';
            icon.setAttribute('aria-hidden', 'true');
            icon.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>';

            const text = document.createElement('span');
            text.className = 'recent-query';
            text.textContent = queryWithSuggestion(input.value, item);

            row.appendChild(icon);
            row.appendChild(text);
            recentDropdown.appendChild(row);
        });

        recentDropdown.hidden = false;
    }

    function latestToken(value) {
        return (value || '').trim().split(/\s+/).pop().replace(/^[-"]/, '').toLowerCase();
    }

    function queryWithSuggestion(query, suggestion) {
        const trimmed = (query || '').trim();
        if (!trimmed) {
            return suggestion;
        }
        const parts = trimmed.split(/\s+/);
        const prefix = parts[parts.length - 1].startsWith('-') ? '-' : '';
        parts[parts.length - 1] = prefix + suggestion;
        return parts.join(' ');
    }

    function normalizeSuggestions(payload) {
        if (Array.isArray(payload)) {
            return payload.filter(item => typeof item === 'string');
        }
        if (payload && Array.isArray(payload.suggestions)) {
            return payload.suggestions.filter(item => typeof item === 'string');
        }
        if (payload && Array.isArray(payload.keywords)) {
            return payload.keywords.filter(item => typeof item === 'string');
        }
        return [];
    }

    function relativeTime(ts) {
        const diff = Date.now() - ts;
        if (diff < 60000) return 'just now';
        if (diff < 3600000) return Math.floor(diff / 60000) + 'm ago';
        if (diff < 86400000) return Math.floor(diff / 3600000) + 'h ago';
        return new Date(ts).toLocaleDateString();
    }

    input.addEventListener('focus', renderSearchDropdown);
    input.addEventListener('input', renderSearchDropdown);
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            recentDropdown.hidden = true;
        }
    });

    function renderSavedResults() {
        const entries = SavedResults.getAll();
        savedCount.textContent = entries.length;
        savedCount.hidden = !entries.length;
        savedEmpty.hidden = !!entries.length;
        savedList.innerHTML = '';
        entries.forEach(entry => {
            const li = document.createElement('li');
            li.className = 'saved-item';

            const a = document.createElement('a');
            a.href = entry.url;
            a.target = '_blank';
            a.rel = 'noopener noreferrer';
            a.className = 'saved-item-link';
            a.textContent = entry.title || entry.url;

            const meta = document.createElement('span');
            meta.className = 'saved-item-meta';
            const source = entry.query ? 'from "' + entry.query + '"' : 'saved result';
            meta.textContent = source + ' · ' + relativeTime(entry.ts);

            li.appendChild(a);
            li.appendChild(meta);
            savedList.appendChild(li);
        });
    }

    clearSavedBtn.addEventListener('click', () => {
        SavedResults.clear();
        renderSavedResults();
    });

    savedToggle.addEventListener('click', (e) => {
        e.stopPropagation();
        const shouldOpen = savedSection.hidden;
        savedSection.hidden = !shouldOpen;
        savedToggle.setAttribute('aria-expanded', shouldOpen ? 'true' : 'false');
    });

    document.addEventListener('click', (e) => {
        if (!savedSection.hidden && !savedSection.contains(e.target) && e.target !== savedToggle) {
            savedSection.hidden = true;
            savedToggle.setAttribute('aria-expanded', 'false');
        }
        if (!recentDropdown.hidden && !form.contains(e.target)) {
            recentDropdown.hidden = true;
        }
    });

    // Save query to history on form submit
    form.addEventListener('submit', () => {
        const q = input.value.trim();
        if (q) SearchHistory.push(q);
    });

    renderSavedResults();
})();
