#!/usr/bin/env python3
# -*- coding: utf-8 -*-


import urllib.request
import urllib.error
import os
from tkinter import *
from tkinter.filedialog import askdirectory
import html2text
import threading


class DownloadTools:

    def __init__(self):
        self.__log_util = LogUtils()
        self.__setup()

    def __start_download(self):
        self.__log_util.clear()
        self.__log_util.set_callback(self.__update_logs)

        url = self.__htmlUrl.get()
        self.__spider = Spider(url, Spider.format_file_path(url, self.__write_path.get()), self.__log_util)
        self.__spider.start_download()

    def __export2text(self):
        Html2Text(
            Spider.format_file_path(self.__htmlUrl.get(), self.__write_path.get()),
            self.__enabled_combined.get() == 1,
            self.__log_util
        ).export2text()

    def __show_dialog(self):
        self.__write_path.set(askdirectory())

    def __setup_log_frame(self, win):
        log_frame = Frame(win)

        self.__logs_var = StringVar()
        self.__log_list = Listbox(log_frame, listvariable=self.__logs_var, height=10).pack(expand=YES, fill=BOTH)

        log_frame.pack(fill=BOTH, expand=YES)

    def __setup(self):
        root = Tk()
        root.title("Download Html Tool")
        root.geometry('300x400')

        self.__htmlUrl = StringVar()
        self.__write_path = StringVar()
        self.__enabled_combined = IntVar()

        # download frame
        frame = Frame(root)

        Entry(frame,  textvariable=self.__htmlUrl).pack(side=LEFT, fill=X, expand=YES)
        Button(frame, text="Download", command=self.__start_download).pack(side=RIGHT)

        frame.pack(fill=X)

        # export frame
        export_frame = Frame(root)

        explore_frame = Frame(export_frame)
        Entry(explore_frame,  textvariable=self.__write_path).pack(side=LEFT)
        Button(explore_frame, text="Explore Saved Path", command=self.__show_dialog).pack(side=RIGHT)
        explore_frame.pack()

        Button(export_frame, text="Export to Text", command=self.__export2text).pack(side=RIGHT)

        check_btn = Checkbutton(export_frame, text="combined", variable=self.__enabled_combined, state='disabled')
        check_btn.select()
        check_btn.pack(side=RIGHT)

        export_frame.pack()

        self.__setup_log_frame(root)

        root.mainloop()

    def __update_logs(self, logs, _):
        self.__logs_var.set(logs)


class Html2Text:

    def __init__(self, base_dir, combined, log_util):
        self.__base_dir = base_dir
        self.__contents = os.listdir(base_dir)
        self.__contents.sort()
        self.__combined = combined
        self.__log_util = log_util

    def export2text(self):
        self.__log_util.append('start export')
        combined_f = None
        if self.__combined:
            combined_f = open(self.__base_dir + '/' + 'html2text.txt', 'a')

        for file_name in self.__contents:
            self.__log_util.append('export %s' % file_name)
            html_f = open(self.__base_dir + '/' + file_name, 'r')
            html = html_f.read()
            html_f.close()

            clean_text = html2text.html2text(html)
            combined_f.write(clean_text)

        combined_f.close()
        self.__log_util.append('finished!!!')


class LogUtils:

    def __init__(self, callback=None):
        self.__logs = []
        self.__callback = callback

    def set_callback(self, callback):
        self.__callback = callback

    def append(self, log_msg):
        self.__logs.append(log_msg)

        if self.__callback is not None:
            self.__callback(self.__logs, log_msg)

    def get_logs(self):
        return self.__logs

    def clear(self):
        self.__logs.clear()

        if self.__callback is not None:
            self.__callback(self.__logs, None)


class Spider:

    def __init__(self, site_url, write_path, log_util):
        self.__siteURL = site_url
        self.__write_path = write_path
        self.__contents = []
        self.__log_util = log_util

    def start_download(self):
        if len(self.__siteURL) == 0:
            return

        try:
            os.mkdir(self.__write_path)
        except FileExistsError as e:
            self.__log_util.append(str(e))

        task_thread = threading.Thread(target=self.__downloading)
        task_thread.setDaemon(TRUE)
        task_thread.start()

    def __downloading(self):
        contents = self.__get_contents()
        for item in contents:
            try:
                self.__save_files(item.decode('utf-8'), item.decode('utf-8').split('/')[-1])
            except UnicodeDecodeError as e:
                self.__log_util.append(str(e))
        self.__log_util.append('download successful!!!')

    def __get_contents(self):
        page = self.__get_page()

        pattern = re.compile(b'<a(.+?)href="(.+?)"', re.S)
        items = re.findall(pattern, page)

        for item in items:
            self.__contents.append(item[1])

        return self.__contents

    def __save_files(self, file_url, filename):
        file_path = self.__write_path + filename
        if '.html' in file_url or '.htm' in file_url:
            if 'http' not in file_url:
                new_file_url = file_url
                if '/' in file_url:
                    new_file_url = file_url.split('/')[-1]

                if 'htm' in self.__siteURL:
                    last_slash_index = self.__siteURL.index(self.__siteURL.split('/')[-1])
                    file_url = self.__siteURL[0:last_slash_index] + new_file_url
                else:
                    file_url = self.__siteURL + '/' + new_file_url

            try:
                u = urllib.request.urlopen(file_url)
                data = u.read()
                f = open(file_path, 'wb')
                f.write(data)
                f.close()

                self.__log_util.append('the url is %s, write path is %s' % (file_url, file_path))
            except(urllib.error.HTTPError, urllib.error.URLError, IOError) as e:
                self.__log_util.append(str(e))

    def __get_page(self):
        headers = {
            'User': 'Agent: Mozilla / 5.0(X11;Ubuntu;Linux x86_64;rv: 57.0) Gecko / 20100101Firefox / 57.0',
            'Accept': 'text / html, application / xhtml + xml, application / xml'
        }
        request = urllib.request.Request(self.__siteURL, headers=headers)
        response = urllib.request.urlopen(request)

        return response.read()

    @staticmethod
    def format_file_path(site_url, base_dir):
        if len(base_dir) == 0:
            return os.getcwd()

        return base_dir + '/' + site_url.split('/')[2] + '/'


if __name__ == '__main__':
    DownloadTools()
