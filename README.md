# Slash Linux - Repository
> An experimental Linux distribution, based on KISS, focused on performance and minimalism.

## Note
This repository is **not** compatible with the KISS package manager. You will need to
use the Slash package manager.

## Why another distribution?
I have always wanted to create my own distribution because it fascinates me. This repository also
has some changes, including:

* every package is built using ThinLTO
* no static libraries
* reduced dependencies

I cannot thank KISS and WyverKISS enough for the work they have done, without them this
distribution would not exist.

## Usage

If you want to use this repository, you will have to do a few things.

### Change your build environment variables

All packages in this repository are supposed to be built using O3 and ThinLTO.
Your build flags should look like this:

```sh
# Add "-falign-functions=32" if you have an Intel CPU.
export COMMON_FLAGS="-march=native -O3 -pipe -flto=thin"
export CFLAGS="${COMMON_FLAGS}"
export CXXFLAGS="${COMMON_FLAGS}"

export LDFLAGS="-Wl,-O3 -Wl,--as-needed ${COMMON_FLAGS}"
```

`BASE_FLAGS` and `BASE_LDFLAGS` are used as a fallback for packages
that cannot be built with ThinLTO due to build failures.

### Enable this repository

To use this repository, you will have to change your `SLASH_PATH`.
It should look like this:

```sh
export SLASH_PATH=PATH/TO/REPO/core
export SLASH_PATH=$SLASH_PATH:PATH/TO/REPO/extra
export SLASH_PATH=$SLASH_PATH:PATH/TO/REPO/gnu

# Remove this if you do not want to use a window manager.
export SLASH_PATH=$SLASH_PATH:PATH/TO/REPO/desktop
```

### Enable the hooks

Because it would be tedious to remove all static libraries per-package, we
have created hooks that will do this. To use them:

```sh
export SLASH_HOOK=PATH/TO/REPO/hooks/clean-files
export SLASH_HOOK=$SLASH_HOOK:PATH/TO/REPO/hooks/remove-static-libraries
```

## Troubleshooting

In case a package fails to build, please open an issue on this repository.
