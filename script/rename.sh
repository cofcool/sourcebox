#!/bin/bash

count = 1
for dir in `ls`
do
    echo $dir
    if [-d "$dir"]; then
        mv $dir `echo $count`
        $count++
    fi
    # for filename in `ls $dir`
    # do
    #     echo $filename
    #     mv $filename `echo $filename.jpg`
    # done
done
