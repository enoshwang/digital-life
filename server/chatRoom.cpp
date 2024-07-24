#include "chatRoom.hpp"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>


void chat_room::join(chat_participant_ptr participant)
{
    SPDLOG_INFO("chat_room join");
    m_participants.insert(participant);
    for (auto msg: this->m_recent_msgs)
        participant->deliver(msg);
}

void chat_room::leave(chat_participant_ptr participant)
{
    SPDLOG_INFO("chat_room leave");
    m_participants.erase(participant);
}

void chat_room::deliver(const std::string& msg)
{
    SPDLOG_INFO("chat_room deliver");
    m_recent_msgs.push_back(msg);
    while (m_recent_msgs.size() > max_recent_msgs)
        m_recent_msgs.pop_front();

    for (auto participant: m_participants)
        participant->deliver(msg);
}