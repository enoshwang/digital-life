#include "reactor.hpp"

#include <arpa/inet.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <stdlib.h>
#include <sys/epoll.h>
#include <sys/socket.h>
#include <unistd.h>

#include <thread>

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

Reactor::Reactor(std::unique_ptr<EpollDemultiplex> demu,
                 const wd::common::ServerConfig&   serverConfig)
    : m_evDemux(std::move(demu)), m_server_config(serverConfig)
{
    {
        std::lock_guard<std::mutex> lock(m_mutex_ep_events);
        m_ep_events.clear();
    }
    SPDLOG_INFO("init Reactor.clear m_ep_events.");
}

Reactor::~Reactor()
{
    {
        std::lock_guard<std::mutex> lock(m_mutex_ep_events);
        m_ep_events.clear();
    }
}

int Reactor::registerEventHandle(std::shared_ptr<EventHandle> eventhandle,
                                 int                          event) noexcept
{
    auto fdNum = eventhandle->GetFd();
    SPDLOG_INFO("the fd {0} is registering. And the event is: {1}", fdNum,
                event);

    {
        std::lock_guard<std::mutex> lock(m_mutex_ep_events);
        auto                        it = m_ep_events.find(fdNum);
        if (it == m_ep_events.end()) {
            m_ep_events.insert(std::make_pair(fdNum, eventhandle));
        } else {
            SPDLOG_WARN("the fd {0} is already in m_ep_events.", fdNum);
        }
    }

    return m_evDemux->regist(eventhandle, event);
}

void Reactor::removeEventHandle(
    std::shared_ptr<EventHandle> eventhandle) noexcept
{
    m_evDemux->remove(eventhandle);

    int  fd = eventhandle->GetFd();

    {
        std::lock_guard<std::mutex> lock(m_mutex_ep_events);
        auto it = m_ep_events.find(fd);
        if (it == m_ep_events.end()) {
            SPDLOG_WARN("fd {} is not found.", fd);
            return;
        }
        m_ep_events.erase(it);
    }

    SPDLOG_INFO("fd {} is removed.", fd);
}

void Reactor::modifyEventHandle(std::shared_ptr<EventHandle> eventhandle,
                                int epoll_event) noexcept
{
    m_evDemux->modify(eventhandle, epoll_event);
}

int Reactor::run() noexcept
{

    if (m_server_config.type == wd::common::ServerType::ONLINE_CHAT_ROOM)
    {
        SPDLOG_INFO("server type is ONLINE_CHAT_ROOM.");
        m_onlineChatRoom = std::make_unique<OnlineChatRoom>();
        m_onlineChatRoom_future = std::async(std::launch::async, &OnlineChatRoom::run, m_onlineChatRoom.get(), this);
    }

    SPDLOG_INFO("reactor is running.");
    m_evDemux->wait_event(this, -1);
    SPDLOG_INFO("reactor run over.");
    return 0;
}

std::shared_ptr<EventHandle> Reactor::getEventHandle(int fd) noexcept
{
    {
        std::lock_guard<std::mutex> lock(m_mutex_ep_events);
        auto it = m_ep_events.find(fd);
        if (it != m_ep_events.end()) {
            SPDLOG_DEBUG("getEventHandle, fd: {}", fd);
            return it->second;
        } else {
            SPDLOG_WARN("getEventHandle, fd: {} is not found in m_ep_events.", fd);
        }
    }

    return nullptr;
}