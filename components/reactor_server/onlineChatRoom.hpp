#ifndef WD_ONLINECHATROOM_HPP
#define WD_ONLINECHATROOM_HPP

#include <vector>
#include <string>

struct OnlineChatRoomMessage
{
    std::vector<char> m_message;
    int m_fd;
    std::string m_addr;
};

struct OCRClientInfo
{
    std::string m_username{""};
};


class Reactor;
class OnlineChatRoom
{

public:
    OnlineChatRoom(/* args */);
    ~OnlineChatRoom();

    void run(Reactor* reactor) noexcept;

public:
    void handleTextMsg(const OnlineChatRoomMessage& msg,Reactor* reactor) noexcept;
    void handleFileMsg(const OnlineChatRoomMessage& msg,Reactor* reactor) noexcept;

private:
    std::vector<std::pair<size_t,size_t>> find_range_brackets(const std::vector<char>& msg) const noexcept;
};

#endif  // WD_ONLINECHATROOM_HPP