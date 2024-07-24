#ifndef COMMON_PROTO_H
#define COMMON_PROTO_H

#include <nlohmann/json.hpp>

#define G_TIMESTAMP std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count()

struct WDHeader {
    int length;
};

inline const nlohmann::json json_base_obj = {
    {"type","text"},
    {"sender","client"},
    {"recipient","server"},
    {"timestamp",""},
    {"content",""}
};


#endif // COMMON_PROTO_H
