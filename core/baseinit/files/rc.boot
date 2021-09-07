#!/bin/sh
# shellcheck disable=1090,1091

# Utilities
. /usr/lib/init/rc.lib

log "Welcome!"

log "Mounting pseudo filesystems..."; {
	mnt nosuid,noexec,nodev				proc		proc /proc
	mnt nosuid,noexec,nodev				sysfs		sys  /sys
	mnt mode=0755,nosuid,nodev			tmpfs		run  /run

	mkdir -p /dev/pts /dev/shm

	mnt mode=0620,gid=5,nosuid,noexec	devpts devpts	/dev/pts
	mnt mode=1777,nosuid,nodev			tmpfs  shm		/dev/shm

	# udev created these for us, however other device managers
	# don't. This is fine even when udev is in use.
	{
		ln -s /proc/self/fd	/dev/fd
		ln -s fd/0			/dev/stdin
		ln -s fd/1			/dev/stdout
		ln -s fd/2			/dev/stderr
	} 2> /dev/null
}

log "Loading settings..."; {
	load_conf
}

log "Running pre-boot hooks..."; {
	run_hook pre.boot
}

log "Starting device manager..."; {
	case ${CONFIG_DEV} in
		mdevd)
			mdevd & pid_mdevd=$!
			mdevd-coldplug
		;;

		mdev)
			mdev -s
			mdev -df & pid_mdev=$!
		;;

		udevd)
			udevd -d
			udevadm trigger -c add -t subsystems
			udevadm trigger -c add -t devices
			udevadm settle
		;;
	esac
}

log "Mounting all local filesystems..."; {
	mount -a || sos
}

log "Seeding random..."; {
	random_seed load
}

log "Setting hostname..."; {
	read -r hostname < /etc/hostname
	printf "%s" "${hostname:-slash}" > /proc/sys/kernel/hostname
} 2> /dev/null

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

log "Killing the device manager to make way for services..."; {
	case ${CONFIG_DEV} in
		udevd)
			udevadm control --exit
		;;

		mdevd)
			kill "${pid_mdevd}"
		;;

		mdev)
			kill "${pid_mdev}"
			command -v mdev > /proc/sys/kernel/hotplug
		;;
	esac
}

log "Running post-boot hooks..."; {
	run_hook post.boot
}

# Calculate how long the boot process took to
# complete. This entire process is too cheap!
IFS=. read -r boot_time _ < /proc/uptime

log "Boot completed in ${boot_time}s."
