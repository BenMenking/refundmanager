refundmanager
=============

We all know that accidents happen. And it's even more unfortunate when it happens to the players. Those accidental nukes and smites. Antioch. Vindictive staff. "Just having fun."

Now make your staff happy with an easy to use refund system! One of the most time-consuming and stressful tasks for staff is player refunds, which could result in item duplication and just straight up dishonesty. The Refunder plugin records inventory and XP when a player dies, giving the staff a quick and easy way to refund inventory to a player.

Features
========

Records player death information such as location, death reason and inventory
Optionally turn off inventory drops on death to avoid duplication by unscrupulous players
Don’t record death information for specified worlds
Simple in-game staff and player commands
Commands/Permissions
====================

Command	Permission	Description
/refund help	refund.help	Allowsplayer to display the help
/refund accept	refund.user.accept	Allows a player to accept a refund that is available
/refund list	refund.user.list	Allows a player to list the items that will be refunded
/refund decline	refund.user.decline	Allows a player to decline a refund that is available
/refund detect	refund.admin.detect	Prints out information about the item currently in hand
/refund show <player> [2]	refund.admin.show	Show the last 2 (default) deaths for <player>
/refund refund <player> <id>	refund.admin.refund	Set <id> to refundable for player

Installation
============

Simply drop the .jar in your Bukkit server’s plugin directory and restart the server.

Configuration
=============
These options are found in the config.yml

allow-drops	true|false	If set to true (default) player’s inventory and XP drop. If set to false, all dropped inventory is removed from the game (helps prevent duplication through the refund system).
ignored-worlds	List	Refunder will not perform any actions in an ignored world when a player dies.
