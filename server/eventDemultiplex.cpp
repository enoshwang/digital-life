#include "eventDemultiplex.hpp"

#include <sys/epoll.h> // epoll_create,epoll_ctl,epoll_wait  epoll_event
#include <cstring> // memset_s

#include "eventHandle.hpp"
#include "reactor.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

EpollDemultiplex::EpollDemultiplex()
{
    m_epollFd = epoll_create(1);
    SPDLOG_INFO("init epoll demultiplex, epollFd: {}", m_epollFd);
}

EpollDemultiplex::~EpollDemultiplex()
{
    close(m_epollFd);
    SPDLOG_INFO("close epoll demultiplex, epollFd: {}", m_epollFd);
}

int EpollDemultiplex::regist(std::shared_ptr<EventHandle> eventhandle, int event) noexcept
{
    SPDLOG_INFO("epoll demultiplex regist event, fd: {0}, events: {1}", eventhandle->GetFd(), event);

    struct epoll_event ep_ev;
#ifdef __STDC_LIB_EXT1__
    auto ret = memset_s(&ep_ev, sizeof(ep_ev), 0, sizeof(ep_ev));
    if (ret != 0)
    {
        SPDLOG_WARN("memset_s return failed");
        return -1;
    }
#else
    memset(&ep_ev, 0, sizeof(ep_ev));
#endif //__STDC_LIB_EXT1__

    auto fd = eventhandle->GetFd();
    ep_ev.data.ptr = eventhandle.get();
    ep_ev.data.fd = fd;
    ep_ev.events = EPOLLIN | EPOLLET | EPOLLRDHUP;

    if (event & Event_Accept)
    {
        m_listen_sock_fd = fd;
        SPDLOG_INFO("set m_listen_sock_fd: {0}", fd);
    }
    eventhandle->SetStatus(1);

    // control interface for an epoll file descriptor
    if (epoll_ctl(m_epollFd, EPOLL_CTL_ADD, fd, &ep_ev) < 0)
    {
        SPDLOG_WARN("epoll_ctl return failed, fd: {}", fd);
        return -1;
    }

    return 0;
}

void EpollDemultiplex::remove(std::shared_ptr<EventHandle> eventhandle) noexcept
{
    SPDLOG_INFO("epoll demultiplex remove event, fd: {}", eventhandle->GetFd());
    
    if (eventhandle->GetStatus() != 1)
        return;
    eventhandle->SetStatus(0);

    struct epoll_event ep_ev;
#ifdef __STDC_LIB_EXT1__
    auto ret = memset_s(&ep_ev, sizeof(ep_ev), 0, sizeof(ep_ev));
    if (ret != 0)
    {
        SPDLOG_WARN("memset_s return failed");
        return -1;
    }
#else
    memset(&ep_ev, 0, sizeof(ep_ev));
#endif //__STDC_LIB_EXT1__

    ep_ev.data.ptr = eventhandle.get();

    int fd = eventhandle->GetFd();
    if (epoll_ctl(m_epollFd, EPOLL_CTL_DEL, fd, &ep_ev) < 0)
    {
        SPDLOG_WARN("epoll_ctl EPOLL_CTL_DEL return failed , and the fd is：{}", fd);
        return;
    }

    return;
}

int EpollDemultiplex::getListenFd() const noexcept
{
    return m_listen_sock_fd;
}

void EpollDemultiplex::modify(std::shared_ptr<EventHandle> eventhandle,int epoll_event) noexcept
{
    SPDLOG_INFO("epoll demultiplex modify event, fd: {}", eventhandle->GetFd());

    struct epoll_event ep_ev;
    memset(&ep_ev, 0, sizeof(ep_ev));

    auto fd = eventhandle->GetFd();
    ep_ev.data.ptr = eventhandle.get();
    ep_ev.data.fd = fd;

    if(epoll_event & EPOLLIN)
    {   
        ep_ev.events = EPOLLIN | EPOLLET | EPOLLRDHUP;
        SPDLOG_INFO("epoll demultiplex modify event, fd: {}, EPOLLIN", eventhandle->GetFd());
    }
    else if(epoll_event & EPOLLOUT)
    {
        ep_ev.events = EPOLLOUT | EPOLLET | EPOLLONESHOT | EPOLLRDHUP;
        SPDLOG_INFO("epoll demultiplex modify event, fd: {}, EPOLLOUT", eventhandle->GetFd());
    }

    epoll_ctl(m_epollFd, EPOLL_CTL_MOD, fd, &ep_ev);
}

void EpollDemultiplex::wait_event(Reactor *reactor, int timeout) noexcept
{
    SPDLOG_INFO("epoll demultiplex wait event, timeout: {}", timeout);
    struct epoll_event events[MAX_EPOLL_ITEM];
 
    while (true)
    {
        int ready = epoll_wait(m_epollFd, events, MAX_EPOLL_ITEM, timeout);
        for (int i = 0; i < ready; ++i)
        {
            SPDLOG_INFO("epoll_wait return events[ {0} ].events: {1}.ready: {2}", i, static_cast<uint32_t>(events[i].events),ready);
            auto eventHandle = reactor->getEventHandle(events[i].data.fd);
            if (eventHandle == nullptr)
                continue;

            auto fd = eventHandle->GetFd();
            if (m_listen_sock_fd == events[i].data.fd)
            {
                SPDLOG_INFO("events[ {} ].events & EPOLLIN for fd: {},do accept", i, fd);
                eventHandle->handAccept(reinterpret_cast<void*>(reactor));
            }
            else if (events[i].events & (EPOLLRDHUP | EPOLLHUP | EPOLLERR))
            {
                SPDLOG_INFO("events[ {} ].events & EPOLLRDHUP/EPOLLHUP/EPOLLERR for fd: {},do close", i, fd);
                eventHandle->handClose(reinterpret_cast<void*>(reactor));
            }
            else if(events[i].events & EPOLLIN)
            {
                SPDLOG_INFO("events[ {} ].events & EPOLLIN for fd: {},do recv", i, fd);
                eventHandle->handRecv(reinterpret_cast<void*>(reactor));
            }
            else if(events[i].events & EPOLLOUT)
            {
                SPDLOG_INFO("events[ {} ].events & EPOLLOUT for fd: {}", i, fd);
                eventHandle->handWrite(reinterpret_cast<void*>(reactor));
            }
            else
            {
                SPDLOG_WARN("events[ {} ].events & other for fd: {},do nothing", i, fd);
            }
        }
    }
}