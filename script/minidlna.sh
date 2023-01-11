#!/bin/sh

# auto mount hard disk,then start minidlna service, and the service will scan media file in the disk.

mount /dev/sda5 /media/pi/backup
if [$? -eq 0];then
	service minidlna restart
fi