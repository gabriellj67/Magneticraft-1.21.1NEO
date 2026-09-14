# Sluice Box

Place sluice boxes in a supported chain and supply the upstream water condition. Loaded input follows `magneticraft:sluice_box` recipes and can yield primary and chance-based secondary products.

The server owns chain discovery, progress, and output insertion. Changing the upstream chain invalidates stale cached routing before processing resumes.

JEI displays each input, duration, and chance output when installed. Blocked output applies backpressure without consuming another input.
