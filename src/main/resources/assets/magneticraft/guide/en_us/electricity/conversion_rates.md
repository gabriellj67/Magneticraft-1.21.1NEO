# Conversion Rates

Magneticraft fixes the adapter rate at `1 joule = 1 Forge Energy`. No hidden multiplier is applied at FE bridges, portable tools, or machine buffers.

FE can cross the boundary only at explicit devices:

- The FE transformer accepts FE and feeds the low-voltage native network, normally up to 100 FE/t.
- A low-voltage connector can output up to 400 FE/t; medium- and high-voltage connectors stay native-only.
- The electric motor consumes medium-voltage native power and can output up to 1000 FE/t.
- Portable battery slots are limited to 500 FE/t and do not turn the battery block into a universal sided FE provider.

Every transaction simulates the receiver first. Only the accepted integer amount is committed, and any rounding remainder stays at the source. A one-to-one ratio therefore never means unlimited transfer or free energy.

Use machine screens and Jade, when installed, to inspect tier, terminal, voltage, current, transfer rate, load, stress, and fault state. Use the voltmeter for network-wide diagnostics.
