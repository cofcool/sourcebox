#!/usr/bin/env python3

import sys
import os

class CombinedTools:

    def __init__(self):
        pass

    def __get_file_list(self,dir):
        files = os.listdir()
        pass

    def __combined(self):
        pass

if __name__ == '__main__':
    params = sys.argv
    if len(params) >= 2:
        params = params[1:]
        for item in params:

        print(params)
        CombinedTools()
    else:
        print('please enter the relative path')
