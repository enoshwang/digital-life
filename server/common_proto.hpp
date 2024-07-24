#ifndef COMMON_PROTO_HPP
#define COMMON_PROTO_HPP

#include <nlohmann/json.hpp>

#define G_TIMESTAMP std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count()

inline const nlohmann::json json_base_obj = {
    {"type","text"},
    {"sender","server"},
    {"recipient","client"},
    {"timestamp",""},
    {"content",""}
};


#endif // COMMON_PROTO_HPP
