from typing import Optional, Any

from PIL import Image
from PIL.ExifTags import TAGS

# exif info
# key：value :
#               DateTime     :  日期和时间
#               Model        :  设备型号
#               Flash        :  是否使用闪光灯
#               ImageWidth  ：  图像宽度，指横向像素数
#               ImageLength ：  图像高度，指纵向像素数
#               GPSInfo     ：  定位数据
#               FocalLength ：  焦距，一般显示镜头物理焦距
# more in bottom


def readExif(img):
    exif_info = {}
    try:
        handle: Optional[Any] = Image.open(img)
        info = handle._getexif()
        mode = handle.mode  # RGB
        size = handle.size   # (1920,1080)
        format = handle.format  # JPEG
        format_description = handle.format_description  # JPEG (ISO 10918)
        if info:
            for (tag, value) in info.items():
                decoded = TAGS.get(tag, tag)
                exif_info[str(decoded)] = str(value)
        if mode:
            exif_info['mode'] = str(mode)
        if size:
            exif_info['size'] = str(size)
        if format:
            exif_info['format'] = str(format)
        if format_description:
            exif_info['format_description'] = str(format_description)
        return exif_info
    except Exception as e:
        return exif_info


if __name__ == "__main__":
    exifInfo = readExif('')
    print(exifInfo)

# handle.mode
# modes	    Description
# 1	        1位像素，黑白图像，存成8位像素
# L	        8位像素，黑白
# P	        9位像素，使用调色板映射到任何其他模式
# RGB	    3*8位像素，真彩
# RGBA	    4*8位像素，真彩+透明通道
# CMYK	    4*8位像素，印刷四色模式或彩色印刷模式
# YCbCr	    3*8位像素，色彩视频格式
# I	        32位整型像素
# F	        33位浮点型像素
