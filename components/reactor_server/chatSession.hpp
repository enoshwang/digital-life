#ifndef CHAT_SESSION_HPP
#define CHAT_SESSION_HPP

#include "asio/asio.hpp"

#include "chatParticipant.hpp"
#include "chatRoom.hpp"

using asio::ip::tcp;
using asio::awaitable;
using asio::co_spawn;
using asio::detached;
using asio::redirect_error;
using asio::use_awaitable;

class chat_session: public chat_participant,public std::enable_shared_from_this<chat_session>
{
public:
  chat_session(tcp::socket socket, chat_room& room);

  void start();
  void deliver(const std::string& msg);

private:
  awaitable<void> reader();
  awaitable<void> writer();

  void stop();

  tcp::socket m_socket;
  asio::steady_timer m_timer;
  chat_room& m_room;
  std::deque<std::string> m_write_msgs;
};

awaitable<void> listener(tcp::acceptor acceptor);

#endif // CHAT_SESSION_HPP