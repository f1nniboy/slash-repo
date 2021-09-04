# Installation Guide

This installation guide is adapted from KISS Linux's and WyverKISS' installation guide.

This guide assumes that you are already logged in as root, have your disks, partitions
and filesystems set up and have an internet connection.

## Index

- [Installation](#installation)
  * [Download the tarball](#download-the-tarball)
  * [Verify checksums](#verify-checksums)
  * [Unpack the tarball](#unpack-the-tarball)
  * [Enter the root](#enter-the-root)
- [Set up repositories](#set-up-repositories)
  * [Set the path](#setting-the-path)
  * [Official repositories](#official-repositories)
  * [Other repositories](#other-repositories)
- [Set up hooks](#set-up-hooks)
- [Rebuild the system](#rebuild-the-system)
  * [Modify compiler options](#modify-compiler-options)
  * [Rebuild all packages](#rebuild-all-packages)
- [Aliases](#aliases)
- [User-space tools](#user-space-tools)
  * [Filesystem utilities](#filesystem-utilities)
  * [DHCP](#dhcp)
- [Hostname configuration](#hostname-configuration)
  * [/etc/hosts](#-etc-hosts)
  * [/etc/hostname](-etc-hostname)
- [The kernel](#the-kernel)
  * [Install required packages](#install-required-packages)
  * [Download the kernel sources](#download-the-kernel-sources)
  * [Download firmware blobs](#download-firmware-blobs)
  * [Patch the kernel](#patch-the-kernel)
  * [Configure the kernel](#configure-the-kernel)
  * [Build the kernel](#build-the-kernel)
  * [Install the kernel](#install-the-kernel)
- [Bootloader](#bootloader)
- [Change the root password](#change-the-root-password)
- [Add a normal user](#add-a-normal-user)
- [Install graphical session](#install-graphical-session)
- [Further steps](#further-steps)

## Installation

Declare the following variables.

```shell-session
$ ver=2021.9
$ file="slash-${ver}-rootfs.tar.xz"
$ url="https://github.com/f1nniboy/slash-repo/releases/download/${ver}"
```

### Donload the tarball

The tarball contains the base system, excluding the kernel and bootloader.

The following packages are installed in this tarball:

`baselayout busybox byacc bzip2 curl flex git libressl llvm m4 make musl pigz slash xz zlib`

To download the tarball, run the following command:

```shell-session
$ curl -fLO "${url}/${file}"
```

### Verify checksums

This step is not needed, but recommended. It makes sure that the downloaded file
matches the file that is hosted remotely.

```shell-session
$ curl -fLO "${url}/${file}.sha256"
$ sha256sum -c < "${file}.sha256"
```

### Unpack the tarball

Make sure that you are `cd`'d into the correct directory before executing this command.
Running this command in the wrong directory may result in a broken system.

```shell-session
$ cd /mnt
$ tar xvf "${file}"
```

### Enter the root

This script handles mounting the pseudo-filesystems and copies `resolv.conf`.
It also cleans everything up on exit.

```shell-session
$ /mnt/usr/bin/slash-chroot /mnt
```

## Set up repositories

The repository is quite different compared to that of other package managers.
The repositories are controlled via the `SLASH_PATH` environment variable.
This variable is a list of paths to repositories, separated by a `:`.

A repository is simply a directory containing package directories, which can
be located anywhere on the file system.

The tarball does not come with any repositories by default, nor does the package manager
search for repositories at a fixed location. This is entirely up to the user.

### Set the path

This variable can be set system-wide (`/etc/profile.d/`), per-user (`.shellrc`), per-session,
per-command and even programmatically. This section will cover setting it system-wide.

Repository layout:

```
  /var/db/slash/repos
  |
  +- slash-repo/
  |  - .git/
  |  - core/
  |  - extra/
  |  - desktop/
  |  - gnu/
```

The user's path should now look like this:

```sh
export SLASH_PATH=/var/db/slash/repos/slash-repo/core
export SLASH_PATH=$SLASH_PATH:/var/db/slash/repos/slash-repo/extra
export SLASH_PATH=$SLASH_PATH:/var/db/slash/repos/slash-repo/gnu

# Remove this if you do not want to use a window manager.
export SLASH_PATH=$SLASH_PATH:/var/db/slash/repos/slash-repo/desktop
```

### Official repositories

The official repositories contain everything from the base system to a
working web browser, Firefox, and window manager, Sway.

Clone the repository to the directory of your choosing.

```shell-session
$ git clone https://github.com/f1nniboy/slash-repo
```

The cloned repository will contain multiple repositories. `core`, `extra` and
`gnu` must be enabled. `desktop` is optional and only contains packages which
require a window manager.

### Other repositories

The Slash package manager is backwards-compatible with KISS repositories which means
that, e.g. the `community` repository will also work.

Please see [this KISS Linux wiki page](https://kisslinux.org/wiki/repositories) for a list
of community-maintained KISS repositories.

## Set the hooks

Hooks are used to delete unneeded stuff (`bash-completion`, `zsh-completion`, etc.) and static
libraries from the system.

```sh
export SLASH_HOOK=PATH/TO/REPO/hooks/clean-files
export SLASH_HOOK=$SLASH_HOOK:PATH/TO/REPO/hooks/remove-static-libraries
```

## Rebuild the system

This step is completely optional and can also be done post-installation.

### Modify compiler options

As all packages are compiled using ThinLTO and O3, you will need to modify your compiler options.

```sh
# Add "-falign-functions=32" if you have an Intel CPU.
export COMMON_FLAGS="-march=native -O3 -pipe -flto=thin"
export CFLAGS="${COMMON_FLAGS}"
export CXXFLAGS="${COMMON_FLAGS}"

export LDFLAGS="-Wl,-O3 -Wl,--as-needed ${COMMON_FLAGS}"
```

### Rebuild all packages

This is also optional and only needs to be done if you have modified your compiler options.

```shell-session
$ cd /var/db/slash/installed && slash build *
```

## Aliases

Each action (`build`, `install`, `remove`, etc.) has a shorthand alias. From now on,
the respective aliases will be used.

## User-space tools

### Filesystem utilities

Open an issue for additional filesystem support.

#### EXT2, EXT3 and EXT4

```shell-session
$ slash b e2fsprogs
```

#### FAT, vFAT

```shell-session
$ slash b dosfstools
```

### DHCP

```shell-session
$ slash b dhcpcd
```

## Hostname configuration

### /etc/hosts

`/etc/hosts` should look like this:

```
127.0.0.1	HOSTNAME.localdomain	HOSTNAME
::1			HOSTNAME.localdomain	HOSTNAME	ip6-localhost
```

### /etc/hostname

`/etc/hostname` contains the hostname. You can also configure the kernel to set your hostname,
which will make this step redundant.

```shell-session
$ echo HOSTNAME > /etc/hostname
```

## Kernel

This step involves configuring and building the Linux kernel. If you have not done this
before, here are a few guides to get started:

- https://wiki.gentoo.org/wiki/Kernel/Gentoo_Kernel_Configuration_Guide
- https://wiki.gentoo.org/wiki/Kernel/Configuration
- https://kernelnewbies.org/KernelBuild

**NOTE**: The kernel is not managed by the package manager, it is instead managed manually
by the user.

### Install required packages

#### libelf

```shell-session
$ slash b libelf
```

#### netbsd-curses

Only required for `make menuconfig`.

```shell-session
$ slash b netbsd-curses
```

### perl

Required for Clang LTO. If not using LTO, you can remove this patch, see [Remove Perl dependency](#remove-perl-dependency).

```shell-session
$ slash b libelf
```

### GNU make

```shell-session
$ slash b gmake
```

### Remove Perl dependency

The Perl dependency can be removed by applying [this patch](https://raw.githubusercontent.com/dylanaraps/wiki/master/kernel/patches/kernel-no-perl.patch).

### Download the kernel sources and required patches

Download the latest kernel sources [here](https://kernel.org).

### Download firmware blobs

To keep the Slash repositories entirely FOSS, the proprietary firmware blobs are omitted.
This step is optional if you don't need any firmware blobs, which is very unlikely due to
GPUs requiring them.

Download them [here](https://git.kernel.org/pub/scm/linux/kernel/git/firmware/linux-firmware.git).

### Patch the kernel

As of kernel version `5.13`, the following patches need to be applied:

#### Fix `objtool` failing with musl systems

```shell-session
$ sed -i 's/$(LIBELF_FLAGS)/$(LIBELF_FLAGS) -D__always_inline=inline/' tools/objtool/Makefile
```
#### `byacc` support

```sh
$ curl -fLo byacc.patch https://patchwork.kernel.org/project/linux-kbuild/patch/20200130162314.31449-1-e5ten.arch@gmail.com/raw/
$ patch -p1 < byacc.patch
```

### Configure the kernel

There are guides on how to configure your kernel linked above, explaining how to configure a kernel is out-of-scope
for this installation guide.

### Build the kernel

This may take a while to complete. The compilation time depends on your hardware and kernel configuration.

```shell-session
$ gmake LLVM=1 LLVM_IAS=1 YACC=byacc
```

### Install the kernel

- Install the built modules to `/usr/lib`, ignore the GCC error.

```shell-session
$ gmake INSTALL_MOD_STRIP=1 modules_install
```

- Install the built kernel to `/boot`, ignore the GCC and LILO error.

```shell-session
$ gmake install
```

## Bootloader

The only "bootloader" in the repositories is EFIstub (`efibootmgr` to configure it).
Nothing is preventing you from using GRUB, but consider using EFIstub instead. (Sorry, prefer-to-use-BIOS guys)

## Init scripts

The default init system is `runit`, though nothing ties you to it. The following commands install the
boot and shutdown scripts.

```shell-session
$ slash b baseinit
```

## Change the root password

```shell-session
$ passwd root
```

## Add a normal user

This step is optional, but recommended.
It is needed for a graphical session.

```shell-session
$ adduser USERNAME
```

## Install graphical session

This step is optional and only needed if you actually want to use a GUI and window manager.

We currently only have `sway` in the repositories, feel free to open an issue for
window manager suggestions.

```shell-session
$ slash b sway
```

This will pull in a lot of dependencies.

## Further steps

The installation is complete!

If everything was done correctly, you should now be able to boot into
your installation.

If you encountered any issues or have any questions, get in touch
on [the Issues page](https://github.com/f1nniboy/slash-repo/issues).
