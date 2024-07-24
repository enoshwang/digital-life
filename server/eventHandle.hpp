#ifndef EVENTHANDLE_HPP
#define EVENTHANDLE_HPP

#include <arpa/inet.h>
#include <fcntl.h>
#include <cstdlib>
#include <sys/socket.h>
#include <unistd.h>
#include <memory>
#include <vector>

enum EventType
{
    Event_Accept = 0X01,
    Event_Recv   = 0X02,
    Event_Send   = 0X04,
};

enum class EpollEventType
{
    EpollEvent_IN  = 0X01,
    EpollEvent_OUT = 0X04
};

class EventHandle final: public std::enable_shared_from_this<EventHandle>
{
public:
    explicit EventHandle(int _fd);
    ~EventHandle();

    EventHandle() = delete;
    EventHandle(const EventHandle &) = delete;
    EventHandle &operator=(const EventHandle &) = delete;
    EventHandle(EventHandle &&) = delete;
    EventHandle &operator=(EventHandle &&) = delete;

    void handAccept(void *arg) noexcept;
    void handWrite(void *arg,std::vector<char> msg = std::vector<char>{}) noexcept;
    void handRecv(void *arg) noexcept;
    void handClose(void *arg) noexcept;

    

    int GetFd() const noexcept;
    int GetStatus() const noexcept;
    void SetStatus(int status) noexcept;

private:
    int m_fd;
    struct sockaddr_in m_sock_client;
    int m_status{0};
};

#endif  // EVENTHANDLE_HPP