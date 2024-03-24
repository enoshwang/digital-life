import pyaudio
import wave

def capture_audio():
    pa = pyaudio.PyAudio()
    stream = pa.open(format=pyaudio.paInt16, channels=1, 
                     rate=16000, input=True, frames_per_buffer=1024)

    wf = wave.open("test.wav", 'wb')
    wf.setnchannels(1)
    wf.setsampwidth(pa.get_sample_size(pyaudio.paInt16))
    wf.setframerate(16000)

    while True:
        data = stream.read(1024)
        wf.writeframes(data)

    stream.stop_stream()
    stream.close()
    pa.terminate()
    wf.close()

if __name__ == '__main__':
    capture_audio()