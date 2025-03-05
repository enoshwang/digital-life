#pragma once

#include <random>
#include <sstream>
#include <string>

namespace wd {
namespace common {

inline std::string generate_uuid_v4(bool with_bar = false)
{
    static std::random_device              rd;
    static std::mt19937_64                 gen(rd());
    static std::uniform_int_distribution<> dis(0, 15);
    static std::uniform_int_distribution<> dis2(8, 11);

    std::stringstream ss;
    ss << std::hex;
    for (int i = 0; i < 8; i++) {
        ss << dis(gen);
    }
    if (with_bar) {
        ss << "-";
    }
    for (int i = 0; i < 4; i++) {
        ss << dis(gen);
    }
    if (with_bar) {
        ss << "-";
    }
    ss << "4";
    for (int i = 0; i < 3; i++) {
        ss << dis(gen);
    }
    if (with_bar) {
        ss << "-";
    }
    ss << dis2(gen);
    for (int i = 0; i < 3; i++) {
        ss << dis(gen);
    }
    if (with_bar) {
        ss << "-";
    }
    for (int i = 0; i < 12; i++) {
        ss << dis(gen);
    };
    return ss.str();
}

}  // namespace common
}  // namespace wd