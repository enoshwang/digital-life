#pragma once

#include <chrono>
#include <string_view>

namespace wd {
namespace common {

class EWTimer
{
private:
    std::chrono::high_resolution_clock             clock;
    std::chrono::high_resolution_clock::time_point createTime;
    std::string_view                               tag;

public:
    EWTimer(std::string_view sv) : tag(sv)
    {
        createTime = clock.now();
    }
    ~EWTimer()
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
#define EW_AUTO_TIME(msg) EWTimer ewAutoTimer = EWTimer(msg)
#else
#define EW_AUTO_TIME
#endif

}  // namespace common
}  // namespace wd