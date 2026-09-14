# Fabricator

The fabricator stores a ghost crafting pattern and searches compatible adjacent inventories for ingredients. Ghost slots describe the recipe and never consume the player's carried stack.

Requesting a craft builds a server-side extraction plan, verifies every source, and commits atomically. Missing or changed ingredients cancel the request without partial consumption.

Use the displayed result interaction to craft or clear the pattern; ordinary item automation follows the machine page.
