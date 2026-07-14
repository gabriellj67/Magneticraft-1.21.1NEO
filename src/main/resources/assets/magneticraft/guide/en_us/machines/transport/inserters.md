# Inserters

An inserter moves items between inventories on its configured input and output sides. Its internal carried stack is persisted so a save, reload, or blocked destination cannot duplicate or delete an item.

Configure direction and filters through the machine interface. Speed upgrades shorten the action delay; stack upgrades permit larger transfers. The four legacy gear prototypes are intentionally absent because no released behavior consumed them.

Unloaded destinations pause the pending transfer, and simulated inventory calls never mutate either inventory.
