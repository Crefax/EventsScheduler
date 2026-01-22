# Events Scheduler

A powerful and flexible event scheduling plugin for Hytale servers that automatically distributes rewards and executes commands at specific times or intervals.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Hytale](https://img.shields.io/badge/Hytale-Early%20Access-orange)
![Java](https://img.shields.io/badge/Java-17%2B-red)
![License](https://img.shields.io/badge/license-MIT-green)

## Features

### 🕐 Dual Scheduling System
- **Interval Events**: Execute commands every X seconds (e.g., hourly rewards)
- **Scheduled Events**: Trigger events at specific times daily (e.g., 09:00, 12:00, 18:00)

### ⚙️ Highly Configurable
- JSON-based configuration system
- Enable/disable individual events
- Customizable timezone support (default: GMT/London)
- Broadcast messages with custom prefix
- Multiple commands per event

### 🎁 Reward Distribution
- Automatically give items to all online players
- Send custom messages to players
- Supports all Hytale items (weapons, tools, armor, etc.)

### 🛠️ Admin Commands
| Command | Description |
|---------|-------------|
| `/events` | List all configured events and their status |
| `/eventstrigger --eventName <name>` | Manually trigger any event |
| `/eventsreload` | Reload configuration without restarting server |

### 🔧 Advanced Options
- Minimum player requirement for events
- Cooldown between consecutive events
- Debug mode for troubleshooting
- Manual event triggering for testing

## Installation

1. Download the latest release from [Releases](https://github.com/Crefax/EventsScheduler/releases)
2. Place the `.jar` file in your server's `mods` folder
3. Start/restart your server
4. Configure events in `mods/EventScheduler/events.json`
5. Use `/eventsreload` to apply changes

## Configuration

The configuration file is located at `mods/EventScheduler/events.json`

### Example Configuration

```json
{
    "broadcastPrefix": "[Events] ",
    
    "intervalEvents": [
        {
            "name": "hourly_reward",
            "enabled": true,
            "interval": 3600,
            "commands": [
                "give Weapon_Sword_Cobalt"
            ],
            "broadcastMessage": "Hourly reward! Cobalt Sword distributed to all players!"
        },
        {
            "name": "bonus_tools",
            "enabled": false,
            "interval": 7200,
            "commands": [
                "give Tool_Pickaxe_Crude",
                "give Tool_Hatchet_Crude"
            ],
            "broadcastMessage": "Bonus reward! Pickaxe and Hatchet distributed!"
        }
    ],
    
    "scheduledEvents": [
        {
            "name": "morning_reward",
            "enabled": true,
            "times": ["09:00", "12:00", "18:00", "21:00"],
            "commands": [
                "give Weapon_Sword_Crude",
                "give Tool_Pickaxe_Crude"
            ],
            "broadcastMessage": "Daily reward time! Sword and Pickaxe distributed!"
        },
        {
            "name": "midnight_bonus",
            "enabled": true,
            "times": ["00:00"],
            "commands": [
                "give Weapon_Staff_Cobalt",
                "message You earned a midnight bonus!"
            ],
            "broadcastMessage": "Midnight bonus! You earned a Cobalt Staff!"
        }
    ],
    
    "settings": {
        "timezone": "Europe/London",
        "debugMode": false,
        "minPlayersRequired": 0,
        "cooldownBetweenEvents": 0
    }
}
```

### Configuration Options

#### Interval Events
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Unique identifier for the event |
| `enabled` | Boolean | Whether the event is active |
| `interval` | Integer | Time between executions in seconds |
| `commands` | Array | List of commands to execute |
| `broadcastMessage` | String | Message to broadcast when event triggers |

#### Scheduled Events
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Unique identifier for the event |
| `enabled` | Boolean | Whether the event is active |
| `times` | Array | List of times in "HH:mm" format |
| `commands` | Array | List of commands to execute |
| `broadcastMessage` | String | Message to broadcast when event triggers |

#### Settings
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `timezone` | String | "Europe/London" | Timezone for scheduled events |
| `debugMode` | Boolean | false | Enable detailed logging |
| `minPlayersRequired` | Integer | 0 | Minimum online players to trigger event |
| `cooldownBetweenEvents` | Integer | 0 | Cooldown in seconds between events |

## Supported Commands

### Command Prefixes

| Prefix | Description | Example |
|--------|-------------|---------|
| `console:` | Execute as server console (full permissions) | `console:save-all` |
| `server:` | Alias for console: | `server:restart` |
| `cmd:` | Execute as the player | `cmd:spawn` |
| `command:` | Alias for cmd: | `command:home` |
| `op:` | Execute with OP permissions | `op:kick {player}` |
| `admin:` | Alias for op: | `admin:ban {player}` |

### Built-in Commands

| Command | Description |
|---------|-------------|
| `give <ItemID> [quantity]` | Give item(s) to player |
| `message <text>` | Send a private message to player |
| `msg <text>` | Alias for message |
| `broadcast <text>` | Send message to all online players |
| `bc <text>` | Alias for broadcast |

### Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Player's username |
| `{name}` | Alias for {player} |
| `{uuid}` | Player's UUID |
| `{display_name}` | Player's display name |

### Examples

```json
"commands": [
    "give Weapon_Sword_Cobalt 1",
    "message Welcome {player}!",
    "console:save-all",
    "console:say Server backup complete!",
    "op:gamemode creative {player}",
    "cmd:spawn",
    "broadcast Event reward distributed to {player}!"
]
```

## Default Events

The plugin comes with 4 pre-configured events:

| Event Name | Type | Timing | Reward | Status |
|------------|------|--------|--------|--------|
| hourly_reward | Interval | Every 1 hour | Cobalt Sword | ✅ Enabled |
| bonus_tools | Interval | Every 2 hours | Crude Pickaxe + Hatchet | ❌ Disabled |
| morning_reward | Scheduled | 09:00, 12:00, 18:00, 21:00 | Crude Sword + Pickaxe | ✅ Enabled |
| midnight_bonus | Scheduled | 00:00 | Cobalt Staff + Message | ✅ Enabled |

## Building from Source

### Requirements
- Java 17 or higher
- Maven 3.6+
- Hytale Server JAR (place in `libs/` folder)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Crefax/EventsScheduler.git
cd EventsScheduler

# Build the project
mvn clean package

# The JAR will be in target/events-scheduler-1.0.0.jar
```

## Permissions

All commands require OP permissions by default.

## Requirements

- Hytale Server (Early Access or newer)
- Java 17+

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

Found a bug or have a suggestion? [Open an issue](https://github.com/Crefax/EventsScheduler/issues)!

---

**Author**: [Crefax](https://github.com/Crefax)  
**Version**: 1.0.0
