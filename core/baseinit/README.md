# baseinit

Portable init framework, based on KISS' init scripts.

## Information

The init scripts have been modified to not run `fsck` on boot
and to not activate swap. If you do not want these changes,
feel free to change the init scripts.

## Usage

If you are using the default init system, no setup is required.
Refer to `$/core/runit` for service management documentation.

## Troubleshooting

As these scripts are a modified version of KISS' init scripts,
open an issue on this repository.
