#include "eventHandle.hpp"

#include <array>
#include <string>

#include "common.hpp"
#include "reactor.hpp"
#include "util.hpp"
#include "wdcommon.hpp"
#include "onlineChatRoom.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

using namespace std::string_literals;


wd::common::EWQueue<OnlineChatRoomMessage> g_ocr_msg_queue;

static inline const size_t MAX_BUF_LEN = 1024;


EventHandle::EventHandle(int _fd) : m_fd(_fd)
{
    SPDLOG_INFO("EventHandle fd: {}", m_fd);
}

EventHandle::~EventHandle()
{
    SPDLOG_INFO("~EventHandle fd: {}", m_fd);
}

int EventHandle::GetFd() const noexcept
{
    return m_fd;
}

int EventHandle::GetStatus() const noexcept
{
    return m_status;
}

void EventHandle::SetStatus(int status) noexcept
{
    m_status = status;
    SPDLOG_INFO("EventHandle fd: {}, set status: {}", m_fd, m_status);
}

void EventHandle::handAccept(void *arg) noexcept
{
    SPDLOG_INFO("event handle accept");
    auto reactor = reinterpret_cast<Reactor *>(arg);

    struct sockaddr_in sock_client;
    socklen_t len = sizeof(sock_client);
    int clientfd  = accept(this->m_fd, (struct sockaddr *)&sock_client, &len);
    if (-1 == clientfd)
    {
        SPDLOG_WARN("accept failed.");
        return;
    }
    else
    {
        SPDLOG_INFO("accept success,client addr: {0}", wd::util::getSockAddrInfo(sock_client));
    }

    do
    {
        int flag = fcntl(clientfd, F_SETFL, O_NONBLOCK);
        if (flag < 0)
        {
            SPDLOG_WARN("set non block failed.");
            break;
        }

        auto evHanlde           = std::make_shared<EventHandle>(clientfd);
        evHanlde->m_sock_client = std::move(sock_client);
        reactor->registerEventHandle(evHanlde, Event_Recv);
    } while (0);
}

void EventHandle::handWrite(void *arg,std::vector<char> msg) noexcept
{
    SPDLOG_INFO("event handle write");

    auto reactor = reinterpret_cast<Reactor *>(arg);
    if (reactor->m_server_config.type == wd::common::ServerType::ECHO_SERVER)
    {
        std::string response{"hello world"s};
        auto retWrite = write(m_fd, response.c_str(), response.length());
        SPDLOG_INFO("write: {0} to fd {1},length:{2}", response, m_fd, retWrite);

        reactor->modifyEventHandle(shared_from_this(), static_cast<int>(EpollEventType::EpollEvent_IN));
    }
    else if(reactor->m_server_config.type == wd::common::ServerType::ONLINE_CHAT_ROOM)
    {
        ssize_t total_send{0};
        while (total_send < msg.size())
        {
            auto bytes_sent = write(m_fd, msg.data() + total_send, msg.size() - total_send);
            if(bytes_sent == -1)
            {
                if(errno == EAGAIN || errno == EWOULDBLOCK)
                {
                    SPDLOG_WARN("Send buffer is full, try again.fd:{0}", m_fd);
                    continue;
                }
                else
                {
                    SPDLOG_ERROR("send error,errno:{0},fd:{1}", errno,m_fd);
                    this->handClose(arg);
                    return;
                }
            }
            else if(bytes_sent == 0)
            {
                SPDLOG_WARN("peer has closed connection,fd:{0}", m_fd);
                this->handClose(arg);
                return;
            }
            else
            {
                total_send += bytes_sent;
            }
        }
        std::string str(msg.begin(), msg.end());
        SPDLOG_INFO("write data to fd {0},length:{1}", m_fd, msg.size());
    }
}

void EventHandle::handRecv(void *arg) noexcept
{
    SPDLOG_INFO("event handle recv");

    std::array<char, MAX_BUF_LEN> buff{0};
    std::vector<char> msg;
    msg.reserve(MAX_BUF_LEN);

    while (true)
    {
        ssize_t bytes_received{0};
        buff.fill(0);
        bytes_received = recv(m_fd, buff.data(), MAX_BUF_LEN, 0);
        if (bytes_received == -1)
        {
            if(errno == EAGAIN || errno == EWOULDBLOCK)
            {
                SPDLOG_INFO("all data has been received,from fd:{0}", m_fd);
                break;
            }
            else
            {
                SPDLOG_ERROR("recv error,errno:{0},fd:{1}", errno,m_fd);
                this->handClose(arg);
                return;
            }
        }
        else if(bytes_received == 0)
        {
            SPDLOG_WARN("peer has closed connection,fd:{0}", m_fd);
            this->handClose(arg);
            return;
        }
        else
        {
            SPDLOG_INFO("recv buff,len:{0},from fd:{1}", bytes_received, m_fd);
            msg.insert(msg.end(), buff.begin(), buff.begin() + bytes_received);
        }
    }

    auto reactor = reinterpret_cast<Reactor *>(arg);
    if (reactor->m_server_config.type == wd::common::ServerType::ECHO_SERVER)
    {
        bool echoHelloWorld = false;
        if (echoHelloWorld)
        {
            reactor->modifyEventHandle(shared_from_this(), static_cast<int>(EpollEventType::EpollEvent_OUT));
        }
        else
        {
            auto retWrite = write(m_fd, msg.data(), msg.size());
            SPDLOG_INFO("write {0} bytes to client for echo", retWrite);
        }
    }
    else if (reactor->m_server_config.type == wd::common::ServerType::ONLINE_CHAT_ROOM)
    {
        OnlineChatRoomMessage ocr_msg;
        ocr_msg.m_fd = m_fd;
        ocr_msg.m_addr = wd::util::getSockAddrInfo(m_sock_client);
        ocr_msg.m_message = std::move(msg);
        
        g_ocr_msg_queue.push(ocr_msg);
        SPDLOG_INFO("push ocr msg to queue,fd:{0},addr:{1},msg size:{2}", ocr_msg.m_fd, ocr_msg.m_addr,ocr_msg.m_message.size());
    }
}

void EventHandle::handClose(void *arg) noexcept
{
    SPDLOG_INFO("event handle close.client {0}, fd: {1}", wd::util::getSockAddrInfo(m_sock_client), m_fd);
    auto reactor = reinterpret_cast<Reactor *>(arg);
    reactor->removeEventHandle(shared_from_this());
    close(m_fd);
}
