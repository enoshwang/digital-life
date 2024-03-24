# -*- coding:utf-8 -*-

import os
import sqlite3
import time
import re
import matplotlib.pyplot as plt
import shutil
import platform

browser_tuple = ("Chrome","Edge")

def dict_slice(adict, start, end):
    keys = adict.keys()
    dict_slice = {}
    for k in list(keys)[start:end]:
        dict_slice[k] = adict[k]
    return dict_slice

def FormatToStamp():
    flag = True
    while flag:
        try:
            formatTime = input("请输入时间（格式：2018-06-24 11:50:00）").strip()
            formatTime = time.mktime(time.strptime(formatTime, '%Y-%m-%d %X'))
            return formatTime
        except Exception as e:
            print("转换失败，请重新输入。失败原因：%s" % e)

def parse(url):
    try:
        parsed_url_components = url.split('//')
        sublevel_split = parsed_url_components[1].split('/', 1)
        domain = sublevel_split[0].replace("www.", "")
        return domain
    except IndexError:
        print('URL format error!')

def filter_data(url, address_count):
    try:
        parsed_url_components = url.split('//')
        sublevel_split = parsed_url_components[1].split('/', 1)
        data = re.search('\w+\.(com|cn|net|tw|la|io|org|cc|info|cm|us|tv|club|co|in|me|art|vip|xyz|top|gov|ac|edu)',
                         sublevel_split[0])
        if data:
            return data.group()
        else:
            address_count.add(sublevel_split[0])
            return "ok"
    except IndexError:
        print('URL format error!')

def barGraph(results):
    # 条形图
    key = []
    value = []
    for i in results:
        key.append(i[0])
        value.append(i[1])
    n = 10
    X = range(n)
    Y = value[:n]

    # plt.bar(X, Y, align='edge')
    plt.bar(X,Y)
    plt.xticks(rotation=35)
    plt.xticks(X, key[:n])
    for x, y in zip(X, Y):
        plt.text(x + 0.4, y + 0.05, y, ha='center', va='bottom')
    plt.savefig(r'./parseGoogleBrowser.png')

def main(path):
    ret = {"code":"0","message":"success"}

    platformName = platform.system()
    if platformName == "Windows":
        if path in browser_tuple:
            if path == "Edge":
                path = os.path.expanduser('~') + r'\AppData\Local\Microsoft\Edge\User Data\Default'
            else:
                path = os.path.expanduser('~') + r'\AppData\Local\Google\Chrome\User Data\Default'
        else:
            ret["code"] = -1
            ret["message"] = "Not support browser."
            return ret
    elif platformName == "Linux":
        if path in browser_tuple:
            if path == "Chrome":
                path = os.path.expanduser('~') + r'/.config/google-chrome/Default'
            else:
                ret["code"] = -1
                ret["message"] = "It's only support google-chrome on linux."
                return ret
        else:
            ret["code"] = -1
            ret["message"] = "It's only support google-chrome on linux."
            return ret
    else:
        ret["code"] = -1
        ret["message"] = "Not support os.Please run in Windows or Linux."
        return ret

    if not os.path.exists(path):
        ret["code"] = -1
        ret["message"] = path + "not exists."
        return ret

    # data struct
    address_count = set()  # 创建一个空的集合，用来收集已经存在国际域名

    historyPath = os.path.join(path,"History")
    if not os.path.exists(historyPath):
        ret["code"] = -1
        ret["message"] = historyPath + "not exists."
        return ret

    historyTmpPath = os.path.join(os.getcwd(),'historyTmp')

    try:
        shutil.copyfile(historyPath,historyTmpPath)
    except Exception as e:
        ret["code"] = -1
        ret["message"] = e.__str__()
        return ret

    if not os.path.exists(historyTmpPath):
        ret["code"] = -1
        ret["message"] = historyTmpPath + "not exists."
        return ret
    
    try:
        conn = sqlite3.connect(historyTmpPath)
    except Exception as e:
        ret["code"] = -1
        ret["message"] = e.__str__()
        return ret

    cursor = conn.cursor()
    SQL = "SELECT urls.url,urls.title,visits.visit_time from visits LEFT JOIN urls on visits.url=urls.id"

    try:
        cursor.execute(SQL)
    except Exception as e:
        ret["code"] = -1
        ret["message"] = e.__str__()
        return ret

    data = cursor.fetchall()

    new_data = {}
    for item in data:
        url_2 = filter_data(item[0], address_count)
        title = item[1]
        if url_2 in new_data:
            new_data[url_2] += 1
        else:
            new_data[url_2] = 1

    new_data = sorted(new_data.items(), key=lambda d: d[1], reverse=True)

    new_data_2 = {}
    # noinspection NonAsciiCharacters
    for K in new_data:
        new_data_2[K[0]] = K[1]  # NOLINT

    del new_data_2["ok"]

    barGraph(new_data)

    cursor.close()
    conn.close()
    
    if os.path.exists(historyTmpPath):
        try:
            os.remove(historyTmpPath)
        except Exception as e:
            print(e.__str__())

    return ret

if __name__ == '__main__':
    ret = main("Edge")
    print(ret)
