#ifndef WD_COMMON_HPP
#define WD_COMMON_HPP

namespace wd
{
    namespace common
    {
        enum class ServerEngine
        {
            ASIO = 0X00,
            REACTOR = 0X01
        };

        enum class ServerType
        {
            ECHO_SERVER = 0X00,
            ONLINE_CHAT_ROOM = 0X01
        };

        struct ServerConfig
        {
            ServerType type;

            ServerConfig():type(ServerType::ECHO_SERVER){}
        };
    } // namespace common
    
} // namespace wd


#endif // WD_COMMON_HPP