#pragma once

#include <map>
#include <string>

namespace wd {
namespace common {
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
}  // namespace common

}  // namespace wd