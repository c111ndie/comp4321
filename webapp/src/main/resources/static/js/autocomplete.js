/**
 * autocomplete.js — shared autocomplete widget.
 * Call initAutocomplete(inputEl, dropdownEl) to attach to any input.
 * Suggestions are optional: failures are swallowed so plain search keeps working.
 */
(function () {
    const DEBOUNCE_MS = 220;
    const BROWSE_LIMIT = 40;
    let keywordCatalogPromise = null;

    function initAutocomplete(inputEl, dropdownEl) {
        if (!inputEl || !dropdownEl) return;

        let timer = null;
        let activeIdx = -1;
        let currentItems = [];
        let lastFetchedQuery = null;

        function show(items) {
            currentItems = items;
            activeIdx = -1;
            dropdownEl.innerHTML = '';

            if (!items.length) { dropdownEl.hidden = true; return; }

            items.forEach((text, i) => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'autocomplete-item';
                btn.textContent = text;
                btn.dataset.idx = i;
                btn.addEventListener('mousedown', (e) => {
                    e.preventDefault();
                    selectItem(i);
                });
                dropdownEl.appendChild(btn);
            });
            dropdownEl.hidden = false;
        }

        function hide() {
            dropdownEl.hidden = true;
            activeIdx = -1;
            currentItems = [];
        }

        function selectItem(idx) {
            if (idx < 0 || idx >= currentItems.length) return;
            applySelection(currentItems[idx]);
            hide();
            inputEl.form && inputEl.form.requestSubmit
                ? inputEl.form.requestSubmit()
                : inputEl.form && inputEl.form.submit();
        }

        function applySelection(text) {
            const raw = inputEl.value;
            const trimmed = raw.trim();
            if (!trimmed) {
                inputEl.value = text;
                return;
            }

            const parts = trimmed.split(/\s+/);
            parts[parts.length - 1] = text;
            inputEl.value = parts.join(' ') + ' ';
        }

        function updateActive(newIdx) {
            const btns = dropdownEl.querySelectorAll('.autocomplete-item');
            btns.forEach((b, i) => b.setAttribute('aria-selected', i === newIdx ? 'true' : 'false'));
            activeIdx = newIdx;
        }

        function fetchSuggestions(query) {
            lastFetchedQuery = query;
            fetch('/api/suggest?q=' + encodeURIComponent(query))
                .then(r => r.ok ? r.json() : [])
                .then(items => {
                    if (lastFetchedQuery !== query) {
                        return;
                    }
                    show(normalizeSuggestions(items).slice(0, BROWSE_LIMIT));
                })
                .catch(() => hide());
        }

        inputEl.addEventListener('input', () => {
            clearTimeout(timer);
            const val = inputEl.value.trim();
            const lastToken = val.split(/\s+/).pop().replace(/^"/, '');

            timer = setTimeout(() => {
                fetchSuggestions(lastToken);
            }, DEBOUNCE_MS);
        });

        inputEl.addEventListener('focus', () => {
            const val = inputEl.value.trim();
            const lastToken = val ? val.split(/\s+/).pop().replace(/^"/, '') : '';
            fetchSuggestions(lastToken);
        });

        inputEl.addEventListener('keydown', (e) => {
            if (dropdownEl.hidden) return;
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                updateActive(Math.min(activeIdx + 1, currentItems.length - 1));
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                updateActive(Math.max(activeIdx - 1, -1));
            } else if (e.key === 'Enter' && activeIdx >= 0) {
                e.preventDefault();
                selectItem(activeIdx);
            } else if (e.key === 'Tab' && activeIdx >= 0) {
                e.preventDefault();
                selectItem(activeIdx);
            } else if (e.key === 'Escape') {
                hide();
            }
        });

        document.addEventListener('click', (e) => {
            if (!dropdownEl.contains(e.target) && e.target !== inputEl) hide();
        });
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

    function initKeywordBrowser(options) {
        const browseButton = options?.browseButton;
        const panel = options?.panel;
        const filterInput = options?.filterInput;
        const listEl = options?.listEl;
        const selectedWrap = options?.selectedWrap;
        const selectedChips = options?.selectedChips;
        const clearButton = options?.clearButton;
        const queryInput = options?.queryInput;
        const onQueryChange = options?.onQueryChange;

        if (!browseButton || !panel || !filterInput || !listEl || !selectedWrap || !selectedChips || !queryInput) {
            return;
        }

        let catalog = [];

        function syncSelectedChips() {
            const tokens = parseQueryTokens(queryInput.value);
            selectedChips.innerHTML = '';
            if (!tokens.length) {
                selectedWrap.hidden = true;
                return;
            }
            selectedWrap.hidden = false;
            tokens.forEach(token => {
                const chip = document.createElement('button');
                chip.type = 'button';
                chip.className = 'keyword-selected-chip';
                chip.textContent = token;
                chip.addEventListener('click', () => {
                    const next = parseQueryTokens(queryInput.value).filter(item => item !== token);
                    updateQuery(next.join(' '));
                });
                selectedChips.appendChild(chip);
            });
        }

        function renderList(filterValue) {
            const needle = (filterValue || '').trim().toLowerCase();
            const items = catalog
                .filter(keyword => !needle || keyword.toLowerCase().includes(needle))
                .slice(0, 120);
            listEl.innerHTML = '';

            if (!items.length) {
                const empty = document.createElement('p');
                empty.className = 'keyword-browser-empty';
                empty.textContent = 'No indexed keywords match this filter.';
                listEl.appendChild(empty);
                return;
            }

            items.forEach(keyword => {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'keyword-browser-item';
                button.textContent = keyword;
                button.addEventListener('click', () => {
                    const nextTokens = parseQueryTokens(queryInput.value);
                    if (!nextTokens.includes(keyword)) {
                        nextTokens.push(keyword);
                    }
                    updateQuery(nextTokens.join(' '), false);
                });
                listEl.appendChild(button);
            });
        }

        function updateQuery(nextQuery, shouldSubmit) {
            queryInput.value = nextQuery;
            syncSelectedChips();
            if (shouldSubmit && typeof onQueryChange === 'function') {
                onQueryChange(nextQuery);
            }
        }

        function openPanel() {
            panel.hidden = false;
            syncSelectedChips();
            loadKeywordCatalog().then(items => {
                catalog = items;
                renderList(filterInput.value);
            });
        }

        browseButton.addEventListener('click', () => {
            openPanel();
            filterInput.focus();
        });

        panel.querySelectorAll('[data-close-keyword-browser]').forEach(btn => {
            btn.addEventListener('click', () => {
                panel.hidden = true;
            });
        });

        filterInput.addEventListener('input', () => {
            renderList(filterInput.value);
        });

        queryInput.addEventListener('input', syncSelectedChips);

        if (clearButton) {
            clearButton.addEventListener('click', () => {
                updateQuery('', false);
                filterInput.value = '';
                renderList(filterInput.value);
                queryInput.focus();
            });
        }

        syncSelectedChips();
    }

    function parseQueryTokens(query) {
        return (query || '').trim().split(/\s+/).filter(Boolean);
    }

    function loadKeywordCatalog() {
        if (!keywordCatalogPromise) {
            keywordCatalogPromise = fetch('/api/keywords')
                .then(r => r.ok ? r.json() : [])
                .then(normalizeSuggestions)
                .catch(() => []);
        }
        return keywordCatalogPromise;
    }

    window.initAutocomplete = initAutocomplete;
    window.initKeywordBrowser = initKeywordBrowser;
})();
