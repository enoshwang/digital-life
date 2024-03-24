#include "chatSession.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

chat_session::chat_session(tcp::socket socket, chat_room& room):m_socket(std::move(socket)), m_timer(m_socket.get_executor()), m_room(room)
{
    m_timer.expires_at(std::chrono::steady_clock::time_point::max());
}

void chat_session::start()
{
    SPDLOG_INFO("chat_session start");
    m_room.join(shared_from_this());

    co_spawn(m_socket.get_executor(),[self = shared_from_this()]{ return self->reader(); },detached);
    co_spawn(m_socket.get_executor(),[self = shared_from_this()]{ return self->writer(); },detached);
}

void chat_session::deliver(const std::string& msg)
{
    SPDLOG_INFO("chat_session deliver");
    m_write_msgs.push_back(msg);
    m_timer.cancel_one();
}

awaitable<void> chat_session::reader()
{
    SPDLOG_INFO("chat_session reader");
    try
    {
        for (std::string read_msg;;)
        {
            std::size_t n = co_await asio::async_read_until(m_socket,asio::dynamic_buffer(read_msg, 1024), "\n", use_awaitable);
            auto deliver_msg = read_msg.substr(0, n);
            SPDLOG_INFO("read msg,msg size:{0}",deliver_msg.size());
            m_room.deliver(deliver_msg);
            read_msg.erase(0, n);
        }
    }
    catch (std::exception&)
    {
        stop();
    }
}

awaitable<void> chat_session::writer()
{
    SPDLOG_INFO("chat_session writer");
    try
    {
        while (m_socket.is_open())
        {
            if (m_write_msgs.empty())
            {
                asio::error_code ec;
                co_await m_timer.async_wait(redirect_error(use_awaitable, ec));
            }
            else
            {
                co_await asio::async_write(m_socket,asio::buffer(m_write_msgs.front()), use_awaitable);
                m_write_msgs.pop_front();
            }
        }
    }
    catch (std::exception&)
    {
        stop();
    }
}

void chat_session::stop()
{
    SPDLOG_INFO("chat_session stop");
    m_room.leave(shared_from_this());
    m_socket.close();
    m_timer.cancel();
}

awaitable<void> listener(tcp::acceptor acceptor)
{
    SPDLOG_INFO("start listener");
    chat_room room;

    for (;;)
    {
        std::make_shared<chat_session>(co_await acceptor.async_accept(use_awaitable),room)->start();
    }
}