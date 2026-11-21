# Figura Relay

Companion mod for [Figura](https://modrinth.com/mod/figura) **0.1.6** (Minecraft **1.21.1**) that
handles Figura avatars through the game server.

One jar, two roles:

| Side | What it does | Figura required |
|---|---|---|
| **Client** | Uploads, downloads and syncs Figura avatars through the game server | **Yes** (0.1.6) |
| **Server** | Relay: avatar storage, hash broadcasts, data and ping forwarding, ban list | **No** |

## Installation

- **Client:** [Figura 0.1.6 for MC 1.21.1](https://modrinth.com/mod/figura) + this mod in `mods/`.
- **Server:** drop the jar into `mods/`.

## Commands (server, permission level 2)

- `/figura relay ban <player>` — block the player from using the relay
- `/figura relay unban <player>` — lift the ban
- `/figura relay banlist` — show all bans
- `/figura relay reload` — reload the config and the ban list

## Configuration

`config/figura-relay.properties`, created with defaults on first run:

| Key | Default | Description |
|---|---|---|
| `maxAvatarSize` | `102400` | Maximum avatar size in bytes |
| `logOperations` | `false` | Verbose logging of relay operations |

## Storage (server)

Stored globally, next to `mods/` / `config/`.

- `figura-avatars/` — avatars
- `figura-player-hashes.json` — current hash per player
- `figura-banned-players.json` — ban list

## Wire protocol

Custom payloads, all in the `figura` namespace:

| Payload | ID | Direction |
|---|---|---|
| `Hello` | `figura:hello` | bidirectional (handshake) |
| `Hash` | `figura:relay_hash` | C2S + S2C |
| `Request` | `figura:relay_req` | C2S |
| `Data` | `figura:relay_data` | bidirectional |
| `Ping` | `figura:relay_ping` | bidirectional |
| `RelayOff` | `figura:relay_off` | S2C |

## Building

```bash
./gradlew build
```

Jars land in `fabric/build/libs`, `forge/build/libs`, `neoforge/build/libs`.

## License

[GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.txt)
