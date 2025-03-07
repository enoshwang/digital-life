#pragma once

#include <chrono>
#include <string_view>

namespace ew {
namespace my_utils {

class Timer
{
private:
    std::chrono::high_resolution_clock             clock;
    std::chrono::high_resolution_clock::time_point createTime;
    std::string_view                               tag;

public:
    Timer(std::string_view sv) : tag(sv)
    {
        createTime = clock.now();
    }
    ~Timer()
    {
        auto endTime  = clock.now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
            endTime - createTime);
        auto secondDuration = std::chrono::duration_cast<std::chrono::seconds>(
            endTime - createTime);
    }
};

#define OPEN_EW_AUTO_TIME
#ifdef OPEN_EW_AUTO_TIME
#define EW_AUTO_TIME(msg) Timer ewAutoTimer = Timer(msg)
#else
#define EW_AUTO_TIME
#endif

}  // namespace my_utils
}  // namespace ew