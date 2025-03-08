# -*- coding: utf-8 -*-

# 一个使用 GPT-3 引擎进行对话生成的 AI 智能助理。

import os
import openai
from enum import Enum, unique
import sys
from PySide6 import QtWidgets,QtCore
from concurrent.futures import ThreadPoolExecutor

# 设置环境变量，确保请求可以通过代理连接到 OpenAI API
os.environ["HTTP_PROXY"] = "127.0.0.1:10809"
os.environ["HTTPS_PROXY"] = "127.0.0.1:10809"

# 确保枚举值唯一
@unique
class InteractiveMode(Enum):
    CMD = 0
    GUI = 1


@unique
class AIEngines(Enum):
    GPT3 = 'gpt-3.5-turbo'
    BERT = 'bert'


openai.api_key = os.getenv("OPENAI_API_KEY")

ew_pool = ThreadPoolExecutor(max_workers=8)


class EWOpenAI:
    def __init__(self, engine: AIEngines = AIEngines.GPT3):
        self.user = "炎冬"
        self.messages = [{"role": "system", "content": "AI 智能助理"}]
        self.engine = engine

    # 根据用户输入生成回答
    def make_chat_completions(self, question: str):
        self.messages.append({"role": "user", "content": question})

        try:
            rsp = openai.ChatCompletion.create(
                model=self.engine.value,
                messages=self.messages,
                timeout = 5
            )
        except Exception as e:
            return e

        ret = rsp.get("choices")[0]["message"]["content"]
        self.messages.append({"role": "assistant", "content": ret})
        return ret

    # 将音频文件转录成文本
    def make_audio_transcriptions(self, audio_file: str):
        # Transcribe audio into whatever language the audio is in.
        # File uploads are currently limited to 25 MB and the following input file types are supported: mp3, mp4, mpeg, mpga, m4a, wav, and webm.

        with open(audio_file, "rb") as f:
            transcript = openai.Audio.transcribe("whisper-1", f)
            return transcript.get("text")

    # 生成图片
    def make_images_generations(self, description: str):
        # Generate an image from a prompt.can have a size of 256x256, 512x512, or 1024x1024 pixels.

        response = openai.Image.create(
            prompt=description,
            n=1,
            size="1024x1024"
        )
        return response['data'][0]['url']

    # 生成图片
    def make_images_create_variation(self, image_url: str):
        # Create a variation of an image.the input image must be a square PNG image less than 4MB in size.

        with open(image_url, "rb") as f:
            response = openai.Image.create_variation(
                image=f,
                n=1,
                size="1024x1024"
            )
            return response['data'][0]['url']


class ChatBot(QtWidgets.QMainWindow):
    def __init__(self, user_name: str = "You", bot_name: str = "ChatGPT"):
        super().__init__()

        self.user_name = user_name
        self.bot_name = bot_name
        self.ew_openai = EWOpenAI()

        # 初始化窗口
        self.init_ui()

    def init_ui(self):
        # 设置标题和窗口大小
        self.setWindowTitle('ChatBot')
        self.setFixedSize(500,600)
        self.setMinimumSize(0,0)
        self.setMaximumSize(16777215,16777215)

        # 聊天框
        self.text_area = QtWidgets.QTextEdit()
        self.text_area.setReadOnly(True)
        self.text_area.setVerticalScrollBarPolicy(QtCore.Qt.ScrollBarPolicy.ScrollBarAlwaysOn)

        # 输入框
        self.input_box = QtWidgets.QLineEdit()

        # 发送按钮
        self.send_button = QtWidgets.QPushButton('Send')
        self.send_button.setFixedSize(50,26)
        self.send_button.setDefault(True)

        # 布局设置
        self.chat_layout = QtWidgets.QVBoxLayout()
        self.chat_layout.addWidget(self.text_area)
        self.chat_layout.addWidget(self.input_box)

        self.input_layout = QtWidgets.QHBoxLayout()
        self.input_layout.addWidget(self.input_box)
        self.input_layout.addWidget(self.send_button)
        
        # 清空聊天记录
        self.clear_button = QtWidgets.QPushButton('Clear Chat')
        self.clear_button.setFixedSize(100,26)

        self.button_layout = QtWidgets.QHBoxLayout()
        self.button_layout.addWidget(self.clear_button)

        self.main_layout = QtWidgets.QVBoxLayout()
        self.main_layout.addLayout(self.chat_layout)
        self.main_layout.addLayout(self.input_layout)
        self.main_layout.addLayout(self.button_layout)

        self.widget = QtWidgets.QWidget()
        self.widget.setLayout(self.main_layout)
        self.setCentralWidget(self.widget)

        self.input_box.returnPressed.connect(self.send_message)
        self.send_button.clicked.connect(self.send_message)
        self.clear_button.clicked.connect(self.clear_text_area)

    def clear_text_area(self):
        self.text_area.clear()
        self.ew_openai.messages = [{"role": "system", "content": "AI 智能助理"}]

    def make_chat_completions_thread(self, question: str):
        response = self.ew_openai.make_chat_completions(question)
        self.add_message(self.bot_name, response)

    def add_message(self, username, message):
        formatted_message = f'{username}: {message}'

        self.text_area.append(formatted_message)
        self.text_area.append("")

    def send_message(self):
        message = self.input_box.text()
        self.input_box.clear()
        self.add_message(self.user_name, message)

        ew_pool.submit(self.make_chat_completions_thread, message)



def main(interactive_mode: InteractiveMode = InteractiveMode.CMD):

    # print(ew_openai.make_audio_transcriptions("nihao.wav"))
    # print(ew_openai.make_images_create_variation("test.png"))

    if interactive_mode == InteractiveMode.GUI:
        app = QtWidgets.QApplication(sys.argv)
        chat_bot = ChatBot(user_name="炎冬")
        chat_bot.show()
        sys.exit(app.exec())

    ew_openai = EWOpenAI()
    while True:
        question = input(f"【{ew_openai.user}】")
        match question:
            case "exit":
                break
            case "clear":
                ew_openai.messages = [{"role": "system", "content": "个人智能助理"}]
                continue
            case _:
                answer = ew_openai.make_chat_completions(question)
                print(f"【Chat】{answer}")


if __name__ == '__main__':
    main(InteractiveMode.GUI)
