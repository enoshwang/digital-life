//
// Created by enosh on 2021/1/12.
//

#pragma once

#include <iostream>
#include <sstream>
#include <string>
#include <thread>

#ifdef unix
#include <sys/types.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>
#endif  // unix

using namespace std::string_literals;

namespace ew {
namespace my_utils {
/**
 * @brief getThreadIdOfString
 * @param id
 * @return string for thread id
 */
inline std::string getThreadIdOfString(const std::thread::id& id) noexcept
{
    std::stringstream sin;
    sin << id;
    return sin.str();
}
/**
 * @brief eExeclp 用于替换当前进程的映像，以运行不同的程序 
 * @param buf
 * @return int
 */
inline int eExeclp(char* buf)
#ifdef unix
{
    pid_t pid;
    while (true) {
        pid = fork();
        if (0 == pid) {
            if (execlp(buf,buf,nullptr) < 0)
                return -1;
            else
                return 0;
        } else if (pid > 0) {
            int status;
            waitpid(pid, &status, 0);
            return status;
        } else {
            return -2;
        }
    }
    return 0;
}
#else
{
    return -3;
}
#endif  // unix

/**
 * @brief isLittleEndian
 * @param
 * @return bool
 */
inline bool isLittleEndian() noexcept
{
    auto tmpA{1};
    auto tmpB = *((char*)(&tmpA));
    if (tmpB == 1)
        return true;
    else
        return false;
}
}  // namespace common
}  // namespace ew
