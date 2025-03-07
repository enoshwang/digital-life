//
// Created by enosh on 2021/1/12.
//

#pragma once

#include <iostream>
#include <sstream>
#include <string>
#include <thread>
#include <map>

#include <my_utils/filesystem.hpp>
#include <my_utils/log.hpp>
#include <my_utils/obj_pool.hpp>
#include <my_utils/queue.hpp>
#include <my_utils/singleton.hpp>
#include <my_utils/uuid.hpp>

#ifdef unix
#include <sys/types.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>
#endif  // unix

using namespace std::string_literals;

namespace ew {
namespace my_utils {

enum class Errno : int {
    SUCCESS = 0,  // It's Ok
    NOENTRY = 1,  // No such file or directory
    CENTRYF = 2,  // Create file or directory failed

    FORKERR = 3  // fork error
};

static const std::map<Errno, std::string> ERRNO_MSG{
    {Errno::SUCCESS, "It's Ok."},
    {Errno::NOENTRY, " No such file or directory."},
    {Errno::CENTRYF, "Create file or directory failed."},
    {Errno::FORKERR, "fork error."}

};

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
}  // namespace my_utils
}  // namespace ew
