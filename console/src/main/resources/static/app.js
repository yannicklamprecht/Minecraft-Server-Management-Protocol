(() => {
    'use strict';

    const SERVER_STORAGE_KEY = 'msmp-console-server-id';
    // How often the sidebar re-checks each configured server's online/offline state and Minecraft
    // version. The rest of the app only refreshes in response to specific SSE events (which never
    // fire for a server that isn't connected in the first place), so without this a server coming
    // online or dropping offline after the initial page load would never be reflected.
    const SERVER_LIST_REFRESH_INTERVAL_MS = 10000;
    // Sections that assume a live connection to the selected server; swapped out for #offline-panel
    // (troubleshooting steps + a retry button) whenever it isn't currently connected. players-section
    // is deliberately NOT here: its "Notify operators" checkbox is a standing preference, not
    // something that only makes sense while connected, so it (and the section housing it) stays
    // visible either way - only the actual player rows get cleared out while offline, below.
    const ONLINE_ONLY_SECTION_IDS = ['status-strip', 'control-section', 'lists-section'];

    let currentServerId = null;
    let eventSource = null;
    let servers = [];
    let hideOfflineServers = false;

    // Tracks "serverId:playerId" pairs with a kick request in flight. Must live outside
    // renderPlayers: the MSMP server emits a "server-status" heartbeat notification independent
    // of any user action, which triggers a fresh renderPlayers() call (new, non-disabled button
    // elements) while a previous kick is still awaiting its response. Without state that survives
    // that re-render, a re-click on the "reset-looking" button sends a second kick for the same
    // player while the first is still being processed, and the Minecraft server does not
    // tolerate a second disconnect for a connection that's already being torn down. (The API
    // itself also rejects a genuine duplicate outright - see ManagementApiController - this just
    // keeps the UI from inviting one in the first place.)
    const kicksInFlight = new Set();

    // Ids/names of players currently listed as operators, kept in sync from renderOperators() so
    // each player row's "Make operator"/"Remove operator" button can show the right label without
    // needing its own round trip. Keyed the same way as everywhere else: id if known, else name.
    let operatorKeys = new Set();

    function operatorKey(player) {
        return player?.id ?? player?.name;
    }

    async function api(path, options) {
        const response = await fetch(path, {
            headers: { 'Content-Type': 'application/json' },
            ...options,
        });
        const text = await response.text();
        if (!response.ok) {
            // The API returns {"error": "..."} for failures the Minecraft server itself reported
            // (see ManagementApiExceptionHandler); fall back to a generic message otherwise.
            let message = `${options?.method ?? 'GET'} ${path} failed: ${response.status}`;
            try {
                const body = text ? JSON.parse(text) : null;
                if (body && body.error) message = body.error;
            } catch {
                // response body wasn't JSON; keep the generic message
            }
            throw new Error(message);
        }
        return text ? JSON.parse(text) : null;
    }

    /** Path for an endpoint scoped to the currently selected server. */
    function serverPath(suffix) {
        return `/api/servers/${encodeURIComponent(currentServerId)}${suffix}`;
    }

    /** The "Notify operators" checkbox's current state, or undefined if console.ui.operator-notifications
     *  is NEVER/ALWAYS (no checkbox shown - the server enforces the mode either way regardless of
     *  what's sent). JSON.stringify drops undefined-valued keys, so spreading this into a request
     *  body naturally omits the field when there's nothing meaningful to send. */
    function notifyOperatorsRequested() {
        return document.getElementById('notify-operators-checkbox')?.checked;
    }

    /** Appends the given params to a URL's query string, for DELETE requests (which have no JSON
     *  body to carry them in) - e.g. `notifyOperators` and `playerDisplayName`. Params that are
     *  undefined, null, or empty are skipped entirely rather than sent as the literal string
     *  "undefined"/"null" (notifyOperatorsRequested() returns undefined when there's no checkbox). */
    function withQuery(path, params) {
        const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '');
        if (entries.length === 0) return path;
        const query = entries.map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&');
        return `${path}${path.includes('?') ? '&' : '?'}${query}`;
    }

    function playerLabel(player) {
        if (!player) return 'unknown';
        return player.name ?? player.id ?? 'unknown';
    }

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function renderList(listElement, items, emptyText, renderItem) {
        listElement.innerHTML = '';
        if (!items || items.length === 0) {
            listElement.appendChild(el('li', 'empty', emptyText));
            return;
        }
        for (const item of items) {
            listElement.appendChild(renderItem(item));
        }
    }

    function removableRow(label, onRemove) {
        const li = el('li');
        li.appendChild(el('span', null, label));
        const button = el('button', null, 'remove');
        button.addEventListener('click', () => onRemove().catch(reportError));
        li.appendChild(button);
        return li;
    }

    function reportError(error) {
        console.error(error);
        logActivity(`error: ${error.message}`, true);
    }

    function logActivity(text, isError, timestamp) {
        const feed = document.getElementById('activity-feed');
        feed.querySelector('.empty')?.remove();
        const li = el('li', isError ? 'log-error' : null);
        li.appendChild(el('span', 'log-time', (timestamp ?? new Date()).toLocaleTimeString()));
        li.appendChild(document.createTextNode(text));
        feed.insertBefore(li, feed.firstChild);
        while (feed.children.length > 80) {
            feed.removeChild(feed.lastChild);
        }
    }

    function resetActivityFeed(serverId) {
        const feed = document.getElementById('activity-feed');
        feed.innerHTML = '';
        feed.appendChild(el('li', 'empty', `Watching ${serverId} for activity…`));
    }

    // "PlayerJoined" -> "player-joined", matching the live SSE event names.
    function activityEventName(type) {
        let result = '';
        for (let i = 0; i < type.length; i++) {
            const c = type[i];
            if (i > 0 && c !== c.toLowerCase()) result += '-';
            result += c.toLowerCase();
        }
        return result;
    }

    // Mirrors how the corresponding live action's own success handler phrases the same activity
    // (see the message-form handler below) so history and live entries read the same way.
    function activityLabel(record) {
        if (record.type === 'MessageSent') return `broadcast: ${record.payload}`;
        if (record.type === 'PrivateMessageSent') return `message to ${record.payload}`;
        if (record.type === 'PlayerJoined' || record.type === 'PlayerLeft') {
            return `${activityEventName(record.type)}: ${playerLabel(record.payload)}`;
        }
        return activityEventName(record.type);
    }

    function loadActivity(serverId) {
        return api(`/api/servers/${encodeURIComponent(serverId)}/activity`)
            .then((records) => {
                if (serverId !== currentServerId) return; // server was switched again while this was in flight
                for (const record of records) {
                    logActivity(activityLabel(record), false, new Date(record.timestamp));
                }
            })
            .catch(reportError);
    }

    /** Swaps the main column between its normal content and the offline-troubleshooting panel,
     *  based on the given server (from the /api/servers listing, not a live status call). */
    function renderConnectionState(server) {
        const offline = Boolean(server) && !server.online;
        document.getElementById('offline-panel').hidden = !offline;
        for (const id of ONLINE_ONLY_SECTION_IDS) {
            document.getElementById(id).hidden = offline;
        }
        if (offline) {
            document.getElementById('offline-server-id').textContent = server.id;
            document.getElementById('offline-server-url').textContent = server.url;
            renderPlayers([]); // clear out any stale roster from before it went offline; the
                                // section itself (heading + "Notify operators" checkbox) stays up
        }
    }

    function setConnectionBadge(state, protocolVersion) {
        const badge = document.getElementById('connection-badge');
        badge.dataset.state = state;
        if (state === 'connected' && protocolVersion) {
            badge.textContent = `MSMP ${protocolVersion}`;
        } else if (state === 'disconnected') {
            badge.textContent = 'reconnecting…';
        } else {
            badge.textContent = 'connecting…';
        }
    }

    function renderStatus(status) {
        document.getElementById('status-started').textContent = status.started ? 'yes' : 'no';
        document.getElementById('status-version').textContent = status.version ? status.version.name : 'unknown';
        document.getElementById('status-player-count').textContent = (status.players ?? []).length;
        renderPlayers(status.players ?? []);
    }

    /** Relabels each player row's op-toggle button in place, without touching the rest of the
     *  players list - a full re-render there would collapse any actions panel the user has open. */
    function syncOperatorButtons() {
        for (const button of document.querySelectorAll('#players-list [data-role="toggle-op"]')) {
            button.textContent = operatorKeys.has(button.dataset.playerId) ? 'Remove operator' : 'Make operator';
        }
    }

    function actionButton(className, text, onClick) {
        const button = el('button', className, text);
        button.type = 'button';
        button.addEventListener('click', onClick);
        return button;
    }

    /** One player's expandable actions panel, built from a native <details> element (no
     *  prompt()/modal): a private message row, and a moderation row with a shared reason field
     *  feeding Kick/Ban plus an operator toggle. */
    function renderPlayers(players) {
        renderList(document.getElementById('players-list'), players, 'No players online', (player) => {
            const playerId = player.id ?? player.name;
            const label = playerLabel(player);

            const li = el('li', 'player-row');
            const details = document.createElement('details');
            details.className = 'player-actions';

            const summary = document.createElement('summary');
            summary.appendChild(el('span', 'player-name', label));
            details.appendChild(summary);

            const panel = el('div', 'player-actions-panel');

            // Private message
            const messageRow = el('div', 'action-row');
            const messageInput = document.createElement('input');
            messageInput.type = 'text';
            messageInput.placeholder = 'Private message';
            const messageButton = actionButton('btn btn-quiet', 'Send', () => {
                const message = messageInput.value.trim();
                if (!message) return;
                messageButton.disabled = true;
                api(serverPath('/messages'), {
                    method: 'POST',
                    body: JSON.stringify({ message, playerNameOrUuid: playerId }),
                }).then(() => {
                    logActivity(`message to ${label}: ${message}`);
                    messageInput.value = '';
                }).catch(reportError)
                    .finally(() => { messageButton.disabled = false; });
            });
            messageRow.append(messageInput, messageButton);
            panel.appendChild(messageRow);

            // Kick / ban / op, sharing one reason field - one toolbar-style row rather than
            // separate ones, so the expanded panel doesn't take up more height than it needs to.
            const modRow = el('div', 'action-row');
            const reasonInput = document.createElement('input');
            reasonInput.type = 'text';
            reasonInput.placeholder = 'Reason (kick/ban)';

            const kickKey = `${currentServerId}:${playerId}`;
            const kickInFlight = kicksInFlight.has(kickKey);
            const kickButton = actionButton('btn btn-quiet', kickInFlight ? 'Kicking…' : 'Kick', () => {
                if (kicksInFlight.has(kickKey)) return;
                kicksInFlight.add(kickKey);
                kickButton.disabled = true;
                kickButton.textContent = 'Kicking…';
                api(serverPath('/players/kick'), {
                    method: 'POST',
                    body: JSON.stringify({ playerNameOrUuid: playerId, reason: reasonInput.value, notifyOperators: notifyOperatorsRequested(), playerDisplayName: label }),
                }).then(() => { logActivity(`kicked ${label}`); reasonInput.value = ''; })
                    .catch(reportError)
                    .finally(() => {
                        kicksInFlight.delete(kickKey);
                        kickButton.disabled = false;
                        kickButton.textContent = 'Kick';
                    });
            });
            kickButton.disabled = kickInFlight;

            const banButton = actionButton('btn btn-danger', 'Ban', () => {
                banButton.disabled = true;
                api(serverPath('/bans/users'), {
                    method: 'POST',
                    body: JSON.stringify({ playerNameOrUuid: playerId, reason: reasonInput.value, source: null, expires: null, notifyOperators: notifyOperatorsRequested(), playerDisplayName: label }),
                }).then(() => { reasonInput.value = ''; })
                    .catch(reportError)
                    .finally(() => { banButton.disabled = false; });
            });

            const opButton = actionButton('btn btn-quiet', operatorKeys.has(playerId) ? 'Remove operator' : 'Make operator', () => {
                opButton.disabled = true;
                const request = operatorKeys.has(playerId)
                    ? api(withQuery(serverPath(`/operators/${encodeURIComponent(playerId)}`),
                        { notifyOperators: notifyOperatorsRequested(), playerDisplayName: label }), { method: 'DELETE' })
                    : api(serverPath('/operators'), {
                        method: 'POST',
                        body: JSON.stringify({ playerNameOrUuid: playerId, permissionLevel: 4, bypassesPlayerLimit: false, notifyOperators: notifyOperatorsRequested(), playerDisplayName: label }),
                    });
                request.catch(reportError).finally(() => { opButton.disabled = false; });
            });
            opButton.dataset.role = 'toggle-op';
            opButton.dataset.playerId = playerId;

            modRow.append(reasonInput, kickButton, banButton, opButton);
            panel.appendChild(modRow);

            details.appendChild(panel);
            li.appendChild(details);
            return li;
        });
    }

    function renderAllowlist(players) {
        renderList(document.getElementById('allowlist-list'), players, 'Allowlist is empty', (player) =>
            removableRow(playerLabel(player), () =>
                api(serverPath(`/allowlist/${encodeURIComponent(player.id ?? player.name)}`), { method: 'DELETE' })
                    .then(loadAllowlist)));
    }

    function renderOperators(operators) {
        operatorKeys = new Set(operators.map((op) => operatorKey(op.player)));
        renderList(document.getElementById('operators-list'), operators, 'No operators', (op) =>
            removableRow(`${playerLabel(op.player)} (lvl ${op.permissionLevel})`, () =>
                api(withQuery(serverPath(`/operators/${encodeURIComponent(op.player.id ?? op.player.name)}`),
                    { notifyOperators: notifyOperatorsRequested(), playerDisplayName: playerLabel(op.player) }), { method: 'DELETE' })
                    .then(loadOperators)));
        syncOperatorButtons();
    }

    function renderBans(bans) {
        renderList(document.getElementById('bans-list'), bans, 'No bans', (ban) =>
            removableRow(`${playerLabel(ban.player)}${ban.reason ? ' — ' + ban.reason : ''}`, () =>
                api(serverPath(`/bans/users/${encodeURIComponent(ban.player.id ?? ban.player.name)}`), { method: 'DELETE' })
                    .then(loadBans)));
    }

    function loadStatus() {
        return api(serverPath('/status')).then(renderStatus).catch(reportError);
    }

    function loadAllowlist() {
        return api(serverPath('/allowlist')).then(renderAllowlist).catch(reportError);
    }

    function loadOperators() {
        return api(serverPath('/operators')).then(renderOperators).catch(reportError);
    }

    function loadBans() {
        return api(serverPath('/bans/users')).then(renderBans).catch(reportError);
    }

    function loadAll() {
        loadStatus();
        loadAllowlist();
        loadOperators();
        loadBans();
    }

    /** Handler for the offline panel's "Retry connection" button: attempts to reconnect whichever
     *  server was selected when clicked (not necessarily the one still selected once it resolves -
     *  the user may have switched away in the meantime), then applies the result everywhere that
     *  server's state is reflected. */
    function retryConnection() {
        const serverId = currentServerId;
        if (!serverId) return;
        const button = document.getElementById('retry-connect-btn');
        button.disabled = true;
        button.textContent = 'Retrying…';
        api(`/api/servers/${encodeURIComponent(serverId)}/reconnect`, { method: 'POST' })
            .then((summary) => {
                const index = servers.findIndex((s) => s.id === serverId);
                if (index >= 0) servers[index] = summary; else servers.push(summary);
                renderServerRail();
                if (serverId !== currentServerId) return; // switched to a different server meanwhile
                renderConnectionState(summary);
                if (summary.online) {
                    logActivity('reconnected');
                    loadAll();
                } else {
                    logActivity('still offline', true);
                }
            })
            .catch(reportError)
            .finally(() => {
                button.disabled = false;
                button.textContent = 'Retry connection';
            });
    }

    function bindForm(id, handler) {
        const form = document.getElementById(id);
        form.addEventListener('submit', (event) => {
            event.preventDefault();
            const data = Object.fromEntries(new FormData(form).entries());
            handler(data, form).catch(reportError);
        });
    }

    function initTabs() {
        const tabs = document.querySelectorAll('.tab');
        for (const tab of tabs) {
            tab.addEventListener('click', () => {
                for (const other of tabs) other.setAttribute('aria-selected', String(other === tab));
                for (const panel of document.querySelectorAll('.tab-panel')) {
                    panel.hidden = panel.dataset.panel !== tab.dataset.tab;
                }
            });
        }
    }

    function visibleServers() {
        return hideOfflineServers ? servers.filter((s) => s.online) : servers;
    }

    function renderServerRail() {
        const list = document.getElementById('server-list');
        list.innerHTML = '';
        const visible = visibleServers();
        if (visible.length === 0) {
            list.appendChild(el('li', 'empty', hideOfflineServers ? 'No servers online' : 'No servers configured'));
            return;
        }
        for (const server of visible) {
            const li = el('li');
            const button = el('button', 'server-item');
            button.type = 'button';
            button.appendChild(el('span', 'server-id', server.id));

            const versionLabel = server.online ? (server.minecraftVersion ?? 'unknown') : 'offline';
            const versionBadge = el('span', 'server-version', versionLabel);
            versionBadge.dataset.state = server.online ? 'online' : 'offline';
            if (server.online) versionBadge.title = `MSMP ${server.protocolVersion}`;
            button.appendChild(versionBadge);

            button.setAttribute('aria-current', String(server.id === currentServerId));
            button.addEventListener('click', () => selectServer(server.id));
            li.appendChild(button);
            list.appendChild(li);
        }
    }

    /** Re-fetches server online/offline state and re-renders the sidebar. If the currently
     *  selected server's online state changed, also swaps the main column's content accordingly -
     *  loading its data if it just came back online, or bumping it to the troubleshooting panel if
     *  it just went offline - without disturbing anything if nothing changed. */
    function refreshServers() {
        return api('/api/servers').then((updated) => {
            const wasOnline = servers.find((s) => s.id === currentServerId)?.online;
            servers = updated;
            renderServerRail();
            const current = servers.find((s) => s.id === currentServerId);
            if (!current) return;
            renderConnectionState(current);
            if (current.online && !wasOnline) {
                logActivity('reconnected');
                loadAll();
            }
        }).catch(reportError);
    }

    async function loadServers() {
        hideOfflineServers = document.getElementById('app-shell').dataset.hideOfflineServers === 'true';
        servers = await api('/api/servers');
        const noServersNotice = document.getElementById('no-servers-notice');
        const shell = document.getElementById('app-shell');

        if (servers.length === 0) {
            noServersNotice.hidden = false;
            shell.hidden = true;
            return null;
        }

        noServersNotice.hidden = true;
        shell.hidden = false;

        let saved = null;
        try {
            saved = localStorage.getItem(SERVER_STORAGE_KEY);
        } catch {
            // localStorage unavailable (private browsing etc.) - fall back to the first server.
        }
        // Prefer a server that's actually visible in the (possibly filtered) sidebar, but fall
        // back to any configured server rather than selecting nothing, e.g. if every server
        // happens to be offline when the page loads.
        const candidates = visibleServers().length > 0 ? visibleServers() : servers;
        return candidates.some((s) => s.id === saved) ? saved : candidates[0].id;
    }

    function selectServer(serverId) {
        currentServerId = serverId;
        try {
            localStorage.setItem(SERVER_STORAGE_KEY, serverId);
        } catch {
            // ignore - just means the choice won't be remembered across reloads
        }
        renderServerRail();
        const server = servers.find((s) => s.id === serverId);
        setConnectionBadge('connected', server?.protocolVersion);
        renderConnectionState(server);
        resetActivityFeed(serverId);
        loadActivity(serverId); // works offline too - it's server-side history, not a live call
        if (server?.online) {
            loadAll();
        }
    }

    function connectEvents() {
        if (eventSource) return; // one connection carries every server's notifications
        eventSource = new EventSource('/api/events');
        eventSource.onerror = () => {
            const server = servers.find((s) => s.id === currentServerId);
            setConnectionBadge('disconnected', server?.protocolVersion);
        };

        const refreshers = {
            'player-joined': loadStatus,
            'player-left': loadStatus,
            'server-status': loadStatus,
            'allowlist-added': loadAllowlist,
            'allowlist-removed': loadAllowlist,
            'operator-added': loadOperators,
            'operator-removed': loadOperators,
            'user-ban-added': loadBans,
            'user-ban-removed': loadBans,
        };

        const otherEventNames = ['server-started', 'server-stopping', 'server-saved', 'server-saving',
            'server-activity', 'ip-ban-added', 'ip-ban-removed', 'game-rule-updated',
            'world-upgrade-started', 'world-upgrade-progress', 'world-upgrade-finished', 'world-upgrade-failed'];

        function handle(eventName, refresh) {
            return (event) => {
                const { serverId, payload } = JSON.parse(event.data);
                if (serverId !== currentServerId) return; // event for a server we're not looking at
                const label = (eventName === 'player-joined' || eventName === 'player-left')
                    ? `${eventName}: ${playerLabel(payload)}`
                    : eventName;
                logActivity(label);
                if (refresh) refresh();
            };
        }

        for (const [eventName, refresh] of Object.entries(refreshers)) {
            eventSource.addEventListener(eventName, handle(eventName, refresh));
        }
        for (const eventName of otherEventNames) {
            eventSource.addEventListener(eventName, handle(eventName, null));
        }
    }

    document.addEventListener('DOMContentLoaded', async () => {
        initTabs();

        document.getElementById('retry-connect-btn').addEventListener('click', retryConnection);

        bindForm('save-form', (data) => api(serverPath('/save'), {
            method: 'POST',
            body: JSON.stringify({ flush: data.flush === 'on' }),
        }).then(() => logActivity('world saved')));

        bindForm('message-form', (data, form) => api(serverPath('/messages'), {
            method: 'POST',
            body: JSON.stringify({ message: data.message }),
        }).then(() => { form.reset(); logActivity(`broadcast: ${data.message}`); }));

        bindForm('allowlist-form', (data, form) => api(serverPath('/allowlist'), {
            method: 'POST',
            body: JSON.stringify({ playerNameOrUuid: data.playerNameOrUuid }),
        }).then(() => { form.reset(); return loadAllowlist(); }));

        bindForm('operator-form', (data, form) => api(serverPath('/operators'), {
            method: 'POST',
            body: JSON.stringify({
                playerNameOrUuid: data.playerNameOrUuid,
                permissionLevel: Number(data.permissionLevel || 4),
                bypassesPlayerLimit: data.bypassesPlayerLimit === 'on',
                notifyOperators: notifyOperatorsRequested(),
            }),
        }).then(() => { form.reset(); return loadOperators(); }));

        bindForm('ban-form', (data, form) => api(serverPath('/bans/users'), {
            method: 'POST',
            body: JSON.stringify({ playerNameOrUuid: data.playerNameOrUuid, reason: data.reason, source: null, expires: null, notifyOperators: notifyOperatorsRequested() }),
        }).then(() => { form.reset(); return loadBans(); }));

        const initialServerId = await loadServers().catch((error) => {
            reportError(error);
            return null;
        });
        if (initialServerId) {
            selectServer(initialServerId);
        }
        connectEvents();
        if (servers.length > 0) {
            setInterval(refreshServers, SERVER_LIST_REFRESH_INTERVAL_MS);
        }
    });
})();
