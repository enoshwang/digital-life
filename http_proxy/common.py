# -*- coding: utf-8 -*-


import subprocess
import shlex
import platform

def run_cmd(cmd:str):

    result = list()
    try:
        if platform.system() == "Windows":
            ret = subprocess.Popen(cmd, stdout=subprocess.PIPE)

            result.append(0)
            content = ret.stdout.readlines()
            strList = "".join([elem.decode("utf-8") for elem in content])
            result.append(strList)
        elif platform.system() == "Linux":
            ret = subprocess.run(shlex.split(cmd), capture_output=True)

            if ret.returncode == 0:
                result.append(0)
                content = ret.stdout.decode(encoding="utf-8")
                result.append(content)
            else:
                result.append(1)
        else:
            result.append(-3)

    except FileNotFoundError as e:
        result.append(-1)
    except Exception as e:
        result.append(-2)
    finally:
        return result

def get_user_choice(strList:list):
    for index,elem in enumerate(strList):
        print(f"{index} : {elem}")
    while True:
        try:
            choice = int(input("Please input your choice:"))
            if choice >= 0 and choice < len(strList):
                break
            else:
                print("Please input your choice again.")
        except Exception as e:
            print("Please input your choice again.")
    return choice
    
def main():
    # run_cmd
    #ret = run_cmd("nvcc -V") 
    #print(ret)

    # get_user_choice
    strList = ["a","b","c"]
    choice = get_user_choice(strList)
    print(strList[choice])

if __name__ == "__main__":
    main()
