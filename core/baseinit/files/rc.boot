#!/bin/sh
# shellcheck disable=1090,1091

# Utilities
. /usr/lib/init/rc.lib

log "Welcome!"

log "Mounting pseudo filesystems..."; {
	mnt nosuid,noexec,nodev		proc		proc /proc
	mnt nosuid,noexec,nodev		sysfs		sys  /sys
	mnt mode=0755,nosuid,nodev	tmpfs		run  /run
	mnt mode=0755,nosuid		devtmpfs	dev  /dev

	mkdir -p /run/runit /run/user /run/lock \
			 /run/log   /dev/pts  /dev/shm

	mnt mode=0620,gid=5,nosuid,noexec	devpts devpts	/dev/pts
	mnt mode=1777,nosuid,nodev			tmpfs  shm		/dev/shm

	# udev created these for us, however other device managers
	# don't. This is fine even when udev is in use.
	{
		ln -s /proc/self/fd	/dev/fd
		ln -s fd/0			/dev/stdin
		ln -s fd/1			/dev/stdout
		ln -s fd/2			/dev/stderr
	} 2>/dev/null
}

log "Loading rc.conf settings..."; {
	[ -f /etc/rc.conf ] && . /etc/rc.conf
}

log "Starting device manager..."; {
	mdev -s
	mdev -df & mdev_pid=$!
}

log "Mounting all local filesystems..."; {
	mount -a || sos
}

log "Seeding random..."; {
	random_seed load
}

log "Setting up loopback..."; {
	ip link set up dev lo
}

log "Loading sysctl settings..."; {
	# This is a portable equivalent to 'sysctl --system'
	# following the exact same semantics.
	for conf in /run/sysctl.d/*.conf \
				/etc/sysctl.d/*.conf \
				/usr/lib/sysctl.d/*.conf \
				/etc/sysctl.conf; do

		[ -f "${conf}" ] || continue

		# Skip conf files we have already seen (basename match).
		case ${seen} in *" ${conf##*/} "*) continue; esac
		seen=" ${seen} ${conf##*/} "

		sysctl -p "${conf}"
	done
}

log "Killing device manager to make way for service..."; {
	if [ "${mdev_pid}" ]; then
		kill "${mdev_pid}"

		# Try to set the hotplug script to mdev.
		# This will silently fail if unavailable.
		#
		# The user should then run the mdev service
		# to enable hotplugging.
		command -v mdev > /proc/sys/kernel/hotplug
	fi 2>/dev/null
}

log "Running boot hooks..."; {
	run_hook boot
}

# Calculate how long the boot process took to
# complete. This entire process is too cheap!
IFS=. read -r boot_time _ < /proc/uptime

log "Boot stage completed in ${boot_time}s..."
