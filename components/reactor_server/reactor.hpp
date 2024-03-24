#ifndef REACTOR_HPP
#define REACTOR_HPP

#include <unordered_map>
#include <memory>
#include <mutex>
#include <future>

#include "eventDemultiplex.hpp"

#include "common.hpp"
#include "onlineChatRoom.hpp"

using evHandle_map = std::unordered_map<int, std::shared_ptr<EventHandle>>;

class Reactor final
{
public:
    Reactor(std::unique_ptr<EpollDemultiplex> demu,const wd::common::ServerConfig& serverConfig);
    ~Reactor();

    Reactor(const Reactor &) = delete;
    Reactor &operator=(const Reactor &) = delete;
    Reactor(Reactor &&) = delete;
    Reactor &operator=(Reactor &&) = delete;

    int registerEventHandle(std::shared_ptr<EventHandle> eventhandle, int event) noexcept;
    void removeEventHandle(std::shared_ptr<EventHandle> eventhandle) noexcept;
    void modifyEventHandle(std::shared_ptr<EventHandle> eventhandle,int epoll_event) noexcept;
    std::shared_ptr<EventHandle> getEventHandle(int fd) noexcept;

    int run() noexcept;

private:
    std::unique_ptr<EpollDemultiplex> m_evDemux;

public:
    // 服务器配置
    wd::common::ServerConfig m_server_config;

    evHandle_map m_ep_events;
    std::mutex m_mutex_ep_events;

    // 在线聊天室
    std::unique_ptr<OnlineChatRoom> m_onlineChatRoom;
    std::future<void> m_onlineChatRoom_future;
    friend class OnlineChatRoom;
};

#endif // REACTOR_HPP