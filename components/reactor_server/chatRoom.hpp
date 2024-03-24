#ifndef CHAT_ROOM_HPP
#define CHAT_ROOM_HPP

#include <set>
#include <deque>

#include "chatParticipant.hpp"

class chat_room
{
public:
  void join(chat_participant_ptr participant);
  void leave(chat_participant_ptr participant);
  void deliver(const std::string& msg);

private:
  std::set<chat_participant_ptr> m_participants;
  enum { max_recent_msgs = 100 };
  std::deque<std::string> m_recent_msgs;
};



#endif // CHAT_ROOM_HPP