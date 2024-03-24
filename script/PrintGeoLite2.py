# -*- coding:utf-8 -*-

import geoip2.database
from scapy.all import *

def printPcapIp(pcapPath):
    # First, generate some packets...
    packets = IP(src="172.0.0.1", dst=Net("180.101.49.11")) / ICMP()

    # Show them with Wireshark
    wireshark(packets)

def printGeoip2(target):
    with geoip2.database.Reader(r'../config/GeoLite2-City.mmdb') as dbHandle:
        response = dbHandle.city(target)
        city = response.city.name
        count = response.country.iso_code
        latitude = response.location.latitude
        longitude = response.location.longitude
        timezone = response.location.time_zone
        print(city,count,latitude,longitude,timezone)

if __name__ == '__main__':
    printGeoip2('180.101.49.12')
    # printPcapIp(r'/home/enosh/Documents/pcap-test.pcap')