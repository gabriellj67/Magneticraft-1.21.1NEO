# Conversion Rates

Magneticraft fixes the adapter rate at `1 joule = 1 Forge Energy`. No hidden multiplier or intermediate FE buffer is applied at conversion boundaries or portable tools.

FE can cross the boundary only at explicit devices:

- The FE transformer accepts FE and feeds the low-voltage native network, normally up to 100 FE/t.
- Every connector can carry native J through its outward face to a same-tier Magneticraft electrical port. A low-voltage connector automatically outputs up to 400 FE/t only when that target is FE-only; medium- and high-voltage connectors stay native-only.
- A wireless receiver carries Tesla-delivered J to a Magneticraft electrical port, or automatically outputs up to 400 FE/t when the outward target is FE-only.
- The electric motor consumes medium-voltage native power and can output up to 1000 FE/t.
- Portable battery slots are limited to 500 FE/t and do not turn the battery block into a universal sided FE provider.

Every transaction simulates the receiver first. Only the accepted integer amount is committed, and any rounding remainder stays at the source. A one-to-one ratio therefore never means unlimited transfer or free energy.

If a target exposes both a Magneticraft electrical port and FE, the native J path always wins and the FE adapter is skipped.

Use machine screens and Jade, when installed, to inspect tier, terminal, voltage, current, transfer rate, load, stress, and fault state. Use the voltmeter for network-wide diagnostics.
