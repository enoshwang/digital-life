import logging
from logging.handlers import RotatingFileHandler
import sys

class Logger(object):
    def __init__(self, logger_name:str='logger',file_name:str=None, level:int=logging.DEBUG,acid:str=None,file_mode:str='a'):
        datefmt = '%Y-%m-%d %H:%M:%S'

        if acid is None:
            self.fmt = logging.Formatter('[%(asctime)s.%(msecs)03d] [%(name)s] [%(process)d] [%(thread)d] [%(filename)s:%(lineno)d] [%(levelname)s] %(message)s', datefmt)
        else:
            self.fmt = logging.Formatter(f'[%(asctime)s.%(msecs)03d] [%(name)s] [%(process)d] [%(thread)d] [%(filename)s:%(lineno)d] [%(levelname)s] [{acid}] %(message)s', datefmt)
        self.logger = logging.getLogger(logger_name)
        self.logger.setLevel(level)
        if file_name is not None:
            if file_mode == 'w':
                self.file_handler = logging.FileHandler(file_name, 'w', encoding='utf-8')
            else:
                # maxBytes = 1 * 1024 * 1024  = 1048576 (1GB)  51200 ： 50M
                self.file_handler = RotatingFileHandler(file_name, maxBytes=51200, backupCount=2,encoding='utf-8')
            self.file_handler.setFormatter(self.fmt)
            self.logger.addHandler(self.file_handler)
        self.console_handler = logging.StreamHandler(stream=sys.stdout)
        self.console_handler.setFormatter(self.fmt)
        self.logger.addHandler(self.console_handler)
               
def main():
    logger.debug("debug")
    logger.info("info")
    logger.warning("warning")
    logger.error("error")
    logger.critical("critical")
    logger.fatal("fatal")

if __name__ == "__main__":
    logger = Logger(acid="123").logger
    main()

