@param boolean hideOfflineServers = false
@param boolean showNotifyOperatorsCheckbox = true
@param boolean notifyOperatorsDefaultChecked = true
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Minecraft Management Console</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Chakra+Petch:wght@500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="/app.css">
</head>
<body>
<a class="skip-link" href="#players-section">Skip to players</a>

<header class="topbar">
    <h1>Minecraft Management Console</h1>
    <div id="connection-badge" class="badge" data-state="unknown">connecting…</div>
</header>

<p id="no-servers-notice" class="notice" hidden>
    No MSMP servers are configured. Set <code>minecraft.management.servers.&lt;id&gt;.*</code>
    (or the legacy <code>minecraft.management.secret</code>/<code>url</code>) and restart the console.
</p>

<div id="app-shell" hidden
     data-hide-offline-servers="${hideOfflineServers ? "true" : "false"}"
     data-show-notify-operators-checkbox="${showNotifyOperatorsCheckbox ? "true" : "false"}"
     data-notify-operators-default-checked="${notifyOperatorsDefaultChecked ? "true" : "false"}">
    <nav id="server-rail" aria-label="Configured servers">
        <h2>Servers</h2>
        <ul id="server-list"></ul>
    </nav>

    <main id="main-column">
        <section id="offline-panel" aria-label="Server offline" hidden>
            <h2>Not connected</h2>
            <p>The console isn't currently connected to <strong id="offline-server-id">this server</strong>
                at <code id="offline-server-url">–</code>.</p>
            <ol>
                <li>Confirm the Minecraft server process is running.</li>
                <li>Confirm <code>management-server-enabled=true</code> is set in that server's
                    <code>server.properties</code>, and the server has been restarted since.</li>
                <li>Confirm the address above matches that server's <code>management-server-host</code>
                    and <code>management-server-port</code>.</li>
                <li>Confirm the configured secret matches that server's <code>management-server-secret</code>.</li>
                <li>Check the console's own startup logs for the specific connection error.</li>
            </ol>
            <button id="retry-connect-btn" type="button" class="btn">Retry connection</button>
        </section>

        <section id="status-strip" aria-label="Server status">
            <div class="stat">
                <span class="stat-label">Started</span>
                <span class="stat-value" id="status-started">–</span>
            </div>
            <div class="stat">
                <span class="stat-label">Version</span>
                <span class="stat-value" id="status-version">–</span>
            </div>
            <div class="stat">
                <span class="stat-label">Online</span>
                <span class="stat-value" id="status-player-count">–</span>
            </div>
        </section>

        <section id="control-section">
            <form id="message-form" class="inline-form">
                <label class="visually-hidden" for="message-input">Message to broadcast</label>
                <input id="message-input" type="text" name="message" placeholder="Message to broadcast to every player" required>
                <button type="submit" class="btn">Broadcast</button>
            </form>
            <form id="save-form" class="inline-form">
                <label class="checkbox"><input type="checkbox" name="flush"> flush to disk</label>
                <button type="submit" class="btn btn-quiet">Save world</button>
            </form>
        </section>

        <section id="players-section" aria-labelledby="players-heading">
            <div class="section-header">
                <h2 id="players-heading">Players</h2>
                @if(showNotifyOperatorsCheckbox)
                    <label class="checkbox">
                        <input type="checkbox" id="notify-operators-checkbox" checked="${notifyOperatorsDefaultChecked}">
                        Notify operators on kick/ban/op changes
                    </label>
                @endif
            </div>
            <ul id="players-list" class="roster"></ul>
        </section>

        <section id="lists-section" aria-label="Allowlist, operators and bans">
            <div class="tabs" role="tablist">
                <button type="button" class="tab" role="tab" data-tab="allowlist" aria-selected="true">Allowlist</button>
                <button type="button" class="tab" role="tab" data-tab="operators" aria-selected="false">Operators</button>
                <button type="button" class="tab" role="tab" data-tab="bans" aria-selected="false">Bans</button>
            </div>

            <div class="tab-panel" data-panel="allowlist">
                <ul id="allowlist-list" class="roster"></ul>
                <form id="allowlist-form" class="inline-form">
                    <input type="text" name="playerNameOrUuid" placeholder="Player name or UUID" required>
                    <button type="submit" class="btn btn-quiet">Add</button>
                </form>
            </div>

            <div class="tab-panel" data-panel="operators" hidden>
                <ul id="operators-list" class="roster"></ul>
                <form id="operator-form" class="inline-form">
                    <input type="text" name="playerNameOrUuid" placeholder="Player name or UUID" required>
                    <input type="number" name="permissionLevel" aria-label="Permission level" value="4" min="1" max="4" class="input-narrow">
                    <label class="checkbox"><input type="checkbox" name="bypassesPlayerLimit"> bypass limit</label>
                    <button type="submit" class="btn btn-quiet">Add</button>
                </form>
            </div>

            <div class="tab-panel" data-panel="bans" hidden>
                <ul id="bans-list" class="roster"></ul>
                <form id="ban-form" class="inline-form">
                    <input type="text" name="playerNameOrUuid" placeholder="Player name or UUID" required>
                    <input type="text" name="reason" placeholder="Reason">
                    <button type="submit" class="btn btn-danger">Ban</button>
                </form>
            </div>
        </section>
    </main>

    <aside id="console-rail" aria-label="Activity log">
        <h2>Console</h2>
        <ul id="activity-feed"></ul>
    </aside>
</div>

<script src="/app.js"></script>
</body>
</html>
