## Apptainer build

Builds and manages the writable Apptainer sandboxes (unpacked OCI images) used to reproduce
breaking dependency updates and run their Maven builds, with different configurations.

Requires the `apptainer` CLI to be installed and on `PATH`, with rootless/fakeroot support
configured (see the root [README](../README.md) for setup). No daemon is required — images
are pulled straight from the OCI registry (e.g. `ghcr.io/...`) via
`apptainer build --sandbox`.

