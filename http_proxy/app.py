#!-*- coding:utf-8 -*-
import time

import util

import redis
from flask import Flask, request


logger = util.Logger(file_name="app.log").logger

app = Flask(__name__)
cache = redis.Redis(host='redis', port=6379)


def get_hit_count():
    retries = 3
    while True:
        try:
            return cache.incr('hits')
        except redis.exceptions.ConnectionError as exc:
            if retries == 0:
                raise exc
            retries -= 1
            time.sleep(0.5)


@app.route('/')
def root():
    count = get_hit_count()
    return f'I have been hits {count} times.'


@app.route("/make_chat", methods=['POST'])
def make_chat():
    try:
        logger.info(
            f"recv request from user:{request.remote_user},addr:{request.remote_addr} to {request.url}")
        params = request.json
        logger.info(f"params:{params}")

        return "make_chat"
    except Exception as e:
        return e
