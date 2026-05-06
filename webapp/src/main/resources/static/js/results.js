/**
 * results.js — Results page logic.
 *
 * 1. Reads ?q= from the URL
 * 2. Calls /api/search
 * 3. Renders result cards from the <template>
 * 4. Shows filter chips for quoted phrases and excluded terms
 * 5. Saves query to history
 */
(function () {
    const form = document.getElementById('resultsSearchForm');
    const input = document.getElementById('resultsQuery');
    const dropdown = document.getElementById('resultsAutocomplete');
    const metaEl = document.getElementById('resultsMeta');
    const loadingEl = document.getElementById('loadingState');
    const errorEl = document.getElementById('errorState');
    const errorMsg = document.getElementById('errorMessage');
    const emptyEl = document.getElementById('emptyState');
    const emptyQuery = document.getElementById('emptyQuery');
    const listEl = document.getElementById('resultsList');
    const withinResults = document.getElementById('withinResults');
    const withinInput = document.getElementById('withinResultsInput');
    const withinCount = document.getElementById('withinResultsCount');
    const withinEmpty = document.getElementById('withinEmpty');
    const filtersBar = document.getElementById('filtersBar');
    const filterChips = document.getElementById('filterChips');
    const template = document.getElementById('resultCardTemplate');
    const browseBtn = document.getElementById('resultsBrowseKeywords');
    let allResults = [];
    let currentQuery = '';
    let excludedTerms = [];

    // Autocomplete
    initAutocomplete(input, dropdown);
    initKeywordBrowser({
        browseButton: browseBtn,
        panel: document.getElementById('resultsKeywordBrowser'),
        filterInput: document.getElementById('resultsKeywordFilter'),
        listEl: document.getElementById('resultsKeywordList'),
        selectedWrap: document.getElementById('resultsKeywordSelected'),
        selectedChips: document.getElementById('resultsKeywordSelectedChips'),
        clearButton: document.getElementById('resultsKeywordClear'),
        queryInput: input,
        onQueryChange(nextQuery) {
            if (!nextQuery.trim()) return;
            window.location.href = '/results.html?q=' + encodeURIComponent(nextQuery.trim());
        }
    });

    // ── Parse query from URL ─────────────────────────────────
    const params = new URLSearchParams(window.location.search);
    const rawQuery = params.get('q') || '';

    if (!rawQuery.trim()) {
        window.location.href = '/index.html';
        return;
    }

    input.value = rawQuery;
    document.title = rawQuery + ' — HKUST Search';

    // Save to history
    SearchHistory.push(rawQuery);

    // Render phrase chips
    renderFilterChips(rawQuery);

    // Kick off search
    doSearch(rawQuery);

    // Re-search on form submit
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const q = input.value.trim();
        if (!q) return;
        window.location.href = '/results.html?q=' + encodeURIComponent(q);
    });

    // ── Filter chips ─────────────────────────────────────────
    function renderFilterChips(query) {
        const parts = parseQueryParts(query);
        excludedTerms = parts.excludedTerms;
        const chips = [];

        parts.phrases.forEach(phrase => chips.push({ text: '"' + phrase + '"', className: 'filter-chip phrase' }));
        parts.excludedTerms.forEach(term => chips.push({ text: '-' + term, className: 'filter-chip exclude' }));

        if (!chips.length) { filtersBar.hidden = true; return; }

        filtersBar.hidden = false;
        filterChips.innerHTML = '';
        chips.forEach(chip => {
            const span = document.createElement('span');
            span.className = chip.className;
            span.textContent = chip.text;
            filterChips.appendChild(span);
        });
    }

    // ── Fetch & render ───────────────────────────────────────
    function show(el) { el.hidden = false; }
    function hide(el) { el.hidden = true; }

    function doSearch(query) {
        hide(errorEl); hide(emptyEl); hide(listEl);
        hide(withinResults); hide(withinEmpty);
        show(loadingEl);
        metaEl.textContent = '';

        fetch('/api/search?q=' + encodeURIComponent(query) + '&max=50')
            .then(r => {
                if (!r.ok) throw new Error('HTTP ' + r.status);
                return r.json();
            })
            .then(data => {
                hide(loadingEl);
                const normalized = normalizeSearchResponse(data, query);
                if (!normalized.results.length) {
                    emptyQuery.textContent = '"' + query + '"';
                    show(emptyEl);
                    metaEl.textContent = 'No results found.';
                    return;
                }
                metaEl.textContent =
                    'About ' + normalized.totalResults + ' result' +
                    (normalized.totalResults !== 1 ? 's' : '') + ' for "' + normalized.query + '"';
                currentQuery = normalized.query;
                allResults = applyExcludedTerms(normalized.results);
                withinResults.hidden = false;
                withinInput.value = '';
                renderResults(allResults);
                updateWithinCount(allResults.length, allResults.length);
                show(listEl);
            })
            .catch(err => {
                hide(loadingEl);
                errorMsg.textContent = 'Search failed: ' + err.message + '. Is the server running?';
                show(errorEl);
            });
    }

    // ── Card rendering ───────────────────────────────────────
    function renderResults(results) {
        listEl.innerHTML = '';
        withinEmpty.hidden = !!results.length || (!allResults.length && !excludedTerms.length);
        results.forEach(r => {
            const card = template.content.cloneNode(true);

            // Score badge
            const badge = card.querySelector('.result-score-badge');
            const scoreText = formatScore(r.score);
            badge.textContent = scoreText;
            badge.setAttribute('aria-label', 'Score ' + scoreText);

            // Title + URL
            const titleLink = card.querySelector('.result-title-link');
            titleLink.textContent = r.title || r.url;
            titleLink.href = r.url;

            const urlEl = card.querySelector('.result-url');
            urlEl.textContent = r.url;
            urlEl.href = r.url;

            // Meta line
            const metaLine = card.querySelector('.result-meta-line');
            const parts = [];
            if (r.lastModified) parts.push(r.lastModified);
            if (typeof r.sizeText === 'string' && r.sizeText) {
                parts.push(r.sizeText);
            } else if (r.sizeBytes > 0) {
                parts.push(formatBytes(r.sizeBytes));
            }
            metaLine.textContent = parts.join(' · ');

            // Keywords
            const kwEl = card.querySelector('.result-keywords');
            const keywordParts = Array.isArray(r.keywords)
                ? r.keywords.slice(0, 5).map(formatKeywordChip).filter(Boolean)
                : [];
            if (keywordParts.length) {
                kwEl.innerHTML = keywordParts.join('');
            } else {
                kwEl.hidden = true;
            }

            const saveBtn = card.querySelector('.result-save');
            if (SavedResults.has(r.url)) {
                saveBtn.textContent = 'Saved';
                saveBtn.disabled = true;
            }
            saveBtn.addEventListener('click', () => {
                SavedResults.push({
                    title: r.title || r.url,
                    url: r.url,
                    score: scoreText,
                    query: currentQuery,
                    ts: Date.now()
                });
                saveBtn.textContent = 'Saved';
                saveBtn.disabled = true;
            });

            const similarBtn = card.querySelector('.result-similar');
            const similarQuery = buildSimilarQuery(r);
            if (!similarQuery) {
                similarBtn.disabled = true;
            }
            similarBtn.addEventListener('click', () => {
                if (!similarQuery) return;
                SearchHistory.push(similarQuery);
                window.location.href = '/results.html?q=' + encodeURIComponent(similarQuery);
            });

            // Links
            const parentList = card.querySelector('.parent-links-list');
            const childList = card.querySelector('.child-links-list');
            populateLinkList(parentList, r.parentLinks);
            populateLinkList(childList, r.childLinks);

            // Hide links section if empty
            if (!r.parentLinks?.length && !r.childLinks?.length) {
                card.querySelector('.result-links-details').hidden = true;
            }

            listEl.appendChild(card);
        });
    }

    withinInput.addEventListener('input', () => {
        const filtered = filterCurrentResults(withinInput.value);
        renderResults(filtered);
        updateWithinCount(filtered.length, allResults.length);
    });

    function filterCurrentResults(filterValue) {
        const needle = normalizeText(filterValue);
        if (!needle) {
            return allResults;
        }
        return allResults.filter(result => normalizeText([
            result.title,
            result.url,
            result.lastModified,
            result.sizeText,
            keywordSearchText(result.keywords),
            result.parentLinks.join(' '),
            result.childLinks.join(' ')
        ].join(' ')).includes(needle));
    }

    function applyExcludedTerms(results) {
        if (!excludedTerms.length) {
            return results;
        }
        return results.filter(result => {
            const haystack = resultSearchText(result);
            return !excludedTerms.some(term => haystack.includes(normalizeText(term)));
        });
    }

    function resultSearchText(result) {
        return normalizeText([
            result.title,
            result.url,
            result.lastModified,
            result.sizeText,
            keywordSearchText(result.keywords),
            result.parentLinks.join(' '),
            result.childLinks.join(' ')
        ].join(' '));
    }

    function updateWithinCount(visible, total) {
        withinCount.textContent = 'Showing ' + visible + ' of ' + total + ' result' + (total !== 1 ? 's' : '');
        if (!visible && total) {
            metaEl.textContent = 'No current results match "' + withinInput.value.trim() + '".';
        } else if (total) {
            metaEl.textContent = 'About ' + total + ' result' + (total !== 1 ? 's' : '') + ' for "' + currentQuery + '"';
        }
    }

    function buildSimilarQuery(result) {
        return result.keywords
            .map(keywordTerm)
            .filter(Boolean)
            .slice(0, 5)
            .join(' ');
    }

    function keywordSearchText(keywords) {
        return keywords.map(entry => {
            const term = keywordTerm(entry);
            const freq = typeof entry === 'object' ? firstFinite(entry.freq, entry.frequency, entry.count) : null;
            return term + (freq === null ? '' : ' ' + freq);
        }).join(' ');
    }

    function keywordTerm(entry) {
        if (typeof entry === 'string') {
            return entry.trim();
        }
        if (!entry || typeof entry !== 'object') {
            return '';
        }
        return firstNonEmpty(entry.term, entry.keyword, entry.word, entry.stem);
    }

    function normalizeText(value) {
        return String(value || '').toLowerCase().trim();
    }

    function parseQueryParts(query) {
        const phrases = [];
        const excludedTerms = [];
        const text = query || '';
        let withoutPhrases = text.replace(/"([^"]+)"/g, (_, phrase) => {
            const clean = phrase.trim();
            if (clean) {
                phrases.push(clean);
            }
            return ' ';
        });

        withoutPhrases.trim().split(/\s+/).forEach(token => {
            const match = token.match(/^-(.+)$/);
            if (!match) {
                return;
            }
            const term = match[1].trim();
            if (term) {
                excludedTerms.push(term);
            }
        });

        return { phrases, excludedTerms };
    }

    function populateLinkList(ul, links) {
        if (!links || !links.length) {
            const li = document.createElement('li');
            li.style.color = 'var(--gray-500)';
            li.style.fontSize = '.78rem';
            li.textContent = 'None';
            ul.appendChild(li);
            return;
        }
        links.forEach(url => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = url;
            a.target = '_blank';
            a.rel = 'noopener noreferrer';
            a.textContent = url;
            li.appendChild(a);
            ul.appendChild(li);
        });
    }

    function formatBytes(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function formatScore(score) {
        const numeric = Number(score);
        if (!Number.isFinite(numeric)) return '0.0';
        if (numeric >= 0 && numeric <= 1) return (numeric * 100).toFixed(1);
        return numeric.toFixed(1);
    }

    function formatKeywordChip(entry) {
        if (typeof entry === 'string') {
            return '<span class="keyword-chip">' + escapeHtml(entry) + '</span>';
        }
        if (!entry || typeof entry !== 'object') {
            return '';
        }
        const term = firstNonEmpty(entry.term, entry.keyword, entry.word, entry.stem);
        const freq = firstFinite(entry.freq, entry.frequency, entry.count);
        if (!term) {
            return '';
        }
        if (freq === null) {
            return '<span class="keyword-chip">' + escapeHtml(term) + '</span>';
        }
        return '<span class="keyword-chip">' +
            '<span class="keyword-chip-term">' + escapeHtml(term) + '</span>' +
            '<span class="keyword-chip-freq">' + freq + '</span>' +
            '</span>';
    }

    function normalizeSearchResponse(data, fallbackQuery) {
        const rawResults = Array.isArray(data?.results)
            ? data.results
            : Array.isArray(data?.items)
                ? data.items
                : [];
        const results = rawResults.map(normalizeResult).filter(r => r.url);
        const totalResults = firstFinite(
            data?.totalResults,
            data?.total,
            data?.count,
            results.length
        );
        return {
            query: firstNonEmpty(data?.query, fallbackQuery) || '',
            totalResults: totalResults === null ? results.length : totalResults,
            results: results.slice(0, 50)
        };
    }

    function normalizeResult(raw) {
        const sizeBytes = firstFinite(raw?.sizeBytes, raw?.pageSizeBytes, raw?.size);
        return {
            score: firstFinite(raw?.score, raw?.similarity, raw?.rankScore, 0) || 0,
            title: firstNonEmpty(raw?.title, raw?.pageTitle, raw?.name) || '',
            url: firstNonEmpty(raw?.url, raw?.link, raw?.pageUrl) || '',
            lastModified: firstNonEmpty(raw?.lastModified, raw?.lastModifiedDate, raw?.date) || '',
            sizeBytes: sizeBytes === null ? 0 : sizeBytes,
            sizeText: typeof raw?.size === 'string' ? raw.size : '',
            keywords: normalizeKeywords(raw),
            parentLinks: normalizeLinks(raw?.parentLinks, raw?.parents, raw?.parentUrls),
            childLinks: normalizeLinks(raw?.childLinks, raw?.children, raw?.childUrls)
        };
    }

    function normalizeKeywords(raw) {
        if (Array.isArray(raw?.keywords)) {
            return raw.keywords;
        }
        if (Array.isArray(raw?.topKeywords)) {
            return raw.topKeywords;
        }
        if (Array.isArray(raw?.keywordFrequencies)) {
            return raw.keywordFrequencies;
        }
        return [];
    }

    function normalizeLinks(...candidates) {
        for (const links of candidates) {
            if (!Array.isArray(links)) {
                continue;
            }
            return links
                .map(link => {
                    if (typeof link === 'string') {
                        return link;
                    }
                    if (link && typeof link === 'object') {
                        return firstNonEmpty(link.url, link.link, link.href) || '';
                    }
                    return '';
                })
                .filter(Boolean);
        }
        return [];
    }

    function firstNonEmpty(...values) {
        for (const value of values) {
            if (typeof value === 'string' && value.trim()) {
                return value.trim();
            }
        }
        return '';
    }

    function firstFinite(...values) {
        for (const value of values) {
            const numeric = Number(value);
            if (Number.isFinite(numeric)) {
                return numeric;
            }
        }
        return null;
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }
})();
