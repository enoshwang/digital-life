# -*- coding: utf-8 -*-

import colorlog
import logging
import ffmpeg
import os
import random
import math

# 创建一个logger
logger = colorlog.getLogger(__name__)
logger.setLevel(logging.DEBUG)

# 创建一个handler，用于在控制台输出
handler = colorlog.StreamHandler()
handler.setLevel(logging.DEBUG)

# 定义handler的输出格式
formatter = colorlog.ColoredFormatter(
    "%(log_color)s[%(levelname)-8s]%(reset)s %(blue)s[%(asctime)s.%(msecs)03d] [%(name)s] [%(process)d] [%(thread)d] [%(filename)s:%(lineno)d] %(message)s",
    datefmt='%Y-%m-%d %H:%M:%S',
    reset=True,
    log_colors={
        'DEBUG':    'cyan',
        'INFO':     'green',
        'WARNING':  'yellow',
        'ERROR':    'red',
        'CRITICAL': 'bold_red',
    }
)
handler.setFormatter(formatter)

# 给logger添加handler
logger.addHandler(handler)

def get_video_codec_name(filename):
    try:
        probe = ffmpeg.probe(filename)
    except ffmpeg.Error as e:
        return None

    video_stream = next((stream for stream in probe['streams'] if stream['codec_type'] == 'video'), None)

    if video_stream is not None:
        #logger.info(f"视频流信息: {video_stream}")
        video_codec = video_stream.get('codec_name')

    return video_codec

def list_files_in_path(directory:str,all_files:list):
    """
    递归遍历指定目录及其子目录中的文件和子目录，并输出文件信息和子目录路径。

    参数:
    directory (str): 要遍历的目录路径
    """

    files = os.listdir(directory)

    for file in files:
        full_path = os.path.join(directory, file)

        if os.path.isfile(full_path):
            all_files.append(full_path)
        elif os.path.isdir(full_path):
            logger.info(f"处理子目录: {full_path}")
            list_files_in_path(full_path,all_files)

def transcode_video(file):
    try:
        base_name = os.path.basename(file)
        file_name, file_extension = os.path.splitext(base_name)
        new_file_name =  file.replace(file_name,file_name + "_2").replace(file_extension,".mp4")   
        logger.info(f"开始处理文件: {file}")
        ffmpeg.input(file).output(new_file_name, vcodec="hevc_nvenc",preset='slow',rc='vbr',cq=23,multipass=2).overwrite_output().run()
        logger.info(f"处理完成: {new_file_name}")
        os.remove(file)
        os.rename(new_file_name,file)
    except ffmpeg.Error as e:
        pass

def scale_image(file,out_file,width:int=1920,height:int=1080):
    try:
        output_width, output_height = width,height

        input_video = ffmpeg.input(file, f='image2')

        scaled_video = input_video.filter('scale', size=f'{output_width}:{output_height}', force_original_aspect_ratio='decrease')
        padded_video = scaled_video.filter('pad', output_width, output_height, color='black', x='(ow-iw)/2', y='(oh-ih)/2')
        sar_video = padded_video.filter('setsar', 1)

        output = ffmpeg.output(sar_video, out_file, vframes=1)
        #logger.info(f"find out the used command line arguments:{output.get_args()}")
        output.overwrite_output().run()
        return True
    except Exception as e:
        logger.error(f"Error occurred while scaling and replacing image: {e}")
        return False

def images_to_video(images_path:list, output_video_path:str, useXfade:bool = True, transition:str = None):
    try:
        if transition is None:
            xfade_type = ['fade','fadeblack','fadewhite','distance',
                        'wipeleft','wiperight','wipeup','wipedown','slideleft','slideright','slideup','slidedown',
                        'smoothleft','smoothright','smoothup','smoothdown','circlecrop','rectcrop','circleclose','circleopen',
                        'horzclose','horzopen','vertclose','vertopen','diagbl','diagbr','diagtl','diagtr',
                        'hlslice','hrslice','vuslice','vdslice','dissolve','pixelize','radial','hblur','wipetl',
                        'wipetr','wipebl','wipebr','fadegrays','squeezev','squeezeh','zoomin','hlwind','hrwind',
                        'vuwind','vdwind','coverleft','coverright','coverup','coverdown','revealleft','revealright','revealup','revealdown']
        else:
            xfade_type = [f'{transition}']
        
        image_framerate = 60
        image_display_time = 4
        transition_animation_duration = 1
        total_duration = 0

        image_files  = images_path
        if not image_files or len(image_files) < 2:
            logger.error("没有找到图片文件.")
            return False
        
        if len(image_files) > 50:
            logger.error(f"图片文件过多，请减少图片数量.{len(image_files)} > 50")
            return False

        inputs = []
        for i,img in enumerate(image_files):
            inputs.append(ffmpeg.input(img, loop = 1, framerate = image_framerate,t = image_display_time))

        curTransition = None
        total_duration = image_display_time
        offset = total_duration - transition_animation_duration
        if useXfade:
            curTransition = random.choice(xfade_type)
            stream = ffmpeg.filter([inputs[0],inputs[1]],'xfade',transition = curTransition,duration = transition_animation_duration,offset = offset)
            total_duration = (image_display_time - transition_animation_duration) * 2 + transition_animation_duration * 1
            for i in range(len(inputs) - 2):
                offset = total_duration - transition_animation_duration
                curTransition = random.choice(xfade_type)
                stream = ffmpeg.filter([stream,inputs[i + 2]],'xfade',transition = curTransition,duration = transition_animation_duration,offset = offset)
                total_duration = (image_display_time - transition_animation_duration) * (2 + i + 1) + transition_animation_duration
        else:
            stream = ffmpeg.filter(inputs,'concat',n=len(image_files),v=1)

        logger.info(f'total duration:{total_duration}s')
        output = ffmpeg.output(stream, output_video_path, vcodec="hevc",preset='slow',rc='vbr',sar='1:1',pix_fmt='yuv420p',cq=23,multipass=2)

        #logger.info(f"find out the used command lincle arguments:{output.get_args()}")
        output.overwrite_output().run()
        return True
    except Exception as e:
        logger.error(f"Error occurred while creating video: {e},curTransition:{curTransition}")
        return  False
    
