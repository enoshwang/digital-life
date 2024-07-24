#ifndef EVENTDEMULTIPLEX_HPP
#define EVENTDEMULTIPLEX_HPP

#include "eventHandle.hpp"

#include <memory>

inline static const int MAX_EPOLL_ITEM {1024};

class Reactor;
class EventDemultiplex
{
public:
    EventDemultiplex() = default;
    virtual ~EventDemultiplex() = default;

    virtual int regist(std::shared_ptr<EventHandle> eventhandle, int event) noexcept = 0;
    virtual void remove(std::shared_ptr<EventHandle> eventhandle) noexcept = 0;
    virtual void modify(std::shared_ptr<EventHandle> eventhandle,int epoll_event) noexcept = 0;
    virtual void wait_event(Reactor *reactor, int timeout = 0) noexcept = 0;
};

class EpollDemultiplex final: public EventDemultiplex
{
public:
    EpollDemultiplex();
    ~EpollDemultiplex();

    int regist(std::shared_ptr<EventHandle> eventhandle, int event) noexcept override;
    void remove(std::shared_ptr<EventHandle> eventhandle) noexcept override;
    void modify(std::shared_ptr<EventHandle> eventhandle,int epoll_event) noexcept override;
    void wait_event(Reactor *reactor, int timeout = 0) noexcept override;

    int getListenFd() const noexcept;
private:
    int m_epollFd;
    int m_listen_sock_fd{0};
};

#endif // EVENTDEMULTIPLEX_HPP