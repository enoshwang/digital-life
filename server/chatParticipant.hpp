#ifndef CHAT_PARTICIPANT_HPP
#define CHAT_PARTICIPANT_HPP

#include <memory>

class chat_participant
{
public:
  virtual ~chat_participant() {}
  virtual void deliver(const std::string& msg) = 0;
};

typedef std::shared_ptr<chat_participant> chat_participant_ptr;

#endif //CHAT_PARTICIPANT_HPP