def video_merge(video_list:list,output_path:str):
    if len(video_list) == 0:
        return False
    
    try:
        inputs = []
        for video in video_list:
            inputs.append(ffmpeg.input(video))

        filter = ffmpeg.filter(inputs,'concat',n=len(inputs),v=1)

        output = ffmpeg.output(filter,output_path,vcodec="hevc",multipass=2)
        output.overwrite_output().run()
    except Exception as e:
        logger.error(f"Error occurred while video merge: {e}")
        return  False
    
def video_to_video(src:str,dst:str):
    try:
        output_width = 1920
        output_height = 1080

        input = ffmpeg.input(src)
        
        scaled_video = input.filter('scale', size=f'{output_width}:{output_height}', force_original_aspect_ratio='decrease')
        padded_video = scaled_video.filter('pad', output_width, output_height, color='black', x='(ow-iw)/2', y='(oh-ih)/2')
        sar_video = padded_video.filter('setsar', 1)

        output = ffmpeg.output(sar_video,dst, vcodec="hevc",preset='slow',crf=28,sar='1:1',pix_fmt='yuv420p')
        output.overwrite_output().run()
        return True
    except ffmpeg.Error as e:
        logger.error(f"Error occurred while video({src}) to video({dst}): {e}")
        return  False

def doTranscode():
    all_files = []
    file_or_path = "E:\material\视频"
    if os.path.isfile(file_or_path):
        all_files.append(file_or_path)
    elif os.path.isdir(file_or_path): 
        list_files_in_path(file_or_path,all_files)

    for file in all_files:
        try:
            if get_video_codec_name(file) == "h264":
                transcode_video(file)
        except ffmpeg.Error as e:
            pass

def delete_files(files:list):
    for file in files:
        try:
            os.remove(file)
        except Exception as e:
            logger.warn(f"删除文件失败:{file}")

def doVideoSynthesis():
    image_folder = "E:\\dd"
    output_video_file = "E:\\dd\\output.mp4"

    gif_files = [f for f in os.listdir(image_folder) if f.lower().endswith(('.gif'))]
    image_files  = sorted([f for f in os.listdir(image_folder) if f.lower().endswith(('.png', '.jpg', '.jpeg'))])
    if not image_files or len(image_files) < 2:
        logger.error("没有找到图片文件.")
        return

    image_paths = []
    for i,img in enumerate(image_files):
        imag_path = os.path.join(image_folder, img)
        image_paths.append(imag_path)

    logger.info(f"image_paths len:{len(image_paths)}")

    image_paths_tmp = []
    for image_path in image_paths:
        output_image_path = os.path.splitext(image_path)[0] + '_tmp' + os.path.splitext(image_path)[1]
        ret = scale_image(image_path, output_image_path)
        if ret == True:
            image_paths_tmp.append(output_image_path)
        else:
            logger.error(f"缩放图片失败:{image_path}")
            delete_files(image_paths_tmp)
            return
        
    videos = []
    images_for_one_video = 30
    video_num = math.floor(len(image_paths_tmp) / images_for_one_video)
    images_to_video_is_ok = True
    for i in range(video_num):
        video_file = os.path.join(image_folder, f"output_{i}.mp4")
        if i == video_num - 1:
            ret = images_to_video(image_paths_tmp[i * images_for_one_video :], video_file)
            if ret == True:
                videos.append(video_file)
            else:
                logger.error(f"图片合成视频失败:{video_file}")
                images_to_video_is_ok = False
                break
        else:
            ret = images_to_video(image_paths_tmp[i * images_for_one_video : (i + 1) * images_for_one_video], video_file)
            if ret == True:
                videos.append(video_file)
            else:
                logger.error(f"图片合成视频失败:{video_file}")
                images_to_video_is_ok = False
                break

    delete_files(image_paths_tmp)
    if(images_to_video_is_ok == False):
        logger.error("图片合成视频失败")
        delete_files(videos)
        return
    
    gif_files_tmp = []
    for gif in gif_files:
        gif_path = os.path.join(image_folder, gif)
        output_gif_path = os.path.splitext(gif_path)[0] + '_tmp.mp4'
        ret = video_to_video(gif_path, output_gif_path)
        if ret == True:
            gif_files_tmp.append(output_gif_path)
        else:
            logger.error(f"gif 转视频失败:{gif_path}")
            delete_files(videos)
            delete_files(gif_files_tmp)
            return

    all_videos = videos + gif_files_tmp
    random.shuffle(all_videos)

    video_merge(all_videos,output_video_file)

    delete_files(all_videos)

    logger.info(f"ret:{ret}")

def main():
    #doTranscode()
    doVideoSynthesis()

if __name__ == '__main__':
    main()