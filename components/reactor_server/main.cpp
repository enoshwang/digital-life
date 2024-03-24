#include "main.hpp"

#include <arpa/inet.h> // htons
#include <cstdlib>     // atoi
#include <cstring>    // memset_s
#include <string>
#include <chrono>
#include <iostream>

#include <sys/types.h>
#include <sys/socket.h> // socket,bind,listen
#include <memory>

#include <unistd.h>
#include <fcntl.h>  // fcntl - manipulate file descriptor

#include "eventDemultiplex.hpp"
#include "eventHandle.hpp"
#include "reactor.hpp"

#include "chatSession.hpp" 

#include "common.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/sinks/rotating_file_sink.h>
#include <spdlog/spdlog.h>

using namespace std::string_literals;

int main(int argc, char *argv[])
{
    // 初始化
    init_log();
    SPDLOG_INFO("start");

    if (argc < 2)
    {
        SPDLOG_ERROR("Usage: wd_server port [<port> ...]");
        return 0;
    }

    wd::common::ServerEngine server_engine = wd::common::ServerEngine::REACTOR;
    switch (server_engine)
    {
        case wd::common::ServerEngine::ASIO:
            {
                try
                {
                    asio::io_context io_context(1);

                    for (int i = 1; i < argc; ++i)
                    {
                        SPDLOG_INFO("start listen port:{}",argv[i]);
                        unsigned short port = std::atoi(argv[i]);
                        co_spawn(io_context,listener(tcp::acceptor(io_context, {tcp::v4(), port})),detached);
                    }

                    asio::signal_set signals(io_context, SIGINT, SIGTERM);
                    signals.async_wait([&](auto, auto){ io_context.stop();});

                    io_context.run();
                }
                catch(const std::exception& e)
                {
                    SPDLOG_ERROR("e what:{}",e.what());
                }
                break;
            }
        case wd::common::ServerEngine::REACTOR:
            {
                // 创建监听套接字
                int sockfd {0};
                if(!create_listen_socket(atoi(argv[1]),sockfd))
                {
                    SPDLOG_ERROR("create_listen_socket failed");
                    return 0;
                }

                // 创建 EventHandle
                auto evHanlde = std::make_shared<EventHandle>(sockfd);

                // 创建 EpollDemultiplex
                auto eventDemux = std::make_unique<EpollDemultiplex>();

                // 创建 Reactor
                wd::common::ServerConfig serverConfig;
                serverConfig.type = wd::common::ServerType::ONLINE_CHAT_ROOM;
                SPDLOG_INFO("ServerConfig: type: {0}", static_cast<int>(serverConfig.type));
                auto reactor = std::make_unique<Reactor>(std::move(eventDemux),serverConfig);

                // 注册事件
                reactor->registerEventHandle(evHanlde, Event_Accept);

                // 运行
                reactor->run();

                close(sockfd);
                break;
            }
        default:
            break;
    }

    return 0;
}

void init_log()
{
    try {
        // rotating_sink : 5mb per file, 0 rotated files
        auto file_name = "wd_reactor_server.log"s;
        auto max_size  = 1048576 * 5;
        auto max_files = 2;
        auto rotating_sink =
            std::make_shared<spdlog::sinks::rotating_file_sink_mt>(
                file_name, max_size, max_files);

        // logger : rotating_sink
        auto logger = std::make_shared<spdlog::logger>("logger", rotating_sink);
        
        int64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        std::string spdlog_pattern =
            "[%Y-%m-%d %H:%M:%S.%f] [%P] [%t] [%s:%#] [%l] "s + std::to_string(now) + " %v"s;
        logger->set_pattern(spdlog_pattern);
        logger->set_level(spdlog::level::debug);
        logger->flush_on(spdlog::level::info);

        spdlog::set_default_logger(logger);
        //spdlog::flush_every(std::chrono::seconds(3));
        SPDLOG_INFO("init_log success");
    } catch (const spdlog::spdlog_ex &ex) {
        std::cerr << "Log init failed: " << ex.what() << std::endl;
        exit(1);
    }
}

bool create_listen_socket(int port, int &sockfd)
{
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd == -1)
    {
        SPDLOG_ERROR("socket return -1,errno is : {0}", errno);
        return false;
    }
    SPDLOG_INFO("create sockfd: {0}", sockfd);

    int opt = 1;
    if(setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)))
    {
        SPDLOG_ERROR("setsockopt");
        close(sockfd);
        return false;
    }
    SPDLOG_INFO("setsockopt SO_REUSEADDR success");

    auto ret = fcntl(sockfd, F_SETFL, O_NONBLOCK);
    if (ret == -1)
    {
        SPDLOG_ERROR("fcntl return -1,errno is : {0}", errno);
        close(sockfd);
        return false;
    }

    struct sockaddr_in sockaddrVar;
#ifdef __STDC_LIB_EXT1__
    ret = memset_s(&sockaddrVar,sizeof(sockaddrVar), 0, sizeof(sockaddrVar));
    if(ret != 0){
        SPDLOG_ERROR("memset_s return failed");
        close(sockfd);
        return false;
    }
#else
    memset(&sockaddrVar, 0, sizeof(sockaddrVar));
#endif //__STDC_LIB_EXT1__

    sockaddrVar.sin_addr.s_addr = htonl(INADDR_ANY);
    sockaddrVar.sin_family = AF_INET;
    sockaddrVar.sin_port = htons(port);
    SPDLOG_INFO("s_addr: INADDR_ANY, sin_family: AF_INET, sin_port: {0}",ntohs(sockaddrVar.sin_port));

    ret = bind(sockfd, (struct sockaddr *)&sockaddrVar, sizeof(sockaddrVar));
    if (ret != 0)
    {
        SPDLOG_ERROR("bind return failed,errno is : {0}", errno);
        close(sockfd);
        return false;
    }
    SPDLOG_INFO("bind success");

    if(listen(sockfd, MAXIMUM_LENGTH_FOR_LISTEN))
    {
        SPDLOG_ERROR("listen return -1,errno is : {0}", errno);
        close(sockfd);
        return false;
    }
    SPDLOG_INFO("start listen,port is: {0}, maximux accept connections: {1}", port, MAXIMUM_LENGTH_FOR_LISTEN);

    return true;
}
