#!/bin/sh

echo -n "Enter the backup file name: "
read name
# ip
echo -n "Enter the source ip: "
read ip
# username
echo -n "Enter the username: "
read username

gitlab_version=`dpkg -s gitlab-ce | grep Version | cut -d ' '  -f 2`
filename=$name"_"$gitlab_version".tar"
# echo "$filename - $ip"

scp $name.tar $username@$ip:/$filename
