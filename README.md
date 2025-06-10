# MonoxiTntTag Plugin

## Overview
MonoxiTntTag is a Minecraft plugin designed to create a fun minigame called "TNT Tag" for players without OP permissions. The plugin allows players to pass the TNT to each other in a limited-time event before it explodes.

## Features
- Global command for OP: `/MonoxiTntTag start <cantidadJugadoresConTnt> <tiempo(minutos:segundos)>` to start the minigame.
- Limited-time gameplay controlled by a timer.
- Custom TNT item represented as a serialized ItemStack in `config.yml`.
- Players with TNT can pass it to others by hitting them.
- Integration with PlaceholdersAPI for dynamic placeholders.
- Clear chat messages for important events.
- Commands for setting the item ID and resetting the minigame.

## Installation
1. Download the latest release of the MonoxiTntTag plugin.
2. Place the `.jar` file into the `plugins` folder of your Minecraft server.
3. Start the server to generate the configuration files.
4. Configure the `config.yml` file as needed.

## Commands
- `/MonoxiTntTag start <cantidadJugadoresConTnt> <tiempo>`: Starts the minigame.
- `/MonoxiTntTag setitem <itemID>`: Sets the item ID for the TNT.
- `/MonoxiTntTag reset`: Resets the minigame state.

## Configuration
The configuration file `config.yml` contains the serialized ItemStack for the TNT item. Ensure that the item is correctly defined to avoid issues during gameplay.

## Usage
Once the minigame is started, players without OP can participate by trying to pass the TNT to others. The players without TNT when the timer ends will be the winners, while those holding TNT will be eliminated.

## Contributing
Contributions are welcome! Please fork the repository and submit a pull request for any improvements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.
